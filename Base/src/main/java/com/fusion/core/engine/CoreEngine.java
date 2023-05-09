package com.fusion.core.engine;

import com.fusion.core.engine.plugin.Plugin;
import com.fusion.core.engine.plugin.PluginManager;
import com.fusion.core.engine.renderer.Renderer;
import com.fusion.core.engine.renderer.RendererReady;
import com.fusion.core.engine.window.Window;
import com.fusion.core.engine.window.WindowReady;

import java.util.ArrayList;

public abstract class CoreEngine {

    private ArrayList<RendererReady> rendererReadyCallback = new ArrayList<>();
    private ArrayList<WindowReady> windowReadyCallback = new ArrayList<>();
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
    public void addWindowReadyCallback(WindowReady onWindowReady){
        windowReadyCallback.add(onWindowReady);
    }

    protected ArrayList<RendererReady> getRendererReadyCallbacks(){
        return rendererReadyCallback;
    }

    protected ArrayList<WindowReady> getWindowReadyCallback() {
        return windowReadyCallback;
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

    public Plugin getPluginById(String id){
        return pluginManager.findPlugin(id);
    }

    abstract boolean init();
    abstract void update();
    abstract void close();
}
