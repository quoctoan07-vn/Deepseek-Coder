package com.deepseek.coder;

import android.graphics.Canvas;

public final class NativeBridge {
    static {
        System.loadLibrary("dccore");
    }

    private NativeBridge() {}

    private static native boolean nativeConnect(String host, int port, String token);
    private static native void nativeDisconnect();
    private static native boolean nativeIsConnected();
    private static native void nativeSetToggle(int mode, boolean on);
    private static native void nativeSetValue(int mode, int value);
    private static native int[] nativeGetStatus();
    private static native void nativeDraw(ESPView view, Canvas canvas, int w, int h);
    private static native String nativeLabel(int id);

    public static boolean connect(String host, int port, String token) {
        return nativeConnect(host, port, token);
    }

    public static void disconnect() {
        nativeDisconnect();
    }

    public static boolean isConnected() {
        return nativeIsConnected();
    }

    public static void setToggle(int mode, boolean on) {
        nativeSetToggle(mode, on);
    }

    public static void setValue(int mode, int value) {
        nativeSetValue(mode, value);
    }

    public static int[] status() {
        int[] s = nativeGetStatus();
        return s != null ? s : new int[]{0, 0, 0, 0, 0};
    }

    public static void draw(ESPView view, Canvas canvas, int w, int h) {
        nativeDraw(view, canvas, w, h);
    }

    public static String label(int id) {
        String value = nativeLabel(id);
        return value != null ? value : "";
    }
}
