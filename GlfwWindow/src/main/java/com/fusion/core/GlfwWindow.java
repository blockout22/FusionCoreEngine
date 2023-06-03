package com.fusion.core;

import com.fusion.core.engine.Debug;
import com.fusion.core.engine.window.Window;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class GlfwWindow extends Window {

    private long window;

    public GlfwWindow() {
        super(1920, 1080);
    }

    @Override
    public void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if(!glfwInit()){
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        if(!glfwVulkanSupported())
        {
            Debug.logWarn("Vulkan isn't supported");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        //vulkan setup
//        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);


//        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
//        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
//        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
//        glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);

        window = glfwCreateWindow(getWidth(), getHeight(), "Window title", NULL, NULL);
        if(window == NULL){
            throw new RuntimeException("Failed to create GLFW window");
        }

        //Centre window on the screen
        try ( MemoryStack stack = stackPush() ) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        }

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);
        GlfwInput.setWindowID(window);

        glfwSetWindowSizeCallback(window, new GLFWWindowSizeCallback() {
            @Override
            public void invoke(long l, int w, int h) {
                setSize(w, h);
            }
        });
    }

    @Override
    public void update() {
        Time.update();
        GlfwInput.update();
        glfwSwapBuffers(window);
        glfwPollEvents();
    }

    @Override
    public void close() {
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    @Override
    public boolean isCloseRequested() {
        return glfwWindowShouldClose(window);
    }

    @Override
    public double[] getCursorPosition() {
        double[] xpos = new double[1];
        double[] ypos = new double[1];
        glfwGetCursorPos(getWindowID(), xpos, ypos);

        return new double[]{xpos[0], ypos[0]};
    }

    public long getWindowID(){
        return window;
    }
}
