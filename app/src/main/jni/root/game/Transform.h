/*
 * DeepSeek Coder - transform and matrix types (root helper).
 *
 * Shared D3D view-matrix layout plus the TRS node layout returned by the
 * Unity transform hierarchy.
 */

#pragma once
#include "../Unity/Vector3.hpp"
#include "../Unity/Quaternion.hpp"
#include "../Includes/Memory.h"

struct Vector4 {
    float X, Y, Z, W;
};

struct D3DMatrix {
    float _11, _12, _13, _14;
    float _21, _22, _23, _24;
    float _31, _32, _33, _34;
    float _41, _42, _43, _44;
};

struct TRSMatrix {
    Vector4 Position;
    Quaternion Rotation;
    Vector4 Scale;
};

Vector3 GetPosition(uintptr_t transform);
Vector3 GetNodePosition(uintptr_t nodeTransform);
