package viewpointvr.instrument;

import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import net.bytebuddy.jar.asm.*;
import static net.bytebuddy.jar.asm.Opcodes.*;
import viewpointvr.diagnostic.BinaryPins;

/** Exact pinned call-site adapters; no schema change and no UI/event replay. */
public final class UiTransform {
    public static final List<String> TARGETS=List.of("viewpoint/Hooks","viewpoint/platform/ImGuiFrame");
    private final Map<String,byte[]> expected=new HashMap<>();
    public UiTransform(Path jar) throws Exception {
        BinaryPins.check(jar,Set.of(BinaryPins.VIEWPOINT));
        try(var input=new JarFile(jar.toFile())){for(String name:TARGETS)try(var stream=input.getInputStream(input.getJarEntry(name+".class"))){expected.put(name,CanonicalClass.encode(stream.readAllBytes()));}}
    }
    public byte[] transform(String name,byte[] source){
        if(!Arrays.equals(expected.get(name),CanonicalClass.encode(source)))throw new IllegalArgumentException("UI target differs: "+name);
        var reader=new ClassReader(source);var writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);int[] hits={0};
        reader.accept(new ClassVisitor(ASM9,writer){
            public MethodVisitor visitMethod(int access,String method,String descriptor,String signature,String[] exceptions){
                return new MethodVisitor(ASM9,super.visitMethod(access,method,descriptor,signature,exceptions)){
                    public void visitMethodInsn(int op,String owner,String called,String desc,boolean itf){
                        if(name.equals(TARGETS.get(0))&&method.equals("mouseUpdated")&&op==INVOKESTATIC&&owner.equals("viewpoint/input/Controls")&&called.equals("pinMouse")&&desc.equals("()V")){
                            super.visitLdcInsn(new Handle(H_INVOKESTATIC,owner,called,desc,false));
                            super.visitMethodInsn(INVOKESTATIC,"viewpointvr/input/InputBridge","mouse","(Ljava/lang/invoke/MethodHandle;)V",false);hits[0]++;return;
                        }
                        if(name.equals(TARGETS.get(0))&&method.equals("inputMoveVector")&&op==INVOKESTATIC&&owner.equals("viewpoint/input/Controls")&&called.equals("moveVector")&&desc.equals("(Lzombie/characters/IsoPlayer;Lzombie/iso/Vector2;)V")){
                            super.visitLdcInsn(new Handle(H_INVOKESTATIC,owner,called,desc,false));
                            super.visitMethodInsn(INVOKESTATIC,"viewpointvr/input/InputBridge","move","(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/invoke/MethodHandle;)V",false);hits[0]++;return;
                        }
                        if(name.equals(TARGETS.get(1))&&method.equals("draw")&&op==INVOKESTATIC&&owner.equals(name)&&called.equals("input")&&desc.equals("()V")){
                            super.visitLdcInsn(new Handle(H_INVOKESTATIC,owner,called,desc,false));
                            super.visitMethodInsn(INVOKESTATIC,"viewpointvr/input/InputBridge","imgui","(Ljava/lang/invoke/MethodHandle;)V",false);hits[0]++;return;
                        }
                        if(name.equals(TARGETS.get(0))&&op==INVOKESTATIC&&desc.equals("()V")){
                            if(method.equals("uiFrameEnding")&&called.equals("draw")&&(owner.equals("viewpoint/interact/LootPanel")||owner.equals("viewpoint/platform/PerformanceOverlay"))){
                                super.visitLdcInsn(new Handle(H_INVOKESTATIC,owner,called,desc,false));
                                super.visitInsn(owner.endsWith("/LootPanel")?ICONST_1:ICONST_2);
                                super.visitMethodInsn(INVOKESTATIC,"viewpointvr/UiBridge","late","(Ljava/lang/invoke/MethodHandle;I)V",false);hits[0]++;return;
                            }
                            if(method.equals("frameSwapping")&&owner.equals("viewpoint/platform/SettingsWindow")&&called.equals("render")){
                                super.visitLdcInsn(new Handle(H_INVOKESTATIC,owner,called,desc,false));
                                super.visitMethodInsn(INVOKESTATIC,"viewpointvr/UiBridge","present","(Ljava/lang/invoke/MethodHandle;)V",false);hits[0]++;return;
                            }
                        }
                        if(name.equals(TARGETS.get(1))&&method.equals("draw")&&op==INVOKEVIRTUAL&&owner.equals("imgui/gl3/ImGuiImplGl3")&&called.equals("renderDrawData")&&desc.equals("(Limgui/ImDrawData;)V")){
                            super.visitLdcInsn(new Handle(H_INVOKEVIRTUAL,owner,called,desc,false));
                            super.visitMethodInsn(INVOKESTATIC,"viewpointvr/UiBridge","settings","(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/invoke/MethodHandle;)V",false);hits[0]++;return;
                        }
                        super.visitMethodInsn(op,owner,called,desc,itf);
                    }
                };
            }
        },0);
        if(hits[0]!=(name.equals(TARGETS.get(0))?5:2))throw new IllegalArgumentException("UI anchor count: "+name+" = "+hits[0]);
        return writer.toByteArray();
    }
}
