package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.VisualOptionsConfig;

public class HitMarkerRenderer {

    private static final HitMarkerRenderer instance = new HitMarkerRenderer();
    private long lastHitTime = -1;
    private boolean isKill = false;
    private boolean isCrit = false;

    public static HitMarkerRenderer getInstance() {
        return instance;
    }

    public void onHit(boolean kill, boolean crit) {
        if (!VisualOptionsConfig.getInstance().hitMarkerEnabled) return;
        this.lastHitTime = System.currentTimeMillis();
        this.isKill = kill;
        this.isCrit = crit;

        if (VisualOptionsConfig.getInstance().hitMarkerSound) {
            Minecraft.getMinecraft().thePlayer.playSound("random.click", 0.5F, 1.5F);
        }
    }

    public void render(ScaledResolution sr) {
        if (!VisualOptionsConfig.getInstance().hitMarkerEnabled) return;
        long now = System.currentTimeMillis();
        long diff = now - lastHitTime;

        if (diff < 150 && lastHitTime != -1) {
            float alpha = 1.0f - (diff / 150.0f);
            alpha *= VisualOptionsConfig.getInstance().hitMarkerOpacity;

            int color = VisualOptionsConfig.getInstance().hitMarkerColor;
            if (isKill) color = 0xFFFF0000;
            if (isCrit) color = 0xFFFFD700; // Gold

            // combine alpha into ARGB color
            int alphaByte = Math.min(255, Math.max(0, (int) (alpha * 255f)));
            int colorWithAlpha = (alphaByte << 24) | (color & 0x00FFFFFF);

            int x = sr.getScaledWidth() / 2;
            int y = sr.getScaledHeight() / 2;

            int size = 4;
            String sizeSetting = VisualOptionsConfig.getInstance().hitMarkerSize;
            if (sizeSetting != null) {
                if (sizeSetting.equalsIgnoreCase("Petit")) size = 2;
                else if (sizeSetting.equalsIgnoreCase("Grand")) size = 6;
            }

            if (isCrit) size += 2;

            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

            int gap = 2;

            // Top Left (horizontal and vertical)
            Gui.drawRect(x - gap - size, y - gap - 1, x - gap, y - gap, colorWithAlpha);
            Gui.drawRect(x - gap - 1, y - gap - size, x - gap, y - gap, colorWithAlpha);

            // Top Right
            Gui.drawRect(x + gap, y - gap - 1, x + gap + size, y - gap, colorWithAlpha);
            Gui.drawRect(x + gap + size - 1, y - gap - size, x + gap + size, y - gap, colorWithAlpha);

            // Bottom Left
            Gui.drawRect(x - gap - size, y + gap, x - gap, y + gap + 1, colorWithAlpha);
            Gui.drawRect(x - gap - 1, y + gap, x - gap, y + gap + size, colorWithAlpha);

            // Bottom Right
            Gui.drawRect(x + gap, y + gap, x + gap + size, y + gap + 1, colorWithAlpha);
            Gui.drawRect(x + gap + size - 1, y + gap, x + gap + size, y + gap + size, colorWithAlpha);

            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }
    }
}
