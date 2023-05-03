package com.fusion.core.engine.plugin;

import com.fusion.core.engine.CoreEngine;

import java.util.List;

public interface Plugin {

    UnmodifiableString id = new UnmodifiableString();

    void init(CoreEngine coreEngine);
    void update();
    void shutdown();
    UnmodifiableString setId();

    List<String> getDependencies();
}
