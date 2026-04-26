package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.MathHelper;

/**
 * Slider stylé "Red Conflict" — remplace GuiOptionSlider vanilla.
 */
public class GuiStyledSlider extends GuiButton
{
    // ── Style ──────────────────────────────────────────────────────────────
    private static final int C_TRACK      = 0xFF0D1017;
    private static final int C_TRACK_FILL = 0xFFDC1E1E;
    private static final int C_THUMB      = 0xFFFFFFFF;
    private static final int C_TEXT       = 0xFFE0E0E0;
    private static final int C_LABEL      = 0xFF707880;

    private float sliderValue;
    public  boolean dragging;
    private final GameSettings.Options options;

    // Hover
    private float hover = 0f;
    private long  lastTime = -1L;

    public GuiStyledSlider(int id, int x, int y, int w, int h, GameSettings.Options opt)
    {
        super(id, x, y, w, h, "");
        this.options    = opt;
        Minecraft mc    = Minecraft.getMinecraft();
        this.sliderValue = opt.normalizeValue(mc.gameSettings.getOptionFloatValue(opt));
        this.displayString = mc.gameSettings.getKeyBinding(opt);
    }

    @Override
    protected int getHoverState(boolean mouseOver) { return 0; }

    @Override
    public void drawButton(Minecraft mc, int mx, int my)
    {
        if (!this.visible) return;

        // Hover anim
        long now = Minecraft.getSystemTime();
        float dt = (lastTime < 0) ? 0f : (float)(now - lastTime);
        lastTime = now;
        this.hovered = mx >= xPosition && my >= yPosition && mx < xPosition + width && my < yPosition + height;
        float step = dt / 120f;
        hover = MathHelper.clamp_float(hover + (hovered ? step : -step), 0f, 1f);
        float t = hover * hover * (3f - 2f * hover);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // ── Fond du widget ──────────────────────────────────────────────
        int bgAlpha = (int)(20 + t * 25);
        Gui.drawRect(xPosition, yPosition, xPosition + width, yPosition + height, (bgAlpha << 24) | 0x1A1A1A);

        // Bordure
        int borderCol = GuiRenderUtils.colorLerp(0x30FFFFFF, 0xFFDC1E1E, t);
        GuiRenderUtils.drawRectOutline(xPosition, yPosition, width, height, borderCol);

        // ── Label (nom de l'option) ─────────────────────────────────────
        int labelColor = GuiRenderUtils.colorLerp(C_LABEL, C_TEXT, t);
        String optName = mc.gameSettings.getKeyBinding(options).split(":")[0].trim();
        mc.fontRendererObj.drawStringWithShadow(optName, xPosition + 6, yPosition + (height - 8) / 2 - 4, labelColor);

        // ── Track ───────────────────────────────────────────────────────
        int trackY  = yPosition + height - 7;
        int trackH  = 3;
        int trackX1 = xPosition + 6;
        int trackX2 = xPosition + width - 6;
        int trackW  = trackX2 - trackX1;

        // fond track
        Gui.drawRect(trackX1, trackY, trackX2, trackY + trackH, C_TRACK);
        // fill track (rouge)
        int fillW = (int)(sliderValue * trackW);
        if (fillW > 0)
            Gui.drawRect(trackX1, trackY, trackX1 + fillW, trackY + trackH, C_TRACK_FILL);

        // ── Thumb ────────────────────────────────────────────────────────
        int thumbX = trackX1 + fillW - 3;
        int thumbY = trackY - 2;
        int thumbCol = GuiRenderUtils.colorLerp(0xFFAAAAAA, C_THUMB, t);
        Gui.drawRect(thumbX, thumbY, thumbX + 6, thumbY + trackH + 4, thumbCol);

        // ── Valeur (à droite) ─────────────────────────────────────────
        String val = displayString.contains(":") ? displayString.split(":", 2)[1].trim() : displayString;
        int valW = mc.fontRendererObj.getStringWidth(val);
        mc.fontRendererObj.drawString(val, xPosition + width - valW - 6, yPosition + (height - 8) / 2 - 4, GuiRenderUtils.colorLerp(0xFFAAAAAA, 0xFFFFFFFF, t));

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    @Override
    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY)
    {
        if (this.visible && this.dragging)
        {
            int trackX1 = xPosition + 6;
            int trackW  = width - 12;
            this.sliderValue = (float)(mouseX - trackX1) / (float)trackW;
            this.sliderValue = MathHelper.clamp_float(this.sliderValue, 0.0F, 1.0F);
            float f = this.options.denormalizeValue(this.sliderValue);
            mc.gameSettings.setOptionFloatValue(this.options, f);
            this.sliderValue = this.options.normalizeValue(f);
            this.displayString = mc.gameSettings.getKeyBinding(this.options);
        }
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY)
    {
        if (super.mousePressed(mc, mouseX, mouseY))
        {
            int trackX1 = xPosition + 6;
            int trackW  = width - 12;
            this.sliderValue = (float)(mouseX - trackX1) / (float)trackW;
            this.sliderValue = MathHelper.clamp_float(this.sliderValue, 0.0F, 1.0F);
            mc.gameSettings.setOptionFloatValue(this.options, this.options.denormalizeValue(this.sliderValue));
            this.displayString = mc.gameSettings.getKeyBinding(this.options);
            this.dragging = true;
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) { this.dragging = false; }
}

