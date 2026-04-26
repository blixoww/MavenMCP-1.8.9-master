package net.minecraft.client.gui;

import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

public class GuiScreenOptionsSounds extends GuiScreen
{
    private static final int ACCENT = 0xFFDC1E1E;

    private final GuiScreen field_146505_f;
    private final GameSettings game_settings_4;
    protected String field_146507_a = "Options";
    private String field_146508_h;

    private float animation = 0f;
    private long  animLastTime = -1L;
    private int[] btnYCache;

    public GuiScreenOptionsSounds(GuiScreen p_i45025_1_, GameSettings p_i45025_2_)
    {
        this.field_146505_f = p_i45025_1_;
        this.game_settings_4 = p_i45025_2_;
    }

    public void initGui()
    {
        this.buttonList.clear();
        this.animation = 0f; this.animLastTime = -1L;
        this.field_146507_a = I18n.format("options.sounds.title");
        this.field_146508_h  = I18n.format("options.off");

        int btnW = 150, btnH = 28, gap = 4;
        int i = 0;
        // MASTER : pleine largeur
        this.buttonList.add(new SoundSlider(SoundCategory.MASTER.getCategoryId(),
                this.width / 2 - btnW - 5, 50 + i * (btnH + gap),
                btnW * 2 + 10, btnH, SoundCategory.MASTER));
        i += 2;
        for (SoundCategory cat : SoundCategory.values()) {
            if (cat == SoundCategory.MASTER) continue;
            int bx = this.width / 2 + (i % 2 == 0 ? -btnW - 5 : 5);
            int by = 50 + (i / 2) * (btnH + gap);
            this.buttonList.add(new SoundSlider(cat.getCategoryId(), bx, by, btnW, btnH, cat));
            ++i;
        }
        int doneY = 50 + ((i + 1) / 2) * (btnH + gap) + 8;
        this.buttonList.add(new GuiMenuButton(200, this.width / 2 - 75, doneY, 150, 22, "DONE", true));
        this.btnYCache = new int[this.buttonList.size()];
    }

    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (button.enabled && button.id == 200) {
            this.mc.gameSettings.saveOptions();
            this.mc.displayGuiScreen(this.field_146505_f);
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        long now = Minecraft.getSystemTime();
        if (animLastTime != -1L) animation = MathHelper.clamp_float(animation + (now - animLastTime) / 250f, 0f, 1f);
        animLastTime = now;
        float e = animation * animation * (3f - 2f * animation);

        this.drawDefaultBackground();
        Gui.drawRect(0, 0, this.width, 36, (int)(e*210) << 24 | 0x05070A);
        Gui.drawRect(0, 36, this.width, 37, (int)(e*255) << 24 | (ACCENT & 0xFFFFFF));
        GuiRenderUtils.drawGradientRect(0, 37, this.width, 52, (int)(e*80) << 24 | 0x05070A, 0x00000000);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, (1f - e) * 8, 0);

        int ta = (int)(e * 255) << 24;
        String t1 = "§c§lOPTIONS ";
        String t2 = "§f§lSONS";
        int tw = fontRendererObj.getStringWidth(t1) + fontRendererObj.getStringWidth(t2);
        int tx = this.width / 2 - tw / 2;
        fontRendererObj.drawStringWithShadow(t1, tx, 13, ta | 0xFFFFFF);
        fontRendererObj.drawStringWithShadow(t2, tx + fontRendererObj.getStringWidth(t1), 13, ta | 0xFFFFFF);
        int dw2 = (int)((tw + 20) * e);
        Gui.drawRect(this.width/2 - dw2/2, 26, this.width/2 + dw2/2, 27, (int)(e*45) << 24 | 0xFFFFFF);

        if (btnYCache == null || btnYCache.length != this.buttonList.size()) btnYCache = new int[this.buttonList.size()];
        for (int i2 = 0; i2 < this.buttonList.size(); i2++) {
            GuiButton b = this.buttonList.get(i2);
            btnYCache[i2] = b.yPosition;
            float ba = MathHelper.clamp_float(animation * 2f - i2 * 0.07f, 0f, 1f);
            ba = ba * ba * (3f - 2f * ba);
            b.yPosition += (int)((1f - ba) * 12);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        for (int i2 = 0; i2 < this.buttonList.size(); i2++) this.buttonList.get(i2).yPosition = btnYCache[i2];

        Gui.drawRect(0, this.height - 36, this.width, this.height, (int)(e*200) << 24 | 0x05070A);
        Gui.drawRect(0, this.height - 36, this.width, this.height - 35, (int)(e*30) << 24 | 0xFFFFFF);
        GlStateManager.popMatrix();
    }

    protected String getSoundVolume(SoundCategory cat)
    {
        float f = this.game_settings_4.getSoundLevel(cat);
        return f == 0f ? this.field_146508_h : (int)(f * 100f) + "%";
    }

    // ── Slider stylé pour les catégories son ──────────────────────────────
    class SoundSlider extends GuiButton
    {
        private final SoundCategory category;
        private final String        label;
        private float sliderValue;
        private boolean dragging;
        private float hover = 0f;
        private long  hoverTime = -1L;

        SoundSlider(int id, int x, int y, int w, int h, SoundCategory cat)
        {
            super(id, x, y, w, h, "");
            this.category   = cat;
            this.label      = I18n.format("soundCategory." + cat.getCategoryName());
            this.sliderValue = GuiScreenOptionsSounds.this.game_settings_4.getSoundLevel(cat);
        }

        protected int getHoverState(boolean mo) { return 0; }

        public void drawButton(Minecraft mc, int mx, int my)
        {
            if (!this.visible) return;
            long now = Minecraft.getSystemTime();
            float dt = (hoverTime < 0) ? 0f : (float)(now - hoverTime);
            hoverTime = now;
            this.hovered = mx >= xPosition && my >= yPosition && mx < xPosition + width && my < yPosition + height;
            hover = MathHelper.clamp_float(hover + (hovered ? dt / 120f : -dt / 120f), 0f, 1f);
            float t = hover * hover * (3f - 2f * hover);

            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

            int bg = (int)(20 + t*25) << 24 | 0x1A1A1A;
            Gui.drawRect(xPosition, yPosition, xPosition + width, yPosition + height, bg);
            int border = GuiRenderUtils.colorLerp(0x30FFFFFF, 0xFFDC1E1E, t);
            GuiRenderUtils.drawRectOutline(xPosition, yPosition, width, height, border);

            // Label
            int lc = GuiRenderUtils.colorLerp(0xFF707880, 0xFFE0E0E0, t);
            mc.fontRendererObj.drawStringWithShadow(label, xPosition + 6, yPosition + 5, lc);

            // Track
            int ty = yPosition + height - 8;
            int tx1 = xPosition + 6, tx2 = xPosition + width - 6, tw2 = tx2 - tx1;
            Gui.drawRect(tx1, ty, tx2, ty + 3, 0xFF0D1017);
            int fill = (int)(sliderValue * tw2);
            if (fill > 0) Gui.drawRect(tx1, ty, tx1 + fill, ty + 3, 0xFFDC1E1E);
            // Thumb
            int thumbX = tx1 + fill - 3;
            Gui.drawRect(thumbX, ty - 2, thumbX + 6, ty + 5,
                    GuiRenderUtils.colorLerp(0xFFAAAAAA, 0xFFFFFFFF, t));

            // Valeur
            String vol = GuiScreenOptionsSounds.this.getSoundVolume(category);
            int vw = mc.fontRendererObj.getStringWidth(vol);
            mc.fontRendererObj.drawString(vol, xPosition + width - vw - 6, yPosition + 5,
                    GuiRenderUtils.colorLerp(0xFFAAAAAA, 0xFFFFFFFF, t));

            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }

        protected void mouseDragged(Minecraft mc, int mouseX, int mouseY)
        {
            if (this.visible && this.dragging) {
                int tx1 = xPosition + 6, tw2 = width - 12;
                sliderValue = MathHelper.clamp_float((float)(mouseX - tx1) / tw2, 0f, 1f);
                mc.gameSettings.setSoundLevel(category, sliderValue);
                mc.gameSettings.saveOptions();
            }
        }

        public boolean mousePressed(Minecraft mc, int mouseX, int mouseY)
        {
            if (super.mousePressed(mc, mouseX, mouseY)) {
                int tx1 = xPosition + 6, tw2 = width - 12;
                sliderValue = MathHelper.clamp_float((float)(mouseX - tx1) / tw2, 0f, 1f);
                mc.gameSettings.setSoundLevel(category, sliderValue);
                mc.gameSettings.saveOptions();
                this.dragging = true;
                return true;
            }
            return false;
        }

        public void playPressSound(SoundHandler sh) {}

        public void mouseReleased(int mouseX, int mouseY)
        {
            if (this.dragging)
                GuiScreenOptionsSounds.this.mc.getSoundHandler()
                        .playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1f));
            this.dragging = false;
        }
    }
}
