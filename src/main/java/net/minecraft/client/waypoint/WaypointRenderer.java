package net.minecraft.client.waypoint;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

import java.util.List;

/**
 * Rendu des waypoints dans le monde : faisceau beacon + nom + coordonnées.
 */
public class WaypointRenderer {

    private static final ResourceLocation BEAM_TEXTURE = new ResourceLocation("textures/entity/beacon_beam.png");

    /** Distance max d'affichage (blocs) */
    private static final double MAX_DIST = 2048.0;

    /**
     * Appelé après le rendu du monde, dans EntityRenderer.
     */
    public static void render(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;

        List<Waypoint> waypoints = WaypointManager.INSTANCE.getWaypoints();
        if (waypoints.isEmpty()) return;

        RenderManager rm = mc.getRenderManager();
        double viewX = rm.viewerPosX;
        double viewY = rm.viewerPosY;
        double viewZ = rm.viewerPosZ;

        for (Waypoint wp : waypoints) {
            if (!wp.isEnabled()) continue;

            double dx = wp.getX() + 0.5 - viewX;
            double dy = wp.getY() - viewY;
            double dz = wp.getZ() + 0.5 - viewZ;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (dist > MAX_DIST) continue;

            // Faisceau beacon : du sol jusqu'au ciel
            if (wp.isBeamVisible()) {
                renderBeam(dx, dy, dz, wp.getColorRf(), wp.getColorGf(), wp.getColorBf(), partialTicks, wp.getY());
            }

            // Nom et coordonnées (carré de texte)
            if (wp.isLabelVisible()) {
                renderLabel(dx, dy, dz, wp, dist, mc, rm);
            }
        }
    }

    /**
     * @param y      coordonnée Y absolue du waypoint (pour calculer la hauteur descente)
     */
    private static void renderBeam(double x, double y, double z,
                                   float r, float g, float b,
                                   float partialTicks, int waypointAbsY) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(BEAM_TEXTURE);

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        GlStateManager.disableCull();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.depthMask(false);

        float beamWidth  = 0.2F;
        // Montée jusqu'au ciel (haut) + descente jusqu'au sol (bas)
        float heightUp   = 256.0F - waypointAbsY;  // vers le ciel
        float heightDown = (float) waypointAbsY;    // vers Y=0

        float time = (float)(mc.theWorld.getTotalWorldTime()) + partialTicks;
        float texOffset = -time * 0.2F - (float) Math.floor(-time * 0.1F);

        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.color(r, g, b, 0.8F);

        // ── Rendu faisceau montant ────────────────────────────────────────────
        drawBeamSection(wr, tess, beamWidth, 0, heightUp, texOffset);

        // ── Rendu faisceau descendant ─────────────────────────────────────────
        drawBeamSection(wr, tess, beamWidth, -heightDown, 0, texOffset);

        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private static void drawBeamSection(WorldRenderer wr, Tessellator tess,
                                        float w, float yBottom, float yTop, float texOffset) {
        float h = yTop - yBottom;
        if (h <= 0) return;

        wr.begin(7, DefaultVertexFormats.POSITION_TEX);
        wr.pos(-w, yBottom, -w).tex(0, texOffset + h / 4.0F).endVertex();
        wr.pos(-w, yTop,   -w).tex(0, texOffset).endVertex();
        wr.pos( w, yTop,   -w).tex(1, texOffset).endVertex();
        wr.pos( w, yBottom, -w).tex(1, texOffset + h / 4.0F).endVertex();
        tess.draw();

        wr.begin(7, DefaultVertexFormats.POSITION_TEX);
        wr.pos( w, yBottom,  w).tex(0, texOffset + h / 4.0F).endVertex();
        wr.pos( w, yTop,     w).tex(0, texOffset).endVertex();
        wr.pos(-w, yTop,     w).tex(1, texOffset).endVertex();
        wr.pos(-w, yBottom,  w).tex(1, texOffset + h / 4.0F).endVertex();
        tess.draw();

        wr.begin(7, DefaultVertexFormats.POSITION_TEX);
        wr.pos( w, yBottom, -w).tex(0, texOffset + h / 4.0F).endVertex();
        wr.pos( w, yTop,    -w).tex(0, texOffset).endVertex();
        wr.pos( w, yTop,     w).tex(1, texOffset).endVertex();
        wr.pos( w, yBottom,  w).tex(1, texOffset + h / 4.0F).endVertex();
        tess.draw();

        wr.begin(7, DefaultVertexFormats.POSITION_TEX);
        wr.pos(-w, yBottom,  w).tex(0, texOffset + h / 4.0F).endVertex();
        wr.pos(-w, yTop,     w).tex(0, texOffset).endVertex();
        wr.pos(-w, yTop,    -w).tex(1, texOffset).endVertex();
        wr.pos(-w, yBottom, -w).tex(1, texOffset + h / 4.0F).endVertex();
        tess.draw();
    }

    private static void renderLabel(double x, double y, double z,
                                    Waypoint wp, double dist, Minecraft mc, RenderManager rm) {
        // Hauteur du label : monte avec la distance pour rester visible au-dessus du terrain
        double labelY = y + 2.5 + Math.min(dist * 0.015, 8.0);

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) labelY, (float) z);

        // Toujours face au joueur
        GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(rm.playerViewX,  1.0F, 0.0F, 0.0F);

        // ── Calcul du scale ────────────────────────────────────────────────────
        WaypointManager.WaypointSettings ws = WaypointManager.INSTANCE.getSettings();
        float sizeMult;
        switch (wp.getTextSize()) {
            case SMALL:  sizeMult = 0.65f; break;
            case LARGE:  sizeMult = 1.50f; break;
            default:     sizeMult = 1.00f; break;
        }

        float scale;
        if (ws.distanceScaleEnabled) {
            // scale = baseScale * (dist / refDistance) → linéaire avec la distance
            float raw = ws.baseScale * ((float) dist / Math.max(ws.refDistance, 1f));
            scale = Math.max(ws.minScale, Math.min(ws.maxScale, raw)) * sizeMult;
        } else {
            scale = ws.fixedScale * sizeMult;
        }
        GlStateManager.scale(-scale, -scale, scale);

        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        String nameStr  = wp.getName();
        String distStr  = String.format("%.0fm", dist);
        String coordStr = wp.isCoordsVisible()
                ? "[" + wp.getX() + ", " + wp.getY() + ", " + wp.getZ() + "]"
                : null;

        int nameWidth  = mc.fontRendererObj.getStringWidth(nameStr);
        int distWidth  = mc.fontRendererObj.getStringWidth(distStr);
        int maxWidth   = Math.max(nameWidth, distWidth);
        if (coordStr != null)
            maxWidth = Math.max(maxWidth, mc.fontRendererObj.getStringWidth(coordStr));

        int   lines   = coordStr != null ? 3 : 2;
        float pad     = 5f;
        float lineH   = 11f;
        float halfW   = maxWidth / 2.0F + pad;
        float top     = -pad + 1;
        float bottom  = top + lines * lineH + pad;

        int wpColorR = wp.getColorR(), wpColorG = wp.getColorG(), wpColorB = wp.getColorB();
        int wpColor  = 0xFF000000 | (wpColorR << 16) | (wpColorG << 8) | wpColorB;

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        GlStateManager.disableTexture2D();

        // Fond semi-transparent foncé
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(-halfW, top,    0).color(0, 0, 0, 160).endVertex();
        wr.pos(-halfW, bottom, 0).color(0, 0, 0, 160).endVertex();
        wr.pos( halfW, bottom, 0).color(0, 0, 0, 160).endVertex();
        wr.pos( halfW, top,    0).color(0, 0, 0, 160).endVertex();
        tess.draw();

        // Bordure gauche colorée (couleur du waypoint)
        float bw = 2f;
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(-halfW,      top,    0).color(wpColorR, wpColorG, wpColorB, 220).endVertex();
        wr.pos(-halfW,      bottom, 0).color(wpColorR, wpColorG, wpColorB, 220).endVertex();
        wr.pos(-halfW + bw, bottom, 0).color(wpColorR, wpColorG, wpColorB, 220).endVertex();
        wr.pos(-halfW + bw, top,    0).color(wpColorR, wpColorG, wpColorB, 220).endVertex();
        tess.draw();

        GlStateManager.enableTexture2D();

        // Nom (couleur du waypoint)
        mc.fontRendererObj.drawStringWithShadow(nameStr,
                -nameWidth / 2.0F, top + 2, wpColor);

        // Distance en gris clair
        mc.fontRendererObj.drawStringWithShadow(distStr,
                -distWidth / 2.0F, top + 2 + lineH, 0xFFCCCCCC);

        // Coordonnées en gris foncé
        if (coordStr != null) {
            int coordWidth = mc.fontRendererObj.getStringWidth(coordStr);
            mc.fontRendererObj.drawStringWithShadow(coordStr,
                    -coordWidth / 2.0F, top + 2 + lineH * 2, 0xFFAAAAAA);
        }

        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}

