package org.lwjglx.input;
public class Controllers { public static final Controller[] physical=new Controller[16];public static int polls; public static Controller getController(int i){return physical[i];} public static void poll(GamepadState[] s){polls++;for(int i=0;i<16;i++)if(physical[i]!=null){s[i].axesButtons.axes(0,.75f);s[i].polled=true;}} }
