package net.minecraft.client.gui;

import java.awt.Color;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.MathHelper;

public class GuiOptions extends GuiScreen implements GuiYesNoCallback {
    
    private final GuiScreen parentScreen;
    private final GameSettings settings;
    
    private float animation = 0.0f;
    private long lastTime = -1L;
    private final int accentColor = new Color(220, 30, 30).getRGB();
    private int[] btnYCache;

    public GuiOptions(GuiScreen parent, GameSettings settings) {
        this.parentScreen = parent;
        this.settings = settings;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.lastTime = Minecraft.getSystemTime();

        int bWidth = 150;
        int bHeight = 22;
        int px = this.width / 2;
        int py = this.height / 2;

        // --- Layout (Remonté de 10px pour que Done rentre mieux) ---
        // Top row
        this.buttonList.add(new GuiMenuButton(101, px - bWidth - 5, py - 55, bWidth, bHeight, "VIDEO SETTINGS", false));
        this.buttonList.add(new GuiMenuButton(106, px + 5, py - 55, bWidth, bHeight, "SOUNDS"));

        // Middle rows
        this.buttonList.add(new GuiMenuButton(100, px - bWidth - 5, py - 29, bWidth, bHeight, "CONTROLS"));
        this.buttonList.add(new GuiMenuButton(103, px + 5, py - 29, bWidth, bHeight, "CHAT"));

        this.buttonList.add(new GuiMenuButton(105, px - bWidth - 5, py - 3, bWidth, bHeight, "RESOURCES"));
        this.buttonList.add(new GuiMenuButton(102, px + 5, py - 3, bWidth, bHeight, "LANGUAGE"));

        this.buttonList.add(new GuiMenuButton(110, px - bWidth - 5, py + 23, bWidth, bHeight, "SKIN CUSTOMS"));
        this.buttonList.add(new GuiMenuButton(104, px + 5, py + 23, bWidth, bHeight, "SNOOPER"));

        // Bottom (Done)
        this.buttonList.add(new GuiMenuButton(200, px - 75, py + 55, 150, bHeight, "DONE"));

        this.btnYCache = new int[this.buttonList.size()];
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) return;

        switch (button.id) {
            case 100: this.mc.displayGuiScreen(new GuiControls(this, this.settings)); break;
            case 101: this.mc.displayGuiScreen(new GuiVideoSettings(this, this.settings)); break;
            case 102: this.mc.displayGuiScreen(new GuiLanguage(this, this.settings, this.mc.getLanguageManager())); break;
            case 103: this.mc.displayGuiScreen(new ScreenChatOptions(this, this.settings)); break;
            case 104: this.mc.displayGuiScreen(new GuiSnooper(this, this.settings)); break;
            case 105: this.mc.displayGuiScreen(new GuiScreenResourcePacks(this)); break;
            case 106: this.mc.displayGuiScreen(new GuiScreenOptionsSounds(this, this.settings)); break;
            case 110: this.mc.displayGuiScreen(new GuiCustomizeSkin(this)); break;
            case 200: 
                this.settings.saveOptions();
                this.mc.displayGuiScreen(this.parentScreen); 
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = Minecraft.getSystemTime();
        if (lastTime != -1) {
            float dt = (now - lastTime) / 1000.0f;
            animation = MathHelper.clamp_float(animation + dt * 4.0f, 0.0f, 1.0f);
        }
        lastTime = now;

        // ── Fond : panorama si on vient du menu principal, sinon carré noir
        if (parentScreen instanceof GuiMainMenu) {
            ((GuiMainMenu) parentScreen).drawBackdrop(mouseX, mouseY, partialTicks);
            // Voile supplémentaire très léger pour assombrir légèrement derrière le panel
            this.drawRect(0, 0, this.width, this.height, (int)(animation * 60) << 24);
        } else {
            this.drawRect(0, 0, this.width, this.height, (int)(animation * 150) << 24);
        }

        float easedAnim = animation * animation * (3.0f - 2.0f * animation);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, (1.0f - easedAnim) * 10, 0);

        int pW = 340;
        int pH = 190; // Légèrement agrandi
        int px = this.width / 2 - pW / 2;
        int py = this.height / 2 - pH / 2 - 10;

        GuiRenderUtils.drawShadow(px, py, pW, pH, 12, (int)(animation * 130));
        // Fond à 80% d'opacité (204 sur 255)
        Gui.drawRect(px, py, px + pW, py + pH, (int)(animation * 204) << 24 | 0x0C0C0C);
        Gui.drawRect(px, py, px + pW, py + 1, (int)(animation * 255) << 24 | (accentColor & 0xFFFFFF));
        GuiRenderUtils.drawRectOutline(px, py, pW, pH, (int)(animation * 40) << 24 | 0xFFFFFF);

        String t1 = "§c§lCLIENT ";
        String t2 = "§f§lOPTIONS";
        int w1 = this.fontRendererObj.getStringWidth(t1);
        int w2 = this.fontRendererObj.getStringWidth(t2);
        int totalW = w1 + w2;
        int titleX = this.width / 2 - totalW / 2;
        int titleY = py + 12;

        int textAlpha = (int)(animation * 255) << 24;
        this.fontRendererObj.drawStringWithShadow(t1, titleX, titleY, textAlpha | 0xFFFFFF);
        this.fontRendererObj.drawStringWithShadow(t2, titleX + w1, titleY, textAlpha | 0xFFFFFF);
        
        int divW = (int)((totalW + 20) * easedAnim);
        int divX = this.width / 2 - divW / 2;
        Gui.drawRect(divX, titleY + 14, divX + divW, titleY + 15, (int)(animation * 50) << 24 | 0xFFFFFF);

        if (btnYCache == null || btnYCache.length != this.buttonList.size()) {
            btnYCache = new int[this.buttonList.size()];
        }

        for (int i = 0; i < this.buttonList.size(); i++) {
            GuiButton b = this.buttonList.get(i);
            btnYCache[i] = b.yPosition;
            float stagger = (i / 2) * 0.15f;
            float btnAnim = MathHelper.clamp_float(animation * 1.5f - stagger, 0.0f, 1.0f);
            btnAnim = btnAnim * btnAnim * (3.0f - 2.0f * btnAnim);
            b.yPosition += (int)((1.0f - btnAnim) * 15);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        for (int i = 0; i < this.buttonList.size(); i++) {
            this.buttonList.get(i).yPosition = btnYCache[i];
        }

        GlStateManager.popMatrix();
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        this.mc.displayGuiScreen(this);
    }
}
