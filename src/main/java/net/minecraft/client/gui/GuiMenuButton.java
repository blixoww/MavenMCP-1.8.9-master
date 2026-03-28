package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;

public class GuiMenuButton extends GuiButton {

    private float hoverFade = 0.0f;
    private final boolean primary;
    private long lastTime = -1L;
    private static final float TRANS_MS = 100f;

    public GuiMenuButton(int id, int x, int y, int w, int h, String text, boolean primary) {
        super(id, x, y, w, h, text);
        this.primary = primary;
    }

    public GuiMenuButton(int id, int x, int y, int w, int h, String text) {
        this(id, x, y, w, h, text, false);
    }

    @Override
    public void drawButton(Minecraft mc, int mx, int my) {
        if (!this.visible) return;

        // Détection de survol avec les coordonnées de souris passées
        this.hovered = mx >= xPosition && my >= yPosition && mx < xPosition + width && my < yPosition + height;

        // Utilisation directe du temps système pour éviter les problèmes de synchronisation entre écrans
        long now = Minecraft.getSystemTime();
        float dt = (lastTime < 0L) ? 0f : (float)(now - lastTime);
        lastTime = now;

        float step = dt / TRANS_MS;
        if (hovered) hoverFade = MathHelper.clamp_float(hoverFade + step, 0.0f, 1.0f);
        else hoverFade = MathHelper.clamp_float(hoverFade - step, 0.0f, 1.0f);

        // Cubic easing pour plus de douceur
        float t = hoverFade * hoverFade * (3.0f - 2.0f * hoverFade);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // Fond - Effet verre progressif
        int bgAlpha = (int)(25 + (t * 35)); // 25 to 60
        int bgCol = (bgAlpha << 24) | 0x1A1A1A;
        Gui.drawRect(xPosition, yPosition, xPosition + width, yPosition + height, bgCol);

        // Bordure - Red Conflict style (Gris -> Rouge Conflict)
        int borderColor = GuiRenderUtils.colorLerp(0x30FFFFFF, 0xFFCC2222, t);
        GuiRenderUtils.drawRectOutline(xPosition, yPosition, width, height, borderColor);

        // Accent rouge si primaire (plus intense au survol)
        if (primary) {
            int accentAlpha = (int)(180 + (t * 75));
            Gui.drawRect(xPosition, yPosition, xPosition + 1, yPosition + height, (accentAlpha << 24) | 0xCC2222);
        }

        // Effet de brillance (Shimmer) au survol
        if (t > 0) {
            int shimA = (int)(20 * t);
            GuiRenderUtils.drawGradientRect(xPosition + 1, yPosition + 1, xPosition + width - 1, yPosition + 3, (shimA << 24) | 0xFFFFFF, 0x00FFFFFF);
        }

        // Texte avec Lerp de couleur
        int textCol = GuiRenderUtils.colorLerp(0xFFBBBBBB, 0xFFFFFFFF, t);
        this.drawCenteredString(mc.fontRendererObj, displayString, xPosition + width / 2, yPosition + (height - 8) / 2, textCol);

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}
