package net.minecraft.client.visuals.ping;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

/**
 * Rendu visuel du système de ping — version améliorée.
 *
 * <p>Améliorations visuelles :
 * <ul>
 *   <li>Style 0 : Anneau + croix intérieure + halo de glow multi-passes</li>
 *   <li>Style 1 : Point avec rayons + glow</li>
 *   <li>Style 2 : Losange plein semi-transparent + contour + glow</li>
 *   <li>Glow adaptatif à la distance (plus fort = plus visible de loin)</li>
 *   <li>Ligne verticale épaissie + dégradé</li>
 *   <li>Billboard distance + nom : fond semi-transparent, plus lisible</li>
 *   <li>Flèche HUD redessinée avec bordure et label de distance</li>
 * </ul>
 *
 * <p>Le glow peut être désactivé / réglé dans {@link PingSettings}.
 */
public final class PingRenderer {

    // ── Géométrie pré-calculée ────────────────────────────────────────────────
    private static final int     RING_SEGMENTS = 48;
    private static final float[] RING_COS      = new float[RING_SEGMENTS];
    private static final float[] RING_SIN      = new float[RING_SEGMENTS];

    static {
        for (int i = 0; i < RING_SEGMENTS; i++) {
            double a = 2.0 * Math.PI * i / RING_SEGMENTS;
            RING_COS[i] = (float) Math.cos(a);
            RING_SIN[i] = (float) Math.sin(a);
        }
    }

    private PingRenderer() {}

    // ─────────────────────────────────────────────────────────────────────────
    // RENDU 3D
    // ─────────────────────────────────────────────────────────────────────────

    public static void renderWorld(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;

        PingManager  pm  = PingManager.INSTANCE;
        int          cnt = pm.renderCount;
        if (cnt == 0) return;

        PingSettings  s  = pm.getSettings();
        RenderManager rm = mc.getRenderManager();
        FontRenderer  fr = mc.fontRendererObj;
        long now = System.currentTimeMillis();
        double rangeSq = s.maxRange * s.maxRange;

        int cr = s.getR(), cg = s.getG(), cb = s.getB();

        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();

        Tessellator   tess = Tessellator.getInstance();
        WorldRenderer wr   = tess.getWorldRenderer();

        for (int idx = 0; idx < cnt; idx++) {
            Ping p = pm.renderSnapshot[idx];
            if (p == null || !p.inUse) continue;

            double dx = p.x - mc.thePlayer.posX;
            double dy = p.y - mc.thePlayer.posY;
            double dz = p.z - mc.thePlayer.posZ;
            double distSq = dx*dx + dy*dy + dz*dz;
            if (distSq > rangeSq) { p.visibleLastFrame = false; continue; }
            p.visibleLastFrame = true;

            float alpha = p.getAlpha();
            if (alpha <= 0.01f) continue;

            // Distance réelle (pour scaling et glow)
            float dist = (float) Math.sqrt(distSq);
            // Facteur de visibilité : les pings lointains sont rendus plus grands
            float distScale = 1.0f + Math.min(dist / 20.0f, 2.5f) * 0.4f;

            int ca = (int)(alpha * 255);

            // Animation pulse + bob
            float pulse  = (float)(0.82 + 0.18 * Math.sin(now / 320.0));
            float bob    = (float)(0.10 * Math.sin(now / 550.0));

            double rx = p.x - rm.viewerPosX;
            double ry = p.y - rm.viewerPosY + bob;
            double rz = p.z - rm.viewerPosZ;

            p.renderX = (float) rx;
            p.renderY = (float) ry;
            p.renderZ = (float) rz;

            float baseR = 0.32f * s.scale * distScale * pulse;

            // ── Glow multi-passes ─────────────────────────────────────────────
            if (s.glowEnabled) {
                int glowLayers = Math.max(1, Math.min(s.glowLayers, 6));
                // Intensité glow boostée si distant (max ×2 à pleine portée)
                float distGlowBoost = 1.0f + Math.min(dist / (float)s.maxRange, 1.0f) * 1.2f;
                float glowBase = s.glowIntensity * distGlowBoost;

                for (int pass = glowLayers; pass >= 1; pass--) {
                    float glowR     = baseR * (1.0f + pass * 0.28f);
                    int   glowAlpha = (int)(alpha * glowBase * (50f / pass));
                    if (glowAlpha < 2) continue;

                    // Couleur du glow : auto = blanc → couleur selon distance
                    int gcr = cr, gcg = cg, gcb = cb;
                    if (s.glowColorAuto) {
                        float t = Math.min(dist / (float)(s.maxRange * 0.5f), 1.0f);
                        gcr = (int)(255 * (1f - t) + cr * t);
                        gcg = (int)(255 * (1f - t) + cg * t);
                        gcb = (int)(255 * (1f - t) + cb * t);
                    }

                    GL11.glLineWidth(s.ringThickness + pass * 2.0f);
                    wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
                    for (int i = 0; i < RING_SEGMENTS; i++) {
                        wr.pos(rx + RING_COS[i] * glowR, ry, rz + RING_SIN[i] * glowR)
                          .color(gcr, gcg, gcb, glowAlpha).endVertex();
                    }
                    tess.draw();
                }
                GL11.glLineWidth(1.0f);
            }

            // ── Style 0 : Anneau + croix intérieure ──────────────────────────
            if (s.markerStyle == 0) {
                // Anneau principal
                GL11.glLineWidth(s.ringThickness);
                wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
                for (int i = 0; i < RING_SEGMENTS; i++) {
                    wr.pos(rx + RING_COS[i] * baseR, ry, rz + RING_SIN[i] * baseR)
                      .color(cr, cg, cb, ca).endVertex();
                }
                tess.draw();

                // Croix intérieure (4 branches)
                float cl = baseR * 0.55f;
                float cg2 = baseR * 0.15f;
                GL11.glLineWidth(s.ringThickness * 0.8f);
                wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
                // +X
                wr.pos(rx + cg2, ry, rz).color(cr, cg, cb, ca).endVertex();
                wr.pos(rx + cl,  ry, rz).color(cr, cg, cb, (int)(ca * 0.3f)).endVertex();
                // -X
                wr.pos(rx - cg2, ry, rz).color(cr, cg, cb, ca).endVertex();
                wr.pos(rx - cl,  ry, rz).color(cr, cg, cb, (int)(ca * 0.3f)).endVertex();
                // +Z
                wr.pos(rx, ry, rz + cg2).color(cr, cg, cb, ca).endVertex();
                wr.pos(rx, ry, rz + cl ).color(cr, cg, cb, (int)(ca * 0.3f)).endVertex();
                // -Z
                wr.pos(rx, ry, rz - cg2).color(cr, cg, cb, ca).endVertex();
                wr.pos(rx, ry, rz - cl ).color(cr, cg, cb, (int)(ca * 0.3f)).endVertex();
                tess.draw();
                GL11.glLineWidth(1.0f);

                // Point central plein
                float pr = 0.055f * s.scale * distScale;
                wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
                wr.pos(rx, ry, rz).color(255, 255, 255, ca).endVertex();
                for (int i = 0; i <= 14; i++) {
                    double a = 2.0 * Math.PI * i / 14;
                    wr.pos(rx + (float)Math.cos(a) * pr, ry, rz + (float)Math.sin(a) * pr)
                      .color(cr, cg, cb, (int)(alpha * 200)).endVertex();
                }
                tess.draw();
            }

            // ── Style 2 : Losange plein + contour ────────────────────────────
            else if (s.markerStyle == 2) {
                float d = baseR * 1.15f;
                // Remplissage semi-transparent
                wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
                wr.pos(rx, ry, rz).color(cr, cg, cb, (int)(alpha * 60)).endVertex();
                wr.pos(rx,     ry, rz - d).color(cr, cg, cb, (int)(alpha * 40)).endVertex();
                wr.pos(rx + d, ry, rz    ).color(cr, cg, cb, (int)(alpha * 40)).endVertex();
                wr.pos(rx,     ry, rz + d).color(cr, cg, cb, (int)(alpha * 40)).endVertex();
                wr.pos(rx - d, ry, rz    ).color(cr, cg, cb, (int)(alpha * 40)).endVertex();
                wr.pos(rx,     ry, rz - d).color(cr, cg, cb, (int)(alpha * 40)).endVertex();
                tess.draw();
                // Contour
                GL11.glLineWidth(s.ringThickness);
                wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
                wr.pos(rx,     ry, rz - d).color(cr, cg, cb, ca).endVertex();
                wr.pos(rx + d, ry, rz    ).color(cr, cg, cb, ca).endVertex();
                wr.pos(rx,     ry, rz + d).color(cr, cg, cb, ca).endVertex();
                wr.pos(rx - d, ry, rz    ).color(cr, cg, cb, ca).endVertex();
                tess.draw();
                GL11.glLineWidth(1.0f);
                // Centre
                float pr = 0.06f * s.scale * distScale;
                wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
                wr.pos(rx, ry, rz).color(255, 255, 255, ca).endVertex();
                for (int i = 0; i <= 10; i++) {
                    double a = 2.0 * Math.PI * i / 10;
                    wr.pos(rx + (float)Math.cos(a) * pr, ry, rz + (float)Math.sin(a) * pr)
                      .color(cr, cg, cb, (int)(alpha * 200)).endVertex();
                }
                tess.draw();
            }

            // ── Style 1 : Point avec rayons ───────────────────────────────────
            else {
                // Rayons
                float rl = baseR * 0.7f;
                GL11.glLineWidth(1.5f);
                wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
                for (int i = 0; i < 4; i++) {
                    double a = Math.PI * i / 2.0;
                    float inner = 0.07f * s.scale;
                    wr.pos(rx + (float)Math.cos(a) * inner, ry, rz + (float)Math.sin(a) * inner)
                      .color(cr, cg, cb, ca).endVertex();
                    wr.pos(rx + (float)Math.cos(a) * rl, ry, rz + (float)Math.sin(a) * rl)
                      .color(cr, cg, cb, (int)(ca * 0.2f)).endVertex();
                }
                tess.draw();
                GL11.glLineWidth(1.0f);
                // Point central
                float pr2 = 0.10f * s.scale * distScale;
                wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
                wr.pos(rx, ry, rz).color(255, 255, 255, ca).endVertex();
                for (int i = 0; i <= 16; i++) {
                    double a = 2.0 * Math.PI * i / 16;
                    wr.pos(rx + (float)Math.cos(a) * pr2, ry, rz + (float)Math.sin(a) * pr2)
                      .color(cr, cg, cb, (int)(alpha * 200)).endVertex();
                }
                tess.draw();
            }

            // ── Ligne verticale avec dégradé ─────────────────────────────────
            float lineH = 2.2f + Math.min(dist * 0.04f, 1.2f); // plus haute si lointain
            GL11.glLineWidth(2.0f);
            wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            wr.pos(rx, ry,          rz).color(cr, cg, cb, ca).endVertex();
            wr.pos(rx, ry + lineH,  rz).color(255, 255, 255, 0).endVertex();
            tess.draw();
            GL11.glLineWidth(1.0f);

            GlStateManager.enableTexture2D();

            // ── Billboard : distance + nom ────────────────────────────────────
            if (s.showSenderName || s.showDistance) {
                GlStateManager.pushMatrix();
                GlStateManager.translate((float) rx, (float)(ry + lineH + 0.12), (float) rz);
                GlStateManager.rotate(-rm.playerViewY, 0f, 1f, 0f);
                GlStateManager.rotate( rm.playerViewX, 1f, 0f, 0f);
                float scl = 0.020f * s.scale * Math.min(1.0f + dist / 30.0f, 2.2f);
                GlStateManager.scale(-scl, -scl, scl);

                float lineY = 0f;

                if (s.showDistance) {
                    double d2 = Math.sqrt(dx*dx + dy*dy + dz*dz);
                    String distStr = (int)d2 + "m";
                    int dw = fr.getStringWidth(distStr);
                    // Fond semi-transparent
                    net.minecraft.client.gui.Gui.drawRect(-dw / 2 - 2, (int)lineY - 1,
                        dw / 2 + 2, (int)lineY + fr.FONT_HEIGHT,
                        (int)(alpha * 140) << 24);
                    int dc = ((int)(alpha * 255) << 24) | (s.getEffectiveColor() & 0x00FFFFFF);
                    fr.drawStringWithShadow(distStr, -dw / 2.0f, lineY, dc);
                    lineY += fr.FONT_HEIGHT + 2;
                }

                if (s.showSenderName) {
                    String name = p.senderName;
                    int nw = fr.getStringWidth(name);
                    net.minecraft.client.gui.Gui.drawRect(-nw / 2 - 2, (int)lineY - 1,
                        nw / 2 + 2, (int)lineY + fr.FONT_HEIGHT,
                        (int)(alpha * 120) << 24);
                    int nc = ((int)(alpha * 255) << 24) | 0xFFFFFF;
                    fr.drawStringWithShadow(name, -nw / 2.0f, lineY, nc);
                }

                GlStateManager.popMatrix();
                GlStateManager.disableTexture2D();
            }
        }

        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HUD 2D
    // ─────────────────────────────────────────────────────────────────────────

    public static void renderHUD(ScaledResolution sr) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        PingManager  pm = PingManager.INSTANCE;
        PingSettings s  = pm.getSettings();
        FontRenderer fr = mc.fontRendererObj;
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();
        long now = System.currentTimeMillis();

        // ── Barre de cooldown ─────────────────────────────────────────────────
        if (pm.isOnCooldown()) {
            float progress = pm.getCooldownProgress();
            int barW = 44;
            int barH = 3;
            int barX = sw / 2 - barW / 2;
            int barY = sh - 36;

            drawRect(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0x44000000);
            drawRect(barX, barY, barX + barW, barY + barH, 0x33FFFFFF);
            int fill = (int)(barW * progress);
            if (fill > 0) {
                int fc = (s.getEffectiveColor() & 0x00FFFFFF) | 0xCC000000;
                drawRect(barX, barY, barX + fill, barY + barH, fc);
                // Petite étincelle à l'extrémité
                if (fill < barW) {
                    drawRect(barX + fill - 1, barY - 1, barX + fill + 1, barY + barH + 1,
                             (s.getEffectiveColor() & 0x00FFFFFF) | 0xFF000000);
                }
            }
        }

        // ── Indicateurs hors-écran ────────────────────────────────────────────
        if (s.showOffScreenIndicator) {
            EntityPlayer player = mc.thePlayer;
            float yaw   = (float) Math.toRadians(player.rotationYaw);
            double sinY = Math.sin(yaw);
            double cosY = Math.cos(yaw);
            float fov   = mc.gameSettings.fovSetting;

            int count = pm.renderCount;
            for (int idx = 0; idx < count; idx++) {
                Ping p = pm.renderSnapshot[idx];
                if (p == null || !p.inUse || p.visibleLastFrame) continue;

                float alpha = p.getAlpha();
                if (alpha <= 0.01f) continue;

                double ddx = p.x - player.posX;
                double ddy = p.y - player.posY;
                double ddz = p.z - player.posZ;

                double cRight   =  ddx * cosY + ddz * sinY;
                double cForward = -ddx * sinY + ddz * cosY;

                float horizDeg = (float) Math.toDegrees(Math.atan2(cRight, Math.abs(cForward)));
                if (cForward > 0 && horizDeg < fov * 0.45f) continue;

                double mag = Math.sqrt(cRight*cRight + cForward*cForward);
                if (mag < 0.001) continue;
                float sxN = (float)(cRight / mag);
                float syN = (float)(-cForward / mag);

                final int MARGIN = 20;
                float hW = sw / 2.0f - MARGIN;
                float hH = sh / 2.0f - MARGIN;
                float t = (Math.abs(sxN) * hH > Math.abs(syN) * hW)
                    ? hW / (Math.abs(sxN) + 1e-6f)
                    : hH / (Math.abs(syN) + 1e-6f);

                float indX = sw / 2.0f + sxN * t;
                float indY = sh / 2.0f + syN * t;

                int ca = (int)(alpha * 235);
                int cr = s.getR(), cg = s.getG(), cb = s.getB();

                // Ombre de la flèche pour la lisibilité
                drawArrow((int) indX + 1, (int) indY + 1, Math.atan2(sxN, -syN), 9, 0, 0, 0, (int)(alpha * 120));
                drawArrow((int) indX, (int) indY, Math.atan2(sxN, -syN), 9, cr, cg, cb, ca);

                // Distance + fond
                double dist = Math.sqrt(ddx*ddx + ddy*ddy + ddz*ddz);
                String distStr = (int)dist + "m";
                int dsw = fr.getStringWidth(distStr);
                float dtx = indX - dsw / 2.0f;
                float dty = indY + 12;
                drawRect((int)dtx - 2, (int)dty - 1, (int)dtx + dsw + 2, (int)dty + fr.FONT_HEIGHT, 0x88000000);
                int dc = (ca << 24) | (s.getEffectiveColor() & 0x00FFFFFF);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                fr.drawStringWithShadow(distStr, dtx, dty, dc);
            }
        }

        // ── Notifications ─────────────────────────────────────────────────────
        int notifX = sw - 5;
        int notifY = 20;
        int nCount = pm.getNotifCount();
        for (int i = 0; i < nCount; i++) {
            long elapsed = now - pm.getNotifTime(i);
            if (elapsed >= PingManager.NOTIF_DURATION_MS) continue;
            float fade;
            if      (elapsed < 400L)                                          fade = elapsed / 400.0f;
            else if (elapsed > PingManager.NOTIF_DURATION_MS - 600L)         fade = (PingManager.NOTIF_DURATION_MS - elapsed) / 600.0f;
            else                                                              fade = 1.0f;
            int a = (int)(fade * 255) & 0xFF;
            String text = pm.getNotifText(i);
            int tw2 = fr.getStringWidth(text);
            // Fond de la notif
            drawRect(notifX - tw2 - 4, notifY + i * 10 - 1, notifX + 1, notifY + i * 10 + fr.FONT_HEIGHT - 1,
                     ((int)(fade * 100) << 24) | 0x000000);
            fr.drawStringWithShadow(text, notifX - tw2, notifY + i * 10, (a << 24) | 0xFFFFFF);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITAIRES
    // ─────────────────────────────────────────────────────────────────────────

    private static void drawArrow(int cx, int cy, double angle, int size, int r, int g, int b, int a) {
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r / 255f, g / 255f, b / 255f, a / 255f);

        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        // Pointe allongée
        float ax  = (float)(cx + cos * size * 1.1);
        float ay  = (float)(cy + sin * size * 1.1);
        float bx  = (float)(cx - cos * size * 0.55 - sin * size * 0.7);
        float by  = (float)(cy - sin * size * 0.55 + cos * size * 0.7);
        float cx2 = (float)(cx - cos * size * 0.55 + sin * size * 0.7);
        float cy2 = (float)(cy - sin * size * 0.55 - cos * size * 0.7);
        // Encoche
        float nx  = (float)(cx - cos * size * 0.2);
        float ny  = (float)(cy - sin * size * 0.2);

        Tessellator   tess = Tessellator.getInstance();
        WorldRenderer wr   = tess.getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION);
        wr.pos(ax,  ay,  0).endVertex();
        wr.pos(bx,  by,  0).endVertex();
        wr.pos(nx,  ny,  0).endVertex();
        wr.pos(ax,  ay,  0).endVertex();
        wr.pos(nx,  ny,  0).endVertex();
        wr.pos(cx2, cy2, 0).endVertex();
        tess.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static void drawRect(int x1, int y1, int x2, int y2, int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >>  8) & 0xFF;
        int b =  color        & 0xFF;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r / 255f, g / 255f, b / 255f, a / 255f);
        Tessellator   tess = Tessellator.getInstance();
        WorldRenderer wr   = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION);
        wr.pos(x1, y2, 0).endVertex();
        wr.pos(x2, y2, 0).endVertex();
        wr.pos(x2, y1, 0).endVertex();
        wr.pos(x1, y1, 0).endVertex();
        tess.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }
}
