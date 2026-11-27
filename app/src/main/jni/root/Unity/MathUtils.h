/*
 * DeepSeek Coder - projection math (root helper).
 *
 * Extracts the camera world position and projects world coordinates to screen
 * space using the in-game view matrix. Screen size is supplied by the app.
 */

#pragma once
#include "Vector3.hpp"
#include "Quaternion.hpp"
#include "../game/Transform.h"
#include <cmath>

inline int g_screenWidth = 0;
inline int g_screenHeight = 0;

inline Vector3 GetCameraWorldPos(D3DMatrix m) {
    float a11 = m._11, a12 = m._21, a13 = m._31;
    float a21 = m._12, a22 = m._22, a23 = m._32;
    float a31 = m._14, a32 = m._24, a33 = m._34;
    float b1 = -m._41, b2 = -m._42, b3 = -m._44;

    float det = a11 * (a22 * a33 - a23 * a32)
              - a12 * (a21 * a33 - a23 * a31)
              + a13 * (a21 * a32 - a22 * a31);

    if (fabs(det) < 0.0001f) return Vector3::Zero();
    float inv = 1.0f / det;

    return Vector3(
        (b1 * (a22 * a33 - a23 * a32) - a12 * (b2 * a33 - a23 * b3) + a13 * (b2 * a32 - a22 * b3)) * inv,
        (a11 * (b2 * a33 - a23 * b3) - b1 * (a21 * a33 - a23 * a31) + a13 * (a21 * b3 - b2 * a31)) * inv,
        (a11 * (a22 * b3 - b2 * a32) - a12 * (a21 * b3 - b2 * a31) + b1 * (a21 * a32 - a22 * a31)) * inv
    );
}

inline Vector3 WorldToScreen(D3DMatrix matrix, Vector3 world) {
    float w = world.X * matrix._14 + world.Y * matrix._24 + world.Z * matrix._34 + matrix._44;
    if (w < 0.001f) return Vector3(-1, -1, -1);

    float x = world.X * matrix._11 + world.Y * matrix._21 + world.Z * matrix._31 + matrix._41;
    float y = world.X * matrix._12 + world.Y * matrix._22 + world.Z * matrix._32 + matrix._42;

    float halfW = g_screenWidth / 2.0f;
    float halfH = g_screenHeight / 2.0f;

    return Vector3(halfW + halfW * x / w, halfH - halfH * y / w, 0);
}
