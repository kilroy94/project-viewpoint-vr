package viewpointvr.rig;

import java.nio.FloatBuffer;
import java.util.*;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Pure retargeter. Matrices here are shader/column-vector matrices, not vanilla row-vector storage. */
public final class ArmRig {
    private final String[] names;
    private final int[] parents;
    private final Matrix4f[] offsets,inverseOffsets;
    private final int[][] chain=new int[2][3];
    private final Quaternionf[] wristCorrection=new Quaternionf[2];
    private final Vector3f[] palmLocal=new Vector3f[2];
    public static final class PoseChanges {
        private final Map<String,Matrix4f> deltas=new HashMap<>();
        public Matrix4f delta(String bone) { Matrix4f d=deltas.get(normalize(bone)); return d==null?null:new Matrix4f(d); }
    }
    private static String normalize(String name) { return name==null?"":name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]",""); }
    public ArmRig(String[] names,int[] parents,Matrix4f[] offsets) {
        int n=names.length;
        if(n==0||n>60||parents.length!=n||offsets.length!=n) throw new IllegalArgumentException("Unsupported palette size");
        this.names=names.clone(); this.parents=parents.clone(); this.offsets=new Matrix4f[n]; inverseOffsets=new Matrix4f[n];
        for(int i=0;i<n;i++) {
            if(offsets[i]==null||!offsets[i].isFinite()||Math.abs(offsets[i].determinant())<1e-8f) throw new IllegalArgumentException("Invalid bind offset");
            this.offsets[i]=new Matrix4f(offsets[i]); inverseOffsets[i]=new Matrix4f(offsets[i]).invert();
            int ancestor=i;
            for(int depth=0;ancestor>=0;depth++) {
                if(ancestor>=n||depth>=n) throw new IllegalArgumentException("Invalid skeleton hierarchy");
                ancestor=parents[ancestor];
            }
        }
        for(int side=0;side<2;side++) {
            String prefix=side==0?"l":"r";
            for(int j=0;j<3;j++) {
                String suffix=prefix+new String[]{"upperarm","forearm","hand"}[j];
                chain[side][j]=-1;
                for(int i=0;i<n;i++) if(names[i]!=null && names[i].toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","").endsWith(suffix)) chain[side][j]=i;
                if(chain[side][j]<0) throw new IllegalArgumentException("Missing arm bone "+suffix);
            }
            if(!descendant(chain[side][1],chain[side][0])||!descendant(chain[side][2],chain[side][1])) throw new IllegalArgumentException("Invalid arm chain");
        }
    }
    public void recenter() { Arrays.fill(wristCorrection,null); Arrays.fill(palmLocal,null); }
    public Vector3f palmLocal(int side) { return new Vector3f(Objects.requireNonNull(palmLocal[side])); }
    private Vector3f palm(Matrix4f[] model,int side) {
        int hand=chain[side][2],lower=chain[side][1];
        Vector3f wrist=model[hand].getTranslation(new Vector3f()),knuckles=new Vector3f(); int count=0;
        // Direct, non-thumb finger bases are stable under finger curling.
        for(int i=0;i<names.length;i++) if(parents[i]==hand && normalize(names[i]).matches(".*[lr]finger[1-4]")) {
            knuckles.add(model[i].getTranslation(new Vector3f())); count++;
        }
        Vector3f forearm=new Vector3f(wrist).sub(model[lower].getTranslation(new Vector3f()));
        Vector3f offset=count==0?new Vector3f(forearm).mul(.15f):knuckles.div(count).sub(wrist).mul(.5f);
        if(offset.length()<1e-5f||offset.length()>forearm.length()*.5f) offset.set(forearm).mul(.15f);
        return new Matrix4f(model[hand]).invert().transformPosition(offset.add(wrist));
    }
    public boolean descendant(int bone,int ancestor) {
        for(int p=bone;p>=0;p=parents[p]) if(p==ancestor) return true;
        return false;
    }
    public float[] mask(boolean left,boolean right,float[] original) {
        float[] result=original==null?new float[60]:Arrays.copyOf(original,60);
        for(int i=0;i<names.length;i++) for(int side=0;side<2;side++)
            if((side==0?left:right)&&descendant(i,chain[side][0])) result[i]=1;
        return result;
    }
    /** Source remains untouched. Buffer positions/limits are preserved; output uses vanilla upload layout. */
    public PoseChanges pose(FloatBuffer source,Matrix4f[] targets,Vector3f[] poles,FloatBuffer output) {
        return pose(source,targets,poles,output,new Vector3f());
    }
    public PoseChanges pose(FloatBuffer source,Matrix4f[] targets,Vector3f[] poles,FloatBuffer output,Vector3f shoulderShift) {
        return pose(source,targets,poles,output,shoulderShift,1);
    }
    public PoseChanges pose(FloatBuffer source,Matrix4f[] targets,Vector3f[] poles,FloatBuffer output,Vector3f shoulderShift,float maxReach) {
        if(!Float.isFinite(shoulderShift.x)||!Float.isFinite(shoulderShift.y)||!Float.isFinite(shoulderShift.z))
            throw new IllegalArgumentException("Invalid shoulder displacement");
        PoseChanges changes=new PoseChanges(); boolean[] changed=new boolean[names.length];
        int n=names.length;
        if(source.remaining()!=n*16 || output.capacity()<n*16) throw new IllegalArgumentException("Palette size mismatch");
        // The game's older JOML uses Unsafe for buffer access, even for heap buffers.
        if(!source.isDirect()||!output.isDirect()||output.isReadOnly()) throw new IllegalArgumentException("Direct palette buffers required");
        Matrix4f[] model=new Matrix4f[n],posed=new Matrix4f[n];
        for(int i=0;i<n;i++) {
            FloatBuffer in=source.duplicate(); in.position(source.position()+i*16);
            // getSkinTransforms stores offset * model; upload transposes it.
            model[i]=new Matrix4f(in).transpose().mul(inverseOffsets[i]);
            if(!model[i].isFinite()) throw new IllegalArgumentException("Invalid captured pose");
            posed[i]=new Matrix4f(model[i]);
        }
        for(int side=0;side<2;side++) {
            // Missing input restores the native pose for this frame, not the calibration.
            // Dashboard/focus loss can last many frames and the controller may move meanwhile.
            Matrix4f target=targets[side]; if(target==null) continue;
            if(!target.isFinite()||target.determinant()<=0) throw new IllegalArgumentException("Invalid hand target basis");
            int upper=chain[side][0],lower=chain[side][1],hand=chain[side][2];
            Vector3f shoulder=model[upper].getTranslation(new Vector3f()),elbow=model[lower].getTranslation(new Vector3f()),wrist=model[hand].getTranslation(new Vector3f());
            shoulder.add(shoulderShift); elbow.add(shoulderShift); wrist.add(shoulderShift);
            Quaternionf orientation=target.getUnnormalizedRotation(new Quaternionf()).normalize();
            if(wristCorrection[side]==null) wristCorrection[side]=new Quaternionf(orientation).invert().mul(model[hand].getUnnormalizedRotation(new Quaternionf()).normalize());
            if(palmLocal[side]==null) palmLocal[side]=palm(model,side);
            Vector3f scale=model[hand].getScale(new Vector3f());
            Matrix4f h=new Matrix4f().rotate(orientation.mul(wristCorrection[side])).scale(scale);
            Vector3f palmOffset=h.transformDirection(new Vector3f(palmLocal[side]));
            Vector3f wristTarget=target.getTranslation(new Vector3f()).sub(palmOffset);
            ArmIk.Solution solution=ArmIk.solve(shoulder,elbow,wrist,wristTarget,poles[side],maxReach);
            Matrix4f u=ArmIk.extended(model[upper],new Vector3f(elbow).sub(shoulder),new Vector3f(solution.elbow()).sub(shoulder),shoulder);
            Matrix4f l=ArmIk.extended(model[lower],new Vector3f(wrist).sub(elbow),new Vector3f(solution.wrist()).sub(solution.elbow()),solution.elbow());
            h.setTranslation(solution.wrist());
            Matrix4f du=new Matrix4f(u).mul(new Matrix4f(model[upper]).invert());
            Matrix4f dl=new Matrix4f(l).mul(new Matrix4f(model[lower]).invert());
            Matrix4f dh=new Matrix4f(h).mul(new Matrix4f(model[hand]).invert());
            for(int i=0;i<n;i++) {
                boolean prop=normalize(names[i]).equals(side==0?"bip01prop2":"bip01prop1");
                Matrix4f delta=prop||descendant(i,hand)?dh:descendant(i,lower)?dl:descendant(i,upper)?du:null;
                if(delta!=null) { posed[i].set(delta).mul(model[i]); changed[i]=true; changes.deltas.put(normalize(names[i]),new Matrix4f(delta)); }
            }
            changes.deltas.put(side==0?"bip01prop2":"bip01prop1",new Matrix4f(dh));
        }
        output.clear();
        for(int i=0;i<n;i++) {
            if(!changed[i]) {
                FloatBuffer in=source.duplicate(); in.position(source.position()+i*16).limit(source.position()+(i+1)*16);
                output.put(in); continue;
            }
            Matrix4f result=posed[i].mul(offsets[i]).transpose();
            if(!result.isFinite()) throw new IllegalArgumentException("Nonfinite IK result");
            result.get(output); output.position(output.position()+16);
        }
        output.flip();
        return changes;
    }
}
