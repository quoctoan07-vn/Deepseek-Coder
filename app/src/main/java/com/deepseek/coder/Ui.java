package com.deepseek.coder;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {
    public static final int BLUE        = 0xFF4D6BFE;
    public static final int BLUE_DARK   = 0xFF3452D9;
    public static final int BLUE_SOFT   = 0xFFE9EEFF;
    public static final int BLUE_SOFT2  = 0xFFF2F5FF;
    public static final int WHITE       = 0xFFFFFFFF;
    public static final int BG          = 0xFFF8FAFD;
    public static final int SURFACE     = 0xFFF6F8FC;
    public static final int TEXT        = 0xFF111827;
    public static final int TEXT2       = 0xFF667085;
    public static final int TEXT3       = 0xFF98A2B3;
    public static final int BORDER      = 0xFFE4E9F2;
    public static final int GREEN       = 0xFF12B76A;
    public static final int GREEN_SOFT  = 0xFFE8F8F0;
    public static final int RED         = 0xFFF04438;
    public static final int RED_SOFT    = 0xFFFEECEB;
    public static final int AMBER       = 0xFFF79009;
    public static final int AMBER_SOFT  = 0xFFFFF4E5;

    private Ui() {}

    public static int dp(Context c, float v) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics());
    }

    public static int sp(Context c, float v) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, v, c.getResources().getDisplayMetrics());
    }

    public static GradientDrawable rect(int color, float radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusPx);
        return d;
    }

    public static GradientDrawable rect(int color, float radiusPx, int strokeColor, int strokeWidthPx) {
        GradientDrawable d = rect(color, radiusPx);
        d.setStroke(strokeWidthPx, strokeColor);
        return d;
    }

    public static GradientDrawable gradient(int[] colors, GradientDrawable.Orientation orientation, float radiusPx) {
        GradientDrawable d = new GradientDrawable(orientation, colors);
        d.setCornerRadius(radiusPx);
        return d;
    }

    public static RippleDrawable ripple(int maskColor, Drawable content) {
        return new RippleDrawable(ColorStateList.valueOf(maskColor), content, null);
    }

    public static TextView text(Context c, String value, float sizeSp, int color, boolean bold) {
        TextView tv = new TextView(c);
        tv.setText(value);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        tv.setTextColor(color);
        tv.setIncludeFontPadding(false);
        tv.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return tv;
    }

    public static TextView medium(Context c, String value, float sizeSp, int color) {
        TextView tv = new TextView(c);
        tv.setText(value);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        tv.setTextColor(color);
        tv.setIncludeFontPadding(false);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return tv;
    }

    public static TextView pill(Context c, String value, int textColor, int bgColor, float radiusDp) {
        TextView tv = medium(c, value, 9.5f, textColor);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(dp(c, 9), dp(c, 4), dp(c, 9), dp(c, 4));
        tv.setBackground(rect(bgColor, dp(c, radiusDp)));
        return tv;
    }

    public static void pad(View v, int l, int t, int r, int b) {
        v.setPadding(l, t, r, b);
    }

    public static void padDp(Context c, View v, float l, float t, float r, float b) {
        v.setPadding(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
    }

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams lpWeight(float weight) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
    }

    public static LinearLayout.LayoutParams withTop(LinearLayout.LayoutParams p, Context c, float topDp) {
        p.topMargin = dp(c, topDp);
        return p;
    }

    public static LinearLayout.LayoutParams withBottom(LinearLayout.LayoutParams p, Context c, float bottomDp) {
        p.bottomMargin = dp(c, bottomDp);
        return p;
    }

    public static View spacer(Context c, int widthDp, int heightDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(c, widthDp), dp(c, heightDp)));
        return v;
    }

    /**
     * A rounded square containing the DeepSeek whale mark.
     * Pass 0 as tintColor to keep the mark white on a blue background.
     */
    public static View logo(Context c, int sizeDp, int bgColor, int tintColor, float radiusDp) {
        FrameLayout box = new FrameLayout(c);
        box.setBackground(rect(bgColor, dp(c, radiusDp)));

        ImageView mark = new ImageView(c);
        mark.setImageResource(R.drawable.ic_deepseek_mark);
        mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if (tintColor != 0) {
            mark.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);
        }

        int pad = Math.round(dp(c, sizeDp * 0.16f));
        FrameLayout.LayoutParams markLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        markLp.setMargins(pad, pad, pad, pad);
        box.addView(mark, markLp);

        box.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        return box;
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(BORDER);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return v;
    }
}
