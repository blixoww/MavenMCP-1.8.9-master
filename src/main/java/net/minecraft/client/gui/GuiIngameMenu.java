package net.minecraft.client.gui;

import java.awt.Color;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.realms.RealmsBridge;
import net.minecraft.util.MathHelper;

public class GuiIngameMenu extends GuiScreen {
    
    private float animation = 0.0f;
    private long lastTime = -1L;
    private final int accentColor = new Color(220, 30, 30).getRGB();
    
    // Static session start time to persist across menu openings
    private static long worldSessionStart = -1L;
    private static int lastWorldHash = -1;

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.lastTime = Minecraft.getSystemTime();
        
        // Track the exact moment the world session started
        // Reset only if it's a new world instance or not yet set
        int currentWorldHash = mc.theWorld != null ? mc.theWorld.hashCode() : -1;
        if (worldSessionStart == -1L || currentWorldHash != lastWorldHash) {
            worldSessionStart = System.currentTimeMillis() - (mc.thePlayer.ticksExisted * 50L);
            lastWorldHash = currentWorldHash;
        }
        
        int bWidth = 150;
        int bHeight = 22;
        int px = this.width / 2;
        int py = this.height / 2;

        // --- BUTTONS ---
        this.buttonList.add(new GuiMenuButton(4, px - bWidth / 2, py - 30, bWidth, bHeight, "§f§lCONTINUE", true));
        
        int halfW = (bWidth / 2) - 2;
        this.buttonList.add(new GuiMenuButton(8, px - bWidth / 2, py - 4, halfW, bHeight, "§7HUD"));
        this.buttonList.add(new GuiMenuButton(0, px + 2, py - 4, halfW, bHeight, "§7SETTINGS"));

        this.buttonList.add(new GuiMenuButton(1, px - bWidth / 2, py + 22, bWidth, bHeight, "§c§lDISCONNECT"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0: this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings)); break;
            case 1:
                button.enabled = false;
                worldSessionStart = -1L; // Reset for next session
                lastWorldHash = -1;
                this.mc.theWorld.sendQuittingDisconnectingPacket();
                this.mc.loadWorld((WorldClient)null);
                this.mc.displayGuiScreen(new GuiMainMenu());
                break;
            case 4: this.mc.displayGuiScreen(null); this.mc.setIngameFocus(); break;
            case 8: this.mc.displayGuiScreen(new GuiUISettings(this)); break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = Minecraft.getSystemTime();
        if (lastTime != -1) {
            float dt = (now - lastTime) / 1000.0f;
            animation = MathHelper.clamp_float(animation + dt * 5.0f, 0.0f, 1.0f);
        }
        lastTime = now;

        // 1. Overlay - Smooth dark mask
        this.drawRect(0, 0, this.width, this.height, (int)(animation * 150) << 24);

        GlStateManager.pushMatrix();
        // Subtle slide up
        GlStateManager.translate(0, (1.0f - animation) * 8, 0);

        // --- PANEL ---
        int pW = 180;
        int pH = 155;
        int px = this.width / 2 - pW / 2;
        int py = this.height / 2 - pH / 2 - 5;

        // Shadow
        GuiRenderUtils.drawShadow(px, py, pW, pH, 12, (int)(animation * 130));
        
        // Background - Deep Matte
        Gui.drawRect(px, py, px + pW, py + pH, (int)(animation * 250) << 24 | 0x0C0C0C);
        
        // Border & Accent
        Gui.drawRect(px, py, px + pW, py + 1, accentColor);
        GuiRenderUtils.drawRectOutline(px, py, pW, pH, (int)(animation * 40) << 24 | 0xFFFFFF);

        // --- HEADER ---
        String t1 = "§c§lRED ";
        String t2 = "§f§lCONFLICT";
        int w1 = this.fontRendererObj.getStringWidth(t1);
        int w2 = this.fontRendererObj.getStringWidth(t2);
        int totalW = w1 + w2;
        int titleX = this.width / 2 - totalW / 2;
        int titleY = py + 12;

        this.fontRendererObj.drawStringWithShadow(t1, titleX, titleY, -1);
        this.fontRendererObj.drawStringWithShadow(t2, titleX + w1, titleY, -1);
        
        // Divider - Perfectly Centered under the logo
        int divW = totalW + 20; // Slightly wider than text for elegance
        int divX = this.width / 2 - divW / 2;
        Gui.drawRect(divX, titleY + 14, divX + divW, titleY + 15, (int)(animation * 50) << 24 | 0xFFFFFF);

        // --- FOOTER STATS ---
        renderStats(px, py, pW, pH);

        super.drawScreen(mouseX, mouseY, partialTicks);
        GlStateManager.popMatrix();
    }

    private void renderStats(int px, int py, int pW, int pH) {
        long duration = System.currentTimeMillis() - worldSessionStart;
        long seconds = (duration / 1000) % 60;
        long minutes = (duration / (1000 * 60)) % 60;
        long hours = (duration / (1000 * 60 * 60));
        
        String timeStr = hours > 0 
            ? String.format("%01dh %02dm %02ds", hours, minutes, seconds) 
            : String.format("%02dm %02ds", minutes, seconds);
            
        String fpsStr = Minecraft.getDebugFPS() + " FPS";
        
        // Precision Session Info
        this.fontRendererObj.drawString("§8Session: §f" + timeStr, px + 12, py + pH - 14, -1);
        
        int fpsW = this.fontRendererObj.getStringWidth(fpsStr);
        this.fontRendererObj.drawString(fpsStr, px + pW - fpsW - 12, py + pH - 14, accentColor);
    }
}
