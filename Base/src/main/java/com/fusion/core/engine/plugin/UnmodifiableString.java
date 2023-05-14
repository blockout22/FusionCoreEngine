package com.fusion.core.engine.plugin;

public class UnmodifiableString {

    private String v = null;

    public void set(String value){
        if(v == null){
            v = value;
        }
    }

//    public static UnmodifiableString fromString(Plugin plugin, String value){
//        UnmodifiableString id = plugin.id;
//        id.set(value);
//        return id;
//    }

    public String get(){
        return v;
    }
}
