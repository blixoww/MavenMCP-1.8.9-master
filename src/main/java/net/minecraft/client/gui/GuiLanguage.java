package net.minecraft.client.gui;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.Language;
import net.minecraft.client.resources.LanguageManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.MathHelper;

public class GuiLanguage extends GuiScreen
{
    private static final int ACCENT   = 0xFFDC1E1E;
    private static final int C_SEL    = 0xFFDC1E1E;

    protected GuiScreen parentScreen;
    private GuiLanguage.List list;
    private final GameSettings game_settings_3;
    private final LanguageManager languageManager;
    private GuiMenuButton forceUnicodeFontBtn;
    private GuiMenuButton confirmSettingsBtn;

    private float animation = 0f;
    private long  animLastTime = -1L;

    public GuiLanguage(GuiScreen screen, GameSettings gameSettingsObj, LanguageManager manager)
    {
        this.parentScreen = screen;
        this.game_settings_3 = gameSettingsObj;
        this.languageManager = manager;
    }

    public void initGui()
    {
        this.animation = 0f; this.animLastTime = -1L;
        this.buttonList.clear();
        this.buttonList.add(this.forceUnicodeFontBtn = new GuiMenuButton(100,
                this.width / 2 - 155, this.height - 38, 150, 20,
                this.game_settings_3.getKeyBinding(GameSettings.Options.FORCE_UNICODE_FONT)));
        this.buttonList.add(this.confirmSettingsBtn = new GuiMenuButton(6,
                this.width / 2 - 155 + 160, this.height - 38, 150, 20,
                "DONE", true));
        this.list = new GuiLanguage.List(this.mc);
        this.list.registerScrollButtons(7, 8);
    }

    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        this.list.handleMouseInput();
    }

    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (!button.enabled) return;
        switch (button.id) {
            case 6: this.mc.displayGuiScreen(this.parentScreen); break;
            case 100:
                this.game_settings_3.setOptionValue(GameSettings.Options.FORCE_UNICODE_FONT, 1);
                button.displayString = this.game_settings_3.getKeyBinding(GameSettings.Options.FORCE_UNICODE_FONT);
                ScaledResolution sr = new ScaledResolution(this.mc);
                this.setWorldAndResolution(this.mc, sr.getScaledWidth(), sr.getScaledHeight());
                break;
            default: this.list.actionPerformed(button); break;
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        long now = Minecraft.getSystemTime();
        if (animLastTime != -1L) animation = MathHelper.clamp_float(animation + (now - animLastTime) / 250f, 0f, 1f);
        animLastTime = now;
        float e = animation * animation * (3f - 2f * animation);

        Gui.drawRect(0, 0, this.width, this.height, 0xFF080B10);
        Gui.drawRect(0, 0, this.width, 36, (int)(e*200) << 24 | 0x05070A);
        Gui.drawRect(0, 36, this.width, 37, (int)(e*255) << 24 | (ACCENT & 0xFFFFFF));
        GuiRenderUtils.drawGradientRect(0, 37, this.width, 52, (int)(e*80) << 24 | 0x05070A, 0x00000000);

        this.list.drawScreen(mouseX, mouseY, partialTicks);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, (1f - e) * 8, 0);

        int ta = (int)(e * 255) << 24;
        String t1 = "§c§lCHOIX DE ";
        String t2 = "§f§lLANGUE";
        int tw = fontRendererObj.getStringWidth(t1) + fontRendererObj.getStringWidth(t2);
        int tx = this.width / 2 - tw / 2;
        fontRendererObj.drawStringWithShadow(t1, tx, 13, ta | 0xFFFFFF);
        fontRendererObj.drawStringWithShadow(t2, tx + fontRendererObj.getStringWidth(t1), 13, ta | 0xFFFFFF);
        int dw = (int)((tw + 20) * e);
        Gui.drawRect(this.width/2 - dw/2, 26, this.width/2 + dw/2, 27, (int)(e*45) << 24 | 0xFFFFFF);

        // Avertissement
        String warn = "(" + I18n.format("options.languageWarning") + ")";
        drawCenteredString(fontRendererObj, warn, this.width / 2, this.height - 54, ta | 0x555555);

        // Footer bar
        Gui.drawRect(0, this.height - 46, this.width, this.height, (int)(e*200) << 24 | 0x05070A);
        Gui.drawRect(0, this.height - 46, this.width, this.height - 45, (int)(e*30) << 24 | 0xFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
        GlStateManager.popMatrix();
    }

    class List extends GuiSlot
    {
        private final java.util.List<String> langCodeList = Lists.newArrayList();
        private final Map<String, Language>  languageMap  = Maps.newHashMap();

        public List(Minecraft mcIn)
        {
            super(mcIn, GuiLanguage.this.width, GuiLanguage.this.height, 40, GuiLanguage.this.height - 56, 18);
            this.renderBackground = false;
            for (Language lang : GuiLanguage.this.languageManager.getLanguages()) {
                this.languageMap.put(lang.getLanguageCode(), lang);
                this.langCodeList.add(lang.getLanguageCode());
            }
        }

        protected int getSize() { return this.langCodeList.size(); }

        protected void elementClicked(int idx, boolean dbl, int mx, int my)
        {
            Language lang = this.languageMap.get(this.langCodeList.get(idx));
            GuiLanguage.this.languageManager.setCurrentLanguage(lang);
            GuiLanguage.this.game_settings_3.language = lang.getLanguageCode();
            this.mc.refreshResources();
            GuiLanguage.this.fontRendererObj.setUnicodeFlag(
                    GuiLanguage.this.languageManager.isCurrentLocaleUnicode()
                    || GuiLanguage.this.game_settings_3.forceUnicodeFont);
            GuiLanguage.this.fontRendererObj.setBidiFlag(GuiLanguage.this.languageManager.isCurrentLanguageBidirectional());
            GuiLanguage.this.confirmSettingsBtn.displayString = "DONE";
            GuiLanguage.this.forceUnicodeFontBtn.displayString =
                    GuiLanguage.this.game_settings_3.getKeyBinding(GameSettings.Options.FORCE_UNICODE_FONT);
            GuiLanguage.this.game_settings_3.saveOptions();
        }

        protected boolean isSelected(int idx)
        {
            return this.langCodeList.get(idx).equals(GuiLanguage.this.languageManager.getCurrentLanguage().getLanguageCode());
        }

        protected int getContentHeight() { return this.getSize() * 18; }
        protected void drawBackground()  { /* fond sombre de GuiLanguage */ }

        @Override
        protected void overlayBackground(int sY, int eY, int sa, int ea)
        {
            GuiRenderUtils.drawGradientRect(0, sY, GuiLanguage.this.width, eY,
                    (sa << 24) | 0x080B10, (ea << 24) | 0x080B10);
        }

        protected void drawSlot(int id, int x, int y, int h, int mx, int my)
        {
            boolean sel = isSelected(id);
            if (sel) {
                int lw = this.getListWidth();
                int lx = this.width / 2 - lw / 2;
                int rx = this.width / 2 + lw / 2;
                int y0 = y - 2;
                int y1 = y + h + 2;
                Gui.drawRect(lx, y0, lx + 2, y1, ACCENT);
                Gui.drawRect(lx + 2, y0, rx, y1, 0x22FFFFFF);
            }
            GuiLanguage.this.fontRendererObj.setBidiFlag(true);
            String label = this.languageMap.get(this.langCodeList.get(id)).toString();
            GuiLanguage.this.drawCenteredString(GuiLanguage.this.fontRendererObj, label,
                    this.width / 2, y + 1, sel ? 0xFFFFFFFF : 0xFFAAAAAA);
            GuiLanguage.this.fontRendererObj.setBidiFlag(
                    GuiLanguage.this.languageManager.getCurrentLanguage().isBidirectional());
        }
    }
}
