package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ui.HudProfileManager;
import net.minecraft.client.gui.ui.UIManager;
import net.minecraft.client.gui.ui.UIElement;
import net.minecraft.util.MathHelper;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * Modern HUD Profiles GUI.
 * Features card-based layout, smooth animations, and clear actions.
 */
public class GuiHudProfiles extends GuiScreen {

    private final GuiScreen parent;
    private static final int ACCENT = 0xFF2A7FFF;
    private static final int BG_CARD = 0xFF16161E;
    private static final int BORDER = 0x15FFFFFF;
    private static final int TEXT_PRIMARY = 0xFFEEEEEE;
    private static final int TEXT_SECONDARY = 0xFFAAAAAA;
    private static final int TEXT_MUTED = 0xFF555555;
    
    private float openAnim = 0.0f;
    private long lastTime = -1L;

    private int renamingSlot = -1;
    private String renameBuffer = "";

    private int confirmDeleteSlot = -1;
    private long confirmDeleteTime = 0;

    public GuiHudProfiles(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        this.lastTime = Minecraft.getSystemTime();
        this.openAnim = 0.0f;
        this.confirmDeleteSlot = -1;
        Keyboard.enableRepeatEvents(true);
        // On active l'affichage des éléments fictifs (preview)
        UIManager.getInstance().setEditorActive(true);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        // On désactive l'affichage des éléments fictifs
        UIManager.getInstance().setEditorActive(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = Minecraft.getSystemTime();
        if (lastTime > 0) {
            float dt = (now - lastTime) / 1000.0f;
            openAnim = MathHelper.clamp_float(openAnim + dt * 4.0f, 0.0f, 1.0f);
        }
        lastTime = now;
        
        float ease = openAnim * (2.0f - openAnim);

        this.drawRect(0, 0, this.width, this.height, (int)(ease * 180) << 24);

        // Rendu des widgets en arrière-plan (avec preview active)
        UIManager.getInstance().renderAll(mouseX, mouseY, partialTicks);

        HudProfileManager pm = HudProfileManager.getInstance();
        
        int cardW = 320;
        int cardH = 45;
        int gap = 8;
        int totalH = (cardH + gap) * HudProfileManager.MAX_PROFILES - gap;
        
        int startX = (this.width - cardW) / 2;
        int startY = (this.height - totalH) / 2;
        // Lift up if footer would overflow
        if (startY + totalH + 40 > this.height) startY = 10;

        String title = "GESTION DES PROFILS";
        int titleW = fontRendererObj.getStringWidth(title);
        GlStateManager.pushMatrix();
        GlStateManager.translate(this.width / 2.0f, startY - 25, 0);
        GlStateManager.scale(1.1f, 1.1f, 1.0f);
        fontRendererObj.drawStringWithShadow(title, -titleW / 2.0f, 0, 0xFF8EC8FF);
        GlStateManager.popMatrix();

        if (confirmDeleteSlot >= 0 && now - confirmDeleteTime > 3000) {
            confirmDeleteSlot = -1;
        }

        for (int i = 0; i < HudProfileManager.MAX_PROFILES; i++) {
            drawProfileCard(i, startX, startY + i * (cardH + gap), cardW, cardH, mouseX, mouseY, pm);
        }

        int btnW = 100, btnH = 18;
        int btnX = (this.width - btnW) / 2;
        int btnY = Math.min(this.height - 25, startY + totalH + 15);
        boolean hover = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;
        drawStyledButton(btnX, btnY, btnW, btnH, "RETOUR", BG_CARD, hover);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawProfileCard(int slot, int x, int y, int w, int h, int mx, int my, HudProfileManager pm) {
        boolean active = pm.getActiveProfile() == slot;
        boolean used = pm.isSlotUsed(slot);
        boolean hoverCard = mx >= x && mx < x + w && my >= y && my < y + h;
        
        int bg = active ? 0xFF1C1C26 : (hoverCard ? 0xFF1A1A22 : BG_CARD);
        Gui.drawRect(x, y, x + w, y + h, bg);
        GuiRenderUtils.drawRectOutline(x, y, w, h, active ? ACCENT : BORDER);
        if (active) Gui.drawRect(x, y, x + 2, y + h, ACCENT);

        int iconS = 24;
        int ix = x + 10, iy = y + (h - iconS) / 2;
        Gui.drawRect(ix, iy, ix + iconS, iy + iconS, active ? ACCENT : 0x20FFFFFF);
        String numStr = String.valueOf(slot + 1);
        fontRendererObj.drawStringWithShadow(numStr, ix + (iconS - fontRendererObj.getStringWidth(numStr))/2f, iy + 8, 0xFFFFFFFF);

        int tx = x + 45;
        if (renamingSlot == slot) {
            String disp = renameBuffer + (System.currentTimeMillis() / 500 % 2 == 0 ? "_" : "");
            Gui.drawRect(tx, y + 10, tx + 120, y + 24, 0x30000000);
            GuiRenderUtils.drawRectOutline(tx, y + 10, 120, 14, ACCENT);
            fontRendererObj.drawStringWithShadow(disp, tx + 4, y + 13, TEXT_PRIMARY);
        } else {
            String name = pm.getProfileName(slot);
            fontRendererObj.drawStringWithShadow(name, tx, y + 10, active ? 0xFFAAD4FF : TEXT_PRIMARY);
            String sub = used ? pm.getProfileDescription(slot) : "\u00A78Emplacement vide";
            if (active) sub = "\u00A7a\u00A7lACTIF";
            fontRendererObj.drawString(sub, tx, y + 24, TEXT_SECONDARY);
        }

        int bx = x + w - 10, bh = 16, by = y + (h - bh) / 2, btnGap = 5;

        // Reset default profiles button (only for slot 0 and 1 if empty)
        if (!used && (slot == 0 || slot == 1)) {
            int resW = 60; bx -= resW;
            boolean hovRes = mx >= bx && mx < bx + resW && my >= by && my < by + bh;
            drawStyledButton(bx, by, resW, bh, "Restaurer", 0x22FFFFFF, hovRes);
            bx -= btnGap;
        }

        if (used) {
            boolean isConfirm = confirmDeleteSlot == slot;
            int delW = isConfirm ? 70 : 18; bx -= delW;
            boolean hovDel = mx >= bx && mx < bx + delW && my >= by && my < by + bh;
            drawStyledButton(bx, by, delW, bh, isConfirm ? "Confirmer?" : "X", 0x22E74C3C, hovDel);
            bx -= btnGap;
        }

        if (used && !active) {
            int loadW = 50; bx -= loadW;
            boolean hovLoad = mx >= bx && mx < bx + loadW && my >= by && my < by + bh;
            drawStyledButton(bx, by, loadW, bh, "Charger", 0x222A7FFF, hovLoad);
            bx -= btnGap;
        }

        int saveW = used ? 55 : 80; bx -= saveW;
        boolean hovSave = mx >= bx && mx < bx + saveW && my >= by && my < by + bh;
        drawStyledButton(bx, by, saveW, bh, used ? "Sauver" : "Sauvegarder", 0x222ECC71, hovSave);

        if (used && renamingSlot != slot) {
            int renX = tx + fontRendererObj.getStringWidth(pm.getProfileName(slot)) + 8;
            int renW = 12, renH = 12;
            boolean hovRen = mx >= renX && mx < renX + renW && my >= y + 10 && my < y + 10 + renH;
            Gui.drawRect(renX, y + 10, renX + renW, y + 10 + renH, hovRen ? 0x22FFFFFF : 0x10FFFFFF);
            GuiRenderUtils.drawRectOutline(renX, y + 10, renW, renH, hovRen ? ACCENT : BORDER);
            fontRendererObj.drawString("\u270E", renX + 2, y + 12, hovRen ? ACCENT : TEXT_MUTED);
        }
    }

    private void drawStyledButton(int x, int y, int w, int h, String text, int baseColor, boolean hovered) {
        int bg = hovered ? GuiRenderUtils.colorLerp(baseColor, 0xFFFFFFFF, 0.12f) : baseColor;
        Gui.drawRect(x, y, x + w, y + h, bg);
        GuiRenderUtils.drawRectOutline(x, y, w, h, hovered ? ACCENT : BORDER);
        int textColor = hovered ? TEXT_PRIMARY : TEXT_SECONDARY;
        if (baseColor == 0x22E74C3C) textColor = hovered ? 0xFFFFFFFF : 0xFFCC4444;
        if (baseColor == 0x222A7FFF) textColor = hovered ? 0xFFFFFFFF : 0xFF88AAFF;
        if (baseColor == 0x222ECC71) textColor = hovered ? 0xFFFFFFFF : 0xFF66AA66;
        fontRendererObj.drawStringWithShadow(text, x + (w - fontRendererObj.getStringWidth(text))/2f, y + (h - 8)/2f, textColor);
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        if (btn != 0) return;
        HudProfileManager pm = HudProfileManager.getInstance();
        int cardW = 320, cardH = 45, gap = 8, totalH = (cardH + gap) * HudProfileManager.MAX_PROFILES - gap;
        int startX = (this.width - cardW) / 2, startY = (this.height - totalH) / 2;
        if (startY + totalH + 40 > this.height) startY = 10;

        int bY = Math.min(this.height - 25, startY + totalH + 15);
        if (mx >= (this.width - 100)/2 && mx < (this.width + 100)/2 && my >= bY && my < bY + 18) {
            this.mc.displayGuiScreen(parent); return;
        }

        if (renamingSlot >= 0) {
            int tx = startX + 45, ty = startY + renamingSlot * (cardH + gap) + 10;
            if (!(mx >= tx && mx < tx + 120 && my >= ty && my < ty + 14)) finishRenaming();
            return;
        }

        for (int i = 0; i < HudProfileManager.MAX_PROFILES; i++) {
            int x = startX, y = startY + i * (cardH + gap);
            if (mx >= x && mx < x + cardW && my >= y && my < y + cardH) {
                boolean used = pm.isSlotUsed(i), active = pm.getActiveProfile() == i;
                int bx = x + cardW - 10, bh = 16, by = y + (cardH - bh) / 2, btnGap = 5;

                // Restore default profile
                if (!used && (i == 0 || i == 1)) {
                    int resW = 60; bx -= resW;
                    if (mx >= bx && mx < bx + resW && my >= by && my < by + bh) {
                        if (i == 0) pm.initDefaultPvPProfile(); else pm.initDefaultExplorationProfile();
                        return;
                    }
                    bx -= btnGap;
                }

                if (used) {
                    boolean isConfirm = confirmDeleteSlot == i;
                    int dw = isConfirm ? 70 : 18; bx -= dw;
                    if (mx >= bx && mx < bx + dw && my >= by && my < by + bh) {
                        if (isConfirm) { pm.deleteSlot(i); confirmDeleteSlot = -1; } else { confirmDeleteSlot = i; confirmDeleteTime = Minecraft.getSystemTime(); }
                        return;
                    }
                    bx -= btnGap;
                }

                if (used && !active) {
                    int loadW = 50; bx -= loadW;
                    if (mx >= bx && mx < bx + loadW && my >= by && my < by + bh) { pm.loadFromSlot(i); confirmDeleteSlot = -1; return; }
                    bx -= btnGap;
                }

                int saveW = used ? 55 : 80; bx -= saveW;
                if (mx >= bx && mx < bx + saveW && my >= by && my < by + bh) { pm.saveToSlot(i); confirmDeleteSlot = -1; return; }

                int tx = x + 45, renX = tx + fontRendererObj.getStringWidth(pm.getProfileName(i)) + 8;
                if (used && mx >= renX && mx < renX + 12 && my >= y + 10 && my < y + 22) { renamingSlot = i; renameBuffer = pm.getProfileName(i); return; }
                if (used && renamingSlot == -1 && mx >= tx && mx < tx + fontRendererObj.getStringWidth(pm.getProfileName(i)) + 5 && my >= y + 10 && my < y + 24) { renamingSlot = i; renameBuffer = pm.getProfileName(i); return; }
            }
        }
        confirmDeleteSlot = -1;
    }

    private void finishRenaming() {
        if (renamingSlot >= 0) {
            if (!renameBuffer.trim().isEmpty()) { HudProfileManager.getInstance().setProfileName(renamingSlot, renameBuffer.trim()); HudProfileManager.getInstance().save(); }
            renamingSlot = -1;
        }
    }

    @Override
    protected void keyTyped(char c, int keyCode) throws IOException {
        if (renamingSlot >= 0) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) finishRenaming();
            else if (keyCode == Keyboard.KEY_BACK) { if (!renameBuffer.isEmpty()) renameBuffer = renameBuffer.substring(0, renameBuffer.length() - 1); }
            else if (renameBuffer.length() < 16 && c >= 32) renameBuffer += c;
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) this.mc.displayGuiScreen(parent);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }
}
