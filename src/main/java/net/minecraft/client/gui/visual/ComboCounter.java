package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.VisualOptionsConfig;
import net.minecraft.entity.EntityLivingBase;

public class ComboCounter {

    private static final ComboCounter instance = new ComboCounter();
    private int comboCount = 0;
    private long lastHitTime = -1;
    private EntityLivingBase lastTarget = null;
    private long lastChangeTime = -1;

    public static ComboCounter getInstance() {
        return instance;
    }

    public void onHit(EntityLivingBase target) {
        if (!VisualOptionsConfig.getInstance().comboCounterEnabled) return;

        long now = System.currentTimeMillis();
        if (target != lastTarget || now - lastHitTime > 2500) {
            comboCount = 1;
        } else {
            comboCount++;
        }

        lastHitTime = now;
        lastTarget = target;
        lastChangeTime = now;
    }

    public void onPlayerHurt() {
        comboCount = 0;
        lastTarget = null;
    }

    public void render(ScaledResolution sr) {
        if (!VisualOptionsConfig.getInstance().comboCounterEnabled || comboCount < 1) return;

        long now = System.currentTimeMillis();
        if (now - lastHitTime > 2500) {
            comboCount = 0;
            return;
        }

        int x = sr.getScaledWidth() / 2 + 20 + VisualOptionsConfig.getInstance().comboCounterX;
        int y = sr.getScaledHeight() / 2 - 10 + VisualOptionsConfig.getInstance().comboCounterY;
        float scale = VisualOptionsConfig.getInstance().comboCounterScale;

        // Bounce animation
        long diff = now - lastChangeTime;
        if (diff < 200) {
            float bounce = 1.0f + (0.2f * (1.0f - (diff / 200.0f)));
            scale *= bounce;
        }

        int color = 0xFFFFFFFF;
        if (comboCount >= VisualOptionsConfig.getInstance().comboThresholdGold) color = 0xFFFFD700;
        else if (comboCount >= VisualOptionsConfig.getInstance().comboThresholdRed) color = 0xFFFF0000;
        else if (comboCount >= VisualOptionsConfig.getInstance().comboThresholdOrange) color = 0xFFFFA500;

        String comboText = "x" + comboCount;
        String subText = "COMBO";
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(scale, scale, 1.0f);

        fr.drawStringWithShadow(comboText, 0, 0, color);
        GlStateManager.scale(0.5f, 0.5f, 1.0f);
        fr.drawStringWithShadow(subText, 0, 20, color);

        GlStateManager.popMatrix();

        // Screen shake at high combo
        if (comboCount >= VisualOptionsConfig.getInstance().comboThresholdRed && diff < 100) {
            float shake = (1.0f - (diff / 100.0f)) * 0.5f;
            Minecraft.getMinecraft().thePlayer.rotationPitch += (float) ((Math.random() - 0.5) * shake);
            Minecraft.getMinecraft().thePlayer.rotationYaw += (float) ((Math.random() - 0.5) * shake);
        }
    }
}
