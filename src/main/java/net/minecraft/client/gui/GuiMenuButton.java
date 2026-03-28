package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

public class GuiMenuButton extends GuiButton {

    private float hoverFade = 0.0f;
    private final boolean primary;

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

        this.hovered = mx >= xPosition && my >= yPosition && mx < xPosition + width && my < yPosition + height;
        hoverFade = GuiRenderUtils.lerp(hoverFade, hovered ? 1.0f : 0.0f, 0.2f);

        GlStateManager.pushMatrix();

        // Background - Toujours visible (fond sombre)
        int bgCol = (int)(40 + (hoverFade * 40)) << 24 | 0x111111;
        Gui.drawRect(xPosition, yPosition, xPosition + width, yPosition + height, bgCol);

        // Bordure - Plus visible
        int borderColor = GuiRenderUtils.colorLerp(0x30FFFFFF, 0xFFCC2222, hoverFade);
        GuiRenderUtils.drawRectOutline(xPosition, yPosition, width, height, borderColor);

        // Accent rouge si primaire
        if (primary) {
            Gui.drawRect(xPosition, yPosition, xPosition + 1, yPosition + height, 0xFFCC2222);
        }

        // Texte avec ombre pour lisibilité maximale
        int textCol = GuiRenderUtils.colorLerp(0xFFCCCCCC, 0xFFFFFFFF, hoverFade);
        this.drawCenteredString(mc.fontRendererObj, displayString, xPosition + width / 2, yPosition + (height - 8) / 2, textCol);

        GlStateManager.popMatrix();
    }
}