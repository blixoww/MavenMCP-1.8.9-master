package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.VisualOptionsConfig;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumParticleTypes;

import java.util.Random;

public class PvPParticleManager {

    private static final PvPParticleManager instance = new PvPParticleManager();
    private final Random rand = new Random();

    public static PvPParticleManager getInstance() {
        return instance;
    }

    public void onHit(EntityLivingBase target) {
        if (!VisualOptionsConfig.getInstance().bloodParticlesEnabled) return;

        int amount = VisualOptionsConfig.getInstance().bloodParticlesAmount;
        int color = VisualOptionsConfig.getInstance().bloodParticlesColor;
        String style = VisualOptionsConfig.getInstance().bloodParticlesStyle;

        float rf = ((color >> 16) & 0xFF) / 255f;
        float gf = ((color >> 8) & 0xFF) / 255f;
        float bf = (color & 0xFF) / 255f;

        for (int i = 0; i < amount; i++) {
            double x = target.posX + (rand.nextDouble() - 0.5) * target.width;
            double y = target.posY + rand.nextDouble() * target.height;
            double z = target.posZ + (rand.nextDouble() - 0.5) * target.width;

            double vx = (rand.nextDouble() - 0.5) * 0.08;
            double vy = -rand.nextDouble() * 0.12;
            double vz = (rand.nextDouble() - 0.5) * 0.08;

            // Use configurable particle style: SPELL (colored) or REDSTONE
            if ("Spell".equalsIgnoreCase(style)) {
                // SPELL particles use the velocity args as RGB in this codebase's factories
                Minecraft.getMinecraft().theWorld.spawnParticle(EnumParticleTypes.SPELL_MOB_AMBIENT, x, y, z, rf, gf, bf);
            } else if ("Redstone".equalsIgnoreCase(style)) {
                // REDSTONE takes rgb and spawns a colored dust particle
                Minecraft.getMinecraft().theWorld.spawnParticle(EnumParticleTypes.REDSTONE, x, y, z, rf, gf, bf);
            } else {
                // Fallback: use CRIT but with small velocity (non 'fire')
                Minecraft.getMinecraft().theWorld.spawnParticle(EnumParticleTypes.CRIT, x, y, z, vx, vy, vz);
            }
        }
    }

    public void onSwordSwing() {
        if (!VisualOptionsConfig.getInstance().swordTrailEnabled) return;

        EntityLivingBase player = Minecraft.getMinecraft().thePlayer;
        int color = VisualOptionsConfig.getInstance().swordTrailColor;
        String style = VisualOptionsConfig.getInstance().swordTrailStyle;

        float rf = ((color >> 16) & 0xFF) / 255f;
        float gf = ((color >> 8) & 0xFF) / 255f;
        float bf = (color & 0xFF) / 255f;

        for (int i = 0; i < 5; i++) {
            double x = player.posX + (rand.nextDouble() - 0.5) * 0.5;
            double y = player.posY + player.getEyeHeight() + (rand.nextDouble() - 0.5) * 0.5;
            double z = player.posZ + (rand.nextDouble() - 0.5) * 0.5;

            if ("Spell".equalsIgnoreCase(style)) {
                Minecraft.getMinecraft().theWorld.spawnParticle(EnumParticleTypes.SPELL_MOB_AMBIENT, x, y, z, rf, gf, bf);
            } else if ("Redstone".equalsIgnoreCase(style)) {
                Minecraft.getMinecraft().theWorld.spawnParticle(EnumParticleTypes.REDSTONE, x, y, z, rf, gf, bf);
            } else {
                Minecraft.getMinecraft().theWorld.spawnParticle(EnumParticleTypes.CLOUD, x, y, z, 0, 0, 0);
            }
        }
    }

    public void onKill(EntityLivingBase target) {
        if (!VisualOptionsConfig.getInstance().killParticlesEnabled) return;

        int color = VisualOptionsConfig.getInstance().killParticlesColor;
        String style = VisualOptionsConfig.getInstance().killParticlesStyle;

        float rf = ((color >> 16) & 0xFF) / 255f;
        float gf = ((color >> 8) & 0xFF) / 255f;
        float bf = (color & 0xFF) / 255f;

        for (int i = 0; i < 30; i++) {
            double x = target.posX;
            double y = target.posY + target.height / 2.0;
            double z = target.posZ;

            double vx = (rand.nextDouble() - 0.5) * 0.5;
            double vy = (rand.nextDouble() - 0.5) * 0.5;
            double vz = (rand.nextDouble() - 0.5) * 0.5;

            if ("Spell".equalsIgnoreCase(style)) {
                Minecraft.getMinecraft().theWorld.spawnParticle(EnumParticleTypes.SPELL_MOB, x, y, z, rf, gf, bf);
            } else if ("Redstone".equalsIgnoreCase(style)) {
                Minecraft.getMinecraft().theWorld.spawnParticle(EnumParticleTypes.REDSTONE, x, y, z, rf, gf, bf);
            } else {
                // fallback to heart-like particle if available
                Minecraft.getMinecraft().theWorld.spawnParticle(EnumParticleTypes.HEART, x + vx, y + vy, z + vz, 0, 0, 0);
            }
        }
    }
}
