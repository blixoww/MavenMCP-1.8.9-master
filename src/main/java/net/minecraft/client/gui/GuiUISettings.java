package net.minecraft.client.gui;

import net.minecraft.client.gui.ui.UIManager;
import net.minecraft.client.gui.ui.UIElement;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;

import java.io.IOException;
import java.util.ArrayList;

public class GuiUISettings extends GuiScreen {
    private final GuiScreen parent;
    private final UIManager ui = UIManager.getInstance();
    private int widgetScroll = 0;

    public GuiUISettings(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(201, this.width / 2 - 100, this.height - 56, 200, 20, "Éditer positions des Widgets..."));
        this.buttonList.add(new GuiButton(200, this.width / 2 - 100, this.height - 28, I18n.format("gui.done")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 201) {
            this.mc.displayGuiScreen(new net.minecraft.client.gui.GuiUIEditor(this));
        } else if (button.id == 200) {
            this.mc.displayGuiScreen(parent);
        }
    }

    // Calcule le nombre max de widgets visibles sans déborder
    private int getMaxVisible() {
        // Hauteur disponible : du haut de la liste jusqu'au dessus de la section options + marges
        // Section options = 80px, marge bas boutons = 65px, marge = 20px
        int available = this.height - 28 - 80 - 65 - 20 - 20; // 20 = header liste, 20 = marge section options
        return Math.max(2, available / 18);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, "UI Settings", this.width / 2, 8, 0xFFFFFF);

        GameSettings gs = this.mc.gameSettings;

        int px = this.width / 2 - 150;
        int py = 28;
        int w = 300;
        int rowH = 18;

        ArrayList<UIElement> list = new ArrayList<>();
        for (UIElement e : ui.all()) {
            if (!"crosshair".equals(e.getId())) list.add(e);
        }
        int total = list.size();
        int maxVis = Math.min(getMaxVisible(), total);
        // Clamp scroll
        widgetScroll = Math.max(0, Math.min(widgetScroll, total - maxVis));

        boolean needsScroll = total > maxVis;
        // La liste occupe les lignes + header (20) + éventuellement 16px pour boutons scroll en bas
        int listH = 20 + maxVis * rowH + (needsScroll ? 18 : 0);

        // Fond de la section widgets
        drawRect(px, py, px + w, py + listH, 0xCC181C24);
        // Bordure bleue 4 côtés
        drawRect(px, py, px + w, py + 1, 0xFF2A7FFF);
        drawRect(px, py + listH - 1, px + w, py + listH, 0xFF2A7FFF);
        drawRect(px, py, px + 1, py + listH, 0xFF2A7FFF);
        drawRect(px + w - 1, py, px + w, py + listH, 0xFF2A7FFF);
        // Header
        drawRect(px + 1, py + 1, px + w - 1, py + 19, 0xFF0D1020);
        this.fontRendererObj.drawStringWithShadow("Widgets", px + 6, py + 6, 0xFFAADDFF);

        // Lignes de widgets
        int y = py + 20;
        int start = widgetScroll;
        int end = Math.min(start + maxVis, total);
        for (int i = start; i < end; i++) {
            UIElement e = list.get(i);
            boolean on = e.isEnabled();
            String name = friendlyName(e.getId());
            // Alternance fond
            if (i % 2 == 0) drawRect(px + 1, y, px + w - 1, y + rowH, 0x110A0A20);
            this.fontRendererObj.drawString(name, px + 10, y + 4, 0xDDDDDD);
            this.fontRendererObj.drawString(on ? "ON" : "OFF", px + w - 68, y + 4, on ? 0xFF88FF88 : 0xFF888888);
            drawToggle(px + w - 40, y + 3, on);
            y += rowH;
        }

        // Boutons scroll en bas de la liste (si besoin)
        if (needsScroll) {
            int btnY = py + 20 + maxVis * rowH + 1;
            int halfW = w / 2 - 2;
            // Bouton ▲ (monter)
            boolean canUp = widgetScroll > 0;
            drawRect(px + 1, btnY, px + 1 + halfW, btnY + 15, canUp ? 0xFF1A2A3A : 0xFF0D1020);
            drawRect(px + 1, btnY, px + 1 + halfW, btnY + 1, canUp ? 0xFF2A7FFF : 0xFF222233);
            int upCol = canUp ? 0xFF88BBFF : 0xFF444455;
            String upLabel = "▲  Précédent";
            this.fontRendererObj.drawString(upLabel,
                    px + 1 + (halfW - this.fontRendererObj.getStringWidth(upLabel)) / 2, btnY + 4, upCol);
            // Bouton ▼ (descendre)
            boolean canDown = widgetScroll < total - maxVis;
            drawRect(px + 3 + halfW, btnY, px + w - 1, btnY + 15, canDown ? 0xFF1A2A3A : 0xFF0D1020);
            drawRect(px + 3 + halfW, btnY, px + w - 1, btnY + 1, canDown ? 0xFF2A7FFF : 0xFF222233);
            int downCol = canDown ? 0xFF88BBFF : 0xFF444455;
            String downLabel = "Suivant  ▼";
            this.fontRendererObj.drawString(downLabel,
                    px + 3 + halfW + (halfW - this.fontRendererObj.getStringWidth(downLabel)) / 2, btnY + 4, downCol);
            // Compteur
            String counter = (widgetScroll + 1) + "-" + end + " / " + total;
            int cw = this.fontRendererObj.getStringWidth(counter);
            this.fontRendererObj.drawString(counter, px + (w - cw) / 2, btnY + 4, 0xFF555577);
        }

        // ── Section Options de mouvement ──────────────────────────────────
        int oy = py + listH + 12;
        int oh = 76;
        drawRect(px, oy, px + w, oy + oh, 0x55001122);
        drawRect(px, oy, px + w, oy + 1, 0xFF22CC66);
        drawRect(px, oy + oh - 1, px + w, oy + oh, 0xFF22CC66);
        drawRect(px, oy, px + 1, oy + oh, 0xFF22CC66);
        drawRect(px + w - 1, oy, px + w, oy + oh, 0xFF22CC66);
        // Header vert
        drawRect(px + 1, oy + 1, px + w - 1, oy + 18, 0xFF001A0D);
        this.fontRendererObj.drawStringWithShadow("Options de mouvement", px + 8, oy + 5, 0xFF88FFCC);

        // Toggle Sneak
        boolean sneakEnabled = gs.toggleSneakEnabled;
        this.fontRendererObj.drawString("Toggle Sneak", px + 10, oy + 24, 0xDDDDDD);
        this.fontRendererObj.drawString(sneakEnabled ? "§aActivé" : "§cDésactivé", px + 130, oy + 24, 0xFFFFFFFF);
        drawToggle(px + w - 40, oy + 22, sneakEnabled);

        // Toggle Sprint
        boolean sprintEnabled = gs.toggleSprintEnabled;
        this.fontRendererObj.drawString("Toggle Sprint", px + 10, oy + 46, 0xDDDDDD);
        this.fontRendererObj.drawString(sprintEnabled ? "§aActivé" : "§cDésactivé", px + 130, oy + 46, 0xFFFFFFFF);
        drawToggle(px + w - 40, oy + 44, sprintEnabled);

        this.fontRendererObj.drawString("§7Utilise la touche Sneak/Sprint classique", px + 10, oy + 62, 0x77AAAAAA);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawToggle(int x, int y, boolean value) {
        int tw = 28, th = 12;
        drawRect(x, y, x + tw, y + th, value ? 0xFF44DD88 : 0xFF555555);
        drawRect(x, y, x + tw, y + 1, value ? 0xFF66FFAA : 0xFF333333);
        int knob = value ? x + tw - th : x;
        drawRect(knob, y, knob + th, y + th, 0xFFFFFFFF);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        GameSettings gs = this.mc.gameSettings;

        int px = this.width / 2 - 150;
        int py = 28;
        int w = 300;
        int rowH = 18;

        ArrayList<UIElement> list = new ArrayList<>();
        for (UIElement e : ui.all()) {
            if (!"crosshair".equals(e.getId())) list.add(e);
        }
        int total = list.size();
        int maxVis = Math.min(getMaxVisible(), total);
        widgetScroll = Math.max(0, Math.min(widgetScroll, total - maxVis));
        boolean needsScroll = total > maxVis;
        int listH = 20 + maxVis * rowH + (needsScroll ? 18 : 0);

        // Clics sur les lignes widgets
        int y = py + 20;
        int start = widgetScroll;
        int end = Math.min(start + maxVis, total);
        for (int i = start; i < end; i++) {
            UIElement e = list.get(i);
            if (mouseY >= y && mouseY <= y + rowH && mouseX >= px && mouseX <= px + w) {
                if (mouseX >= px + w - 40 && mouseX <= px + w - 12) {
                    // Clic toggle ON/OFF
                    e.setEnabled(!e.isEnabled());
                    ui.saveConfig();
                    return;
                }
                // Clic sur le nom → ouvrir l'éditeur positionné sur ce widget
                this.mc.displayGuiScreen(new net.minecraft.client.gui.GuiUIEditor(this, e.getId()));
                return;
            }
            y += rowH;
        }

        // Clics boutons scroll
        if (needsScroll) {
            int btnY = py + 20 + maxVis * rowH + 1;
            int halfW = w / 2 - 2;
            // Bouton ▲
            if (mouseY >= btnY && mouseY <= btnY + 15 && mouseX >= px + 1 && mouseX <= px + 1 + halfW) {
                if (widgetScroll > 0) widgetScroll--;
                return;
            }
            // Bouton ▼
            if (mouseY >= btnY && mouseY <= btnY + 15 && mouseX >= px + 3 + halfW && mouseX <= px + w - 1) {
                if (widgetScroll < total - maxVis) widgetScroll++;
                return;
            }
        }

        // Clics options de mouvement
        int oy = py + listH + 12;
        // Toggle Sneak
        if (mouseX >= px + w - 40 && mouseX <= px + w - 12 && mouseY >= oy + 22 && mouseY <= oy + 34) {
            gs.toggleSneakEnabled = !gs.toggleSneakEnabled;
            if (!gs.toggleSneakEnabled) gs.isToggleSneakActive = false;
            gs.saveOptions();
            return;
        }
        // Toggle Sprint
        if (mouseX >= px + w - 40 && mouseX <= px + w - 12 && mouseY >= oy + 44 && mouseY <= oy + 56) {
            gs.toggleSprintEnabled = !gs.toggleSprintEnabled;
            if (!gs.toggleSprintEnabled) gs.isToggleSprintActive = false;
            gs.saveOptions();
        }
    }

    private String friendlyName(String id) {
        if ("armor_group".equals(id)) return "Armure";
        if ("keystrokes".equals(id)) return "Touches";
        if ("fps".equals(id)) return "FPS";
        if ("ping".equals(id)) return "Ping";
        if ("biome".equals(id)) return "Biome";
        if ("coords".equals(id)) return "Coordonnées";
        if ("dir".equals(id)) return "Direction";
        if ("date".equals(id)) return "Date";
        if ("helditem".equals(id)) return "Objet tenu";
        if ("potions".equals(id)) return "Potions";
        if ("toggle_sneak".equals(id)) return "Toggle Sneak";
        if ("toggle_sprint".equals(id)) return "Toggle Sprint";
        if ("cps".equals(id)) return "CPS";
        String s = id.replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
