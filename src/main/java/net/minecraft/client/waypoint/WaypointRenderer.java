package net.minecraft.client.waypoint;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
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

        Entity viewer = mc.getRenderViewEntity();
        double viewX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partialTicks;
        double viewY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partialTicks;
        double viewZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partialTicks;

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

            // Nom et coordonnées
            renderLabel(dx, dy, dz, wp, dist, mc);
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
        GlStateManager.enableBlend();
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
                                    Waypoint wp, double dist, Minecraft mc) {
        // Hauteur du label : au-dessus du waypoint
        double labelY = y + 3.0;

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) labelY, (float) z);

        // Toujours face au joueur
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX,  1.0F, 0.0F, 0.0F);

        // ── Calcul du scale ────────────────────────────────────────────────────
        // scale ∝ dist pour garder une taille angulaire constante.
        // Coefficients réduits pour des labels moins envahissants.
        final double NEAR_DIST = 5.0;
        float scale;
        switch (wp.getTextSize()) {
            case SMALL: {
                // ~0.7° angulaire — très discret, lisible seulement de près
                double target = Math.max(NEAR_DIST, dist) * 0.006;
                scale = (float) Math.min(target, 0.025);
                break;
            }
            case LARGE: {
                // ~1.5° angulaire — bien visible même de loin
                double target = Math.max(NEAR_DIST, dist) * 0.013;
                scale = (float) Math.min(target, 0.06);
                break;
            }
            default: { // MEDIUM
                // ~1.1° angulaire — intermédiaire confortable
                double target = Math.max(NEAR_DIST, dist) * 0.009;
                scale = (float) Math.min(target, 0.04);
                break;
            }
        }
        GlStateManager.scale(-scale, -scale, scale);

        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();

        String nameStr  = wp.getName();
        String distStr  = String.format("%.0fm", dist);
        String coordStr = wp.isCoordsVisible()
                ? "[" + wp.getX() + ", " + wp.getY() + ", " + wp.getZ() + "]"
                : null;

        int nameWidth  = mc.fontRendererObj.getStringWidth(nameStr);
        int maxWidth   = nameWidth;
        if (coordStr != null) {
            maxWidth = Math.max(maxWidth, mc.fontRendererObj.getStringWidth(coordStr));
        }
        maxWidth = Math.max(maxWidth, mc.fontRendererObj.getStringWidth(distStr));

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        int   lines  = coordStr != null ? 3 : 2;
        float halfW  = maxWidth / 2.0F + 4;
        float top    = -2;
        float bottom = top + lines * 11 + 2;

        // Fond semi-transparent
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(-halfW, top,    0).color(0, 0, 0, 120).endVertex();
        wr.pos(-halfW, bottom, 0).color(0, 0, 0, 120).endVertex();
        wr.pos( halfW, bottom, 0).color(0, 0, 0, 120).endVertex();
        wr.pos( halfW, top,    0).color(0, 0, 0, 120).endVertex();
        tess.draw();

        GlStateManager.enableTexture2D();

        // Nom (couleur du waypoint)
        int wpColor = 0xFF000000 | (wp.getColorR() << 16) | (wp.getColorG() << 8) | wp.getColorB();
        mc.fontRendererObj.drawStringWithShadow(nameStr, -nameWidth / 2.0F, 0, wpColor);

        // Distance
        mc.fontRendererObj.drawStringWithShadow(distStr,
                -mc.fontRendererObj.getStringWidth(distStr) / 2.0F, 11, 0xFFCCCCCC);

        // Coordonnées
        if (coordStr != null) {
            mc.fontRendererObj.drawStringWithShadow(coordStr,
                    -mc.fontRendererObj.getStringWidth(coordStr) / 2.0F, 22, 0xFFAAAAAA);
        }

        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}

