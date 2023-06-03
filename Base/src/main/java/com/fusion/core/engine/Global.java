package com.fusion.core.engine;

import com.fusion.core.engine.plugin.Plugin;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class Global {

    private static File assetDir = new File("Assets");

    public static File getAssetDir(){
        return assetDir;
    }

    public static float[] toFloatArray(List<?> list){
        Object elem = list.get(0);
        if(elem instanceof Float) {
            List<Float> floatList = (List<Float>) list;
            float[] arr = new float[floatList.size()];
            for (int i = 0; i < floatList.size(); i++) {
                arr[i] = floatList.get(i);
            }

            return arr;
        }else if(elem instanceof Vector3f){
            List<Vector3f> vectorList = (List<Vector3f>) list;
            float[] arr = new float[vectorList.size() * 3];
            for (int i = 0; i < vectorList.size(); i++) {
                arr[i * 3 + 0] = vectorList.get(i).x;
                arr[i * 3 + 1] = vectorList.get(i).y;
                arr[i * 3 + 2] = vectorList.get(i).z;
            }

            return arr;
        }else{
            throw new IllegalArgumentException("List must contain either Float or Vector3f objects.");
        }
    }

    public static int[] toIntArray(List<Integer> list){
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }

        return arr;
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
