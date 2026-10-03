package org.lwjgl.glfw;
/** Authored input test double, never packaged. */
public final class GLFW {
 public static boolean focused=true;public static int glfwGetWindowAttrib(long window,int attrib){return focused?1:0;}
}
