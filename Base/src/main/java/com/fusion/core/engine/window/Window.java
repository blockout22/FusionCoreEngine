package com.fusion.core.engine.window;

import com.fusion.core.engine.interfaces.IWindowResize;

import java.util.ArrayList;

public abstract class Window {

    private ArrayList<IWindowResize> resizeListeners = new ArrayList<>();

    private int width = 1;
    private int height = 1;

    public Window(int width, int height){
        setSize(width, height);
    }

    public abstract void init();
    public abstract void update();
    public abstract void close();

    public abstract boolean isCloseRequested();

    public void addResizeListener(IWindowResize resizeListener){
        resizeListeners.add(resizeListener);
    }

    public void setWidth(int width){
        this.width = width;
        for (int i = 0; i < resizeListeners.size(); i++) {
            resizeListeners.get(i).onChanged(width, height);
        }
    }

    public void setHeight(int height){
        this.height = height;
        for (int i = 0; i < resizeListeners.size(); i++) {
            resizeListeners.get(i).onChanged(width, height);
        }
    }

    public void setSize(int width, int height){
        if(width == 0){
            width = 1;
        }

        if(height == 0){
            height = 1;
        }
        this.width = width;
        this.height = height;
        for (int i = 0; i < resizeListeners.size(); i++) {
            resizeListeners.get(i).onChanged(width, height);
        }
    }

    public abstract double[] getCursorPosition();

//    public void triggerKeyPressed(){
//
//    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
