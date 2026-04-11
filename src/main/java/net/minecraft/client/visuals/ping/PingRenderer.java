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
 * Rendu visuel du système de ping.
 *
 * <p>Tous les paramètres visuels (couleur, taille, style) proviennent des
 * {@link PingSettings} du <b>viewer local</b> — chaque joueur voit les pings
 * selon SES propres réglages.
 *
 * <ul>
 *   <li>{@link #renderWorld(float)} – marqueurs 3D billboardés dans le monde</li>
 *   <li>{@link #renderHUD(ScaledResolution)} – indicateurs hors-écran + barre cooldown discrète</li>
 * </ul>
 */
public final class PingRenderer {

    // ── Géométrie pré-calculée ────────────────────────────────────────────────
    private static final int     RING_SEGMENTS = 32;
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

        // Couleur effective du viewer
        int cr = s.getR(), cg = s.getG(), cb = s.getB();

        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableDepth();

        Tessellator   tess = Tessellator.getInstance();
        WorldRenderer wr   = tess.getWorldRenderer();

        for (int idx = 0; idx < cnt; idx++) {
            Ping p = pm.renderSnapshot[idx];
            if (p == null || !p.inUse) continue;

            double dx = p.x - mc.thePlayer.posX;
            double dy = p.y - mc.thePlayer.posY;
            double dz = p.z - mc.thePlayer.posZ;
            if (dx*dx + dy*dy + dz*dz > rangeSq) { p.visibleLastFrame = false; continue; }
            p.visibleLastFrame = true;

            float alpha = p.getAlpha();
            if (alpha <= 0.01f) continue;

            int ca = (int)(alpha * 255);

            // ── Animation ─────────────────────────────────────────────────────
            float pulse = (float)(0.80 + 0.20 * Math.sin(now / 350.0));
            float bob   = (float)(0.08 * Math.sin(now / 600.0));

            double rx = p.x - rm.viewerPosX;
            double ry = p.y - rm.viewerPosY + bob;
            double rz = p.z - rm.viewerPosZ;

            p.renderX = (float) rx;
            p.renderY = (float) ry;
            p.renderZ = (float) rz;

            float baseR = 0.30f * s.scale * pulse;

            // ── Style 0 : Anneau + point central ─────────────────────────────
            // ── Style 2 : Losange (deux anneaux décalés à 45°) ────────────────
            if (s.markerStyle == 0 || s.markerStyle == 2) {
                GlStateManager.disableTexture2D();
                int caGlow = (int)(alpha * 60);

                if (s.markerStyle == 2) {
                    // Losange : 4 lignes en diagonale
                    GL11.glLineWidth(s.ringThickness);
                    wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
                    float d = baseR * 1.1f;
                    wr.pos(rx,     ry, rz - d).color(cr, cg, cb, ca).endVertex();
                    wr.pos(rx + d, ry, rz    ).color(cr, cg, cb, ca).endVertex();
                    wr.pos(rx,     ry, rz + d).color(cr, cg, cb, ca).endVertex();
                    wr.pos(rx - d, ry, rz    ).color(cr, cg, cb, ca).endVertex();
                    tess.draw();
                } else {
                    // Anneau externe (glow)
                    GL11.glLineWidth(s.ringThickness + 1.5f);
                    wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
                    for (int i = 0; i < RING_SEGMENTS; i++) {
                        wr.pos(rx + RING_COS[i] * baseR * 1.30f, ry, rz + RING_SIN[i] * baseR * 1.30f)
                          .color(cr, cg, cb, caGlow).endVertex();
                    }
                    tess.draw();
                    // Anneau principal
                    GL11.glLineWidth(s.ringThickness);
                    wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
                    for (int i = 0; i < RING_SEGMENTS; i++) {
                        wr.pos(rx + RING_COS[i] * baseR, ry, rz + RING_SIN[i] * baseR)
                          .color(cr, cg, cb, ca).endVertex();
                    }
                    tess.draw();
                }

                // Point central plein (commun styles 0 et 2)
                float pr = 0.06f * s.scale;
                wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
                wr.pos(rx, ry, rz).color(cr, cg, cb, ca).endVertex();
                for (int i = 0; i <= 12; i++) {
                    double a = 2.0 * Math.PI * i / 12;
                    wr.pos(rx + (float)Math.cos(a) * pr, ry, rz + (float)Math.sin(a) * pr)
                      .color(cr, cg, cb, (int)(alpha * 180)).endVertex();
                }
                tess.draw();

                GL11.glLineWidth(1.0f);
                GlStateManager.enableTexture2D();
            }

            // ── Ligne verticale (communes à tous les styles) ──────────────────
            GlStateManager.disableTexture2D();
            GL11.glLineWidth(1.5f);
            wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            wr.pos(rx, ry,       rz).color(cr, cg, cb, ca).endVertex();
            wr.pos(rx, ry + 2.4, rz).color(cr, cg, cb,  0).endVertex();
            tess.draw();
            GL11.glLineWidth(1.0f);
            GlStateManager.enableTexture2D();

            // ── Style 1 : Point seul (pas d'anneau, juste un gros point) ─────
            if (s.markerStyle == 1) {
                GlStateManager.disableTexture2D();
                float pr2 = 0.12f * s.scale;
                wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
                wr.pos(rx, ry, rz).color(cr, cg, cb, ca).endVertex();
                for (int i = 0; i <= 16; i++) {
                    double a = 2.0 * Math.PI * i / 16;
                    wr.pos(rx + (float)Math.cos(a) * pr2, ry, rz + (float)Math.sin(a) * pr2)
                      .color(cr, cg, cb, (int)(alpha * 200)).endVertex();
                }
                tess.draw();
                GlStateManager.enableTexture2D();
            }

            // ── Billboard : nom expéditeur + distance ─────────────────────────
            if (s.showSenderName || s.showDistance) {
                GlStateManager.pushMatrix();
                GlStateManager.translate((float) rx, (float)(ry + 0.55), (float) rz);
                GlStateManager.rotate(-rm.playerViewY, 0f, 1f, 0f);
                GlStateManager.rotate( rm.playerViewX, 1f, 0f, 0f);
                float scl = 0.022f * s.scale;
                GlStateManager.scale(-scl, -scl, scl);

                int nameAlpha = (int)(alpha * 220);
                float lineY = 0f;

                if (s.showDistance) {
                    double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                    String distStr = (int)dist + "m";
                    int dw = fr.getStringWidth(distStr);
                    int dc = (nameAlpha << 24) | (s.getEffectiveColor() & 0x00FFFFFF);
                    fr.drawStringWithShadow(distStr, -dw / 2.0f, lineY, dc);
                    lineY += 10f;
                }

                if (s.showSenderName) {
                    String name = p.senderName;
                    int nw = fr.getStringWidth(name);
                    int nc = (nameAlpha << 24) | 0xFFFFFF;
                    fr.drawStringWithShadow(name, -nw / 2.0f, lineY, nc);
                }

                GlStateManager.popMatrix();
            }
        }

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

        // ── Barre de cooldown discrète (2 px, sans texte) ────────────────────
        if (pm.isOnCooldown()) {
            float progress = pm.getCooldownProgress();
            int barW = 40;
            int barH = 2;
            int barX = sw / 2 - barW / 2;
            int barY = sh - 32;

            drawRect(barX, barY, barX + barW, barY + barH, 0x55000000);
            int fill = (int)(barW * progress);
            if (fill > 0) {
                int fc = (s.getEffectiveColor() & 0x00FFFFFF) | 0xBB000000;
                drawRect(barX, barY, barX + fill, barY + barH, fc);
            }
        }

        // ── Indicateurs hors-écran (flèche triangulaire au bord) ─────────────
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
                double ddz = p.z - player.posZ;

                double cRight   =  ddx * cosY + ddz * sinY;
                double cForward = -ddx * sinY + ddz * cosY;

                float horizDeg = (float) Math.toDegrees(Math.atan2(cRight, Math.abs(cForward)));
                if (cForward > 0 && horizDeg < fov * 0.45f) continue;

                double mag = Math.sqrt(cRight*cRight + cForward*cForward);
                if (mag < 0.001) continue;
                float sxN = (float)(cRight / mag);
                float syN = (float)(-cForward / mag);

                final int MARGIN = 22;
                float hW = sw / 2.0f - MARGIN;
                float hH = sh / 2.0f - MARGIN;
                float t = (Math.abs(sxN) * hH > Math.abs(syN) * hW)
                    ? hW / (Math.abs(sxN) + 1e-6f)
                    : hH / (Math.abs(syN) + 1e-6f);

                float indX = sw / 2.0f + sxN * t;
                float indY = sh / 2.0f + syN * t;

                int ca = (int)(alpha * 220);
                int cr = s.getR(), cg = s.getG(), cb = s.getB();

                // Flèche triangulaire
                double angle = Math.atan2(sxN, -syN);
                drawArrow((int) indX, (int) indY, angle, 7, cr, cg, cb, ca);

                // Distance
                double dist = Math.sqrt(ddx*ddx + (p.y - player.posY)*(p.y - player.posY) + ddz*ddz);
                String distStr = (int)dist + "m";
                int dc = (ca << 24) | (s.getEffectiveColor() & 0x00FFFFFF);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                fr.drawStringWithShadow(distStr,
                    indX - fr.getStringWidth(distStr) / 2.0f,
                    indY + 9, dc);
            }
        }

        // ── Notifications (haut-droite) ───────────────────────────────────────
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
            fr.drawStringWithShadow(text, notifX - fr.getStringWidth(text), notifY + i * 10, (a << 24) | 0xFFFFFF);
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
        float ax  = (float)(cx + cos * size);
        float ay  = (float)(cy + sin * size);
        float bx  = (float)(cx - cos * size * 0.5 - sin * size * 0.65);
        float by  = (float)(cy - sin * size * 0.5 + cos * size * 0.65);
        float cx2 = (float)(cx - cos * size * 0.5 + sin * size * 0.65);
        float cy2 = (float)(cy - sin * size * 0.5 - cos * size * 0.65);

        Tessellator   tess = Tessellator.getInstance();
        WorldRenderer wr   = tess.getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION);
        wr.pos(ax, ay, 0).endVertex();
        wr.pos(bx, by, 0).endVertex();
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
