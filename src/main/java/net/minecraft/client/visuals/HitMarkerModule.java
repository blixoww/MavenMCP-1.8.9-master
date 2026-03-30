package net.minecraft.client.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

/**
 * Hit marker style CoD : 4 barres diagonales autour du crosshair
 * (entre les branches du +), avec fade smooth.
 */
public class HitMarkerModule {

    private long hitTime;
    private boolean active;

    public void onHit() {
        hitTime = System.currentTimeMillis();
        active = true;
    }

    public void render(Minecraft mc, int screenW, int screenH, float partialTicks, VisualSettings s) {
        if (!active) return;

        long elapsed = System.currentTimeMillis() - hitTime;
        if (elapsed > s.hitMarkerDurationMs) {
            active = false;
            return;
        }

        float progress = (float) elapsed / s.hitMarkerDurationMs;
        float alpha = s.hitMarkerOpacity;

        if (s.hitMarkerFade) {
            alpha *= 1.0f - (progress * progress);
        }

        if (alpha <= 0.01f) return;

        float cx = screenW / 2.0f;
        float cy = screenH / 2.0f;

        // Léger punch vers l'intérieur au début
        float punch = (1.0f - progress) * 0.25f;
        float sizeRatio = s.hitMarkerSize / 8.0f;
        float gap = (3.0f - punch * 2.0f) * sizeRatio;
        float barLen = s.hitMarkerSize * 0.55f;
        float thickness = 1.5f * sizeRatio;

        int r = (s.hitMarkerColor >> 16) & 0xFF;
        int g = (s.hitMarkerColor >> 8) & 0xFF;
        int b = s.hitMarkerColor & 0xFF;
        int a = (int)(alpha * 255);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        // 4 barres diagonales (entre les branches du crosshair +)
        // Haut-gauche : du centre vers haut-gauche
        drawLine(wr, tess, cx - gap, cy - gap, cx - gap - barLen, cy - gap - barLen, thickness, r, g, b, a);
        // Haut-droite
        drawLine(wr, tess, cx + gap, cy - gap, cx + gap + barLen, cy - gap - barLen, thickness, r, g, b, a);
        // Bas-gauche
        drawLine(wr, tess, cx - gap, cy + gap, cx - gap - barLen, cy + gap + barLen, thickness, r, g, b, a);
        // Bas-droite
        drawLine(wr, tess, cx + gap, cy + gap, cx + gap + barLen, cy + gap + barLen, thickness, r, g, b, a);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public boolean isActive() {
        return active;
    }

    public long getHitTime() {
        return hitTime;
    }

    private void drawLine(WorldRenderer wr, Tessellator tess, float x1, float y1, float x2, float y2,
                           float thickness, int r, int g, int b, int a) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f) return;

        float nx = -dy / len * thickness * 0.5f;
        float ny = dx / len * thickness * 0.5f;

        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(x1 + nx, y1 + ny, 0).color(r, g, b, a).endVertex();
        wr.pos(x1 - nx, y1 - ny, 0).color(r, g, b, a).endVertex();
        wr.pos(x2 - nx, y2 - ny, 0).color(r, g, b, a).endVertex();
        wr.pos(x2 + nx, y2 + ny, 0).color(r, g, b, a).endVertex();
        tess.draw();
    }
}
