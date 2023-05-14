package com.fusion.core.engine;

import com.fusion.core.engine.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Global {

    private static File assetDir = new File("Assets");

    public static File getAssetDir(){
        return assetDir;
    }

    public static void extractAsset(Plugin plugin, String assetPath){
        //move asset to assetDir

        InputStream in = plugin.getClass().getResourceAsStream("/" + assetPath);

        if(in == null){
            System.out.println("NULL ASSET PATH");
        }else{
            System.out.println("SUCESSFULLY Found the Asset Path!!!");
            Path dest = new File(assetDir.getAbsolutePath() + File.separator + assetPath).toPath();
            try {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
