/*
 * DeepSeek Coder - single APK edition
 * Shared wire protocol between the Android app and the embedded root helper.
 */
#pragma once
#include <cstdint>

#define DC_MAX_PLAYERS 100
#define DC_MAGIC 0x44434331u /* "DCC1" */

namespace Mode {
    constexpr int32_t Esp       = 2;
    constexpr int32_t Aimbot    = 3;
    constexpr int32_t AimOnFire = 4;
    constexpr int32_t BoxEsp    = 5;
    constexpr int32_t HealthBar = 6;
    constexpr int32_t DistEsp   = 7;
    constexpr int32_t LineEsp   = 8;
    constexpr int32_t NameEsp   = 9;
    constexpr int32_t EspColor  = 10;
    constexpr int32_t OffScreen = 11;
    constexpr int32_t HudInfo   = 12;
    constexpr int32_t HeadLock  = 13;
    constexpr int32_t AimFov    = 14;
    constexpr int32_t AimDist   = 15;
    constexpr int32_t SpeedHack = 16;
    constexpr int32_t Thickness = 17;
    constexpr int32_t AimSilent = 18;
    constexpr int32_t AimScoped = 19;
    constexpr int32_t AimBone   = 20;

    constexpr int32_t EspUpdate = 99;
}

#pragma pack(push, 1)

struct Config {
    uint8_t espEnabled;
    uint8_t aimbotEnabled;
    uint8_t aimOnFire;
    uint8_t aimScoped;
    uint8_t aimBone;
    uint8_t headLock;
    uint8_t silentAim;

    uint8_t box;
    uint8_t health;
    uint8_t distance;
    uint8_t line;
    uint8_t name;
    uint8_t tracker;
    uint8_t hud;
    uint8_t speedHack;

    int32_t espColor;
    int32_t thickness;
    float   aimFov;
    float   aimDistance;
};

struct Request {
    int32_t Mode;
    int32_t value;
    int32_t ScreenWidth;
    int32_t ScreenHeight;
    Config  cfg;
};

struct PlayerData {
    float   headX;
    float   headY;
    float   toeX;
    float   toeY;
    float   Distance;
    int32_t Health;
    uint8_t IsDying;
    uint8_t IsBot;
    uint8_t pad[2];
    char    PlayerName[64];
};

struct Response {
    uint8_t Success;
    uint8_t GameFound;
    uint8_t InMatch;
    uint8_t pad;
    int32_t PlayerCount;
    int32_t BotCount;
    PlayerData Players[DC_MAX_PLAYERS];
};

struct Handshake {
    uint32_t magic;
    char     token[64];
};

struct HandshakeResponse {
    uint32_t magic;
    uint8_t  accepted;
    uint8_t  pad[3];
};

#pragma pack(pop)

static_assert(sizeof(Handshake) == 68, "Handshake layout changed");
static_assert(sizeof(HandshakeResponse) == 8, "HandshakeResponse layout changed");
