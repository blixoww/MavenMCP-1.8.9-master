package net.minecraft.client.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;

/**
 * Central manager for all visual effect modules.
 */
public class VisualManager {

    private static final VisualManager INSTANCE = new VisualManager();

    private VisualSettings settings;
    private final ComboModule combo = new ComboModule();
    private final HitMarkerModule hitMarker = new HitMarkerModule();
    private final ParticleModule particles = new ParticleModule();
    private final HeartModule hearts = new HeartModule();

    private VisualManager() {
        this.settings = VisualSettings.load();
    }

    public static VisualManager getInstance() {
        return INSTANCE;
    }

    public VisualSettings getSettings() {
        return settings;
    }

    public void setSettings(VisualSettings s) {
        this.settings = s;
    }

    public ComboModule getCombo() {
        return combo;
    }

    public HitMarkerModule getHitMarker() {
        return hitMarker;
    }

    public ParticleModule getParticles() {
        return particles;
    }

    public HeartModule getHearts() {
        return hearts;
    }

    // ── Event hooks ─────────────────────────────────────────────────────

    public void onAttack(Entity target, float healthBefore) {
        if (target instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) target;

            if (settings.comboEnabled) {
                combo.onHit(settings);
            }
            boolean isCritical = false;
            Minecraft mc2 = Minecraft.getMinecraft();
            if (mc2 != null && mc2.thePlayer != null) {
                isCritical = mc2.thePlayer.fallDistance > 0 && !mc2.thePlayer.onGround
                        && !mc2.thePlayer.isSprinting() && !mc2.thePlayer.isOnLadder()
                        && !mc2.thePlayer.isInWater();
            }
            if (settings.hitMarkerEnabled) {
                hitMarker.onHit(isCritical);
            }
            if (settings.particlesEnabled && passesFilter(target, settings.particleFilter)) {
                if (settings.particleTrigger == 0 || settings.particleTrigger == 2) {
                    particles.spawnHitParticles(living, settings);
                }
            }
            if (settings.heartsEnabled && passesFilter(target, settings.heartsFilter)) {
                hearts.onHit(living, healthBefore, settings);
            }
        }
    }

    /**
     * Appelé quand le joueur clique dans le vide (miss) — reset le combo.
     */
    public void onMiss() {
        if (settings.comboEnabled) {
            combo.reset();
        }
    }

    /**
     * Appelé depuis EntityArrow ou PlayerControllerMP quand une flèche touche.
     */
    public void onArrowHit() {
        if (settings.hitMarkerEnabled) {
            hitMarker.onHit();
        }
    }

    public void onEntityDeath(Entity entity) {
        if (entity instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) entity;
            if (settings.particlesEnabled && passesFilter(entity, settings.particleFilter)) {
                if (settings.particleTrigger == 1 || settings.particleTrigger == 2) {
                    particles.spawnKillParticles(living, settings);
                }
            }
        }
    }

    public void renderHUD(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        if (settings.comboEnabled) {
            combo.render(mc, sw, sh, partialTicks, settings);
        }
        if (settings.hitMarkerEnabled) {
            hitMarker.render(mc, sw, sh, partialTicks, settings);
        }
    }

    public void renderWorld(float partialTicks) {
        if (settings.heartsEnabled) {
            hearts.render(partialTicks, settings);
        }
    }

    public void tick() {
        combo.tick(settings);
        hearts.tick();
    }

    private boolean passesFilter(Entity entity, int filter) {
        if (filter == 0) return entity instanceof EntityPlayer;
        if (filter == 1) return !(entity instanceof EntityPlayer) && entity instanceof EntityLivingBase;
        return true; // 2 = both
    }
}
