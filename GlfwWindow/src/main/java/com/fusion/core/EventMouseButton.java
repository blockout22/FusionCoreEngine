package com.fusion.core;

public interface EventMouseButton {

    /**
     * @param button
     * @param action
     * @param mods
     */
    void handle(int button, int action, int mods);
}
