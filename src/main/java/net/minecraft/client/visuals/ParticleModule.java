package net.minecraft.client.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumParticleTypes;

import java.util.Random;

/**
 * Spawns colored particles on hit or kill using vanilla particle system.
 */
public class ParticleModule {

    private final Random rand = new Random();

    public void spawnHitParticles(EntityLivingBase target, VisualSettings s) {
        spawnParticles(target, s, s.particleQuantity);
    }

    public void spawnKillParticles(EntityLivingBase target, VisualSettings s) {
        spawnParticles(target, s, s.particleQuantity * 2);
    }

    private void spawnParticles(EntityLivingBase target, VisualSettings s, int count) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;

        double tx = target.posX;
        double ty = target.posY + target.height * 0.5;
        double tz = target.posZ;

        for (int i = 0; i < count; i++) {
            int color = pickRandomColor(s);
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;

            double ox = (rand.nextDouble() - 0.5) * 0.8;
            double oy = (rand.nextDouble() - 0.5) * 0.8;
            double oz = (rand.nextDouble() - 0.5) * 0.8;

            double vx = (rand.nextDouble() - 0.5) * 0.3;
            double vy = rand.nextDouble() * 0.3 + 0.1;
            double vz = (rand.nextDouble() - 0.5) * 0.3;

            switch (s.particleType) {
                case 0: // Redstone dust
                    mc.theWorld.spawnParticle(
                        EnumParticleTypes.REDSTONE,
                        tx + ox, ty + oy, tz + oz,
                        r - 1.0, g, b // MC quirk: red channel offset by -1 for redstone
                    );
                    break;
                case 1: // Spell/Potion
                    mc.theWorld.spawnParticle(
                        EnumParticleTypes.SPELL_MOB,
                        tx + ox, ty + oy, tz + oz,
                        r, g, b
                    );
                    break;
                case 2: // Firework
                    mc.theWorld.spawnParticle(
                        EnumParticleTypes.FIREWORKS_SPARK,
                        tx + ox, ty + oy, tz + oz,
                        vx, vy, vz
                    );
                    break;
            }
        }
    }

    private int pickRandomColor(VisualSettings s) {
        int roll = rand.nextInt(3);
        switch (roll) {
            case 0: return s.particleColor1;
            case 1: return s.particleColor2;
            default: return s.particleColor3;
        }
    }
}
