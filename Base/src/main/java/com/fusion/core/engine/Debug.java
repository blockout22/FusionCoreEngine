package com.fusion.core.engine;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.remotery.Remotery;
import org.lwjgl.util.remotery.RemoteryGL;

import static org.lwjgl.opengl.GL11.GL_NO_ERROR;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Debug {

    private static boolean isEnabled = false;

    public static String WARNING = "[WARNING]";
    public static String ERROR = "[ERROR]";
    public static String INFO = "[INFO]";

    private static long rmt;

    public enum Type{
        WARNING, ERROR, INFO
    }

    public static void enable(){
        isEnabled = true;
    }

    public void disable(){
        isEnabled = false;
    }

    public static void logWarn(String value){
        log(Type.WARNING, value);
    }

    public static void logWarn(Object x){
        log(Type.WARNING, x);
    }

    public static void logError(String value){
        log(Type.ERROR, value);
    }

    public static void logError(Object x){
        log(Type.ERROR, x);
    }

    public static void logInfo(String value){
        log(Type.INFO, value);
    }

    public static void logInfo(Object x){
        log(Type.INFO, x);
    }

    public static void log(Type type, String value){
        StackTraceElement[] stackTraceElements = new Exception().getStackTrace();

        StackTraceElement callerElement = null;
        String currentClassName = Debug.class.getName();
        for (StackTraceElement element : stackTraceElements) {
            if (!element.getClassName().equals(currentClassName)) {
                callerElement = element;
                break;
            }
        }

        System.out.println("[" + callerElement.getClassName() + ":" + callerElement.getLineNumber() + "][" + type.name() + "] " + value);
    }

    public static void startProfiling(){
        if(rmt == NULL){
            try(MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer pRmt = stack.mallocPointer(1);
                int result = Remotery.rmt_CreateGlobalInstance(pRmt);
                if(result != 0){
                    throw new RuntimeException("Failed to create Remotery instance");
                }else{
                    Debug.logWarn("Remotery Results: " + result);
                }

                rmt = pRmt.get(0);
                Debug.logWarn(rmt);
            }
        }

        int glError = glGetError();
        if (glError != GL_NO_ERROR) {
            System.err.println("OpenGL error before RemoteryGL call: " + glError);
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            RemoteryGL.rmt_BeginOpenGLSample(stack.UTF8("Fusion Core"), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void endProfiling(){
        RemoteryGL.rmt_EndOpenGLSample();

        if (rmt != 0) {
            Remotery.rmt_DestroyGlobalInstance(rmt);
            rmt = 0;
        }
    }

    public static void log(Type type, Object x){
        StackTraceElement[] stackTraceElements = new Exception().getStackTrace();

        StackTraceElement callerElement = null;
        String currentClassName = Debug.class.getName();
        for (StackTraceElement element : stackTraceElements) {
            if (!element.getClassName().equals(currentClassName)) {
                callerElement = element;
                break;
            }
        }

        System.out.println("[" + callerElement.getClassName() + ":" + callerElement.getLineNumber() + "][" + type.name() + "] " + x);
    }
}
