/*
 * DeepSeek Coder - user interface strings (app side).
 *
 * Every visible menu label lives here instead of Java/resources so the UI
 * text can be changed in one native place. IDs must stay in sync with
 * Text.java.
 */
#pragma once

namespace UiStrings {

enum Id : int {
    MenuTitle = 0,
    Footer,
    TabAim,
    TabEsp,
    TabMisc,

    SectionAimbot,
    Aimbot,
    SilentAim,
    ScopeOnly,
    FireOnly,
    FieldOfView,
    MaxDistance,

    SectionPlayerEsp,
    EnableEsp,
    PlayerLine,
    PlayerBox,
    Name,
    Distance,
    OffScreenTracker,
    HudInfo,

    SectionStyle,
    EspColor,
    LineThickness,

    SectionMovement,
    SpeedHack,

    SectionSession,
    SessionControl,
    SessionSubtitle,
    StopSession,
    ResetSettings,
    ResetGuest,
    ResetGuestConfirm,
    ResetGuestBusy,

    BrandDeepSeek,
    BrandCoder,
    Tagline,
    VersionPill,
    SystemStatus,
    Live,
    RootAccess,
    RootAccessSub,
    GameProcess,
    GameProcessSub,
    OverlayPermission,
    OverlayPermissionSub,
    Session,
    SessionSub,
    StartSession,
    Starting,

    FeatureAim,
    FeatureAimBody,
    FeatureEsp,
    FeatureEspBody,
    FeatureMisc,
    FeatureMiscBody,
    InfoTitle,
    InfoBody,

    Checking,
    Ready,
    Missing,
    Waiting,
    NotFound,
    Idle,
    Active,

    Connecting,
    WaitingGame,
    GameReady,
    InMatch,
    PlayersSuffix,

    SessionStopped,
    SettingsReset,
    ResetGuestStarted,
    ResetGuestDone,
    ResetGuestFailed,
    OverlayRequired,

    SectionAccount,
    ResetGuestToggle,

    AimBone,
    BoneChest,
    BoneNeck,
    BoneHead,

    Count
};

inline const char* get(int id) {
    static const char* const kStrings[Count] = {
        /* MenuTitle          */ "DeepSeek Coder",
        /* Footer             */ "\xC2\xA9 QTD Games",
        /* TabAim             */ "AIM",
        /* TabEsp             */ "ESP",
        /* TabMisc            */ "MISC",

        /* SectionAimbot      */ "AIMBOT",
        /* Aimbot             */ "Enable aim",
        /* SilentAim          */ "Silent aim",
        /* ScopeOnly          */ "Aim when scoped",
        /* FireOnly           */ "Aim when firing",
        /* FieldOfView        */ "Field of view",
        /* MaxDistance        */ "Max distance",

        /* SectionPlayerEsp   */ "PLAYER ESP",
        /* EnableEsp          */ "Enable ESP",
        /* PlayerLine         */ "Player line",
        /* PlayerBox          */ "Player box",
        /* Name               */ "Player name",
        /* Distance           */ "Player distance",
        /* OffScreenTracker   */ "Player tracker",
        /* HudInfo            */ "HUD info",

        /* SectionStyle       */ "STYLE",
        /* EspColor           */ "ESP color",
        /* LineThickness      */ "Line thickness",

        /* SectionMovement    */ "MOVEMENT",
        /* SpeedHack          */ "Speed (risk)",

        /* SectionSession     */ "SESSION",
        /* SessionControl     */ "Session control",
        /* SessionSubtitle    */ "Closing this panel keeps the overlay running.",
        /* StopSession        */ "STOP SESSION",
        /* ResetSettings      */ "RESET SESSION",
        /* ResetGuest         */ "RESET GUEST ACCOUNT",
        /* ResetGuestConfirm  */ "TAP AGAIN TO CONFIRM",
        /* ResetGuestBusy     */ "RESETTING...",

        /* BrandDeepSeek      */ "DeepSeek",
        /* BrandCoder         */ " Coder",
        /* Tagline            */ "FREE FIRE MAX \xC2\xB7 ROOT TOOLKIT",
        /* VersionPill        */ "OFFLINE \xC2\xB7 v1.0",
        /* SystemStatus       */ "System status",
        /* Live               */ "LIVE",
        /* RootAccess         */ "Root access",
        /* RootAccessSub      */ "Shell access for the memory helper",
        /* GameProcess        */ "Game process",
        /* GameProcessSub     */ "com.dts.freefiremax",
        /* OverlayPermission  */ "Overlay permission",
        /* OverlayPermissionSub */ "Draw the ESP above the game",
        /* Session            */ "Session",
        /* SessionSub         */ "Embedded root helper \xC2\xB7 loopback only",
        /* StartSession       */ "START SESSION",
        /* Starting           */ "STARTING\xE2\x80\xA6",

        /* FeatureAim         */ "AIM",
        /* FeatureAimBody     */ "Aimbot\nFOV \xC2\xB7 Snap",
        /* FeatureEsp         */ "ESP",
        /* FeatureEspBody     */ "Box \xC2\xB7 Names\nTracker \xC2\xB7 HUD",
        /* FeatureMisc        */ "MISC",
        /* FeatureMiscBody    */ "Speed \xC2\xB7 HUD\nOffline",
        /* InfoTitle          */ "Single APK \xC2\xB7 root required",
        /* InfoBody           */ "The helper is embedded in the APK and started with su. It talks to the app over 127.0.0.1 only, then exits when the session stops. No license panel, no account, no remote server.",

        /* Checking           */ "CHECKING",
        /* Ready              */ "READY",
        /* Missing            */ "MISSING",
        /* Waiting            */ "WAITING",
        /* NotFound           */ "NOT FOUND",
        /* Idle               */ "IDLE",
        /* Active             */ "ACTIVE",

        /* Connecting         */ "connecting\xE2\x80\xA6",
        /* WaitingGame        */ "waiting for game",
        /* GameReady          */ "game ready",
        /* InMatch            */ "in match \xC2\xB7 ",
        /* PlayersSuffix      */ " players",

        /* SessionStopped     */ "Session stopped",
        /* SettingsReset      */ "Settings reset",
        /* ResetGuestStarted  */ "Resetting guest account\xE2\x80\xA6",
        /* ResetGuestDone     */ "Guest reset ready. Tap Guest on the login screen to finish.",
        /* ResetGuestFailed   */ "Reset failed. Check root and game install.",
        /* OverlayRequired    */ "Overlay permission required",

        /* SectionAccount     */ "ACCOUNT",
        /* ResetGuestToggle   */ "Reset guest account",

        /* AimBone            */ "Aim bone",
        /* BoneChest          */ "Chest",
        /* BoneNeck           */ "Neck",
        /* BoneHead           */ "Head",
    };

    if (id < 0 || id >= Count) return "";
    return kStrings[id];
}

} // namespace UiStrings
