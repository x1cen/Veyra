package org.veyra.client;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;

public class VeyraLogoDrawable extends Drawable {

    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final String text;
    private final float textWidth;
    private final int intrinsicHeight;

    public VeyraLogoDrawable() {
        this("Veyra", 22);
    }

    public VeyraLogoDrawable(String text) {
        this(text, 22);
    }

    public VeyraLogoDrawable(String text, int textSizeDp) {
        this.text = text != null ? text : "Veyra";
        textPaint.setTypeface(AndroidUtilities.bold());
        textPaint.setTextSize(AndroidUtilities.dp(textSizeDp));
        textPaint.setColor(0xffffffff);
        textWidth = textPaint.measureText(this.text);
        intrinsicHeight = AndroidUtilities.dp(textSizeDp + 2);
    }

    @Override
    public int getIntrinsicWidth() {
        return (int) Math.ceil(textWidth);
    }

    @Override
    public int getIntrinsicHeight() {
        return intrinsicHeight;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float baseline = bounds.centerY() - (fm.descent + fm.ascent) / 2f;
        canvas.drawText(text, bounds.left, baseline, textPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        textPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        textPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
