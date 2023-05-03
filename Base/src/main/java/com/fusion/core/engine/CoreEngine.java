package com.fusion.core.engine;

import com.fusion.core.engine.plugin.PluginManager;
import com.fusion.core.engine.renderer.Renderer;
import com.fusion.core.engine.renderer.RendererReady;
import com.fusion.core.engine.window.Window;

import java.util.ArrayList;

public abstract class CoreEngine {

    private ArrayList<RendererReady> rendererReadyCallback = new ArrayList<>();
    protected PluginManager pluginManager;
    protected Window window;
    protected Renderer renderer;

    public CoreEngine() {
        pluginManager = new PluginManager(this);
        pluginManager.loadPlugins();
    }

    public void addRendererReadyCallback(RendererReady onRendererReady){
        rendererReadyCallback.add(onRendererReady);
    }

    protected ArrayList<RendererReady> getRendererReadyCallbacks(){
        return rendererReadyCallback;
    }

    public void setWindow(Window window){
        this.window = window;
    }

    public boolean isPluginLoaded(String id){
        return pluginManager.isPluginLoaded(id);
    }

    public void setRenderer(Renderer renderer){
        this.renderer = renderer;
    }

    public Renderer getRenderer()
    {
        return renderer;
    }

    public Window getWindow(){
        return window;
    }

    abstract boolean init();
    abstract void update();
    abstract void close();
}
