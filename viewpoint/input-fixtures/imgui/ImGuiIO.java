package imgui;
/** Authored input test double, never packaged. */
public final class ImGuiIO {
 public float x,y,wheel,width=1600,height=900;public boolean down;public float getMousePosX(){return x;}public float getMousePosY(){return y;}public float getDisplaySizeX(){return width;}public float getDisplaySizeY(){return height;}public boolean getMouseDown(int button){return down;}public void setMouseDown(int button,boolean value){down=value;}public void setMousePos(float a,float b){x=a;y=b;}public float getMouseWheel(){return wheel;}public void setMouseWheel(float value){wheel=value;}
}
