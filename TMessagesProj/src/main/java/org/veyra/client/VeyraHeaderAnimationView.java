/*
 * Veyra Project - Header Title Animation Engine
 * Advanced particle & canvas animation effects for DialogsActivity action bar
 */
package org.veyra.client;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;

import java.util.ArrayList;
import java.util.Random;

public class VeyraHeaderAnimationView extends View {

    public static final int MODE_OFF = 0;
    public static final int MODE_LIGHTNING = 1;
    public static final int MODE_RAIN = 2;
    public static final int MODE_FIREWORKS = 3;
    public static final int MODE_METEORS = 4;
    public static final int MODE_MATRIX = 5;
    public static final int MODE_GLITCH = 6;
    public static final int MODE_SNOW = 7;
    public static final int MODE_FIRE = 8;
    public static final int MODE_AURORA = 9;

    private int currentMode = MODE_OFF;
    private boolean isRunning = false;
    private final Random random = new Random();
    private long lastFrameTime = 0;

    // Paints
    private final Paint mainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint secondaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ==================== 1. LIGHTNING STATE ====================
    private static class LightningBolt {
        final ArrayList<PointF> points = new ArrayList<>();
        final ArrayList<ArrayList<PointF>> branches = new ArrayList<>();
        float alpha = 0f;
        long lastStrikeTime = 0;
    }
    private final LightningBolt lightning = new LightningBolt();
    private final Path lightningPath = new Path();
    private final Path branchPath = new Path();

    // ==================== 2. RAIN STATE ====================
    private static class RainDrop {
        float x, y;
        float speed;
        float length;
        float alpha;
    }
    private static class SplashRipple {
        float x, y;
        float radius;
        float maxRadius;
        float alpha;
    }
    private RainDrop[] rainDrops;
    private SplashRipple[] splashRipples;

    // ==================== 3. FIREWORKS / SPARKLERS STATE ====================
    private static class SparkleParticle {
        float x, y;
        float vx, vy;
        float alpha;
        float decay;
        int color;
        float size;
    }
    private static class FireworkRocket {
        float x, y;
        float targetY;
        float vy;
        boolean exploded;
        int color;
    }
    private SparkleParticle[] sparklePool;
    private FireworkRocket[] rockets;
    private long nextFireworkTime = 0;

    // ==================== 4. METEORS / SHOOTING STARS STATE ====================
    private static class Meteor {
        float x, y;
        float speed;
        float length;
        float alpha;
        boolean active;
        long nextSpawn;
    }
    private Meteor[] meteors;

    // ==================== 5. MATRIX DIGITAL RAIN STATE ====================
    private static class MatrixColumn {
        float x;
        float y;
        float speed;
        char[] chars;
        long lastChange;
    }
    private MatrixColumn[] matrixColumns;
    private static final String MATRIX_CHARS = "0123456789ABCDEFｦｱｳｴｵｶｷｹｺｻｼｽｾｿﾀﾂﾃﾅﾆﾇﾈﾊﾋﾎﾏﾐﾑﾒﾓﾔﾕﾗﾘﾜ";

    // ==================== 6. GLITCH STATE ====================
    private long nextGlitchTime = 0;
    private boolean isGlitching = false;
    private float glitchSliceY = 0f;
    private float glitchSliceHeight = 0f;
    private float glitchOffset = 0f;

    // ==================== 7. SNOW STATE ====================
    private static class Snowflake {
        float x, y;
        float radius;
        float speedY;
        float swayFreq;
        float swayAmp;
        float swayPhase;
        float alpha;
    }
    private Snowflake[] snowflakes;

    // ==================== 8. FIRE EMBERS STATE ====================
    private static class Ember {
        float x, y;
        float vx, vy;
        float size;
        float alpha;
        float life;
        float maxLife;
        int startColor;
    }
    private Ember[] embers;

    // ==================== 9. AURORA BOREALIS STATE ====================
    private float auroraPhase = 0f;
    private final Path auroraPath1 = new Path();
    private final Path auroraPath2 = new Path();

    public VeyraHeaderAnimationView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setWillNotDraw(false);

        // Preallocate Rain
        rainDrops = new RainDrop[45];
        for (int i = 0; i < rainDrops.length; i++) {
            rainDrops[i] = new RainDrop();
            resetRainDrop(rainDrops[i], true);
        }
        splashRipples = new SplashRipple[15];
        for (int i = 0; i < splashRipples.length; i++) {
            splashRipples[i] = new SplashRipple();
            splashRipples[i].alpha = 0f;
        }

        // Preallocate Fireworks
        sparklePool = new SparkleParticle[120];
        for (int i = 0; i < sparklePool.length; i++) {
            sparklePool[i] = new SparkleParticle();
            sparklePool[i].alpha = 0f;
        }
        rockets = new FireworkRocket[3];
        for (int i = 0; i < rockets.length; i++) {
            rockets[i] = new FireworkRocket();
            rockets[i].exploded = true;
        }

        // Preallocate Meteors
        meteors = new Meteor[5];
        for (int i = 0; i < meteors.length; i++) {
            meteors[i] = new Meteor();
            resetMeteor(meteors[i], true);
        }

        // Preallocate Snow
        snowflakes = new Snowflake[50];
        for (int i = 0; i < snowflakes.length; i++) {
            snowflakes[i] = new Snowflake();
            resetSnowflake(snowflakes[i], true);
        }

        // Preallocate Fire Embers
        embers = new Ember[40];
        for (int i = 0; i < embers.length; i++) {
            embers[i] = new Ember();
            resetEmber(embers[i], true);
        }
    }

    public void setMode(int mode) {
        if (currentMode != mode) {
            currentMode = mode;
            if (currentMode == MODE_OFF) {
                pause();
                setVisibility(GONE);
            } else {
                setVisibility(VISIBLE);
                resume();
            }
            invalidate();
        }
    }

    public int getMode() {
        return currentMode;
    }

    public void resume() {
        if (!isRunning && currentMode != MODE_OFF) {
            isRunning = true;
            lastFrameTime = SystemClock.elapsedRealtime();
            postInvalidateOnAnimation();
        }
    }

    public void pause() {
        isRunning = false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (currentMode != MODE_OFF) {
            resume();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        pause();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            initMatrixColumns(w);
        }
    }

    private void initMatrixColumns(int w) {
        int colWidth = AndroidUtilities.dp(12);
        int count = Math.max(1, w / colWidth);
        matrixColumns = new MatrixColumn[count];
        for (int i = 0; i < count; i++) {
            MatrixColumn col = new MatrixColumn();
            col.x = i * colWidth + AndroidUtilities.dp(3);
            col.y = -random.nextInt(getHeight() > 0 ? getHeight() : 150);
            col.speed = AndroidUtilities.dp(1.5f + random.nextFloat() * 2.2f);
            col.chars = new char[8];
            for (int c = 0; c < col.chars.length; c++) {
                col.chars[c] = MATRIX_CHARS.charAt(random.nextInt(MATRIX_CHARS.length()));
            }
            col.lastChange = SystemClock.elapsedRealtime();
            matrixColumns[i] = col;
        }
    }

    // ==================== DRAW DISPATCHER ====================
    @Override
    protected void onDraw(Canvas canvas) {
        if (currentMode == MODE_OFF || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }

        long now = SystemClock.elapsedRealtime();
        float dt = lastFrameTime > 0 ? Math.min((now - lastFrameTime) / 1000f, 0.1f) : 0.016f;
        lastFrameTime = now;

        switch (currentMode) {
            case MODE_LIGHTNING:
                drawLightning(canvas, dt, now);
                break;
            case MODE_RAIN:
                drawRain(canvas, dt);
                break;
            case MODE_FIREWORKS:
                drawFireworks(canvas, dt, now);
                break;
            case MODE_METEORS:
                drawMeteors(canvas, dt, now);
                break;
            case MODE_MATRIX:
                drawMatrix(canvas, dt, now);
                break;
            case MODE_GLITCH:
                drawGlitch(canvas, dt, now);
                break;
            case MODE_SNOW:
                drawSnow(canvas, dt);
                break;
            case MODE_FIRE:
                drawFire(canvas, dt);
                break;
            case MODE_AURORA:
                drawAurora(canvas, dt);
                break;
        }

        if (isRunning) {
            postInvalidateOnAnimation();
        }
    }

    // ==================== 1. LIGHTNING ENGINE ====================
    private void drawLightning(Canvas canvas, float dt, long now) {
        int w = getWidth();
        int h = getHeight();

        // Decay strike alpha
        if (lightning.alpha > 0) {
            lightning.alpha = Math.max(0f, lightning.alpha - dt * 3.5f);
        }

        // Trigger new strike
        if (now - lightning.lastStrikeTime > (800 + random.nextInt(2200))) {
            lightning.lastStrikeTime = now;
            lightning.alpha = 1.0f;
            generateLightningBolt(w, h);
        }

        if (lightning.alpha > 0.01f && !lightning.points.isEmpty()) {
            int alphaInt = (int) (lightning.alpha * 255);

            // Ambient background flash
            mainPaint.setStyle(Paint.Style.FILL);
            mainPaint.setColor(Color.argb((int) (lightning.alpha * 45), 180, 220, 255));
            canvas.drawRect(0, 0, w, h, mainPaint);

            // Main bolt outer glow
            glowPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setStrokeWidth(AndroidUtilities.dp(4.5f));
            glowPaint.setColor(Color.argb((int) (alphaInt * 0.6f), 64, 196, 255));
            glowPaint.setStrokeCap(Paint.Cap.ROUND);
            glowPaint.setStrokeJoin(Paint.Join.ROUND);
            canvas.drawPath(lightningPath, glowPaint);

            // Main bolt inner core (white/electric)
            mainPaint.setStyle(Paint.Style.STROKE);
            mainPaint.setStrokeWidth(AndroidUtilities.dp(1.8f));
            mainPaint.setColor(Color.argb(alphaInt, 255, 255, 255));
            mainPaint.setStrokeCap(Paint.Cap.ROUND);
            mainPaint.setStrokeJoin(Paint.Join.ROUND);
            canvas.drawPath(lightningPath, mainPaint);

            // Secondary branches
            secondaryPaint.setStyle(Paint.Style.STROKE);
            secondaryPaint.setStrokeWidth(AndroidUtilities.dp(1.0f));
            secondaryPaint.setColor(Color.argb((int) (alphaInt * 0.75f), 130, 210, 255));
            canvas.drawPath(branchPath, secondaryPaint);
        }
    }

    private void generateLightningBolt(int w, int h) {
        lightning.points.clear();
        lightning.branches.clear();
        lightningPath.reset();
        branchPath.reset();

        float startX = w * (0.15f + random.nextFloat() * 0.7f);
        float currentX = startX;
        float currentY = 0f;

        lightning.points.add(new PointF(currentX, currentY));
        lightningPath.moveTo(currentX, currentY);

        int steps = 7 + random.nextInt(5);
        float dy = (float) h / steps;

        for (int i = 0; i < steps; i++) {
            currentY += dy;
            currentX += (random.nextFloat() - 0.5f) * AndroidUtilities.dp(35);
            lightning.points.add(new PointF(currentX, currentY));
            lightningPath.lineTo(currentX, currentY);

            // Chance to create a branch
            if (random.nextFloat() < 0.45f && i < steps - 1) {
                float branchX = currentX;
                float branchY = currentY;
                branchPath.moveTo(branchX, branchY);
                int branchSteps = 2 + random.nextInt(3);
                for (int b = 0; b < branchSteps; b++) {
                    branchX += (random.nextBoolean() ? 1 : -1) * (AndroidUtilities.dp(10 + random.nextInt(15)));
                    branchY += AndroidUtilities.dp(6 + random.nextInt(10));
                    branchPath.lineTo(branchX, branchY);
                }
            }
        }
    }

    // ==================== 2. RAIN ENGINE ====================
    private void drawRain(Canvas canvas, float dt) {
        int w = getWidth();
        int h = getHeight();

        mainPaint.setStyle(Paint.Style.STROKE);
        mainPaint.setStrokeCap(Paint.Cap.ROUND);
        mainPaint.setColor(0x99A8D5E5);

        float slantX = AndroidUtilities.dp(3.5f);

        for (RainDrop drop : rainDrops) {
            drop.y += drop.speed * dt;
            drop.x += (drop.speed * dt * 0.22f);

            if (drop.y > h - AndroidUtilities.dp(4)) {
                // Trigger splash ripple at the bottom
                triggerSplash(drop.x, h - AndroidUtilities.dp(2));
                resetRainDrop(drop, false);
            }

            mainPaint.setStrokeWidth(AndroidUtilities.dp(1.1f));
            mainPaint.setAlpha((int) (drop.alpha * 255));
            canvas.drawLine(drop.x, drop.y, drop.x - slantX, drop.y - drop.length, mainPaint);
        }

        // Draw splash ripples
        secondaryPaint.setStyle(Paint.Style.STROKE);
        secondaryPaint.setStrokeWidth(AndroidUtilities.dp(1.0f));
        for (SplashRipple ripple : splashRipples) {
            if (ripple.alpha > 0.01f) {
                ripple.radius += AndroidUtilities.dp(18f) * dt;
                ripple.alpha = Math.max(0f, ripple.alpha - dt * 2.8f);
                secondaryPaint.setColor(Color.argb((int) (ripple.alpha * 180), 168, 213, 229));
                canvas.drawOval(
                        ripple.x - ripple.radius,
                        ripple.y - ripple.radius * 0.35f,
                        ripple.x + ripple.radius,
                        ripple.y + ripple.radius * 0.35f,
                        secondaryPaint
                );
            }
        }
    }

    private void resetRainDrop(RainDrop drop, boolean randomY) {
        drop.x = random.nextInt(getWidth() > 0 ? getWidth() + 100 : 800) - 50;
        drop.y = randomY ? random.nextInt(getHeight() > 0 ? getHeight() : 200) : -AndroidUtilities.dp(10);
        drop.speed = AndroidUtilities.dp(280 + random.nextInt(200));
        drop.length = AndroidUtilities.dp(7 + random.nextInt(8));
        drop.alpha = 0.35f + random.nextFloat() * 0.45f;
    }

    private void triggerSplash(float x, float y) {
        for (SplashRipple ripple : splashRipples) {
            if (ripple.alpha <= 0.05f) {
                ripple.x = x;
                ripple.y = y;
                ripple.radius = AndroidUtilities.dp(2);
                ripple.maxRadius = AndroidUtilities.dp(8);
                ripple.alpha = 0.8f;
                break;
            }
        }
    }

    // ==================== 3. FIREWORKS / SPARKLERS ENGINE ====================
    private void drawFireworks(Canvas canvas, float dt, long now) {
        int w = getWidth();
        int h = getHeight();

        // Launch new rockets
        if (now > nextFireworkTime) {
            nextFireworkTime = now + 900 + random.nextInt(1200);
            for (FireworkRocket r : rockets) {
                if (r.exploded) {
                    r.x = w * (0.2f + random.nextFloat() * 0.6f);
                    r.y = h;
                    r.targetY = h * (0.2f + random.nextFloat() * 0.45f);
                    r.vy = -AndroidUtilities.dp(160 + random.nextInt(60));
                    r.exploded = false;
                    r.color = randomFireworkColor();
                    break;
                }
            }
        }

        // Update rockets
        mainPaint.setStyle(Paint.Style.FILL);
        for (FireworkRocket r : rockets) {
            if (!r.exploded) {
                r.y += r.vy * dt;
                mainPaint.setColor(r.color);
                canvas.drawCircle(r.x, r.y, AndroidUtilities.dp(2.2f), mainPaint);
                // Trail spark
                spawnSparkle(r.x, r.y, (random.nextFloat() - 0.5f) * 15f, 20f, 0.6f, r.color, 1.2f);
                if (r.y <= r.targetY) {
                    r.exploded = true;
                    explodeFirework(r.x, r.y, r.color);
                }
            }
        }

        // Update and draw sparkles
        for (SparkleParticle p : sparklePool) {
            if (p.alpha > 0.01f) {
                p.x += p.vx * dt;
                p.y += p.vy * dt;
                p.vy += AndroidUtilities.dp(55f) * dt; // gravity
                p.vx *= (1f - dt * 0.9f);             // drag
                p.alpha = Math.max(0f, p.alpha - p.decay * dt);

                mainPaint.setColor(p.color);
                mainPaint.setAlpha((int) (p.alpha * 255));
                canvas.drawCircle(p.x, p.y, AndroidUtilities.dp(p.size), mainPaint);
            }
        }
    }

    private int randomFireworkColor() {
        int[] palette = new int[]{
                0xFFFFD700, // Gold
                0xFFFF3D00, // Bright Orange-Red
                0xFF00E5FF, // Cyan
                0xFFFF007F, // Neon Pink
                0xFF00E676, // Neon Green
                0xFFD500F9  // Bright Violet
        };
        return palette[random.nextInt(palette.length)];
    }

    private void explodeFirework(float cx, float cy, int color) {
        int count = 24 + random.nextInt(12);
        for (int i = 0; i < count; i++) {
            float angle = (float) (random.nextFloat() * 2 * Math.PI);
            float speed = AndroidUtilities.dp(35 + random.nextInt(55));
            float vx = (float) Math.cos(angle) * speed;
            float vy = (float) Math.sin(angle) * speed;
            spawnSparkle(cx, cy, vx, vy, 1.0f, color, 1.8f);
        }
    }

    private void spawnSparkle(float x, float y, float vx, float vy, float alpha, int color, float size) {
        for (SparkleParticle p : sparklePool) {
            if (p.alpha <= 0.05f) {
                p.x = x;
                p.y = y;
                p.vx = vx;
                p.vy = vy;
                p.alpha = alpha;
                p.color = color;
                p.size = size;
                p.decay = 1.2f + random.nextFloat() * 1.0f;
                break;
            }
        }
    }

    // ==================== 4. METEORS ENGINE ====================
    private void drawMeteors(Canvas canvas, float dt, long now) {
        int w = getWidth();
        int h = getHeight();

        mainPaint.setStyle(Paint.Style.STROKE);
        mainPaint.setStrokeCap(Paint.Cap.ROUND);

        for (Meteor m : meteors) {
            if (!m.active) {
                if (now > m.nextSpawn) {
                    m.active = true;
                    m.x = w * (0.3f + random.nextFloat() * 0.8f);
                    m.y = -AndroidUtilities.dp(15);
                    m.speed = AndroidUtilities.dp(340 + random.nextInt(180));
                    m.length = AndroidUtilities.dp(35 + random.nextInt(25));
                    m.alpha = 0.9f;
                }
                continue;
            }

            m.x -= m.speed * dt;
            m.y += m.speed * dt * 0.55f;

            if (m.x < -m.length || m.y > h + m.length) {
                resetMeteor(m, false);
                continue;
            }

            // Draw glowing gradient trail
            LinearGradient gradient = new LinearGradient(
                    m.x, m.y,
                    m.x + m.length, m.y - m.length * 0.55f,
                    new int[]{Color.argb((int) (m.alpha * 255), 255, 255, 255), Color.argb((int) (m.alpha * 180), 0, 229, 255), Color.TRANSPARENT},
                    new float[]{0f, 0.35f, 1f},
                    Shader.TileMode.CLAMP
            );
            mainPaint.setShader(gradient);
            mainPaint.setStrokeWidth(AndroidUtilities.dp(2.4f));
            canvas.drawLine(m.x, m.y, m.x + m.length, m.y - m.length * 0.55f, mainPaint);
            mainPaint.setShader(null);

            // Glowing head
            secondaryPaint.setStyle(Paint.Style.FILL);
            secondaryPaint.setColor(Color.argb((int) (m.alpha * 255), 255, 255, 255));
            canvas.drawCircle(m.x, m.y, AndroidUtilities.dp(2.0f), secondaryPaint);
        }
    }

    private void resetMeteor(Meteor m, boolean init) {
        m.active = false;
        m.nextSpawn = SystemClock.elapsedRealtime() + (init ? random.nextInt(1500) : (600 + random.nextInt(1800)));
    }

    // ==================== 5. MATRIX DIGITAL RAIN ENGINE ====================
    private void drawMatrix(Canvas canvas, float dt, long now) {
        if (matrixColumns == null) return;
        int h = getHeight();

        mainPaint.setTextSize(AndroidUtilities.dp(10.5f));
        mainPaint.setStyle(Paint.Style.FILL);

        for (MatrixColumn col : matrixColumns) {
            col.y += col.speed * dt;
            if (col.y > h + AndroidUtilities.dp(50)) {
                col.y = -AndroidUtilities.dp(40);
                col.speed = AndroidUtilities.dp(1.5f + random.nextFloat() * 2.2f);
            }

            if (now - col.lastChange > 120) {
                col.lastChange = now;
                col.chars[random.nextInt(col.chars.length)] = MATRIX_CHARS.charAt(random.nextInt(MATRIX_CHARS.length()));
            }

            for (int i = 0; i < col.chars.length; i++) {
                float charY = col.y - i * AndroidUtilities.dp(11.5f);
                if (charY < -AndroidUtilities.dp(10) || charY > h + AndroidUtilities.dp(10)) continue;

                if (i == 0) {
                    // Lead character is bright neon white/green
                    mainPaint.setColor(0xFFE8F5E9);
                } else if (i == 1) {
                    mainPaint.setColor(0xFF00E676);
                } else {
                    int alpha = Math.max(10, 200 - (i * 26));
                    mainPaint.setColor(Color.argb(alpha, 0, 200, 83));
                }
                canvas.drawText(String.valueOf(col.chars[i]), col.x, charY, mainPaint);
            }
        }
    }

    // ==================== 6. CYBERPUNK GLITCH ENGINE ====================
    private void drawGlitch(Canvas canvas, float dt, long now) {
        int w = getWidth();
        int h = getHeight();

        if (now > nextGlitchTime) {
            isGlitching = true;
            nextGlitchTime = now + 400 + random.nextInt(1200);
            glitchSliceY = random.nextFloat() * h;
            glitchSliceHeight = AndroidUtilities.dp(4 + random.nextInt(14));
            glitchOffset = (random.nextBoolean() ? 1 : -1) * AndroidUtilities.dp(6 + random.nextInt(12));
        }

        if (isGlitching) {
            // Cyan split bar
            mainPaint.setStyle(Paint.Style.FILL);
            mainPaint.setColor(0x8000E5FF);
            canvas.drawRect(glitchOffset, glitchSliceY, w + glitchOffset, glitchSliceY + glitchSliceHeight, mainPaint);

            // Magenta split bar offset
            secondaryPaint.setStyle(Paint.Style.FILL);
            secondaryPaint.setColor(0x80FF0055);
            canvas.drawRect(-glitchOffset * 0.7f, glitchSliceY + AndroidUtilities.dp(2), w - glitchOffset * 0.7f, glitchSliceY + glitchSliceHeight + AndroidUtilities.dp(2), secondaryPaint);

            // Subtle scan lines
            glowPaint.setStyle(Paint.Style.STROKE);
            glowPaint.setStrokeWidth(AndroidUtilities.dp(1));
            glowPaint.setColor(0x40FFFFFF);
            for (float y = 0; y < h; y += AndroidUtilities.dp(3)) {
                canvas.drawLine(0, y, w, y, glowPaint);
            }

            isGlitching = false;
        }
    }

    // ==================== 7. WINTER SNOWFALL ENGINE ====================
    private void drawSnow(Canvas canvas, float dt) {
        int w = getWidth();
        int h = getHeight();

        mainPaint.setStyle(Paint.Style.FILL);

        for (Snowflake s : snowflakes) {
            s.y += s.speedY * dt;
            s.swayPhase += s.swayFreq * dt;
            float currentX = s.x + (float) Math.sin(s.swayPhase) * s.swayAmp;

            if (s.y > h + s.radius * 2) {
                resetSnowflake(s, false);
            }

            mainPaint.setColor(Color.argb((int) (s.alpha * 255), 245, 250, 255));
            canvas.drawCircle(currentX, s.y, s.radius, mainPaint);
        }
    }

    private void resetSnowflake(Snowflake s, boolean randomY) {
        s.x = random.nextInt(getWidth() > 0 ? getWidth() + 40 : 800) - 20;
        s.y = randomY ? random.nextInt(getHeight() > 0 ? getHeight() : 200) : -AndroidUtilities.dp(6);
        s.radius = AndroidUtilities.dp(1.2f + random.nextFloat() * 2.4f);
        s.speedY = AndroidUtilities.dp(22 + random.nextFloat() * 38);
        s.swayFreq = 1.2f + random.nextFloat() * 2.0f;
        s.swayAmp = AndroidUtilities.dp(4 + random.nextFloat() * 8);
        s.swayPhase = (float) (random.nextFloat() * 2 * Math.PI);
        s.alpha = 0.4f + random.nextFloat() * 0.55f;
    }

    // ==================== 8. FIRE EMBERS ENGINE ====================
    private void drawFire(Canvas canvas, float dt) {
        int w = getWidth();
        int h = getHeight();

        mainPaint.setStyle(Paint.Style.FILL);

        for (Ember e : embers) {
            e.life += dt;
            if (e.life >= e.maxLife || e.y < -AndroidUtilities.dp(10)) {
                resetEmber(e, false);
                continue;
            }

            e.x += e.vx * dt;
            e.y += e.vy * dt;
            // horizontal organic jitter
            e.vx += (random.nextFloat() - 0.5f) * AndroidUtilities.dp(20) * dt;

            float progress = e.life / e.maxLife;
            float currentAlpha = (1f - progress) * e.alpha;

            // Interpolate color from gold/orange to deep flame red
            int r = 255;
            int g = (int) (180 * (1f - progress));
            int b = 0;

            mainPaint.setColor(Color.argb((int) (currentAlpha * 255), r, g, b));
            canvas.drawCircle(e.x, e.y, AndroidUtilities.dp(e.size * (1f - progress * 0.4f)), mainPaint);
        }
    }

    private void resetEmber(Ember e, boolean randomY) {
        e.x = random.nextInt(getWidth() > 0 ? getWidth() : 800);
        e.y = randomY ? random.nextInt(getHeight() > 0 ? getHeight() : 200) : (getHeight() > 0 ? getHeight() : 150) + AndroidUtilities.dp(4);
        e.vx = (random.nextFloat() - 0.5f) * AndroidUtilities.dp(22);
        e.vy = -AndroidUtilities.dp(35 + random.nextInt(45));
        e.size = 1.2f + random.nextFloat() * 2.2f;
        e.alpha = 0.7f + random.nextFloat() * 0.3f;
        e.maxLife = 1.4f + random.nextFloat() * 1.2f;
        e.life = 0f;
    }

    // ==================== 9. AURORA BOREALIS ENGINE ====================
    private void drawAurora(Canvas canvas, float dt) {
        int w = getWidth();
        int h = getHeight();

        auroraPhase += dt * 0.85f;

        auroraPath1.reset();
        auroraPath2.reset();

        float baseY1 = h * 0.45f;
        float baseY2 = h * 0.65f;

        auroraPath1.moveTo(0, baseY1);
        auroraPath2.moveTo(0, baseY2);

        int segments = 8;
        float dx = (float) w / segments;

        for (int i = 1; i <= segments; i++) {
            float x = i * dx;
            float y1 = baseY1 + (float) Math.sin(auroraPhase + i * 0.8f) * AndroidUtilities.dp(9);
            float y2 = baseY2 + (float) Math.cos(auroraPhase * 1.2f + i * 0.6f) * AndroidUtilities.dp(12);
            auroraPath1.lineTo(x, y1);
            auroraPath2.lineTo(x, y2);
        }

        // Draw first wave (Emerald & Cyan shimmer)
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(AndroidUtilities.dp(14));
        glowPaint.setColor(0x3500E676);
        canvas.drawPath(auroraPath1, glowPaint);

        // Draw second wave (Violet & Electric Cyan shimmer)
        secondaryPaint.setStyle(Paint.Style.STROKE);
        secondaryPaint.setStrokeWidth(AndroidUtilities.dp(16));
        secondaryPaint.setColor(0x307C4DFF);
        canvas.drawPath(auroraPath2, secondaryPaint);
    }
}
