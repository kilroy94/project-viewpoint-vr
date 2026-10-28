package zombie.core.input;
import org.lwjglx.input.*;
/** Original fixture models the native poll/publish/consume boundary and button edges. */
public final class Input {
    public final Controller[] current=new Controller[16],polledControllers=new Controller[16];
    private GamepadState[] active=states(),pending=states();
    private boolean hasPending,created;
    public final boolean[][] held=new boolean[16][15];
    public int connects,disconnects,presses,releases;
    private static GamepadState[] states(){GamepadState[] s=new GamepadState[16];for(int i=0;i<16;i++)s[i]=new GamepadState();return s;}
    public boolean isButtonPressedD(int button,int id){return held[id][button];}
    public Controller getController(int i){return current[i];}
    public void pollFixture(){if(hasPending)return;for(int i=0;i<16;i++)polledControllers[i]=Controllers.getController(i);Controllers.poll(pending);hasPending=true;}
    private void swap(){if(!hasPending)return;var previous=active;active=pending;pending=previous;created=true;hasPending=false;for(int i=0;i<16;i++)if(polledControllers[i]!=null)polledControllers[i].gamepadState=active[i];}
    public void updateGameThread(){
        if(!created){swap();return;}
        for(int i=0;i<16;i++){
            Controller c=polledControllers[i];
            if(current[i]!=c){if(current[i]!=null)disconnects++;if(c!=null)connects++;}current[i]=c;
            if(c==null)continue;
            for(int b=0;b<15;b++){boolean down=c.isButtonPressed(b);if(down&&!held[i][b])presses++;if(!down&&held[i][b])releases++;held[i][b]=down;}
        }
        swap();
    }
}
