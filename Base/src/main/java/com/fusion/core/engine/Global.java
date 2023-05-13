package com.fusion.core.engine;

import java.io.File;

public class Global {

    private static File assetDir = new File("Assets");

    public static File getAssetDir(){
        return assetDir;
    }
}
