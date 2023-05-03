package com.fusion.core.engine;

import com.fusion.core.engine.plugin.PluginManager;


public class FusionCore extends CoreEngine {

    private boolean isRunning = false;


    public FusionCore() {
        Debug.enable();
        if(init()) {
            update();
        }
        close();
    }

    @Override
    public boolean init() {
        isRunning = true;
        //if no Window is set then close the application
        if(window == null){
            Debug.logError("No Window loaded");
            close();
            return false;
        }else{
            window.init();
        }

        //if no renderer is set then close the application
        if(renderer == null){
            Debug.logError("No Renderer loaded");
            close();
            return false;
        }else{
            renderer.init();
            for (int i = 0; i < getRendererReadyCallbacks().size(); i++) {
                getRendererReadyCallbacks().get(i).onReady();
            }
        }

        return true;
    }

    @Override
    public void update() {
        while(isRunning && !window.isCloseRequested()){
            renderer.update();
            window.update();
            pluginManager.update();
        }
    }

    @Override
    public void close() {
        isRunning = false;
        if(renderer != null){
            renderer.close();
        }
        if(window != null){
            window.close();
        }

        pluginManager.shutdown();
    }
}
