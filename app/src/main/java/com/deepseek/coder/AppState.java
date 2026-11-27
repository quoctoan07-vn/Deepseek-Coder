package com.deepseek.coder;

public final class AppState {
    public static volatile boolean rootGranted = false;
    public static volatile boolean overlayGranted = false;
    public static volatile boolean gameRunning = false;
    public static volatile boolean serviceRunning = false;
    public static volatile boolean serviceStarting = false;
    public static volatile boolean connected = false;
    public static volatile boolean inMatch = false;
    public static volatile int players = 0;
    public static volatile int bots = 0;

    private AppState() {}
}
