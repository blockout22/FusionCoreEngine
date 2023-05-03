package com.fusion.core;

import static org.lwjgl.glfw.GLFW.glfwGetTime;

public class Time {
    private static double lastTime;
    private static double deltaTime;

    public static void init() {
        lastTime = glfwGetTime();
        deltaTime = 0.0;
    }

    public static void update() {
        double currentTime = glfwGetTime();
        deltaTime = currentTime - lastTime;
        lastTime = currentTime;
    }

    public static double getDeltaTime() {
        return deltaTime;
    }
}
