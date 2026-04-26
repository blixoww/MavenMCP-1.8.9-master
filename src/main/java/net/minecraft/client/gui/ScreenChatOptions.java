package net.minecraft.client.gui;

import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.MathHelper;

public class ScreenChatOptions extends GuiScreen
{
    private static final GameSettings.Options[] OPTIONS = new GameSettings.Options[] {
        GameSettings.Options.CHAT_VISIBILITY, GameSettings.Options.CHAT_COLOR,
        GameSettings.Options.CHAT_LINKS, GameSettings.Options.CHAT_OPACITY,
        GameSettings.Options.CHAT_LINKS_PROMPT, GameSettings.Options.CHAT_SCALE,
        GameSettings.Options.CHAT_HEIGHT_FOCUSED, GameSettings.Options.CHAT_HEIGHT_UNFOCUSED,
        GameSettings.Options.CHAT_WIDTH, GameSettings.Options.REDUCED_DEBUG_INFO
    };

    private static final int ACCENT  = 0xFFDC1E1E;
    private static final int C_MUTED = 0xFF707880;

    private final GuiScreen parentScreen;
    private final GameSettings game_settings;
    private String field_146401_i;

    private float animation = 0f;
    private long  animLastTime = -1L;
    private int[] btnYCache;

    public ScreenChatOptions(GuiScreen parentScreenIn, GameSettings gameSettingsIn)
    {
        this.parentScreen = parentScreenIn;
        this.game_settings = gameSettingsIn;
    }

    public void initGui()
    {
        this.buttonList.clear();
        this.animation = 0f; this.animLastTime = -1L;
        this.field_146401_i = I18n.format("options.chat.title");

        int cols = 2, btnW = 150, btnH = 22, gap = 4;
        int startX = this.width / 2 - btnW - 5;
        int startY = 50;
        int i = 0;
        for (GameSettings.Options opt : OPTIONS) {
            int bx = this.width / 2 + (i % cols == 0 ? -btnW - 5 : 5);
            int by = startY + (i / cols) * (btnH + gap);
            if (opt.getEnumFloat()) {
                this.buttonList.add(new GuiStyledSlider(opt.returnEnumOrdinal(), bx, by, btnW, btnH + 6, opt));
            } else {
                this.buttonList.add(new GuiMenuButton(opt.returnEnumOrdinal(), bx, by, btnW, btnH,
                        this.game_settings.getKeyBinding(opt)));
            }
            ++i;
        }
        int doneY = startY + ((i + 1) / cols) * (btnH + gap) + 10;
        this.buttonList.add(new GuiMenuButton(200, this.width / 2 - 75, doneY, 150, btnH, "DONE", true));
        this.btnYCache = new int[this.buttonList.size()];
    }

    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (!button.enabled) return;
        if (button.id < 100) {
            GameSettings.Options opt = GameSettings.Options.getEnumOptions(button.id);
            if (opt != null && !opt.getEnumFloat()) {
                this.game_settings.setOptionValue(opt, 1);
                button.displayString = this.game_settings.getKeyBinding(opt);
            }
        } else if (button.id == 200) {
            this.mc.gameSettings.saveOptions();
            this.mc.displayGuiScreen(this.parentScreen);
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        long now = Minecraft.getSystemTime();
        if (animLastTime != -1L) animation = MathHelper.clamp_float(animation + (now - animLastTime) / 250f, 0f, 1f);
        animLastTime = now;
        float e = animation * animation * (3f - 2f * animation);

        this.drawDefaultBackground();

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, (1f - e) * 8, 0);

        int ta = (int)(e * 255) << 24;
        // Ombre douce sous le titre
        GuiRenderUtils.drawGradientRect(0, 0, this.width, 38, (int)(e*160) << 24 | 0x000000, 0x00000000);
        // Ligne rouge fine
        Gui.drawRect(0, 0, this.width, 1, (int)(e * 255) << 24 | (ACCENT & 0xFFFFFF));

        String t1 = "§c§lOPTIONS ";
        String t2 = "§f§lCHAT";
        int tw = fontRendererObj.getStringWidth(t1) + fontRendererObj.getStringWidth(t2);
        int tx = this.width / 2 - tw / 2;
        fontRendererObj.drawStringWithShadow(t1, tx, 11, ta | 0xFFFFFF);
        fontRendererObj.drawStringWithShadow(t2, tx + fontRendererObj.getStringWidth(t1), 11, ta | 0xFFFFFF);
        int dw = (int)((tw + 20) * e);
        Gui.drawRect(this.width/2 - dw/2, 23, this.width/2 + dw/2, 24, (int)(e*60) << 24 | 0xFFFFFF);

        // Stagger buttons
        if (btnYCache == null || btnYCache.length != this.buttonList.size()) btnYCache = new int[this.buttonList.size()];
        for (int i2 = 0; i2 < this.buttonList.size(); i2++) {
            GuiButton b = this.buttonList.get(i2);
            btnYCache[i2] = b.yPosition;
            float ba = MathHelper.clamp_float(animation * 2f - i2 * 0.08f, 0f, 1f);
            ba = ba * ba * (3f - 2f * ba);
            b.yPosition += (int)((1f - ba) * 12);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        for (int i2 = 0; i2 < this.buttonList.size(); i2++) this.buttonList.get(i2).yPosition = btnYCache[i2];


        GlStateManager.popMatrix();
    }
}
