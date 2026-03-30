package net.minecraft.client.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Indicateurs de dégâts flottants au-dessus des entités frappées.
 * Approche différée : on capture la vie au moment du hit, puis on vérifie
 * les ticks suivants pour détecter la vraie réduction de vie serveur.
 */
public class HeartModule {

    private static final int MAX_INDICATORS = 50;
    private static final int LIFETIME_MS = 1500;
    private static final int PATCH_TIMEOUT_MS = 500; // max attente pour le patch serveur

    private final List<DamageIndicator> active = new ArrayList<DamageIndicator>(MAX_INDICATORS);
    private final List<DamageIndicator> pool = new ArrayList<DamageIndicator>(MAX_INDICATORS);

    public HeartModule() {
        for (int i = 0; i < MAX_INDICATORS; i++) {
            pool.add(new DamageIndicator());
        }
    }

    /**
     * Appelé au moment de l'attaque. On enregistre la vie AVANT que le serveur
     * ne confirme les dégâts (car côté client, attackEntityFrom retourne false
     * quand worldObj.isRemote == true).
     */
    public void onHit(EntityLivingBase target, float healthBefore, VisualSettings s) {
        DamageIndicator ind = acquire();
        if (ind == null) return;

        ind.x = target.posX + (Math.random() - 0.5) * 0.6;
        ind.y = target.posY + target.height + 0.3 + Math.random() * 0.3;
        ind.z = target.posZ + (Math.random() - 0.5) * 0.6;
        ind.spawnTime = System.currentTimeMillis();
        ind.color = s.heartsColor;
        ind.entityId = target.getEntityId();
        ind.healthAtHit = healthBefore;
        ind.patched = false;
        ind.showDamage = s.heartsShowDamage;
        ind.text = ""; // sera rempli au prochain tick

        active.add(ind);
    }

    /**
     * Appelé chaque tick. Vérifie les entités pour patcher les indicateurs
     * avec la vraie valeur de dégâts du serveur.
     */
    public void tick() {
        long now = System.currentTimeMillis();
        Minecraft mc = Minecraft.getMinecraft();

        Iterator<DamageIndicator> it = active.iterator();
        while (it.hasNext()) {
            DamageIndicator ind = it.next();

            // Supprimer les expirés
            if (now - ind.spawnTime > LIFETIME_MS) {
                it.remove();
                pool.add(ind);
                continue;
            }

            // Essayer de patcher les non-patchés en vérifiant la santé actuelle
            if (!ind.patched && mc.theWorld != null) {
                Entity entity = mc.theWorld.getEntityByID(ind.entityId);
                if (entity instanceof EntityLivingBase) {
                    float currentHealth = ((EntityLivingBase) entity).getHealth();
                    float damage = ind.healthAtHit - currentHealth;

                    if (damage > 0.001f) {
                        // Dégâts détectés !
                        if (ind.showDamage) {
                            ind.text = String.format("%.1f", damage);
                        } else {
                            ind.text = String.format("%.1f", currentHealth);
                        }
                        ind.patched = true;
                    } else if (now - ind.spawnTime > PATCH_TIMEOUT_MS) {
                        // Timeout — probablement frames d'invulnérabilité
                        if (ind.showDamage) {
                            ind.text = "0";
                        } else {
                            ind.text = String.format("%.1f", currentHealth);
                        }
                        ind.patched = true;
                    }
                } else if (now - ind.spawnTime > PATCH_TIMEOUT_MS) {
                    // Entité disparue
                    ind.text = ind.showDamage ? "?" : "?";
                    ind.patched = true;
                }
            }
        }
    }

    public void render(float partialTicks, VisualSettings s) {
        if (active.isEmpty()) return;

        Minecraft mc = Minecraft.getMinecraft();
        RenderManager rm = mc.getRenderManager();
        FontRenderer fr = mc.fontRendererObj;

        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableDepth();

        long now = System.currentTimeMillis();

        for (int i = 0; i < active.size(); i++) {
            DamageIndicator ind = active.get(i);

            // Ne pas afficher tant que pas patché (évite le "?" flash)
            if (!ind.patched) continue;
            if (ind.text.isEmpty()) continue;

            float age = (float)(now - ind.spawnTime) / LIFETIME_MS;

            double renderX = ind.x - rm.viewerPosX;
            double renderY = ind.y - rm.viewerPosY + age * 1.2;
            double renderZ = ind.z - rm.viewerPosZ;

            float alpha = age > 0.6f ? 1.0f - ((age - 0.6f) / 0.4f) : 1.0f;
            if (alpha <= 0.01f) continue;

            GlStateManager.pushMatrix();
            GlStateManager.translate((float) renderX, (float) renderY, (float) renderZ);
            GlStateManager.rotate(-rm.playerViewY, 0.0f, 1.0f, 0.0f);
            GlStateManager.rotate(rm.playerViewX, 1.0f, 0.0f, 0.0f);
            float scale = 0.025f;
            GlStateManager.scale(-scale, -scale, scale);

            int color = (ind.color & 0x00FFFFFF) | ((int)(alpha * 255) << 24);

            String display = "\u2764 " + ind.text;
            int displayW = fr.getStringWidth(display);
            fr.drawStringWithShadow(display, -displayW / 2.0f, 0, color);

            GlStateManager.popMatrix();
        }

        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    public List<DamageIndicator> getActive() {
        return active;
    }

    private DamageIndicator acquire() {
        if (!pool.isEmpty()) {
            return pool.remove(pool.size() - 1);
        }
        if (active.size() >= MAX_INDICATORS) {
            return active.remove(0);
        }
        return new DamageIndicator();
    }

    static class DamageIndicator {
        double x, y, z;
        long spawnTime;
        String text = "";
        int color;
        int entityId;
        float healthAtHit;
        boolean patched;
        boolean showDamage;
    }
}
