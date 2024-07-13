package net.fabricmc.chunky_extended.util;


public class ModUtil {
    private static boolean enabled = false;
    public static void enableMod(){
        enabled = true;
    }
    public static void disableMod(){
        enabled = false;
    }
    public static boolean returnModEnabled(){
        return enabled;
    }
}
