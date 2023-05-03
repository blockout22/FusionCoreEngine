package com.fusion.core.engine.window.input;

import java.util.ArrayList;

public class InputHandler {

    private static ArrayList<KeyListener> keyListeners = new ArrayList<>();

    public static void addKeyListener(KeyListener listener){
        keyListeners.add(listener);
    }
    
    public void update(){
        for (int i = 0; i < keyListeners.size(); i++) {
            
        }
    }
}
