package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.VisualOptionsConfig;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DamageIndicatorRenderer {

    private static final DamageIndicatorRenderer instance = new DamageIndicatorRenderer();
    private final List<DamageIndicatorEntry> entries = new ArrayList<>();

    public static DamageIndicatorRenderer getInstance() {
        return instance;
    }

    public void addEntry(EntityLivingBase entity, double damage, DamageType type) {
        if (!VisualOptionsConfig.getInstance().damageIndicatorEnabled) return;
        if (entity instanceof EntityPlayer && !VisualOptionsConfig.getInstance().damageIndicatorPlayers) return;
        if (!(entity instanceof EntityPlayer) && !VisualOptionsConfig.getInstance().damageIndicatorMobs) return;
        if (damage <= 0 && !VisualOptionsConfig.getInstance().damageIndicatorShowHeal) return;

        double x = entity.posX + (Math.random() - 0.5) * 0.5;
        double y = entity.posY + entity.height + (Math.random() * 0.2);
        double z = entity.posZ + (Math.random() - 0.5) * 0.5;

        entries.add(new DamageIndicatorEntry(x, y, z, damage, type));
    }

    public void render(float partialTicks) {
        if (!VisualOptionsConfig.getInstance().damageIndicatorEnabled || entries.isEmpty()) return;

        long now = System.currentTimeMillis();
        int duration = VisualOptionsConfig.getInstance().damageIndicatorDuration;
        RenderManager rm = Minecraft.getMinecraft().getRenderManager();
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;

        Iterator<DamageIndicatorEntry> it = entries.iterator();
        while (it.hasNext()) {
            DamageIndicatorEntry entry = it.next();
            long age = now - entry.timestamp;

            if (age > duration) {
                it.remove();
                continue;
            }

            float progress = (float) age / duration;
            double x = entry.x - rm.viewerPosX;
            // Animation: monte de 0.5 bloc
            double y = (entry.y + (progress * 0.5)) - rm.viewerPosY;
            double z = entry.z - rm.viewerPosZ;

            // Fade out
            float alpha = progress < 0.75f ? 1.0f : 1.0f - ((progress - 0.75f) / 0.25f);
            float scale = 0.02666667F * VisualOptionsConfig.getInstance().damageIndicatorScale;

            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);
            GL11.glNormal3f(0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(rm.playerViewX, 1.0F, 0.0F, 0.0F);
            GlStateManager.scale(-scale, -scale, scale);
            GlStateManager.disableLighting();
            GlStateManager.depthMask(false);
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

            // Formatage du texte: 1 chiffre après la virgule
            String text = String.format("%s %.1f \u2764", entry.damage > 0 ? "-" : "+", Math.abs(entry.damage));
            int color = entry.type.color;
            int alphaInt = (int) (alpha * 255) << 24;
            
            fr.drawStringWithShadow(text, -fr.getStringWidth(text) / 2, 0, color | alphaInt);

            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }

    public static class DamageIndicatorEntry {
        public double x, y, z, damage;
        public long timestamp;
        public DamageType type;

        public DamageIndicatorEntry(double x, double y, double z, double damage, DamageType type) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.damage = damage;
            this.type = type;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public enum DamageType {
        NORMAL(0xFFFF0000), // Rouge
        CRIT(0xFFFFA500),   // Orange
        POISON(0xFFFFFF00), // Jaune
        HEAL(0xFF00FF00);   // Vert

        public final int color;

        DamageType(int color) {
            this.color = color;
        }
    }
}
