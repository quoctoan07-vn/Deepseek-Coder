package com.deepseek.coder;

/**
 * UI text IDs. The actual strings live in native code (UiStrings.h), so menu
 * titles, button labels and the footer can be changed in one C++ table.
 */
public final class Text {
    public static final int MENU_TITLE = 0;
    public static final int FOOTER = 1;
    public static final int TAB_AIM = 2;
    public static final int TAB_ESP = 3;
    public static final int TAB_MISC = 4;

    public static final int SECTION_AIMBOT = 5;
    public static final int AIMBOT = 6;
    public static final int SILENT_AIM = 7;
    public static final int SCOPE_ONLY = 8;
    public static final int FIRE_ONLY = 9;
    public static final int FIELD_OF_VIEW = 10;
    public static final int MAX_DISTANCE = 11;

    public static final int SECTION_PLAYER_ESP = 12;
    public static final int ENABLE_ESP = 13;
    public static final int PLAYER_LINE = 14;
    public static final int PLAYER_BOX = 15;
    public static final int NAME = 16;
    public static final int DISTANCE = 17;
    public static final int OFF_SCREEN_TRACKER = 18;
    public static final int HUD_INFO = 19;

    public static final int SECTION_STYLE = 20;
    public static final int ESP_COLOR = 21;
    public static final int LINE_THICKNESS = 22;

    public static final int SECTION_MOVEMENT = 23;
    public static final int SPEED_HACK = 24;

    public static final int SECTION_SESSION = 25;
    public static final int SESSION_CONTROL = 26;
    public static final int SESSION_SUBTITLE = 27;
    public static final int STOP_SESSION = 28;
    public static final int RESET_SETTINGS = 29;
    public static final int RESET_GUEST = 30;
    public static final int RESET_GUEST_CONFIRM = 31;
    public static final int RESET_GUEST_BUSY = 32;

    public static final int BRAND_DEEPSEEK = 33;
    public static final int BRAND_CODER = 34;
    public static final int TAGLINE = 35;
    public static final int VERSION_PILL = 36;
    public static final int SYSTEM_STATUS = 37;
    public static final int LIVE = 38;
    public static final int ROOT_ACCESS = 39;
    public static final int ROOT_ACCESS_SUB = 40;
    public static final int GAME_PROCESS = 41;
    public static final int GAME_PROCESS_SUB = 42;
    public static final int OVERLAY_PERMISSION = 43;
    public static final int OVERLAY_PERMISSION_SUB = 44;
    public static final int SESSION = 45;
    public static final int SESSION_SUB = 46;
    public static final int START_SESSION = 47;
    public static final int STARTING = 48;

    public static final int FEATURE_AIM = 49;
    public static final int FEATURE_AIM_BODY = 50;
    public static final int FEATURE_ESP = 51;
    public static final int FEATURE_ESP_BODY = 52;
    public static final int FEATURE_MISC = 53;
    public static final int FEATURE_MISC_BODY = 54;
    public static final int INFO_TITLE = 55;
    public static final int INFO_BODY = 56;

    public static final int CHECKING = 57;
    public static final int READY = 58;
    public static final int MISSING = 59;
    public static final int WAITING = 60;
    public static final int NOT_FOUND = 61;
    public static final int IDLE = 62;
    public static final int ACTIVE = 63;

    public static final int CONNECTING = 64;
    public static final int WAITING_GAME = 65;
    public static final int GAME_READY = 66;
    public static final int IN_MATCH = 67;
    public static final int PLAYERS_SUFFIX = 68;

    public static final int SESSION_STOPPED = 69;
    public static final int SETTINGS_RESET = 70;
    public static final int RESET_GUEST_STARTED = 71;
    public static final int RESET_GUEST_DONE = 72;
    public static final int RESET_GUEST_FAILED = 73;
    public static final int OVERLAY_REQUIRED = 74;
    public static final int SECTION_ACCOUNT = 75;
    public static final int RESET_GUEST_TOGGLE = 76;
    public static final int AIM_BONE = 77;
    public static final int BONE_CHEST = 78;
    public static final int BONE_NECK = 79;
    public static final int BONE_HEAD = 80;

    private Text() {}

    public static String get(int id) {
        return NativeBridge.label(id);
    }
}
