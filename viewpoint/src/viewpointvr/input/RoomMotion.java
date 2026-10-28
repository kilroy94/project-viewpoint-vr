package viewpointvr.input;
import org.joml.Vector2f;
/** Physical displacement in metres -> bounded native world XY displacement. */
public final class RoomMotion {
 public static final float LEAN=.04f,MAX_OFFSET=1.5f,MAX_STEP=.12f;
 public record Step(float x,float y) {public static final Step NONE=new Step(0,0);}
 public static Step request(BodyTracking.Head h,float nativeYaw){
  float length=(float)Math.hypot(h.x(),h.z());if(!Float.isFinite(length)||length<=LEAN||length>MAX_OFFSET)return Step.NONE;
  float amount=Math.min(MAX_STEP,(length-LEAN)*h.scale()),factor=amount/length;
  float a=(float)Math.PI/2-nativeYaw,c=(float)Math.cos(a),s=(float)Math.sin(a);
  return new Step(-(c*h.x()+s*h.z())*factor,-(-s*h.x()+c*h.z())*factor);
 }
 /** Adapter must resolve collision before committing; native stick delta is translated intact. */
 public interface Body {float x();float y();float nextX();float nextY();Vector2f resolve(float x,float y);boolean accepts(float x,float y);void move(float x,float y,float nextX,float nextY);}
 public static Step apply(Body body,Step request){
  if(request.equals(Step.NONE))return Step.NONE;
  float x=body.x(),y=body.y(),nx=body.nextX(),ny=body.nextY();var result=body.resolve(x+request.x(),y+request.y());
  if(result==null||!Float.isFinite(result.x+result.y)||Math.hypot(result.x-x,result.y-y)>MAX_STEP+.001||!body.accepts(result.x,result.y))return Step.NONE;
  float dx=result.x-x,dy=result.y-y;if(Math.hypot(dx,dy)<.00001)return Step.NONE;
  body.move(result.x,result.y,nx+dx,ny+dy);return new Step(dx,dy);
 }
}
