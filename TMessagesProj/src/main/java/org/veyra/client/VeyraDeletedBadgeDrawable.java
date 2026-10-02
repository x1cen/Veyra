package org.veyra.client;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

/**
 * Veyra: a compact "DELETED" badge drawn identically to ScamDrawable.
 * Use instead of raw emoji. Renders as a rounded-rect outline label
 * with the localised "deleted" string inside.
 * Default colour: desaturated grey (#78909C) — override with setColor().
 */
public class VeyraDeletedBadgeDrawable extends Drawable {

    private final RectF rect = new RectF();
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private int textWidth;
    private String text;
    private int colorAlpha = 255;

    public VeyraDeletedBadgeDrawable(int textSizeSp) {
        textPaint.setTextSize(AndroidUtilities.dp(textSizeSp));
        textPaint.setTypeface(AndroidUtilities.bold());

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(AndroidUtilities.dp(1));

        // default: muted blue-grey
        setColor(0xFF78909C);

        refreshText();
    }

    public void refreshText() {
        text = LocaleController.getString("DeletedMessage", R.string.DeletedMessage).toUpperCase();
        textWidth = (int) Math.ceil(textPaint.measureText(text));
        invalidateSelf();
    }

    public void setColor(int color) {
        textPaint.setColor(color);
        borderPaint.setColor(color);
        colorAlpha = Color.alpha(color);
    }

    @Override
    public void setAlpha(int alpha) {
        int local = (int) (colorAlpha * (alpha / 255f));
        borderPaint.setAlpha(local);
        textPaint.setAlpha(local);
    }

    @Override
    public int getIntrinsicWidth() {
        return textWidth + AndroidUtilities.dp(10); // 5dp padding each side
    }

    @Override
    public int getIntrinsicHeight() {
        return AndroidUtilities.dp(16);
    }

    @Override
    public void draw(Canvas canvas) {
        android.graphics.Rect b = getBounds();
        if (b == null || b.isEmpty()) return;

        float cx = b.centerX();
        float cy = b.centerY();
        float halfW = getIntrinsicWidth() / 2f;
        float halfH = getIntrinsicHeight() / 2f;
        float radius = AndroidUtilities.dp(2);

        rect.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH);
        canvas.drawRoundRect(rect, radius, radius, borderPaint);

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = cy - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(text, cx - textWidth / 2f, textY, textPaint);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        textPaint.setColorFilter(cf);
        borderPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }
}
