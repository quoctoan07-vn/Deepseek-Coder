package com.deepseek.coder;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private static final String FILE = "dc_prefs";
    private static final String CONFIG_VERSION = "config_v4";

    private final SharedPreferences sp;

    public Prefs(Context context) {
        sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);

        // One-time migration: the old build defaulted several toggles to ON.
        // The new default is everything OFF, including the aim triggers.
        if (!sp.getBoolean(CONFIG_VERSION, false)) {
            sp.edit().clear().putBoolean(CONFIG_VERSION, true).apply();
        }
    }

    public boolean esp()         { return sp.getBoolean("esp", false); }
    public boolean aimbot()      { return sp.getBoolean("aimbot", false); }
    public boolean aimOnFire()   { return sp.getBoolean("aim_on_fire", false); }
    public boolean aimScoped()   { return sp.getBoolean("aim_scoped", false); }
    public boolean headLock()    { return sp.getBoolean("head_lock", false); }
    public boolean silentAim()   { return sp.getBoolean("silent_aim", false); }
    public boolean box()         { return sp.getBoolean("box", false); }
    public boolean health()      { return sp.getBoolean("health", false); }
    public boolean distance()    { return sp.getBoolean("distance", false); }
    public boolean line()        { return sp.getBoolean("line", false); }
    public boolean name()        { return sp.getBoolean("name", false); }
    public boolean tracker()     { return sp.getBoolean("tracker", false); }
    public boolean hud()         { return sp.getBoolean("hud", false); }
    public boolean speed()       { return sp.getBoolean("speed", false); }

    public int fov()             { return sp.getInt("fov", 360); }
    public int aimDistance()     { return sp.getInt("aim_distance", 500); }
    public int aimBone()         { return sp.getInt("aim_bone", 0); }
    public int espColor()        { return sp.getInt("esp_color", 0); }
    public int thickness()       { return sp.getInt("thickness", 2); }

    public void setBool(String key, boolean value) {
        sp.edit().putBoolean(key, value).apply();
    }

    public void setInt(String key, int value) {
        sp.edit().putInt(key, value).apply();
    }

    public void syncToNative() {
        NativeBridge.setToggle(Mode.ESP, esp());
        NativeBridge.setToggle(Mode.AIMBOT, aimbot());
        NativeBridge.setToggle(Mode.AIM_ON_FIRE, aimOnFire());
        NativeBridge.setToggle(Mode.AIM_SCOPED, aimScoped());
        NativeBridge.setToggle(Mode.HEAD_LOCK, headLock());
        NativeBridge.setToggle(Mode.SILENT, silentAim());
        NativeBridge.setToggle(Mode.BOX, box());
        NativeBridge.setToggle(Mode.HEALTH, health());
        NativeBridge.setToggle(Mode.DISTANCE, distance());
        NativeBridge.setToggle(Mode.LINE, line());
        NativeBridge.setToggle(Mode.NAME, name());
        NativeBridge.setToggle(Mode.TRACKER, tracker());
        NativeBridge.setToggle(Mode.HUD, hud());
        NativeBridge.setToggle(Mode.SPEED, speed());

        NativeBridge.setValue(Mode.AIM_FOV, fov());
        NativeBridge.setValue(Mode.AIM_DIST, aimDistance());
        NativeBridge.setValue(Mode.AIM_BONE, aimBone());
        NativeBridge.setValue(Mode.ESP_COLOR, espColor());
        NativeBridge.setValue(Mode.THICKNESS, thickness());
    }

    public void reset() {
        sp.edit().clear().putBoolean(CONFIG_VERSION, true).apply();
    }
}
