package viewpointvr.input;

/** One owner updates this with a monotonic game-thread clock. Angles are radians. */
public final class TurnFilter {
    private long last;
    private boolean armed;
    public void reset(){last=0;armed=false;}
    public float update(long now,float x,float y,boolean valid,int mode,int snap,int speed){
        if(!valid||mode==0||!Float.isFinite(x+y)){reset();return 0;}
        double dt=last==0?0:(now-last)*1e-9;last=now;
        if(dt<0||dt>.25){armed=false;return 0;}
        if(Math.hypot(x,y)<.25){armed=true;return 0;}
        if(!armed||Math.abs(x)<=.3||Math.abs(x)<Math.abs(y))return 0;
        if(mode==1){if(Math.abs(x)<.7)return 0;armed=false;return (float)Math.toRadians(Math.copySign(snap,x));}
        return (float)Math.toRadians(Math.copySign(Math.min(1,(Math.abs(x)-.3)/.7)*speed*Math.min(dt,.05),x));
    }
}
