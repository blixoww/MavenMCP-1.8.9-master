package net.minecraft.client.gui;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.TreeMap;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.MathHelper;

public class GuiSnooper extends GuiScreen
{
    private static final int ACCENT = 0xFFDC1E1E;

    private final GuiScreen field_146608_a;
    private final GameSettings game_settings_2;
    private final java.util.List<String> field_146604_g = Lists.newArrayList();
    private final java.util.List<String> field_146609_h = Lists.newArrayList();
    private String field_146610_i;
    private String[] field_146607_r;
    private GuiSnooper.List field_146606_s;
    private GuiButton field_146605_t;

    private float animation = 0f;
    private long  animLastTime = -1L;

    public GuiSnooper(GuiScreen p_i1061_1_, GameSettings p_i1061_2_)
    {
        this.field_146608_a = p_i1061_1_;
        this.game_settings_2 = p_i1061_2_;
    }

    public void initGui()
    {
        this.animation = 0f; this.animLastTime = -1L;
        this.field_146610_i = I18n.format("options.snooper.title");
        String s = I18n.format("options.snooper.desc");
        java.util.List<String> list = Lists.newArrayList();
        for (String s1 : this.fontRendererObj.listFormattedStringToWidth(s, this.width - 30))
            list.add(s1);
        this.field_146607_r = list.toArray(new String[0]);

        this.field_146604_g.clear();
        this.field_146609_h.clear();
        this.buttonList.clear();

        this.buttonList.add(this.field_146605_t = new GuiMenuButton(1, this.width / 2 - 152, this.height - 30, 150, 20,
                this.game_settings_2.getKeyBinding(GameSettings.Options.SNOOPER_ENABLED)));
        this.buttonList.add(new GuiMenuButton(2, this.width / 2 + 2, this.height - 30, 150, 20, "DONE", true));

        boolean flag = this.mc.getIntegratedServer() != null && this.mc.getIntegratedServer().getPlayerUsageSnooper() != null;
        for (Entry<String, String> entry : (new TreeMap<>(this.mc.getPlayerUsageSnooper().getCurrentStats())).entrySet()) {
            this.field_146604_g.add((flag ? "C " : "") + entry.getKey());
            this.field_146609_h.add(this.fontRendererObj.trimStringToWidth(entry.getValue(), this.width - 220));
        }
        if (flag) {
            for (Entry<String, String> e2 : (new TreeMap<>(this.mc.getIntegratedServer().getPlayerUsageSnooper().getCurrentStats())).entrySet()) {
                this.field_146604_g.add("S " + e2.getKey());
                this.field_146609_h.add(this.fontRendererObj.trimStringToWidth(e2.getValue(), this.width - 220));
            }
        }
        this.field_146606_s = new GuiSnooper.List();
    }

    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        this.field_146606_s.handleMouseInput();
    }

    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (!button.enabled) return;
        if (button.id == 2) {
            this.game_settings_2.saveOptions();
            this.mc.displayGuiScreen(this.field_146608_a);
        } else if (button.id == 1) {
            this.game_settings_2.setOptionValue(GameSettings.Options.SNOOPER_ENABLED, 1);
            this.field_146605_t.displayString = this.game_settings_2.getKeyBinding(GameSettings.Options.SNOOPER_ENABLED);
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        long now = Minecraft.getSystemTime();
        if (animLastTime != -1L) animation = MathHelper.clamp_float(animation + (now - animLastTime) / 250f, 0f, 1f);
        animLastTime = now;
        float e = animation * animation * (3f - 2f * animation);

        Gui.drawRect(0, 0, this.width, this.height, 0xFF080B10);
        this.field_146606_s.drawScreen(mouseX, mouseY, partialTicks);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, (1f - e) * 8, 0);

        int ta = (int)(e * 255) << 24;

        // Header discret — dégradé + ligne rouge fine (style ChatOptions, sans terre)
        GuiRenderUtils.drawGradientRect(0, 0, this.width, 34, (int)(e*160) << 24 | 0x000000, 0x00000000);
        Gui.drawRect(0, 0, this.width, 1, (int)(e*255) << 24 | (ACCENT & 0xFFFFFF));

        String t1 = "§cSNOOPER ";
        String t2 = "§7SETTINGS";
        int tw = fontRendererObj.getStringWidth(t1) + fontRendererObj.getStringWidth(t2);
        int tx = this.width / 2 - tw / 2;
        fontRendererObj.drawStringWithShadow(t1, tx, 11, ta | 0xFFFFFF);
        fontRendererObj.drawStringWithShadow(t2, tx + fontRendererObj.getStringWidth(t1), 11, ta | 0xFFFFFF);
        int dw = (int)((tw + 20) * e);
        Gui.drawRect(this.width/2 - dw/2, 23, this.width/2 + dw/2, 24, (int)(e*45) << 24 | 0xFFFFFF);

        // Description (en dessous du header)
        int iy = 42;
        for (String line : this.field_146607_r) {
            drawCenteredString(fontRendererObj, line, this.width / 2, iy, ta | 0x808080);
            iy += fontRendererObj.FONT_HEIGHT;
        }

        // Footer discret — dégradé sombre
        GuiRenderUtils.drawGradientRect(0, this.height - 40, this.width, this.height, 0x00000000, (int)(e*140) << 24 | 0x000000);

        super.drawScreen(mouseX, mouseY, partialTicks);
        GlStateManager.popMatrix();
    }

    class List extends GuiSlot
    {
        public List()
        {
            super(GuiSnooper.this.mc, GuiSnooper.this.width, GuiSnooper.this.height, 80, GuiSnooper.this.height - 40,
                    GuiSnooper.this.fontRendererObj.FONT_HEIGHT + 1);
            this.renderBackground = false;
        }

        protected int getSize()                                                    { return GuiSnooper.this.field_146604_g.size(); }
        protected void elementClicked(int s, boolean d, int mx, int my)           {}
        protected boolean isSelected(int s)                                        { return false; }
        protected void drawBackground()                                            { /* pas de terre */ }

        @Override
        protected void overlayBackground(int sY, int eY, int sa, int ea)
        {
            GuiRenderUtils.drawGradientRect(0, sY, GuiSnooper.this.width, eY,
                    (sa << 24) | 0x080B10, (ea << 24) | 0x080B10);
        }

        protected void drawSlot(int id, int x, int y, int h, int mx, int my)
        {
            GuiSnooper.this.fontRendererObj.drawString(GuiSnooper.this.field_146604_g.get(id), 10, y, 0xFFE0E0E0);
            GuiSnooper.this.fontRendererObj.drawString(GuiSnooper.this.field_146609_h.get(id), 230, y, 0xFF707880);
        }

        protected int getScrollBarX() { return GuiSnooper.this.width - 10; }
        protected int getContentHeight() { return this.getSize() * (GuiSnooper.this.fontRendererObj.FONT_HEIGHT + 1); }
    }
}
