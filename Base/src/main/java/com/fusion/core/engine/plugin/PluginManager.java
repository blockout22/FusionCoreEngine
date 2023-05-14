package com.fusion.core.engine.plugin;

import com.fusion.core.engine.CoreEngine;
import com.fusion.core.engine.Debug;
import com.fusion.core.engine.Global;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class PluginManager {

    private CoreEngine core;
    private Map<String, Plugin> plugins = new HashMap();

    private List<String> assetExtensions = Arrays.asList(".glsl", ".png", ".jpg", ".fbx", ".ttf");

    public PluginManager(CoreEngine core) {
        this.core = core;
    }

    public void update(){
        for (Plugin plugin : plugins.values()){
            plugin.update();
        }
    }

    public void shutdown(){
        for(Plugin plugin : plugins.values()){
            plugin.shutdown();
        }
    }

    public boolean isPluginLoaded(String id)
    {
        Plugin plugin = plugins.get(id);

        if(plugin == null){
            return false;
        }else{
            return true;
        }
    }

    private void extractAssets(Plugin plugin, String pluginId, String assetPath){
        String filePath = pluginId + File.separator + assetPath;
        InputStream in = plugin.getClass().getResourceAsStream("/" + assetPath);

        if(in == null){
            System.out.println("NULL ASSET PATH");
        }else{
            File destFile = new File(Global.getAssetDir().getAbsolutePath() + File.separator + filePath);

            if(destFile.exists()){
                return;
            }

            if(!destFile.getParentFile().exists()){
                destFile.getParentFile().mkdirs();
            }

            Path dest = destFile.toPath();

            try {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadPlugins(){
        File pluginDir = new File("Plugins");
        if(!pluginDir.exists()){
            pluginDir.mkdir();
        }
        Debug.log(Debug.Type.INFO, pluginDir.getAbsolutePath());

        ArrayList<PluginLoadedCallback> callbacks = new ArrayList<>();
        Map<String, Boolean> initCalled = new HashMap<>();

        try {
            loadJarFiles(Arrays.stream(pluginDir.listFiles()).toList());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        for (File pluginJar : pluginDir.listFiles()){
            if(pluginJar.getName().endsWith(".jar")) {
                try {
//                    loadJarFile(pluginJar);
                    Plugin plugin = loadPlugin(pluginJar);
                    String pluginId = plugin.getId();
                    initCalled.put(pluginId, false);
                    plugins.put(pluginId, plugin);

                    List<String> deps = plugin.getDependencies();

                    if (deps == null) {
                        Debug.logInfo("Plugin Load Before: " + pluginId);
                        plugin.init(core);
                        initCalled.put(pluginId, true);
                        Debug.logInfo("Plugin Load After: " + pluginId);
                    } else {
                        PluginLoadedCallback pluginLoadedCallback = new PluginLoadedCallback() {
                            @Override
                            public void loaded() {
                                Debug.logInfo("Plugin Load Before: " + pluginId);
                                plugin.init(core);
                                initCalled.put(pluginId, true);
                                Debug.logInfo("Plugin Load After: " + pluginId);
                            }
                        };
                        Debug.logInfo(" ======================================");
                        Debug.logInfo(pluginId + " has dependencies");
                        for (int i = 0; i < deps.size(); i++) {
                            Debug.logInfo(deps.get(i));
                        }
                        Debug.logInfo(" ======================================");
                        pluginLoadedCallback.deps = deps;
                        callbacks.add(pluginLoadedCallback);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                int initialSize = callbacks.size();
                while (callbacks.size() > 0) {
                    for (int i = callbacks.size() - 1; i >= 0; i--) {
                        PluginLoadedCallback plc = callbacks.get(i);
                        boolean[] bools = new boolean[plc.deps.size()];

                        for (int j = 0; j < bools.length; j++) {
                            Plugin p = plugins.get(plc.deps.get(j));
                            boolean init = initCalled.get(plc.deps.get(j));
                            Debug.logInfo("init: " + init);
                            if (p != null && init) {
                                bools[j] = true;
                            } else {
                                bools[j] = false;
                            }
                        }

                        //if all bools are true call plc.loaded() then remove plc from the list;

                        if (allTrue(bools)) {
                            plc.loaded();
                            callbacks.remove(plc);
                        }
                    }

                    //check if any progress was made loading the plugins
                    if(callbacks.size() == initialSize){
                        Debug.logError("Missing plugin dependencies");
                        break;
                    }

                    initialSize = callbacks.size();
                }
            }
        }
    }

    private boolean allTrue(boolean[] bools) {
        for (boolean b : bools) {
            if (!b) {
                return false;
            }
        }
        return true;
    }

    private void loadJarFiles(List<File> jarFiles) throws MalformedURLException {
        List<URL> urls = new ArrayList<>();
        for (File jarFile : jarFiles) {
            urls.add(new URL("file:" + jarFile.getAbsolutePath()));
        }
        URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    private void loadJarFile(File jarFilePath) throws IOException {
//        URLClassLoader classLoader = new URLClassLoader(new URL[] { new URL("file:" + jarFilePath) });
//        Thread.currentThread().setContextClassLoader(classLoader);

        File pluginDir = jarFilePath.getParentFile();
        List<URL> urls = new ArrayList<>();
        urls.add(new URL("file:" + jarFilePath));
        for (File file : pluginDir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".jar")) {
                urls.add(new URL("file:" + file.getAbsolutePath()));
            }
        }
        URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]));
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    private Plugin loadPlugin(File pluginJar){
        Plugin plugin = null;
        List<String> assets = new ArrayList<>();

        try{
            JarFile jarFile = new JarFile(pluginJar.getAbsolutePath().toString());
            Enumeration<JarEntry> entries = jarFile.entries();
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entryName.endsWith(".class")) {
                    String className = entryName.replace("/", ".").replace(".class", "");
                    try {
                        Class<?> clazz = Class.forName(className, false, classLoader);
                        if (Plugin.class.isAssignableFrom(clazz)) {
                            plugin = (Plugin) clazz.getDeclaredConstructor().newInstance();
                        }
                    }catch (NoClassDefFoundError | UnsupportedClassVersionError e){
//                        e.printStackTrace();
                    }
                }else {
                    for (String ext : assetExtensions) {
                        if (entryName.endsWith(ext)) {
                            assets.add(entryName);
                        }
                    }
                }
            }
            jarFile.close();
        }catch (Exception e){
            e.printStackTrace();
        }

        if(plugin == null){
            plugin = createVirtualPlugin(pluginJar.getName());
        }

        if(plugin != null){
            plugin.setId();
        }

        for (int i = 0; i < assets.size(); i++) {
            System.out.println(assets.get(i));
            extractAssets(plugin, plugin.getId(), assets.get(i));
        }

//        return null;
        return plugin;
    }

    private Plugin createVirtualPlugin(String jarName){
        return new Plugin(){
            private String pluginId = jarName.replace(".jar", "");

            @Override
            public void init(CoreEngine coreEngine) {
                Debug.logInfo("Loaded Virtual Plugin: " + pluginId);
            }

            @Override
            public void update() {

            }

            @Override
            public void shutdown() {

            }

            @Override
            public void setId() {
                id.set(pluginId);
//                return UnmodifiableString.fromString(pluginId);
            }

            @Override
            public List<String> getDependencies() {
                return null;
            }
        };

    }

    public Plugin findPlugin(String id) {
        return plugins.get(id);
    }
}
