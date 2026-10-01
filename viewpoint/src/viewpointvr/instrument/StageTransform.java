package viewpointvr.instrument;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.jar.*;
import net.bytebuddy.jar.asm.*;
import static net.bytebuddy.jar.asm.Opcodes.*;

/** Schema-preserving split of the pinned renderer's native calls, without copied implementations. */
public final class StageTransform {
    public static final List<String> TARGETS = List.of("viewpoint/render/WorldRenderer", "viewpoint/render/FarPass", "viewpoint/render/TemporalPass");
    private static final String R="viewpoint/render/";
    private static final String PIN=viewpointvr.diagnostic.BinaryPins.VIEWPOINT;
    private static final Handle BOOT = new Handle(H_INVOKESTATIC,"viewpointvr/StageHooks","bootstrap",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodHandle;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/invoke/CallSite;",false);
    private final Map<String,byte[]> expected;
    private StageTransform(Map<String,byte[]> expected) { this.expected=expected; }
    public static StageTransform fromPinnedJar(Path path) throws Exception {
        byte[] bytes=Files.readAllBytes(path);
        if (!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).equals(PIN))
            throw new IllegalArgumentException("Unsupported Viewpoint JAR");
        Map<String,byte[]> classes=new HashMap<>();
        try(var jar=new JarInputStream(new ByteArrayInputStream(bytes))) {
            for(var e=jar.getNextJarEntry();e!=null;e=jar.getNextJarEntry()) {
                String name=e.getName().replaceFirst("\\.class$","");
                if(TARGETS.contains(name)) classes.put(name,CanonicalClass.encode(jar.readAllBytes()));
            }
        }
        if(classes.size()!=TARGETS.size()) throw new IOException("Missing native stage classes");
        return new StageTransform(classes);
    }
    public byte[] transform(String name,byte[] bytes) {
        if(!expected.containsKey(name) || !Arrays.equals(expected.get(name),CanonicalClass.encode(bytes)))
            throw new IllegalArgumentException("Unsupported native stage executable: "+name);
        return patch(name,bytes);
    }
    static byte[] patch(String name,byte[] bytes) {
        ClassReader reader=new ClassReader(bytes);
        if(!name.equals(reader.getClassName()) || !TARGETS.contains(name)) throw new IllegalArgumentException("Wrong native stage class");
        ClassWriter writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);
        Map<String,Integer> hits=new TreeMap<>();
        reader.accept(new ClassVisitor(ASM9,writer) {
            @Override public MethodVisitor visitMethod(int access,String method,String descriptor,String signature,String[] exceptions) {
                return new MethodVisitor(ASM9,super.visitMethod(access,method,descriptor,signature,exceptions)) {
                    void wrap(int op,String owner,String member,String desc,boolean itf,String policy,Handle alternate) {
                        Handle original=new Handle(op==INVOKESTATIC?H_INVOKESTATIC:op==INVOKEINTERFACE?H_INVOKEINTERFACE:H_INVOKEVIRTUAL,owner,member,desc,itf);
                        String invocation=op==INVOKESTATIC?desc:"(L"+owner+";"+desc.substring(1);
                        String key=method+":"+owner+"."+member;
                        hits.merge(key,1,Integer::sum);
                        super.visitInvokeDynamicInsn("stage",invocation,BOOT,original,alternate==null?original:alternate,policy,key);
                    }
                    @Override public void visitMethodInsn(int op,String owner,String m,String desc,boolean itf) {
                        String policy=null; Handle alternate=null;
                        if(name.equals(R+"WorldRenderer")) {
                            if(method.equals("begin")) {
                                if((m.equals("developer") && owner.equals(name) || owner.equals(R+"PackLinks") && m.equals("follow"))
                                    || owner.equals(R+"FrameStream") && m.equals("frameStarted")
                                    || owner.equals(R+"FloorBakes") && m.equals("update")
                                    || owner.equals(R+"ShellGround") && m.equals("update")
                                    || owner.equals(R+"PackModels") && m.equals("upload")
                                    || owner.equals(R+"Meshes") && m.equals("prepare")) policy="once";
                                if(owner.equals(R+"ModelPass") && m.equals("prepare")) {
                                    policy="model"; alternate=new Handle(H_INVOKEVIRTUAL,owner,"endFrame","()V",false);
                                }
                                if(owner.equals(R+"FrameUniforms") && m.equals("set")) {
                                    policy="uniforms"; alternate=new Handle(H_INVOKESTATIC,owner,"bind","(L"+R+"Targets;)V",false);
                                }
                                if(owner.equals(R+"TemporalPass") && m.equals("begin")) policy="temporal";
                                if(owner.equals(name) && m.equals("drawWorld")) policy="world";
                                if(owner.equals(R+"IrisMode") && m.equals("draw")) policy="false";
                            }
                            if(owner.equals(name) && m.equals("runPasses")) policy="pass";
                            if(method.equals("drawWorld") && (owner.equals(R+"ShadowPass") && m.equals("draw")
                                    || owner.equals(R+"WeatherMap") && m.equals("draw")
                                    || owner.equals(R+"MousePick") && m.equals("read"))) policy="once";
                            if(method.equals("finish") || method.equals("passes")) {
                                if(owner.equals(R+"ModelPass") && m.equals("endFrame")) policy="end";
                                if(owner.equals(R+"TemporalPass") && m.equals("remember")) policy="skip";
                                if(owner.equals(R+"PackStages") && m.equals("any")) policy="false";
                            }
                        }
                        if(name.equals(R+"FarPass") && method.equals("prepare")) {
                            if(owner.equals(R+"FarGpu") && (m.equals("uploadShells")||m.equals("uploadCells"))
                                || owner.equals(R+"BandLight") && m.equals("update")
                                || owner.equals(R+"CellLightPages") && m.equals("update")
                                || owner.equals(R+"TreeBaker") && m.equals("bake")) policy="once";
                            if(owner.equals(R+"FarShadow") && m.equals("draw")) policy="once";
                        }
                        if(name.equals(R+"TemporalPass") && method.equals("begin") && owner.equals("java/util/function/LongSupplier") && m.equals("getAsLong")) policy="once";
                        if(policy!=null) wrap(op,owner,m,desc,itf,policy,alternate);
                        else super.visitMethodInsn(op,owner,m,desc,itf);
                    }
                    @Override public void visitFieldInsn(int op,String owner,String member,String desc) {
                        if(name.equals(R+"WorldRenderer") && method.equals("begin") && op==PUTFIELD && owner.equals(R+"FrameContext") && member.equals("index") && desc.equals("J")) {
                            String key="begin:index"; hits.merge(key,1,Integer::sum);
                            Handle put=new Handle(H_PUTFIELD,owner,member,desc,false);
                            super.visitInvokeDynamicInsn("stage","(L"+owner+";J)V",BOOT,put,put,"once",key);
                        } else super.visitFieldInsn(op,owner,member,desc);
                    }
                    @Override public void visitInsn(int op) {
                        if(name.equals(R+"WorldRenderer") && method.equals("off") && descriptor.equals("(I)Z") && op==IRETURN) {
                            hits.merge("off:return",1,Integer::sum);
                            super.visitVarInsn(ILOAD,0);
                            super.visitMethodInsn(INVOKESTATIC,"viewpointvr/StageHooks","off","(ZI)Z",false);
                        }
                        super.visitInsn(op);
                    }
                };
            }
        },0);
        // Exact class pin plus anchor counts: a missing native stage must never silently become a partial split.
        int expected=name.equals(R+"WorldRenderer")?22:name.equals(R+"FarPass")?6:1;
        if(hits.size()!=expected || hits.entrySet().stream().anyMatch(e->e.getValue()!=(e.getKey().equals("passes:"+R+"WorldRenderer.runPasses")?2:1)))
            throw new IllegalArgumentException("Native stage anchors changed: "+hits);
        return writer.toByteArray();
    }
}
