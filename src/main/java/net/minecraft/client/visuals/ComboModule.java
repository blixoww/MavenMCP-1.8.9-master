package net.minecraft.client.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;

/**
 * Affiche le compteur de combo au hit.
 * "Combo" (optionnel) au-dessus, "xN" en dessous — même couleur.
 * Position libre via comboPosX / comboPosY (pixels offset depuis le crosshair).
 */
public class ComboModule {

    private int comboCount;
    private long lastHitTime;
    private float displayScale = 1.0f;
    private float displayAlpha = 0.0f;
    private String cachedCountText = "";

    public void onHit(VisualSettings s) {
        comboCount++;
        lastHitTime = System.currentTimeMillis();
        cachedCountText = "x" + comboCount;
        displayAlpha = 1.0f;
        if (comboCount >= s.comboThreshold3) {
            displayScale = s.comboScaleThreshold3;
        } else if (comboCount >= s.comboThreshold2) {
            displayScale = s.comboScaleThreshold2;
        } else if (comboCount >= s.comboThreshold1) {
            displayScale = s.comboScaleThreshold1;
        } else {
            displayScale = 1.4f;
        }
    }

    public void onHit() {
        comboCount++;
        lastHitTime = System.currentTimeMillis();
        displayScale = 1.4f;
        displayAlpha = 1.0f;
        cachedCountText = "x" + comboCount;
    }

    public void tick(VisualSettings s) {
        long now = System.currentTimeMillis();
        long elapsed = now - lastHitTime;

        if (comboCount > 0 && elapsed > s.comboResetDelayMs) {
            comboCount = 0;
            displayAlpha = 0.0f;
            cachedCountText = "";
        }
    }

    public void render(Minecraft mc, int screenW, int screenH, float partialTicks, VisualSettings s) {
        if (comboCount <= 0 || displayAlpha <= 0.01f) return;

        long now = System.currentTimeMillis();
        long elapsed = now - lastHitTime;

        if (s.comboAnimScale) {
            displayScale += (1.0f - displayScale) * 0.15f;
        } else {
            displayScale = 1.0f;
        }

        if (s.comboAnimFade && elapsed > s.comboResetDelayMs - 500) {
            float fadeProgress = (float)(elapsed - (s.comboResetDelayMs - 500)) / 500.0f;
            displayAlpha = Math.max(0.0f, 1.0f - fadeProgress);
        } else {
            displayAlpha = 1.0f;
        }

        if (displayAlpha <= 0.01f) return;

        FontRenderer fr = mc.fontRendererObj;
        // Position en pixels depuis le centre de l'écran
        int x = screenW / 2 + (int)(s.comboPosX * screenW * 0.3f);
        int y = screenH / 2 + (int)(s.comboPosY * screenH * 0.3f);

        int color = getComboColor(s);
        int alphaInt = (int)(displayAlpha * 255) << 24;
        color = (color & 0x00FFFFFF) | alphaInt;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        float scale = s.comboSize * displayScale;
        float shakeOffset = 0;
        if (s.comboScreenShake && comboCount >= s.comboThreshold3) {
            long sinceHit = now - lastHitTime;
            if (sinceHit < 300) {
                float shakeDecay = 1.0f - sinceHit / 300.0f;
                shakeOffset = (float)(Math.sin(sinceHit * 0.05) * s.comboScreenShakeIntensity * shakeDecay);
            }
        }
        GlStateManager.translate(x + shakeOffset, y, 0);
        GlStateManager.scale(scale, scale, 1.0f);

        if (s.comboShowLabel) {
            // Ligne 1 : "Combo" (même couleur, légèrement plus discret)
            String comboLabel = "Combo";
            int labelW = fr.getStringWidth(comboLabel);
            // Même teinte mais légèrement plus sombre
            int labelAlpha = (int)(displayAlpha * 200) << 24;
            int labelColor = (color & 0x00FFFFFF) | labelAlpha;
            fr.drawStringWithShadow(comboLabel, -labelW / 2.0f, -fr.FONT_HEIGHT, labelColor);
        }

        // Ligne 2 : "xN"
        String countText = cachedCountText;
        int countW = fr.getStringWidth(countText);
        float countY = s.comboShowLabel ? 1.0f : -fr.FONT_HEIGHT / 2.0f;
        fr.drawStringWithShadow(countText, -countW / 2.0f, countY, color);

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public int getComboCount() {
        return comboCount;
    }

    public float getDisplayAlpha() {
        return displayAlpha;
    }

    public float getDisplayScale() {
        return displayScale;
    }

    public void reset() {
        comboCount = 0;
        displayAlpha = 0.0f;
        cachedCountText = "";
    }

    private int getComboColor(VisualSettings s) {
        if (comboCount >= s.comboThreshold3) return s.comboColor3;
        if (comboCount >= s.comboThreshold2) return s.comboColor2;
        if (comboCount >= s.comboThreshold1) return s.comboColor1;
        return s.comboColor;
    }
}
