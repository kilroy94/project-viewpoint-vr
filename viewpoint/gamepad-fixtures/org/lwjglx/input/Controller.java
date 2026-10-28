package org.lwjglx.input;
public final class Controller {
 private final String joystickName,gamepadName,guid; private final int buttonsCount,axisCount,hatCount,id; private final boolean isGamepad; private final float[] deadZone;
 public static int physicalConstructions; public GamepadState gamepadState;
 public Controller(int value) { physicalConstructions++; id=value; joystickName="physical";gamepadName="physical";guid="physical";buttonsCount=15;axisCount=6;hatCount=1;isGamepad=true;deadZone=new float[6]; }
 public int getID(){return id;} public String getGUID(){return guid;} public String getGamepadName(){return gamepadName;} public boolean isGamepad(){return isGamepad;}
 public int getAxisCount(){return axisCount;} public int getButtonCount(){return buttonsCount;} public int getHatCount(){return hatCount;} public float getDeadZone(int i){return deadZone[i];}
 public float getAxisValue(int i){return gamepadState==null||!gamepadState.polled?0:gamepadState.axesButtons.axes(i);} public boolean isButtonPressed(int i){return gamepadState!=null&&gamepadState.polled&&gamepadState.axesButtons.buttons(i)==1;}
}