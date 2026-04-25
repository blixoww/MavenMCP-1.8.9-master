package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.waypoint.Waypoint;
import net.minecraft.client.waypoint.WaypointManager;

import java.util.List;

public class CompassWidget extends BaseWidget {

    public CompassWidget(String id, int x, int y) {
        super(id, x, y);
        this.width = 90;
        this.height = 20;
        this.defaultWidth = 90;
        this.defaultHeight = 20;
        this.minWidth = 50;
        this.minHeight = 16;
        this.maxWidth = 240;
        this.maxHeight = 34;
    }

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;

        FontRenderer fr = mc.fontRendererObj;
        int w = this.width;
        int centerX = w / 2;

        // Minecraft yaw → degrés boussole (0=N, 90=E, 180=S, 270=O)
        float yaw = mc.thePlayer.rotationYaw;
        float compassDeg = ((yaw + 180.0f) % 360.0f + 360.0f) % 360.0f;

        int col = getColor();
        if ((col >>> 24) == 0) col = 0xFFFFFFFF;

        // ── Layout ──
        // La barre de ticks est fine et positionnée en haut
        final int BAR_TOP    = 1;
        final int BAR_H      = 7;   // plus fine qu'avant (était 9)
        final int BAR_BOTTOM = BAR_TOP + BAR_H;  // 8
        final int DEG_Y      = BAR_BOTTOM + 1;   // 9

        // Pixels par degré
        float ppd = 1.1f;   // un peu plus resserré (était 1.4)
        Object ppdProp = getPropOrDefault("pixelsPerDeg", null);
        if (ppdProp instanceof Number) {
            ppd = Math.max(0.5f, Math.min(5.0f, ((Number) ppdProp).floatValue()));
        }

        // Zone de fondu aux bords (aspect flottant)
        int fadeW = Math.min(w / 4, 18);
        // Fond très transparent, quasi invisible
        int bgColor = 0x33000000;

        // ── 1. Fond avec fondu horizontal aux bords ──
        drawHGradient(0,         BAR_TOP, fadeW,     BAR_BOTTOM, 0x00000000, bgColor);
        Gui.drawRect (fadeW,     BAR_TOP, w - fadeW, BAR_BOTTOM, bgColor);
        drawHGradient(w - fadeW, BAR_TOP, w,         BAR_BOTTOM, bgColor, 0x00000000);

        // Fine ligne de séparation en haut, en fondu
        drawHGradient(0,         BAR_TOP, fadeW,     BAR_TOP + 1, 0x00FFFFFF, 0x18FFFFFF);
        Gui.drawRect (fadeW,     BAR_TOP, w - fadeW, BAR_TOP + 1, 0x18FFFFFF);
        drawHGradient(w - fadeW, BAR_TOP, w,         BAR_TOP + 1, 0x18FFFFFF, 0x00FFFFFF);

        // ── 2. Ticks et labels ──
        float startDeg = compassDeg - (centerX / ppd);
        float endDeg   = compassDeg + ((w - centerX) / ppd);
        int firstTick  = (int) Math.floor(startDeg / 5.0) * 5;

        for (int deg = firstTick; deg <= (int) endDeg + 5; deg += 5) {
            int nd = ((deg % 360) + 360) % 360;

            boolean isCardinal      = (nd % 90 == 0);
            boolean isIntercardinal = (nd % 45 == 0 && !isCardinal);
            boolean isMinor         = (nd % 10 == 0);
            if (!isCardinal && !isIntercardinal && !isMinor) continue;

            float sx = centerX + (deg - compassDeg) * ppd;
            if (sx < 0.0f || sx >= w) continue;
            int isx = Math.round(sx);

            int tickH, tickColor;
            if (isCardinal) {
                tickH = BAR_H - 2;
                tickColor = 0x99FFFFFF;
            } else if (isIntercardinal) {
                tickH = BAR_H / 2;
                tickColor = 0x44FFFFFF;
            } else {
                tickH = 1;
                tickColor = 0x22FFFFFF;
            }
            // Tick aligné en bas de la barre
            Gui.drawRect(isx, BAR_BOTTOM - tickH, isx + 1, BAR_BOTTOM, tickColor);

            // Labels cardinaux/intercardinal dans la zone visible (hors fondu)
            if (nd % 45 == 0 && isx > fadeW + 1 && isx < w - fadeW - 1) {
                String label = getCardinalLabel(nd);
                if (label.isEmpty()) continue;

                // Scale plus petite = plus discret
                float lscale = isCardinal ? 0.7f : 0.6f;
                // Nord en rouge vif, cardinaux en couleur du widget atténuée, intercardinal très discret
                int lcolor;
                if (nd == 0) {
                    lcolor = 0xFFFF4444;
                } else if (nd == 180) {
                    lcolor = applyAlpha(col, 0xCC);
                } else if (isCardinal) {
                    lcolor = applyAlpha(col, 0xBB);
                } else {
                    lcolor = 0x55FFFFFF;
                }

                float lw = fr.getStringWidth(label) * lscale;
                GlStateManager.pushMatrix();
                // Centrer verticalement dans la barre
                GlStateManager.translate(sx - lw / 2.0f, BAR_TOP + 0.5f, 0);
                GlStateManager.scale(lscale, lscale, 1.0f);
                fr.drawStringWithShadow(label, 0, 0, lcolor);
                GlStateManager.popMatrix();
            }
        }

        // ── 3. Waypoints sur la barre ──
        boolean showWaypoints      = !Boolean.FALSE.equals(getPropOrDefault("showWaypoints", Boolean.TRUE));
        boolean showWaypointLabels = !Boolean.FALSE.equals(getPropOrDefault("showWaypointLabels", Boolean.TRUE));
        if (showWaypoints && mc.thePlayer != null) {
            List<Waypoint> waypoints = WaypointManager.INSTANCE.getWaypoints();
            double px = mc.thePlayer.posX;
            double pz = mc.thePlayer.posZ;

            for (Waypoint wp : waypoints) {
                if (!wp.isEnabled()) continue;

                // Angle horizontal du waypoint depuis le joueur (0=N, 90=E, 180=S, 270=O)
                double dx = wp.getX() - px;
                double dz = wp.getZ() - pz;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist < 1.0) continue; // trop proche, pas d'angle fiable

                // atan2 standard → azimut en degrés (N=0, E=90...)
                double wpYaw = Math.toDegrees(Math.atan2(dx, -dz));
                double wpDeg = ((wpYaw % 360.0) + 360.0) % 360.0;

                // Différence angulaire (plus court chemin)
                double delta = wpDeg - compassDeg;
                while (delta > 180)  delta -= 360;
                while (delta < -180) delta += 360;

                float sx = centerX + (float)(delta * ppd);
                if (sx < fadeW || sx >= w - fadeW) continue; // hors zone visible

                int isx = Math.round(sx);
                int wpColor = 0xFF000000 | (wp.getColorR() << 16) | (wp.getColorG() << 8) | wp.getColorB();

                // Petite flèche/triangle vers le bas pointant sur la barre (3px de base)
                Gui.drawRect(isx - 1, BAR_TOP,     isx + 2, BAR_TOP + 1, applyAlpha(wpColor, 0xCC));
                Gui.drawRect(isx,     BAR_TOP + 1, isx + 1, BAR_TOP + 2, applyAlpha(wpColor, 0xFF));

                if (showWaypointLabels) {
                    // Nom du waypoint en petit, au-dessus de la barre
                    String name = wp.getName();
                    float nscale = 0.55f;
                    int maxNameW = 30;
                    if (fr.getStringWidth(name) * nscale > maxNameW) {
                        // Tronquer
                        while (name.length() > 1 && fr.getStringWidth(name + "..") * nscale > maxNameW) {
                            name = name.substring(0, name.length() - 1);
                        }
                        name = name + "..";
                    }
                    // Distance en mètres
                    String distStr = dist >= 1000 ? String.format("%.1fk", dist / 1000.0) : String.format("%.0fm", dist);

                    float nw = fr.getStringWidth(name) * nscale;
                    float dw2 = fr.getStringWidth(distStr) * nscale;

                    // Afficher nom (couleur WP) au-dessus de la barre, distance en dessous dans zone DEG_Y
                    GlStateManager.pushMatrix();
                    GlStateManager.translate(sx - nw / 2.0f, BAR_TOP - 6.5f, 0);
                    GlStateManager.scale(nscale, nscale, 1.0f);
                    fr.drawStringWithShadow(name, 0, 0, applyAlpha(wpColor, 0xDD));
                    GlStateManager.popMatrix();

                    GlStateManager.pushMatrix();
                    GlStateManager.translate(sx - dw2 / 2.0f, DEG_Y + 0.5f, 0);
                    GlStateManager.scale(nscale, nscale, 1.0f);
                    fr.drawStringWithShadow(distStr, 0, 0, 0x88FFFFFF);
                    GlStateManager.popMatrix();
                }
            }
        }

        // ── 4. Marqueur central (triangle pointant vers le bas, 3px de large) ──
        // Petite pointe triangulaire au lieu d'une barre pleine
        int accentColor = applyAlpha(col, 0xFF);
        Gui.drawRect(centerX - 1, BAR_TOP,     centerX + 2, BAR_TOP + 1, applyAlpha(col, 0x88));
        Gui.drawRect(centerX,     BAR_TOP + 1, centerX + 1, BAR_TOP + 3, accentColor);

        // ── 5. Degré exact, centré, très compact ──
        // N'afficher le degré que s'il n'y a pas de waypoints trop proches du centre
        int displayDeg = Math.round(compassDeg) % 360;
        String degStr = displayDeg + "\u00B0";
        float dscale = 0.7f;
        float dw = fr.getStringWidth(degStr) * dscale;
        GlStateManager.pushMatrix();
        GlStateManager.translate(centerX - dw / 2.0f, DEG_Y, 0);
        GlStateManager.scale(dscale, dscale, 1.0f);
        fr.drawStringWithShadow(degStr, 0, 0, applyAlpha(col, 0xBB));
        GlStateManager.popMatrix();
    }

    /** Remplace le canal alpha d'une couleur ARGB par la valeur donnée (0x00–0xFF). */
    private static int applyAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    /** Dégradé horizontal (gauche → droite), analogue à drawGradientRect (haut → bas). */
    private static void drawHGradient(int x1, int y1, int x2, int y2, int colLeft, int colRight) {
        float al = ((colLeft  >> 24) & 0xFF) / 255f, rl = ((colLeft  >> 16) & 0xFF) / 255f;
        float gl = ((colLeft  >>  8) & 0xFF) / 255f, bl = ( colLeft         & 0xFF) / 255f;
        float ar = ((colRight >> 24) & 0xFF) / 255f, rr = ((colRight >> 16) & 0xFF) / 255f;
        float gr = ((colRight >>  8) & 0xFF) / 255f, br = ( colRight        & 0xFF) / 255f;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425); // GL_SMOOTH

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x2, y1, 0).color(rr, gr, br, ar).endVertex();
        wr.pos(x1, y1, 0).color(rl, gl, bl, al).endVertex();
        wr.pos(x1, y2, 0).color(rl, gl, bl, al).endVertex();
        wr.pos(x2, y2, 0).color(rr, gr, br, ar).endVertex();
        tess.draw();

        GlStateManager.shadeModel(7424); // GL_FLAT
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    private static String getCardinalLabel(int deg) {
        switch (deg) {
            case 0:   return "N";
            case 45:  return "NE";
            case 90:  return "E";
            case 135: return "SE";
            case 180: return "S";
            case 225: return "SO";
            case 270: return "O";
            case 315: return "NO";
            default:  return "";
        }
    }
}
