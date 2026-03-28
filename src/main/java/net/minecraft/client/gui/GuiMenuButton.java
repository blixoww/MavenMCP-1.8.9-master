package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

public class GuiMenuButton extends GuiButton {

    private float hoverFade = 0.0f;
    private final boolean primary;
    private long lastTime = -1L;
    private static final float TRANS_MS = 80f; // durée de transition hover en ms (courte pour réactivité)

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

        // delta-time based smoothing to make hover transitions snappy and frame-rate independent
        // prefer global frame time (set by GuiMainMenu) when available to sync all buttons
        long now = (GuiMainMenu.FRAME_TIME > 0L) ? GuiMainMenu.FRAME_TIME : Minecraft.getSystemTime();
        float dt = (lastTime < 0L) ? 0f : (float)(now - lastTime);
        lastTime = now;

        float step = dt / TRANS_MS;
        if (step < 0f) step = 0f;
        if (step > 1f) step = 1f;
        if (hovered) hoverFade = Math.min(1f, hoverFade + step);
        else hoverFade = Math.max(0f, hoverFade - step);

        // apply a gentle ease curve for visual smoothness
        float t = Math.max(0f, Math.min(1f, hoverFade));
        t = (t < 0.5f) ? 2f * t * t : 1f - 2f * (1f - t) * (1f - t);

        GlStateManager.pushMatrix();

        // Background - Toujours visible (fond sombre)
        int bgCol = (int)(40 + (t * 40)) << 24 | 0x111111;
        Gui.drawRect(xPosition, yPosition, xPosition + width, yPosition + height, bgCol);

        // Bordure - Plus visible
        int borderColor = GuiRenderUtils.colorLerp(0x30FFFFFF, 0xFFCC2222, t);
        GuiRenderUtils.drawRectOutline(xPosition, yPosition, width, height, borderColor);

        // Accent rouge si primaire
        if (primary) {
            Gui.drawRect(xPosition, yPosition, xPosition + 1, yPosition + height, 0xFFCC2222);
        }

        // Texte avec ombre pour lisibilité maximale
        int textCol = GuiRenderUtils.colorLerp(0xFFCCCCCC, 0xFFFFFFFF, t);
        this.drawCenteredString(mc.fontRendererObj, displayString, xPosition + width / 2, yPosition + (height - 8) / 2, textCol);

        GlStateManager.popMatrix();
    }
}