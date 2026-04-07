package net.optifine.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiOptionSlider;
import net.minecraft.client.gui.GuiRenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.MathHelper;

public class GuiOptionSliderOF extends GuiOptionSlider implements IOptionControl
{
    private GameSettings.Options option = null;
    private float hoverFade = 0f;
    private long lastTime   = -1L;

    public GuiOptionSliderOF(int id, int x, int y, GameSettings.Options option)
    {
        super(id, x, y, option);
        this.option = option;
    }

    public GameSettings.Options getOption()
    {
        return this.option;
    }

    @Override
    public void drawButton(Minecraft mc, int mx, int my)
    {
        if (!this.visible) return;

        this.hovered = mx >= xPosition && my >= yPosition
                && mx < xPosition + width && my < yPosition + height;

        long now   = Minecraft.getSystemTime();
        float dt   = (lastTime < 0L) ? 0f : (float)(now - lastTime);
        lastTime   = now;
        float step = dt / 100f;
        if (enabled) hoverFade = MathHelper.clamp_float(hoverFade + (hovered ? step : -step), 0f, 1f);
        else         hoverFade = MathHelper.clamp_float(hoverFade - step * 2f, 0f, 1f);
        float t = hoverFade * hoverFade * (3f - 2f * hoverFade);

        // Update slider value first (math only, no texture drawn)
        this.mouseDragged(mc, mx, my);

        // Current normalised value (0-1) from game settings
        float sv = option.normalizeValue(mc.gameSettings.getOptionFloatValue(option));
        sv = MathHelper.clamp_float(sv, 0f, 1f);
        int fillW = (int)(sv * (width - 8));

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // Background
        int bgA = enabled ? (int)(25 + t * 35) : 12;
        Gui.drawRect(xPosition, yPosition, xPosition + width, yPosition + height, (bgA << 24) | 0x1A1A1A);

        // Filled track
        if (enabled && fillW > 0)
        {
            int trackA = (int)(60 + t * 40);
            Gui.drawRect(xPosition + 1, yPosition + 1,
                    xPosition + 1 + fillW, yPosition + height - 1,
                    (trackA << 24) | 0x991111);
        }

        // Knob
        int knobX = xPosition + fillW;
        int knobBg = enabled ? GuiRenderUtils.colorLerp(0xFF2A2A2A, 0xFFCC2222, t) : 0xFF1E1E1E;
        Gui.drawRect(knobX, yPosition + 1, knobX + 8, yPosition + height - 1, knobBg);
        if (enabled)
            GuiRenderUtils.drawRectOutline(knobX, yPosition + 1, 8, height - 2, 0x55FFFFFF);
        // Knob top highlight
        if (enabled && t > 0f)
            Gui.drawRect(knobX + 1, yPosition + 2, knobX + 7, yPosition + 4, (int)(20 * t) << 24 | 0xFFFFFF);

        // Outer border: grey → red on hover
        int border = enabled ? GuiRenderUtils.colorLerp(0x30FFFFFF, 0xFFCC2222, t) : 0x18FFFFFF;
        GuiRenderUtils.drawRectOutline(xPosition, yPosition, width, height, border);

        // Text (drawn last, always on top)
        int textCol = enabled ? GuiRenderUtils.colorLerp(0xFFBBBBBB, 0xFFFFFFFF, t) : 0xFF444444;
        this.drawCenteredString(mc.fontRendererObj, displayString,
                xPosition + width / 2, yPosition + (height - 8) / 2, textCol);

        GlStateManager.disableBlend();
    }

    @Override
    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY)
    {
        // Math update only — no texture rendering
        if (this.visible && this.dragging)
        {
            float sv = (float)(mouseX - (xPosition + 4)) / (float)(width - 8);
            sv = MathHelper.clamp_float(sv, 0f, 1f);
            float val = option.denormalizeValue(sv);
            mc.gameSettings.setOptionFloatValue(option, val);
            this.displayString = mc.gameSettings.getKeyBinding(option);
        }
    }
}
