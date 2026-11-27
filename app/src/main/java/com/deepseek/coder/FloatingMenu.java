package com.deepseek.coder;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class FloatingMenu {

    private static final int PANEL_WIDTH_DP = 300;
    private static final int PANEL_SCROLL_DP = 210;

    private final Context ctx;
    private final WindowManager wm;
    private final Prefs prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private WindowManager.LayoutParams params;
    private FrameLayout rootFrame;
    private WhaleBubbleView bubbleView;
    private LinearLayout panelView;
    private ScrollView scrollView;
    private LinearLayout headerView;
    private TextView statusText;
    private View statusDot;
    private TextView[] tabViews;
    private LinearLayout[] tabPages;
    private int currentTab = 0;
    private boolean panelOpen = false;
    private int bubbleX;
    private int bubbleY;
    private boolean connected = false;
    private boolean gameReady = false;

    public FloatingMenu(Context context, WindowManager windowManager) {
        this.ctx = context;
        this.wm = windowManager;
        this.prefs = new Prefs(context);
        initParams();
        buildUi();
        startStatusLoop();
    }

    private void initParams() {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                android.graphics.PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = dp(14);
        params.y = dp(110);
        bubbleX = params.x;
        bubbleY = params.y;
    }

    private void buildUi() {
        rootFrame = new FrameLayout(ctx);

        bubbleView = new WhaleBubbleView(ctx);
        FrameLayout.LayoutParams bubbleLp = new FrameLayout.LayoutParams(dp(58), dp(58));
        bubbleView.setLayoutParams(bubbleLp);
        bubbleView.setOnTouchListener(new DragTouch());
        bubbleView.setOnClickListener(v -> openPanel());

        panelView = buildPanel();
        panelView.setVisibility(View.GONE);

        rootFrame.addView(bubbleView);
        rootFrame.addView(panelView);

        try {
            wm.addView(rootFrame, params);
        } catch (Throwable t) {
            Toast.makeText(ctx, Text.get(Text.OVERLAY_REQUIRED), Toast.LENGTH_LONG).show();
        }
    }

    private LinearLayout buildPanel() {
        LinearLayout panel = new LinearLayout(ctx);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(Ui.rect(Ui.WHITE, dp(22), Ui.BORDER, dp(1)));
        panel.setElevation(dp(16));
        panel.setLayoutParams(new FrameLayout.LayoutParams(dp(PANEL_WIDTH_DP), ViewGroup.LayoutParams.WRAP_CONTENT));

        // Header
        headerView = new LinearLayout(ctx);
        headerView.setOrientation(LinearLayout.HORIZONTAL);
        headerView.setGravity(Gravity.CENTER_VERTICAL);
        Ui.padDp(ctx, headerView, 14, 12, 12, 10);
        headerView.setOnTouchListener(new DragTouch());

        View logo = Ui.logo(ctx, 34, Ui.BLUE, 0, 10);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(34), dp(34));
        logoLp.rightMargin = dp(10);
        logo.setLayoutParams(logoLp);
        headerView.addView(logo);

        LinearLayout titleBox = new LinearLayout(ctx);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = Ui.text(ctx, Text.get(Text.MENU_TITLE), 13f, Ui.TEXT, true);
        titleBox.addView(title);

        LinearLayout statusRow = new LinearLayout(ctx);
        statusRow.setOrientation(LinearLayout.HORIZONTAL);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusLp.topMargin = dp(3);
        statusRow.setLayoutParams(statusLp);

        statusDot = new View(ctx);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(Ui.AMBER);
        statusDot.setBackground(dotBg);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(6), dp(6));
        dotLp.rightMargin = dp(5);
        statusDot.setLayoutParams(dotLp);
        statusRow.addView(statusDot);

        statusText = Ui.medium(ctx, Text.get(Text.CONNECTING), 9f, Ui.TEXT3);
        statusRow.addView(statusText);
        titleBox.addView(statusRow);
        headerView.addView(titleBox);

        TextView close = Ui.medium(ctx, "×", 15f, Ui.TEXT2);
        close.setGravity(Gravity.CENTER);
        close.setBackground(Ui.rect(Ui.SURFACE, dp(14)));
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(dp(28), dp(28));
        closeLp.leftMargin = dp(6);
        close.setLayoutParams(closeLp);
        close.setOnClickListener(v -> closePanel());
        headerView.addView(close);

        panel.addView(headerView);
        panel.addView(Ui.divider(ctx));

        // Segmented tabs
        LinearLayout tabs = new LinearLayout(ctx);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setBackground(Ui.rect(0xFFF1F4F9, dp(13)));
        Ui.padDp(ctx, tabs, 3, 3, 3, 3);
        LinearLayout.LayoutParams tabsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tabsLp.leftMargin = dp(12);
        tabsLp.rightMargin = dp(12);
        tabsLp.topMargin = dp(10);
        tabsLp.bottomMargin = dp(10);
        tabs.setLayoutParams(tabsLp);

        tabViews = new TextView[3];
        tabPages = new LinearLayout[3];
        String[] names = {
                Text.get(Text.TAB_AIM),
                Text.get(Text.TAB_ESP),
                Text.get(Text.TAB_MISC)
        };
        for (int i = 0; i < 3; i++) {
            final int index = i;
            TextView tab = Ui.medium(ctx, names[i], 10.5f, Ui.TEXT2);
            tab.setGravity(Gravity.CENTER);
            tab.setLetterSpacing(0.08f);
            tab.setLayoutParams(new LinearLayout.LayoutParams(0, dp(32), 1f));
            tab.setOnClickListener(v -> setTab(index));
            tabViews[i] = tab;
            tabs.addView(tab);
        }
        panel.addView(tabs);

        // Scroll content
        scrollView = new ScrollView(ctx);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setClipToPadding(false);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(PANEL_SCROLL_DP)));

        FrameLayout host = new FrameLayout(ctx);
        host.setBackgroundColor(Ui.BG);
        Ui.padDp(ctx, host, 12, 8, 12, 12);

        tabPages[0] = buildAimPage();
        tabPages[1] = buildEspPage();
        tabPages[2] = buildMiscPage();
        for (int i = 0; i < 3; i++) {
            host.addView(tabPages[i], new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        scrollView.addView(host);
        panel.addView(scrollView);

        TextView footer = Ui.text(ctx, Text.get(Text.FOOTER), 8.5f, Ui.TEXT3, false);
        footer.setGravity(Gravity.CENTER);
        Ui.padDp(ctx, footer, 8, 7, 8, 9);
        panel.addView(footer);

        setTab(currentTab);
        return panel;
    }

    private LinearLayout newPage() {
        LinearLayout page = new LinearLayout(ctx);
        page.setOrientation(LinearLayout.VERTICAL);
        return page;
    }

    private LinearLayout buildAimPage() {
        LinearLayout page = newPage();
        addSection(page, Text.get(Text.SECTION_AIMBOT));

        LinearLayout card = newCard(page);
        addToggleRow(card, Text.get(Text.AIMBOT), Mode.AIMBOT, prefs.aimbot(), "aimbot");
        addToggleRow(card, Text.get(Text.SCOPE_ONLY), Mode.AIM_SCOPED, prefs.aimScoped(), "aim_scoped");
        addToggleRow(card, Text.get(Text.FIRE_ONLY), Mode.AIM_ON_FIRE, prefs.aimOnFire(), "aim_on_fire");

        LinearLayout boneCard = newCard(page);
        addBoneSelector(boneCard);

        LinearLayout sliders = newCard(page);
        addSliderRow(sliders, Text.get(Text.FIELD_OF_VIEW), Mode.AIM_FOV, 10, 360, prefs.fov(), "°", "fov", false);
        addSliderRow(sliders, Text.get(Text.MAX_DISTANCE), Mode.AIM_DIST, 50, 1000, prefs.aimDistance(), "m", "aim_distance", false);

        return page;
    }

    private LinearLayout buildEspPage() {
        LinearLayout page = newPage();
        addSection(page, Text.get(Text.SECTION_PLAYER_ESP));

        LinearLayout card = newCard(page);
        addToggleRow(card, Text.get(Text.ENABLE_ESP), Mode.ESP, prefs.esp(), "esp");
        addToggleRow(card, Text.get(Text.PLAYER_LINE), Mode.LINE, prefs.line(), "line");
        addToggleRow(card, Text.get(Text.PLAYER_BOX), Mode.BOX, prefs.box(), "box");
        addToggleRow(card, Text.get(Text.NAME), Mode.NAME, prefs.name(), "name");
        addToggleRow(card, Text.get(Text.DISTANCE), Mode.DISTANCE, prefs.distance(), "distance");
        addToggleRow(card, Text.get(Text.OFF_SCREEN_TRACKER), Mode.TRACKER, prefs.tracker(), "tracker");
        addToggleRow(card, Text.get(Text.HUD_INFO), Mode.HUD, prefs.hud(), "hud");

        addSection(page, Text.get(Text.SECTION_STYLE));
        LinearLayout style = newCard(page);
        addSliderRow(style, Text.get(Text.ESP_COLOR), Mode.ESP_COLOR, 0, 9, prefs.espColor(), "", "esp_color", true);
        addSliderRow(style, Text.get(Text.LINE_THICKNESS), Mode.THICKNESS, 1, 6, prefs.thickness(), "px", "thickness", false);

        return page;
    }

    private LinearLayout buildMiscPage() {
        LinearLayout page = newPage();
        addSection(page, Text.get(Text.SECTION_MOVEMENT));

        LinearLayout card = newCard(page);
        addToggleRow(card, Text.get(Text.SPEED_HACK), Mode.SPEED, prefs.speed(), "speed");

        addSection(page, Text.get(Text.SECTION_ACCOUNT));
        LinearLayout account = newCard(page);
        addGuestResetToggle(account);

        addSection(page, Text.get(Text.SECTION_SESSION));
        LinearLayout session = newCard(page);
        LinearLayout textBox = new LinearLayout(ctx);
        textBox.setOrientation(LinearLayout.VERTICAL);
        Ui.padDp(ctx, textBox, 12, 11, 12, 10);
        TextView title = Ui.medium(ctx, Text.get(Text.SESSION_CONTROL), 11.5f, Ui.TEXT);
        TextView sub = Ui.text(ctx, Text.get(Text.SESSION_SUBTITLE), 9.5f, Ui.TEXT2, false);
        sub.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(3);
        sub.setLayoutParams(subLp);
        textBox.addView(title);
        textBox.addView(sub);
        session.addView(textBox);

        TextView stop = Ui.medium(ctx, Text.get(Text.STOP_SESSION), 10.5f, Ui.RED);
        stop.setGravity(Gravity.CENTER);
        stop.setBackground(Ui.ripple(0x22F04438, Ui.rect(Ui.RED_SOFT, dp(12))));
        stop.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));
        stop.setOnClickListener(v -> {
            ctx.stopService(new Intent(ctx, OverlayService.class));
            Toast.makeText(ctx, Text.get(Text.SESSION_STOPPED), Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams stopLp = (LinearLayout.LayoutParams) stop.getLayoutParams();
        stopLp.leftMargin = dp(12);
        stopLp.rightMargin = dp(12);
        stopLp.bottomMargin = dp(8);
        session.addView(stop);

        TextView reset = Ui.medium(ctx, Text.get(Text.RESET_SETTINGS), 10.5f, Ui.BLUE);
        reset.setGravity(Gravity.CENTER);
        reset.setBackground(Ui.ripple(0x1A4D6BFE, Ui.rect(Ui.WHITE, dp(12), Ui.BLUE, dp(1))));
        reset.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));
        reset.setOnClickListener(v -> {
            prefs.reset();
            prefs.syncToNative();
            rebuildPanel();
            Toast.makeText(ctx, Text.get(Text.SETTINGS_RESET), Toast.LENGTH_SHORT).show();
            if (ctx instanceof OverlayService) {
                ((OverlayService) ctx).restartSession();
            }
        });
        LinearLayout.LayoutParams resetLp = (LinearLayout.LayoutParams) reset.getLayoutParams();
        resetLp.leftMargin = dp(12);
        resetLp.rightMargin = dp(12);
        resetLp.bottomMargin = dp(12);
        session.addView(reset);

        return page;
    }

    private void addSection(LinearLayout page, String label) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(4);
        lp.bottomMargin = dp(6);
        lp.leftMargin = dp(3);
        lp.rightMargin = dp(3);
        row.setLayoutParams(lp);

        TextView title = Ui.medium(ctx, label, 8.5f, Ui.BLUE);
        title.setLetterSpacing(0.18f);
        row.addView(title);

        View line = new View(ctx);
        line.setBackgroundColor(0xFFDCE5FF);
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(0, dp(1), 1f);
        lineLp.leftMargin = dp(8);
        line.setLayoutParams(lineLp);
        row.addView(line);

        page.addView(row);
    }

    private LinearLayout newCard(LinearLayout page) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Ui.rect(Ui.WHITE, dp(16), Ui.BORDER, dp(1)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        card.setLayoutParams(lp);
        page.addView(card);
        return card;
    }

    private void addRowDivider(LinearLayout card) {
        if (card.getChildCount() == 0) return;
        View divider = new View(ctx);
        divider.setBackgroundColor(0xFFF0F3F8);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lp.leftMargin = dp(12);
        divider.setLayoutParams(lp);
        card.addView(divider);
    }

    private void addToggleRow(LinearLayout card, String label, int mode, boolean on, String key) {
        addRowDivider(card);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        Ui.padDp(ctx, row, 12, 10, 12, 10);

        TextView text = Ui.medium(ctx, label, 11.5f, Ui.TEXT);
        text.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(text);

        DcSwitch sw = new DcSwitch(ctx, on);
        sw.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(24)));
        sw.setListener(value -> {
            prefs.setBool(key, value);
            NativeBridge.setToggle(mode, value);
        });
        row.addView(sw);

        card.addView(row);
    }

    private void addGuestResetToggle(LinearLayout card) {
        addRowDivider(card);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        Ui.padDp(ctx, row, 12, 10, 12, 10);

        TextView label = Ui.medium(ctx, Text.get(Text.RESET_GUEST_TOGGLE), 11.5f, Ui.TEXT);
        label.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(label);

        DcSwitch sw = new DcSwitch(ctx, false);
        sw.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(24)));

        final boolean[] running = {false};

        sw.setListener(value -> {
            if (running[0] || !value) return;

            running[0] = true;
            sw.setEnabled(false);
            label.setText(Text.get(Text.RESET_GUEST_BUSY));
            Toast.makeText(ctx, Text.get(Text.RESET_GUEST_STARTED), Toast.LENGTH_LONG).show();

            new Thread(() -> {
                boolean ok;
                try {
                    ok = RootManager.resetGuestAccount();
                } catch (Throwable t) {
                    ok = false;
                }
                final boolean result = ok;
                handler.post(() -> {
                    running[0] = false;
                    Toast.makeText(ctx,
                            Text.get(result ? Text.RESET_GUEST_DONE : Text.RESET_GUEST_FAILED),
                            Toast.LENGTH_LONG).show();
                    if (rootFrame != null) rebuildPanel();
                });
            }, "dc-reset-guest").start();
        });

        row.setOnClickListener(v -> {
            if (!running[0]) sw.performClick();
        });

        row.addView(sw);
        card.addView(row);
    }

    private void addBoneSelector(LinearLayout card) {
        addRowDivider(card);

        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        Ui.padDp(ctx, box, 12, 10, 12, 10);

        LinearLayout top = new LinearLayout(ctx);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = Ui.medium(ctx, Text.get(Text.AIM_BONE), 11.5f, Ui.TEXT);
        label.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(label);

        final int initial = Math.max(0, Math.min(2, prefs.aimBone()));
        TextView value = Ui.pill(ctx, boneName(initial), Ui.BLUE, Ui.BLUE_SOFT, 8);
        top.addView(value);
        box.addView(top);

        LinearLayout choices = new LinearLayout(ctx);
        choices.setOrientation(LinearLayout.HORIZONTAL);
        choices.setBackground(Ui.rect(0xFFF1F4F9, dp(12)));
        Ui.padDp(ctx, choices, 3, 3, 3, 3);
        LinearLayout.LayoutParams choicesLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        choicesLp.topMargin = dp(8);
        choices.setLayoutParams(choicesLp);

        TextView[] options = new TextView[3];
        for (int i = 0; i < 3; i++) {
            final int index = i;
            TextView option = Ui.medium(ctx, boneName(i), 10.5f, Ui.TEXT2);
            option.setGravity(Gravity.CENTER);
            option.setLayoutParams(new LinearLayout.LayoutParams(0, dp(30), 1f));
            option.setOnClickListener(v -> {
                prefs.setInt("aim_bone", index);
                NativeBridge.setValue(Mode.AIM_BONE, index);
                value.setText(boneName(index));
                for (int j = 0; j < options.length; j++) {
                    styleBoneOption(options[j], j == index);
                }
            });
            options[i] = option;
            choices.addView(option);
        }
        box.addView(choices);
        card.addView(box);

        for (int i = 0; i < 3; i++) {
            styleBoneOption(options[i], i == initial);
        }
    }

    private String boneName(int bone) {
        if (bone == 1) return Text.get(Text.BONE_NECK);
        if (bone == 2) return Text.get(Text.BONE_HEAD);
        return Text.get(Text.BONE_CHEST);
    }

    private void styleBoneOption(TextView option, boolean selected) {
        option.setTextColor(selected ? Ui.WHITE : Ui.TEXT2);
        option.setBackground(selected
                ? Ui.rect(Ui.BLUE, dp(9))
                : Ui.rect(Color.TRANSPARENT, dp(9)));
    }

    private void addSliderRow(LinearLayout card, String label, int mode, int min, int max,
                              int value, String unit, String key, boolean showSwatch) {
        addRowDivider(card);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        Ui.padDp(ctx, row, 12, 10, 12, 8);

        LinearLayout top = new LinearLayout(ctx);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView text = Ui.medium(ctx, label, 11.5f, Ui.TEXT);
        text.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(text);

        View swatch = null;
        if (showSwatch) {
            swatch = new View(ctx);
            GradientDrawable sg = new GradientDrawable();
            sg.setShape(GradientDrawable.OVAL);
            sg.setColor(colorForIndex(value));
            swatch.setBackground(sg);
            LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(dp(12), dp(12));
            swLp.rightMargin = dp(6);
            swatch.setLayoutParams(swLp);
            top.addView(swatch);
        }

        TextView valueView = Ui.pill(ctx, value + unit, Ui.BLUE, Ui.BLUE_SOFT, 8);
        top.addView(valueView);
        row.addView(top);

        SeekBar seek = new SeekBar(ctx);
        seek.setMax(max - min);
        seek.setProgress(Math.max(0, Math.min(max - min, value - min)));
        seek.setSplitTrack(false);
        seek.setPadding(0, dp(2), 0, 0);
        seek.setProgressTintList(ColorStateList.valueOf(Ui.BLUE));
        seek.setThumbTintList(ColorStateList.valueOf(Ui.BLUE));
        seek.setProgressBackgroundTintList(ColorStateList.valueOf(0xFFE4E9F2));
        LinearLayout.LayoutParams seekLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(30));
        seek.setLayoutParams(seekLp);

        final View swatchFinal = swatch;
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int v = min + progress;
                valueView.setText(v + unit);
                if (swatchFinal != null) {
                    GradientDrawable sg = new GradientDrawable();
                    sg.setShape(GradientDrawable.OVAL);
                    sg.setColor(colorForIndex(v));
                    swatchFinal.setBackground(sg);
                }
                if (fromUser) {
                    prefs.setInt(key, v);
                    NativeBridge.setValue(mode, v);
                }
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        row.addView(seek);
        card.addView(row);
    }

    private void setTab(int index) {
        if (tabViews == null || tabPages == null) return;
        currentTab = index;

        for (int i = 0; i < tabViews.length; i++) {
            boolean selected = i == index;
            tabViews[i].setTextColor(selected ? Ui.WHITE : Ui.TEXT2);
            tabViews[i].setBackground(selected
                    ? Ui.rect(Ui.BLUE, dp(10))
                    : Ui.rect(Color.TRANSPARENT, dp(10)));
            tabPages[i].setVisibility(selected ? View.VISIBLE : View.GONE);
        }
        if (scrollView != null) scrollView.scrollTo(0, 0);
    }

    private void openPanel() {
        if (panelOpen) return;
        panelOpen = true;
        bubbleX = params.x;
        bubbleY = params.y;

        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(dm);
        int screenW = dm.widthPixels;
        int screenH = dm.heightPixels;
        int maxX = Math.max(dp(8), screenW - dp(PANEL_WIDTH_DP) - dp(8));
        int maxY = Math.max(dp(8), screenH - dp(370));

        params.x = clamp(params.x, dp(8), maxX);
        params.y = clamp(params.y, dp(8), maxY);

        bubbleView.setVisibility(View.GONE);
        panelView.setVisibility(View.VISIBLE);
        if (scrollView != null) scrollView.scrollTo(0, 0);
        wm.updateViewLayout(rootFrame, params);
    }

    private void closePanel() {
        if (!panelOpen) return;
        panelOpen = false;

        panelView.setVisibility(View.GONE);
        bubbleView.setVisibility(View.VISIBLE);
        params.x = bubbleX;
        params.y = bubbleY;
        wm.updateViewLayout(rootFrame, params);
    }

    private void rebuildPanel() {
        if (rootFrame == null) return;
        rootFrame.removeView(panelView);
        panelView = buildPanel();
        panelView.setVisibility(panelOpen ? View.VISIBLE : View.GONE);
        rootFrame.addView(panelView);
        setTab(currentTab);
    }

    private void startStatusLoop() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int[] s = NativeBridge.status();
                updateStatus(s);
                handler.postDelayed(this, 700);
            }
        }, 500);
    }

    public void updateStatus(int[] s) {
        connected = s[0] == 1;
        gameReady = s[1] == 1;
        boolean inMatch = s[2] == 1;
        int players = s[3];

        int dotColor;
        String text;
        int textColor;

        if (!connected) {
            dotColor = Ui.AMBER;
            text = Text.get(Text.CONNECTING);
            textColor = Ui.TEXT3;
        } else if (!gameReady) {
            dotColor = Ui.AMBER;
            text = Text.get(Text.WAITING_GAME);
            textColor = Ui.AMBER;
        } else if (!inMatch) {
            dotColor = Ui.GREEN;
            text = Text.get(Text.GAME_READY);
            textColor = Ui.GREEN;
        } else {
            dotColor = Ui.GREEN;
            text = Text.get(Text.IN_MATCH) + players + Text.get(Text.PLAYERS_SUFFIX);
            textColor = Ui.GREEN;
        }

        if (statusDot != null) {
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(dotColor);
            statusDot.setBackground(d);
        }
        if (statusText != null) {
            statusText.setText(text);
            statusText.setTextColor(textColor);
        }
        if (bubbleView != null) {
            bubbleView.setState(connected, gameReady);
        }
    }

    public void setConnected(boolean value) {
        connected = value;
        if (statusText != null && !value) {
            statusText.setText(Text.get(Text.CONNECTING));
            statusText.setTextColor(Ui.TEXT3);
        }
        if (bubbleView != null) bubbleView.setState(value, gameReady);
    }

    public void destroy() {
        handler.removeCallbacksAndMessages(null);
        if (rootFrame != null) {
            try { wm.removeView(rootFrame); } catch (Throwable ignored) {}
            rootFrame = null;
        }
    }

    private int dp(float v) {
        return Ui.dp(ctx, v);
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }

    private static int colorForIndex(int index) {
        int[] colors = {
                0xFF4D6BFE, 0xFFFFFFFF, 0xFF00E5FF, 0xFF00FF87, 0xFFFFD60A,
                0xFFFF4B6E, 0xFFFF00C8, 0xFFFF9100, 0xFFB4BECD, 0xFF141923
        };
        return colors[Math.max(0, Math.min(9, index))];
    }

    // ------------------------------------------------------------------
    // Drag handling
    // ------------------------------------------------------------------
    private class DragTouch implements View.OnTouchListener {
        private int startX, startY;
        private float downRawX, downRawY;
        private boolean moved;

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startX = params.x;
                    startY = params.y;
                    downRawX = e.getRawX();
                    downRawY = e.getRawY();
                    moved = false;
                    v.setPressed(true);
                    return true;

                case MotionEvent.ACTION_MOVE:
                    int dx = (int) (e.getRawX() - downRawX);
                    int dy = (int) (e.getRawY() - downRawY);
                    if (!moved && (Math.abs(dx) > dp(4) || Math.abs(dy) > dp(4))) {
                        moved = true;
                    }
                    if (moved) {
                        DisplayMetrics dm = new DisplayMetrics();
                        wm.getDefaultDisplay().getRealMetrics(dm);
                        int width = rootFrame != null ? rootFrame.getWidth() : dp(PANEL_WIDTH_DP);
                        int height = rootFrame != null ? rootFrame.getHeight() : dp(420);
                        params.x = clamp(startX + dx, -dp(4), Math.max(0, dm.widthPixels - width + dp(4)));
                        params.y = clamp(startY + dy, -dp(4), Math.max(0, dm.heightPixels - height + dp(4)));
                        wm.updateViewLayout(rootFrame, params);
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false);
                    if (!moved) v.performClick();
                    return true;
            }
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Floating bubble
    // ------------------------------------------------------------------
    private class WhaleBubbleView extends View {
        private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Drawable mark;

        private boolean connected;
        private boolean gameReady;

        WhaleBubbleView(Context context) {
            super(context);
            setLayerType(LAYER_TYPE_SOFTWARE, null);
            setClickable(true);

            bgPaint.setStyle(Paint.Style.FILL);
            bgPaint.setColor(Ui.BLUE);
            bgPaint.setShadowLayer(dp(8), 0, dp(3), 0x33000000);

            mark = context.getDrawable(R.drawable.ic_deepseek_mark);

            dotPaint.setStyle(Paint.Style.FILL);
            dotPaint.setColor(Ui.RED);

            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setColor(Ui.WHITE);
            ringPaint.setStrokeWidth(dp(1.6f));
        }

        void setState(boolean connected, boolean gameReady) {
            if (this.connected == connected && this.gameReady == gameReady) return;
            this.connected = connected;
            this.gameReady = gameReady;
            dotPaint.setColor(!connected ? Ui.RED : (gameReady ? Ui.GREEN : Ui.AMBER));
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth();
            float h = getHeight();
            float cx = w / 2f;
            float cy = h / 2f;
            float r = Math.min(w, h) / 2f - dp(4);

            c.drawCircle(cx, cy, r, bgPaint);

            if (mark != null) {
                float mw = r * 1.12f;
                float mh = mw * 41.3595f / 55.6128f;
                mark.setBounds(
                        Math.round(cx - mw / 2f),
                        Math.round(cy - mh / 2f),
                        Math.round(cx + mw / 2f),
                        Math.round(cy + mh / 2f));
                mark.draw(c);
            }

            float dotX = cx + r * 0.72f;
            float dotY = cy + r * 0.72f;
            float dotR = dp(4.5f);
            c.drawCircle(dotX, dotY, dotR + dp(1.8f), ringPaint);
            c.drawCircle(dotX, dotY, dotR, dotPaint);
        }
    }

    // ------------------------------------------------------------------
    // iOS-style switch
    // ------------------------------------------------------------------
    private class DcSwitch extends View {
        private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean on;
        private float pos;
        private ValueAnimator animator;
        private OnChanged listener;

        interface OnChanged {
            void onChanged(boolean value);
        }

        DcSwitch(Context context, boolean initial) {
            super(context);
            on = initial;
            pos = initial ? 1f : 0f;
            setClickable(true);
            setFocusable(true);
        }

        void setListener(OnChanged l) {
            listener = l;
        }

        void setOn(boolean value) {
            if (on == value) return;
            on = value;
            if (animator != null) animator.cancel();
            animator = ValueAnimator.ofFloat(pos, value ? 1f : 0f);
            animator.setDuration(130);
            animator.addUpdateListener(a -> {
                pos = (float) a.getAnimatedValue();
                invalidate();
            });
            animator.start();
            if (listener != null) listener.onChanged(value);
        }

        @Override
        public boolean performClick() {
            super.performClick();
            setOn(!on);
            return true;
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth();
            float h = getHeight();
            float radius = h / 2f;

            trackPaint.setColor(on ? Ui.BLUE : 0xFFE4E9F2);
            c.drawRoundRect(0, 0, w, h, radius, radius, trackPaint);

            float thumbR = radius - dp(3);
            float pad = dp(3);
            float x = pad + thumbR + pos * (w - pad * 2 - thumbR * 2);
            thumbPaint.setColor(0xFF000000);
            thumbPaint.setAlpha(20);
            c.drawCircle(x, h / 2f + dp(0.8f), thumbR, thumbPaint);
            thumbPaint.setColor(Ui.WHITE);
            thumbPaint.setAlpha(255);
            c.drawCircle(x, h / 2f, thumbR, thumbPaint);
        }
    }
}
