package com.fusion.core;

import org.joml.Vector2f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.system.MemoryUtil.NULL;

public class GlfwInput {
    private static Set<Integer> downKeys = new HashSet<>();
    private static Set<Integer> downMouseButtons = new HashSet<>();
    private static long windowID;

    private static ArrayList<Integer> keysAlreadyDown = new ArrayList<>();

    private static Vector2f cursorPos = new Vector2f();
    private static DoubleBuffer xpos = BufferUtils.createDoubleBuffer(1);
    private static DoubleBuffer ypos = BufferUtils.createDoubleBuffer(1);


    public static void setWindowID(long window) {
        if(windowID == NULL) {
            windowID = window;
        }
    }

    public static long getWindowID(){
        return windowID;
    }

    public static void update() {
        downKeys.removeIf(key -> !isKeyDown(key));
        downMouseButtons.removeIf(button -> !isMouseDown(button));

        if(keysAlreadyDown.size() > 0){
            for (int i = keysAlreadyDown.size() - 1; i >= 0; i--) {
                int keyCode = keysAlreadyDown.get(i);
                if(!isKeyDown(keyCode)){
                    keysAlreadyDown.remove(i);
                }
            }
        }
    }

    public static Vector2f getCursorPos(){
        xpos.rewind();
        ypos.rewind();
        GLFW.glfwGetCursorPos(windowID, xpos, ypos);

        double x = xpos.get();
        double y = ypos.get();

        xpos.clear();
        ypos.clear();

        cursorPos.set((float)x, (float)y);

        return cursorPos;
    }

    public static boolean isKeyPressed(String keyName){
        return isKeyPressed(GlfwKey.getKeyCode(keyName));
    }

    public static boolean isKeyPressed(int keyCode){
        if(isKeyDown(keyCode)){
            for (int i = 0; i < keysAlreadyDown.size(); i++) {
                if(keysAlreadyDown.get(i).equals(keyCode)){
                    return false;
                }
            }
            keysAlreadyDown.add(keyCode);
            return true;
        }
        return false;
    }

    public static boolean isKeyDown(int keyCode) {
        if(windowID == NULL){
            return false;
        }
        return GLFW.glfwGetKey(windowID, keyCode) == GLFW.GLFW_PRESS;
    }

    public static boolean isMouseDown(int buttonCode) {
        return GLFW.glfwGetMouseButton(windowID, buttonCode) == GLFW.GLFW_PRESS;
    }

    public static void KeyEvent(int key, PressedEvent evtPressed, ReleasedEvent evtReleased, HeldEvent evtDown) {
        if (downKeys.add(key)) {
            evtPressed.exec(GlfwKey.getKeyName(key));
        } else if (!isKeyDown(key) && downKeys.remove(key)) {
            evtReleased.exec(GlfwKey.getKeyName(key));
        } else if (isKeyDown(key)) {
            evtDown.exec(GlfwKey.getKeyName(key));
        }
    }

    public static void MouseEvent(int button, PressedEvent evtPressed, ReleasedEvent evtReleased) {
        if (downMouseButtons.add(button)) {
            evtPressed.exec(GlfwKey.getKeyName(button));
        } else if (!isMouseDown(button) && downMouseButtons.remove(button)) {
            evtReleased.exec(GlfwKey.getKeyName(button));
        }
    }

    public interface PressedEvent {
        void exec(String keyName);
    }

    public interface ReleasedEvent {
        void exec(String keyName);
    }

    public interface HeldEvent {
        void exec(String keyName);
    }
}
