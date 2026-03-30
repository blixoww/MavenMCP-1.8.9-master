package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.VisualOptionsConfig;
import net.minecraft.util.ResourceLocation;

public class HitColorRenderer {

    private static final HitColorRenderer instance = new HitColorRenderer();
    private static final ResourceLocation vignetteTexPath = new ResourceLocation("textures/misc/vignette.png");
    private long lastHurtTime = -1;
    private float hurtIntensity = 0.0f;

    public static HitColorRenderer getInstance() {
        return instance;
    }

    public void onPlayerHurt(float amount) {
        if (!VisualOptionsConfig.getInstance().vignetteEnabled) return;
        this.lastHurtTime = System.currentTimeMillis();
        this.hurtIntensity = Math.min(1.0f, amount / 5.0f);
    }

    public void renderVignette(ScaledResolution sr) {
        if (!VisualOptionsConfig.getInstance().vignetteEnabled || lastHurtTime == -1) return;

        long now = System.currentTimeMillis();
        long diff = now - lastHurtTime;
        if (diff > 400) {
            lastHurtTime = -1;
            return;
        }

        float progress = 1.0f - (diff / 400.0f);
        float alpha = progress * hurtIntensity * VisualOptionsConfig.getInstance().vignetteIntensity;

        int color = VisualOptionsConfig.getInstance().vignetteColor;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, alpha);
        
        Minecraft.getMinecraft().getTextureManager().bindTexture(vignetteTexPath);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(0.0D, (double) sr.getScaledHeight(), -90.0D).tex(0.0D, 1.0D).endVertex();
        worldrenderer.pos((double) sr.getScaledWidth(), (double) sr.getScaledHeight(), -90.0D).tex(1.0D, 1.0D).endVertex();
        worldrenderer.pos((double) sr.getScaledWidth(), 0.0D, -90.0D).tex(1.0D, 0.0D).endVertex();
        worldrenderer.pos(0.0D, 0.0D, -90.0D).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
}
