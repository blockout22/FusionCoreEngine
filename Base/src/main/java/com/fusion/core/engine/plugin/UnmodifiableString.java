package com.fusion.core.engine.plugin;

public class UnmodifiableString {

    private String v = null;

    private void set(String value){
        if(v == null){
            v = value;
        }
    }

    public static UnmodifiableString fromString(String value){
        UnmodifiableString unmodifiableString = new UnmodifiableString();
        unmodifiableString.set(value);
        return unmodifiableString;
    }

    public String get(){
        return v;
    }
}
