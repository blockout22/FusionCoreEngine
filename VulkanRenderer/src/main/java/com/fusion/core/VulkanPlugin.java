package com.fusion.core;

import com.fusion.core.engine.CoreEngine;
import com.fusion.core.engine.Debug;
import com.fusion.core.engine.plugin.Plugin;
import com.fusion.core.engine.plugin.UnmodifiableString;
import com.fusion.core.engine.window.WindowReady;

import java.util.List;

public class VulkanPlugin extends Plugin {

    private VulkanRenderer renderer;

    @Override
    public void setId() {
        id.set("Vulkan");
//        return UnmodifiableString.fromString("Vulkan");
    }

    @Override
    public void init(CoreEngine coreEngine) {
        Debug.logInfo("Vulkan Rendering Plugin init");

        coreEngine.addWindowReadyCallback(() -> {
            renderer = new VulkanRenderer((GlfwWindow) coreEngine.getWindow());
            coreEngine.setRenderer(renderer);

        });
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
