/*
 * DeepSeek Coder - overlay renderer (app side).
 * All coordinates arrive already projected to screen space by the root helper.
 */
#pragma once
#include <jni.h>
#include <cmath>
#include <cstdio>
#include "../common/Protocol.h"

class ESPRenderer {
public:
    void draw(JNIEnv* env, jobject espView, jobject canvas, int w, int h,
              const Response& resp, const Config& cfg) {

        jclass cls = env->GetObjectClass(espView);

        mDrawLine = env->GetMethodID(cls, "DrawLine",
            "(Landroid/graphics/Canvas;IIIIFFFFF)V");
        mDrawRect = env->GetMethodID(cls, "DrawFilledRect",
            "(Landroid/graphics/Canvas;IIIIIFFFF)V");
        mDrawText = env->GetMethodID(cls, "DrawText",
            "(Landroid/graphics/Canvas;IIIILjava/lang/String;FFF)V");
        mDrawOval = env->GetMethodID(cls, "DrawOval",
            "(Landroid/graphics/Canvas;IIIIFFFF)V");
        mDrawRoundRect = env->GetMethodID(cls, "DrawRoundRect",
            "(Landroid/graphics/Canvas;IIIIFFFFF)V");
        mDrawCircle = env->GetMethodID(cls, "DrawCircle",
            "(Landroid/graphics/Canvas;IIIIFFFF)V");

        if (!mDrawLine || !mDrawRect || !mDrawText || !mDrawOval
                || !mDrawRoundRect || !mDrawCircle) return;

        int cr, cg, cb;
        getESPColor(cfg.espColor, cr, cg, cb);

        float centerX = w / 2.0f;
        float centerY = h / 2.0f;

        // Aim field ring: matches the screen-space radius used by Aimbot and
        // SilentAim, so the circle is the actual activation area.
        if ((cfg.aimbotEnabled) && cfg.aimFov > 0.0f) {
            env->CallVoidMethod(espView, mDrawCircle, canvas,
                100, 77, 107, 254, 1.5f, centerX, centerY, cfg.aimFov);
        }

        int botCount = resp.BotCount;
        int playerCount = resp.PlayerCount - botCount;
        if (playerCount < 0) playerCount = 0;

        for (int i = 0; i < resp.PlayerCount; i++) {
            const PlayerData& p = resp.Players[i];

            int pr = cr, pg = cg, pb = cb;
            int alpha = 255;

            if (p.IsDying) {
                pr = 255; pg = 90; pb = 110;
                alpha = 190;
            }

            bool onScreen = (p.headX > 0 && p.headY > 0
                          && p.headX < w && p.headY < h);

            if (onScreen) {
                float headY = p.headY;
                float footY = p.toeY > 0 ? p.toeY : headY + 80;
                float cx    = p.headX;
                float boxH  = footY - headY;
                float boxW  = boxH * 0.60f;

                float nameSize = clamp(boxH * 0.14f, 15.0f, 29.0f);
                float distSize = nameSize * 0.92f;
                float padding  = nameSize * 0.30f;

                if (cfg.box)
                    drawBox(env, espView, canvas, pr, pg, pb, alpha, cx, headY, footY, boxW, cfg.thickness);

                if (cfg.health && !p.IsDying)
                    drawHealthBar(env, espView, canvas, cx, headY, footY, boxW, boxH, p.Health);

                if (cfg.name)
                    drawName(env, espView, canvas, p.PlayerName, cx, headY, padding, nameSize, alpha);

                if (cfg.distance)
                    drawDistance(env, espView, canvas, p.Distance, cx, footY, padding, distSize);

                if (cfg.line)
                    drawSnapLine(env, espView, canvas, pr, pg, pb, alpha, w, h, cfg.hud, cx, headY, cfg.thickness);
            } else if (cfg.tracker) {
                drawTracker(env, espView, canvas, p.headX, p.headY, p.Distance, w, h, centerX, centerY);
            }
        }

        if (cfg.hud) {
            drawHUD(env, espView, canvas, w, playerCount, botCount);
        }
    }

private:
    jmethodID mDrawLine = nullptr;
    jmethodID mDrawRect = nullptr;
    jmethodID mDrawText = nullptr;
    jmethodID mDrawOval = nullptr;
    jmethodID mDrawRoundRect = nullptr;
    jmethodID mDrawCircle = nullptr;

    static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    static void getESPColor(int index, int& r, int& g, int& b) {
        static const int colors[][3] = {
            { 77, 107, 254},  // DeepSeek blue
            {255, 255, 255},  // white
            {  0, 229, 255},  // cyan
            {  0, 255, 135},  // green
            {255, 214,  10},  // yellow
            {255,  75, 110},  // red
            {255,   0, 200},  // magenta
            {255, 145,   0},  // orange
            {180, 190, 205},  // gray
            { 20,  25,  35},  // near-black
        };
        int i = (index >= 0 && index <= 9) ? index : 0;
        r = colors[i][0]; g = colors[i][1]; b = colors[i][2];
    }

    void drawBox(JNIEnv* env, jobject v, jobject c,
                 int r, int g, int b, int a,
                 float cx, float top, float bot, float w, int thick) {

        float lw = clamp((float)thick, 1.0f, 10.0f);
        float hw = w / 2;

        env->CallVoidMethod(v, mDrawLine, c, a, r, g, b, lw, cx - hw, top, cx + hw, top);
        env->CallVoidMethod(v, mDrawLine, c, a, r, g, b, lw, cx - hw, bot, cx + hw, bot);
        env->CallVoidMethod(v, mDrawLine, c, a, r, g, b, lw, cx - hw, top, cx - hw, bot);
        env->CallVoidMethod(v, mDrawLine, c, a, r, g, b, lw, cx + hw, top, cx + hw, bot);
    }

    void drawHealthBar(JNIEnv* env, jobject v, jobject c,
                       float cx, float top, float bot,
                       float boxW, float boxH, int hp) {

        if (hp <= 0) return;

        float pct = clamp(hp / 200.0f, 0.0f, 1.0f);

        int hr, hg;
        if (pct > 0.5f) {
            hr = (int)(255 * (1.0f - (pct - 0.5f) * 2));
            hg = 255;
        } else {
            hr = 255;
            hg = (int)(255 * pct * 2);
        }

        float bw   = clamp(boxH * 0.04f, 3.0f, 6.0f);
        float bx   = cx - boxW / 2 - bw - 3;
        float fill = boxH * pct;

        env->CallVoidMethod(v, mDrawRect, c, 140, 0, 0, 0, 0, bx, top, bw, boxH);
        env->CallVoidMethod(v, mDrawRect, c, 255, hr, hg, 0, 0, bx, bot - fill, bw, fill);
    }

    void drawName(JNIEnv* env, jobject v, jobject c,
                  const char* name, float cx, float headY,
                  float pad, float size, int alpha) {

        if (!name || name[0] == '\0') return;
        jstring js = env->NewStringUTF(name);
        env->CallVoidMethod(v, mDrawText, c, alpha, 255, 255, 255, js, cx, headY - pad, size);
        env->DeleteLocalRef(js);
    }

    void drawDistance(JNIEnv* env, jobject v, jobject c,
                      float dist, float cx, float footY,
                      float pad, float size) {

        char buf[32];
        snprintf(buf, sizeof(buf), "%.0fm", dist);

        jstring js = env->NewStringUTF(buf);
        env->CallVoidMethod(v, mDrawText, c, 220, 235, 242, 255, js, cx, footY + pad + size, size);
        env->DeleteLocalRef(js);
    }

    void drawSnapLine(JNIEnv* env, jobject v, jobject c,
                      int r, int g, int b, int a,
                      int screenW, int screenH, bool hud,
                      float cx, float headY, int thick) {

        (void)screenH;
        float lw = clamp((float)thick, 1.0f, 10.0f);
        float startY = hud ? 74.0f : 0.0f; // bottom of the HUD pill, or screen top

        env->CallVoidMethod(v, mDrawLine, c, (int)(a * 0.55f), r, g, b, lw,
            (float)(screenW / 2), startY, cx, headY);
    }

    void drawTracker(JNIEnv* env, jobject v, jobject c,
                     float headX, float headY, float dist,
                     int w, int h, float cx, float cy) {

        float margin = 14.0f;
        float dirX = headX - cx;
        float dirY = headY - cy;
        float len = sqrtf(dirX * dirX + dirY * dirY);
        if (len < 1.0f) return;
        dirX /= len;
        dirY /= len;

        float maxX = w - margin;
        float maxY = h - margin;
        float scale = 9999.0f;

        if (fabsf(dirX) > 0.001f) {
            float sx = dirX > 0 ? (maxX - cx) / dirX : (margin - cx) / dirX;
            if (sx > 0 && sx < scale) scale = sx;
        }
        if (fabsf(dirY) > 0.001f) {
            float sy = dirY > 0 ? (maxY - cy) / dirY : (margin - cy) / dirY;
            if (sy > 0 && sy < scale) scale = sy;
        }

        float ix = clamp(cx + dirX * scale, margin, maxX);
        float iy = clamp(cy + dirY * scale, margin, maxY);

        env->CallVoidMethod(v, mDrawOval, c, 235, 77, 107, 254, ix, iy, 22.0f, 22.0f);

        char buf[16];
        snprintf(buf, sizeof(buf), "%.0fm", dist);
        jstring js = env->NewStringUTF(buf);
        env->CallVoidMethod(v, mDrawText, c, 255, 255, 255, 255, js, ix, iy + 4.5f, 12.0f);
        env->DeleteLocalRef(js);
    }

    void drawHUD(JNIEnv* env, jobject v, jobject c,
                 int w, int players, int bots) {

        float hudX = w / 2.0f;
        float hudY = 58.0f;
        float bgW = 250.0f;
        float bgH = 32.0f;

        env->CallVoidMethod(v, mDrawRoundRect, c, 215, 77, 107, 254, 16.0f,
            hudX - bgW / 2, hudY - bgH / 2, bgW, bgH);

        char buf[48];
        snprintf(buf, sizeof(buf), "Players %d  ·  Bots %d", players, bots);

        jstring js = env->NewStringUTF(buf);
        env->CallVoidMethod(v, mDrawText, c, 255, 255, 255, 255, js, hudX, hudY + 5.0f, 14.0f);
        env->DeleteLocalRef(js);
    }
};
