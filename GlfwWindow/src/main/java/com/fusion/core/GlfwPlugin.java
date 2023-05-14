package com.fusion.core;

import com.fusion.core.engine.CoreEngine;
import com.fusion.core.engine.Debug;
import com.fusion.core.engine.plugin.Plugin;
import com.fusion.core.engine.plugin.UnmodifiableString;

import java.util.Collections;
import java.util.List;

public class GlfwPlugin extends Plugin {

    private GlfwWindow window;

    @Override
    public void setId() {
        id.set("GLFW");
//        return UnmodifiableString.fromString("GLFW");
    }

    @Override
    public void init(CoreEngine coreEngine) {
        Debug.log(Debug.Type.INFO, "Glfw  Window Plugin");
        window = new GlfwWindow();
        coreEngine.setWindow(window);
    }

    @Override
    public void update() {
    }

    @Override
    public void shutdown() {

    }

    @Override
    public List<String> getDependencies() {
        return null;
    }
}
