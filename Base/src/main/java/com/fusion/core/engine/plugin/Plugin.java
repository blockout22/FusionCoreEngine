package com.fusion.core.engine.plugin;

import com.fusion.core.engine.CoreEngine;

import java.util.List;

public abstract class Plugin {

    protected final UnmodifiableString id = new UnmodifiableString();

    public abstract void init(CoreEngine coreEngine);
    public abstract void update();
    public abstract void shutdown();

    /**
     * requires call to id.set([value]) to work as expected
     */
    public abstract void setId();

    public final String getId(){
        return id.get();
    }

    public abstract List<String> getDependencies();
}
