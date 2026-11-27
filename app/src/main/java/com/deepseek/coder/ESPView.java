package com.deepseek.coder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.view.View;

public class ESPView extends View implements Runnable {

    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint filledPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private volatile boolean running;
    private Thread renderThread;
    private final long frameDelay;

    public ESPView(Context context) {
        super(context, null, 0);
        setFocusable(false);
        setFocusableInTouchMode(false);
        setBackgroundColor(Color.TRANSPARENT);
        setWillNotDraw(false);

        frameDelay = 1000L / 60L;

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);

        filledPaint.setStyle(Paint.Style.FILL);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        textPaint.setShadowLayer(3f, 0f, 1f, 0xCC000000);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startRenderLoop();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopRenderLoop();
        super.onDetachedFromWindow();
    }

    private synchronized void startRenderLoop() {
        if (running) return;
        running = true;
        renderThread = new Thread(this, "dc-esp");
        renderThread.start();
    }

    private synchronized void stopRenderLoop() {
        running = false;
        if (renderThread != null) {
            renderThread.interrupt();
            renderThread = null;
        }
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);
        while (running) {
            try {
                long start = System.currentTimeMillis();
                postInvalidate();
                long elapsed = System.currentTimeMillis() - start;
                Thread.sleep(Math.max(1L, frameDelay - elapsed));
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas == null) return;
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        int w = getWidth();
        int h = getHeight();
        if (w > 0 && h > 0) {
            try {
                NativeBridge.draw(this, canvas, w, h);
            } catch (Throwable ignored) {
                // Never let the overlay crash the game surface.
            }
        }
    }

    public void DrawLine(Canvas cvs, int a, int r, int g, int b, float width,
                         float x1, float y1, float x2, float y2) {
        strokePaint.setColor(Color.rgb(r, g, b));
        strokePaint.setAlpha(a);
        strokePaint.setStrokeWidth(width);
        cvs.drawLine(x1, y1, x2, y2, strokePaint);
    }

    public void DrawFilledRect(Canvas cvs, int a, int r, int g, int b, int unused,
                               float x, float y, float w, float h) {
        filledPaint.setColor(Color.rgb(r, g, b));
        filledPaint.setAlpha(a);
        cvs.drawRect(x, y, x + w, y + h, filledPaint);
    }

    public void DrawRoundRect(Canvas cvs, int a, int r, int g, int b, float radius,
                              float x, float y, float w, float h) {
        filledPaint.setColor(Color.rgb(r, g, b));
        filledPaint.setAlpha(a);
        cvs.drawRoundRect(x, y, x + w, y + h, radius, radius, filledPaint);
    }

    public void DrawText(Canvas cvs, int a, int r, int g, int b, String txt,
                         float x, float y, float size) {
        textPaint.setColor(Color.rgb(r, g, b));
        textPaint.setAlpha(a);
        textPaint.setTextSize(size);
        cvs.drawText(txt, x, y, textPaint);
    }

    public void DrawOval(Canvas cvs, int a, int r, int g, int b,
                         float cx, float cy, float rx, float ry) {
        filledPaint.setColor(Color.rgb(r, g, b));
        filledPaint.setAlpha(a);
        cvs.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, filledPaint);
    }

    public void DrawCircle(Canvas cvs, int a, int r, int g, int b, float width,
                           float cx, float cy, float radius) {
        strokePaint.setColor(Color.rgb(r, g, b));
        strokePaint.setAlpha(a);
        strokePaint.setStrokeWidth(width);
        cvs.drawCircle(cx, cy, radius, strokePaint);
    }
}
