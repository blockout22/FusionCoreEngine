package com.fusion.core.engine.plugin;

import java.util.ArrayList;
import java.util.List;

public abstract class PluginLoadedCallback {

    public List<String> deps = new ArrayList<>();
    public abstract void loaded();
}
