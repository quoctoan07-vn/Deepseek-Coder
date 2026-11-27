package com.deepseek.coder;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQ_OVERLAY = 1001;
    private static final int REQ_NOTIFICATIONS = 1002;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView rootPill;
    private TextView gamePill;
    private TextView overlayPill;
    private TextView sessionPill;
    private TextView startBtn;
    private TextView stopBtn;

    private boolean rootChecking = false;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateState();
            handler.postDelayed(this, 800);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(Ui.BLUE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.setNavigationBarColor(Ui.WHITE);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        } else {
            window.setNavigationBarColor(0xFF111827);
        }

        setContentView(buildUi());

        requestNotificationPermission();
        refreshEnvironment();
        handler.post(ticker);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshEnvironment();
        updateState();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private View buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);

        // ------------------------------------------------------------------
        // Header
        // ------------------------------------------------------------------
        FrameLayout header = new FrameLayout(this);
        GradientDrawable headerBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF4D6BFE, 0xFF6F8BFF});
        float hr = Ui.dp(this, 34);
        headerBg.setCornerRadii(new float[]{0, 0, 0, 0, hr, hr, hr, hr});
        header.setBackground(headerBg);
        root.addView(header, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 260)));

        header.addView(new CodePatternView(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout headerContent = new LinearLayout(this);
        headerContent.setOrientation(LinearLayout.VERTICAL);
        headerContent.setGravity(Gravity.CENTER_HORIZONTAL);
        Ui.padDp(this, headerContent, 24, 30, 24, 0);
        header.addView(headerContent, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View logo = Ui.logo(this, 62, Color.WHITE, Ui.BLUE, 18);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(Ui.dp(this, 62), Ui.dp(this, 62));
        logoLp.bottomMargin = Ui.dp(this, 14);
        headerContent.addView(logo, logoLp);

        LinearLayout wordmarkRow = new LinearLayout(this);
        wordmarkRow.setOrientation(LinearLayout.HORIZONTAL);
        wordmarkRow.setGravity(Gravity.CENTER);

        TextView deep = Ui.text(this, Text.get(Text.BRAND_DEEPSEEK), 28f, Ui.WHITE, true);
        deep.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        deep.setLetterSpacing(0.01f);
        wordmarkRow.addView(deep);

        TextView coder = Ui.text(this, Text.get(Text.BRAND_CODER), 28f, 0xFFD5DEFF, true);
        coder.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        coder.setLetterSpacing(0.01f);
        wordmarkRow.addView(coder);
        headerContent.addView(wordmarkRow);

        TextView tagline = Ui.medium(this, Text.get(Text.TAGLINE), 9f, 0xD9FFFFFF);
        tagline.setLetterSpacing(0.16f);
        LinearLayout.LayoutParams tagLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tagLp.topMargin = Ui.dp(this, 7);
        headerContent.addView(tagline, tagLp);

        TextView version = Ui.pill(this, Text.get(Text.VERSION_PILL), Ui.WHITE, 0x24FFFFFF, 10);
        LinearLayout.LayoutParams versionLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        versionLp.topMargin = Ui.dp(this, 14);
        headerContent.addView(version, versionLp);

        // ------------------------------------------------------------------
        // Scrollable body overlapping the header
        // ------------------------------------------------------------------
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        FrameLayout.LayoutParams scrollLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        scrollLp.topMargin = Ui.dp(this, 202);
        root.addView(scroll, scrollLp);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        Ui.padDp(this, content, 18, 0, 18, 28);
        scroll.addView(content);

        // Status card
        LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setBackground(Ui.rect(Ui.WHITE, Ui.dp(this, 22), Ui.BORDER, Ui.dp(this, 1)));
        statusCard.setElevation(Ui.dp(this, 8));
        Ui.padDp(this, statusCard, 16, 16, 16, 8);
        content.addView(statusCard, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout statusHeader = new LinearLayout(this);
        statusHeader.setOrientation(LinearLayout.HORIZONTAL);
        statusHeader.setGravity(Gravity.CENTER_VERTICAL);
        statusHeader.setPadding(0, 0, 0, Ui.dp(this, 6));

        TextView statusTitle = Ui.text(this, Text.get(Text.SYSTEM_STATUS), 12.5f, Ui.TEXT, true);
        statusTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        statusHeader.addView(statusTitle);

        TextView live = Ui.pill(this, Text.get(Text.LIVE), Ui.GREEN, Ui.GREEN_SOFT, 10);
        statusHeader.addView(live);
        statusCard.addView(statusHeader);

        rootPill = addStatusRow(statusCard, Text.get(Text.ROOT_ACCESS), Text.get(Text.ROOT_ACCESS_SUB));
        gamePill = addStatusRow(statusCard, Text.get(Text.GAME_PROCESS), Text.get(Text.GAME_PROCESS_SUB));
        overlayPill = addStatusRow(statusCard, Text.get(Text.OVERLAY_PERMISSION), Text.get(Text.OVERLAY_PERMISSION_SUB));
        sessionPill = addStatusRow(statusCard, Text.get(Text.SESSION), Text.get(Text.SESSION_SUB));

        // Start / stop
        startBtn = Ui.medium(this, Text.get(Text.START_SESSION), 12.5f, Ui.WHITE);
        startBtn.setGravity(Gravity.CENTER);
        startBtn.setLetterSpacing(0.08f);
        startBtn.setBackground(Ui.ripple(0x33FFFFFF, Ui.rect(Ui.BLUE, Ui.dp(this, 16))));
        LinearLayout.LayoutParams startLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 56));
        startLp.topMargin = Ui.dp(this, 16);
        startBtn.setLayoutParams(startLp);
        startBtn.setOnClickListener(v -> startSession());
        content.addView(startBtn);

        stopBtn = Ui.medium(this, Text.get(Text.STOP_SESSION), 11.5f, Ui.RED);
        stopBtn.setGravity(Gravity.CENTER);
        stopBtn.setLetterSpacing(0.06f);
        stopBtn.setBackground(Ui.ripple(0x22F04438, Ui.rect(Ui.RED_SOFT, Ui.dp(this, 14))));
        LinearLayout.LayoutParams stopLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48));
        stopLp.topMargin = Ui.dp(this, 8);
        stopBtn.setLayoutParams(stopLp);
        stopBtn.setVisibility(View.GONE);
        stopBtn.setOnClickListener(v -> stopSession());
        content.addView(stopBtn);

        // Feature summary
        LinearLayout features = new LinearLayout(this);
        features.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams featuresLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        featuresLp.topMargin = Ui.dp(this, 16);
        features.setLayoutParams(featuresLp);
        content.addView(features);

        addFeatureCard(features, Text.get(Text.FEATURE_AIM), Text.get(Text.FEATURE_AIM_BODY), 0);
        addFeatureCard(features, Text.get(Text.FEATURE_ESP), Text.get(Text.FEATURE_ESP_BODY), 1);
        addFeatureCard(features, Text.get(Text.FEATURE_MISC), Text.get(Text.FEATURE_MISC_BODY), 2);

        // Info card
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setBackground(Ui.rect(Ui.BLUE_SOFT2, Ui.dp(this, 16), 0xFFDCE5FF, Ui.dp(this, 1)));
        Ui.padDp(this, info, 14, 13, 14, 13);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        infoLp.topMargin = Ui.dp(this, 14);
        info.setLayoutParams(infoLp);

        TextView infoTitle = Ui.text(this, Text.get(Text.INFO_TITLE), 11.5f, Ui.TEXT, true);
        info.addView(infoTitle);

        TextView infoBody = Ui.text(this, Text.get(Text.INFO_BODY), 9.5f, Ui.TEXT2, false);
        infoBody.setLineSpacing(Ui.dp(this, 2), 1f);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bodyLp.topMargin = Ui.dp(this, 4);
        infoBody.setLayoutParams(bodyLp);
        info.addView(infoBody);
        content.addView(info);

        TextView footer = Ui.text(this, Text.get(Text.FOOTER), 8.5f, Ui.TEXT3, false);
        footer.setGravity(Gravity.CENTER);
        footer.setLetterSpacing(0.04f);
        LinearLayout.LayoutParams footerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footerLp.topMargin = Ui.dp(this, 18);
        footer.setLayoutParams(footerLp);
        content.addView(footer);

        return root;
    }

    private TextView addStatusRow(LinearLayout parent, String title, String subtitle) {
        if (parent.getChildCount() > 1) {
            View divider = Ui.divider(this);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 1);
            dlp.leftMargin = Ui.dp(this, 18);
            divider.setLayoutParams(dlp);
            parent.addView(divider);
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        Ui.padDp(this, row, 0, 9, 0, 9);

        View dot = new View(this);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(Ui.BLUE);
        dot.setBackground(dotBg);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(Ui.dp(this, 7), Ui.dp(this, 7));
        dotLp.rightMargin = Ui.dp(this, 11);
        row.addView(dot, dotLp);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView titleView = Ui.medium(this, title, 11.5f, Ui.TEXT);
        texts.addView(titleView);

        TextView subView = Ui.text(this, subtitle, 9f, Ui.TEXT3, false);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = Ui.dp(this, 2);
        subView.setLayoutParams(subLp);
        texts.addView(subView);

        row.addView(texts);

        TextView pill = Ui.pill(this, Text.get(Text.CHECKING), Ui.AMBER, Ui.AMBER_SOFT, 9);
        row.addView(pill);

        parent.addView(row);
        return pill;
    }

    private void addFeatureCard(LinearLayout parent, String title, String body, int index) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Ui.rect(Ui.WHITE, Ui.dp(this, 15), Ui.BORDER, Ui.dp(this, 1)));
        Ui.padDp(this, card, 11, 11, 11, 11);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = index == 0 ? 0 : Ui.dp(this, 4);
        lp.rightMargin = index == 2 ? 0 : Ui.dp(this, 4);
        card.setLayoutParams(lp);

        TextView titleView = Ui.medium(this, title, 8.5f, Ui.BLUE);
        titleView.setLetterSpacing(0.12f);
        card.addView(titleView);

        TextView bodyView = Ui.text(this, body, 9f, Ui.TEXT2, false);
        bodyView.setLineSpacing(Ui.dp(this, 2), 1f);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bodyLp.topMargin = Ui.dp(this, 4);
        bodyView.setLayoutParams(bodyLp);
        card.addView(bodyView);

        parent.addView(card);
    }

    private void refreshEnvironment() {
        AppState.overlayGranted = Settings.canDrawOverlays(this);
        if (rootChecking) return;
        rootChecking = true;

        new Thread(() -> {
            boolean root = RootManager.isRootAvailable();
            boolean game = root && RootManager.isGameRunning();
            handler.post(() -> {
                AppState.rootGranted = root;
                AppState.gameRunning = game;
                rootChecking = false;
                updateState();
            });
        }, "dc-env").start();
    }

    private void updateState() {
        AppState.overlayGranted = Settings.canDrawOverlays(this);

        if (rootPill != null) {
            if (rootChecking) {
                setPill(rootPill, Text.get(Text.CHECKING), Ui.AMBER, Ui.AMBER_SOFT);
            } else if (AppState.rootGranted) {
                setPill(rootPill, Text.get(Text.READY), Ui.GREEN, Ui.GREEN_SOFT);
            } else {
                setPill(rootPill, Text.get(Text.MISSING), Ui.RED, Ui.RED_SOFT);
            }
        }

        if (gamePill != null) {
            if (AppState.serviceRunning) {
                if (AppState.connected && AppState.gameRunning) {
                    setPill(gamePill, Text.get(Text.READY), Ui.GREEN, Ui.GREEN_SOFT);
                } else if (AppState.connected) {
                    setPill(gamePill, Text.get(Text.WAITING), Ui.AMBER, Ui.AMBER_SOFT);
                } else {
                    setPill(gamePill, Text.get(Text.CHECKING), Ui.AMBER, Ui.AMBER_SOFT);
                }
            } else if (AppState.gameRunning) {
                setPill(gamePill, Text.get(Text.READY), Ui.GREEN, Ui.GREEN_SOFT);
            } else {
                setPill(gamePill, Text.get(Text.NOT_FOUND), Ui.TEXT3, 0xFFF2F4F7);
            }
        }

        if (overlayPill != null) {
            if (AppState.overlayGranted) {
                setPill(overlayPill, Text.get(Text.READY), Ui.GREEN, Ui.GREEN_SOFT);
            } else {
                setPill(overlayPill, Text.get(Text.MISSING), Ui.RED, Ui.RED_SOFT);
            }
        }

        if (sessionPill != null) {
            if (AppState.serviceRunning) {
                if (AppState.connected) {
                    String label = AppState.inMatch
                            ? Text.get(Text.ACTIVE) + " \u00B7 " + AppState.players + "P"
                            : Text.get(Text.ACTIVE);
                    setPill(sessionPill, label, Ui.GREEN, Ui.GREEN_SOFT);
                } else if (AppState.serviceStarting) {
                    setPill(sessionPill, Text.get(Text.STARTING), Ui.AMBER, Ui.AMBER_SOFT);
                } else {
                    setPill(sessionPill, Text.get(Text.WAITING), Ui.AMBER, Ui.AMBER_SOFT);
                }
            } else {
                setPill(sessionPill, Text.get(Text.IDLE), Ui.TEXT3, 0xFFF2F4F7);
            }
        }

        if (startBtn != null && stopBtn != null) {
            boolean running = AppState.serviceRunning;
            startBtn.setVisibility(running ? View.GONE : View.VISIBLE);
            stopBtn.setVisibility(running ? View.VISIBLE : View.GONE);
            startBtn.setText(AppState.serviceStarting
                    ? Text.get(Text.STARTING)
                    : Text.get(Text.START_SESSION));
            startBtn.setEnabled(!AppState.serviceStarting);
            startBtn.setAlpha(AppState.serviceStarting ? 0.7f : 1f);
        }
    }

    private void setPill(TextView pill, String text, int textColor, int bgColor) {
        pill.setText(text);
        pill.setTextColor(textColor);
        pill.setBackground(Ui.rect(bgColor, Ui.dp(this, 9)));
    }

    private void startSession() {
        if (!Settings.canDrawOverlays(this)) {
            toast("Allow overlay permission first");
            startActivityForResult(new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())), REQ_OVERLAY);
            return;
        }

        if (!AppState.rootGranted) {
            toast("Root access is required");
            refreshEnvironment();
            return;
        }

        AppState.serviceStarting = true;
        updateState();

        new Thread(RootManager::launchGame, "dc-launch-game").start();

        Intent service = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(service);
        } else {
            startService(service);
        }
    }

    private void stopSession() {
        stopService(new Intent(this, OverlayService.class));
        AppState.serviceStarting = false;
        handler.postDelayed(this::updateState, 250);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OVERLAY) {
            AppState.overlayGranted = Settings.canDrawOverlays(this);
            updateState();
        }
    }

    // ------------------------------------------------------------------
    // Subtle header decoration
    // ------------------------------------------------------------------
    private static class CodePatternView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        CodePatternView(android.content.Context context) {
            super(context);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(Ui.dp(context, 2));
            paint.setColor(0x18FFFFFF);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth();
            float h = getHeight();
            if (w <= 0 || h <= 0) return;

            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(0x18FFFFFF);
            paint.setStrokeWidth(Ui.dp(getContext(), 2));

            // Large, faint code brackets in the bottom-right corner.
            float s = Math.min(w, h) * 0.42f;
            float cx = w - s * 0.55f;
            float cy = h - s * 0.35f;

            c.drawLine(cx - s * 0.16f, cy - s * 0.22f, cx - s * 0.34f, cy, paint);
            c.drawLine(cx - s * 0.34f, cy, cx - s * 0.16f, cy + s * 0.22f, paint);
            c.drawLine(cx + s * 0.16f, cy - s * 0.22f, cx + s * 0.34f, cy, paint);
            c.drawLine(cx + s * 0.34f, cy, cx + s * 0.16f, cy + s * 0.22f, paint);
            c.drawLine(cx + s * 0.07f, cy - s * 0.28f, cx - s * 0.07f, cy + s * 0.28f, paint);

            // Dot grid.
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0x10FFFFFF);
            float step = Ui.dp(getContext(), 26);
            for (float x = step; x < w; x += step) {
                for (float y = step; y < h * 0.55f; y += step) {
                    c.drawCircle(x, y, Ui.dp(getContext(), 0.9f), paint);
                }
            }
        }
    }
}
