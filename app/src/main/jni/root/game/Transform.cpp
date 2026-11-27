/*
 * DeepSeek Coder - Unity transform math (root helper).
 *
 * Reads the local TRS matrix and walks the parent chain (up to 50 levels) to
 * produce a world-space position for any transform node. Sibling fields are
 * fetched with process_vm_readv batches where possible.
 */
#include "Transform.h"

Vector3 GetPosition(uintptr_t transform) {
    Vector3 pos = Vector3::Zero();

    uintptr_t transformObj = Read<uintptr_t>(transform + 0x10);
    if (!transformObj) return pos;

    uintptr_t index = 0;
    uintptr_t matrixPtr = 0;
    {
        MemRange ranges[] = {
            {transformObj + 0x40, &index,     sizeof(index)},
            {transformObj + 0x38, &matrixPtr, sizeof(matrixPtr)},
        };
        ReadRanges(ranges, 2);
    }
    if (!matrixPtr) return pos;

    uintptr_t matrixList = 0;
    uintptr_t matrixIndices = 0;
    {
        MemRange ranges[] = {
            {matrixPtr + 0x18, &matrixList,    sizeof(matrixList)},
            {matrixPtr + 0x20, &matrixIndices, sizeof(matrixIndices)},
        };
        ReadRanges(ranges, 2);
    }
    if (!matrixList || !matrixIndices) return pos;

    Vector3 result = Read<Vector3>(matrixList + (index * 0x30));

    int parentIndex = Read<int>(matrixIndices + (index * 4));
    int tries = 0;

    while (parentIndex >= 0 && tries < 50) {
        tries++;

        TRSMatrix trs{};
        int nextParent = -1;
        {
            MemRange ranges[] = {
                {matrixList + ((uintptr_t)parentIndex * 0x30), &trs,        sizeof(trs)},
                {matrixIndices + ((uintptr_t)parentIndex * 4), &nextParent, sizeof(nextParent)},
            };
            ReadRanges(ranges, 2);
        }

        float rX = trs.Rotation.X;
        float rY = trs.Rotation.Y;
        float rZ = trs.Rotation.Z;
        float rW = trs.Rotation.W;

        float sX = result.X * trs.Scale.X;
        float sY = result.Y * trs.Scale.Y;
        float sZ = result.Z * trs.Scale.Z;

        result.X = trs.Position.X + sX
            + sX * (rY * rY * -2.0 - rZ * rZ * 2.0)
            + sY * (rW * rZ * -2.0 - rY * rX * -2.0)
            + sZ * (rZ * rX * 2.0 - rW * rY * -2.0);

        result.Y = trs.Position.Y + sY
            + sX * (rX * rY * 2.0 - rW * rZ * -2.0)
            + sY * (rZ * rZ * -2.0 - rX * rX * 2.0)
            + sZ * (rW * rX * -2.0 - rZ * rY * -2.0);

        result.Z = trs.Position.Z + sZ
            + sX * (rW * rY * -2.0 - rX * rZ * -2.0)
            + sY * (rY * rZ * 2.0 - rW * rX * -2.0)
            + sZ * (rX * rX * -2.0 - rY * rY * 2.0);

        parentIndex = nextParent;
    }

    if (tries < 50) pos = result;
    return pos;
}

Vector3 GetNodePosition(uintptr_t nodeTransform) {
    uintptr_t inner = Read<uintptr_t>(nodeTransform + 0x10);
    if (!inner) return Vector3::Zero();
    return GetPosition(inner);
}
