package net.minecraft.client.gui;

import net.minecraft.client.gui.ui.UIManager;
import net.minecraft.client.gui.ui.UIElement;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GuiUISettings extends GuiScreen {
    private final GuiScreen parent;
    private final UIManager ui = UIManager.getInstance();
    private int widgetScroll = 0;
    private int lastMouseX, lastMouseY;
    private final Map<String, Float> toggleAnimMap = new HashMap<>();

    public GuiUISettings(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int btnW = 200;
        // Repositionnés pour éviter les chevauchements
        this.buttonList.add(new GuiButton(201, this.width / 2 - 100, this.height - 52, btnW, 20, "Éditer positions des Widgets..."));
        this.buttonList.add(new GuiButton(200, this.width / 2 - 100, this.height - 28, btnW, 20, I18n.format("gui.done")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 201) {
            this.mc.displayGuiScreen(new GuiUIEditor(this));
        } else if (button.id == 200) {
            this.mc.displayGuiScreen(parent);
        }
    }

    private int getMaxVisible() {
        int available = this.height - 30 - 60 - 70 - 20;
        return Math.max(3, available / 20);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        
        // Title Bar
        int titleW = 120, titleH = 18;
        int tx = (this.width - titleW) / 2, ty = 4;
        GuiRenderUtils.drawRoundedPanel(tx, ty, titleW, titleH, 0xCC111122, 0xFF1A1A30, 0, 0xFF2A7FFF);
        String title = "UI SETTINGS";
        this.fontRendererObj.drawStringWithShadow(title, tx + (titleW - this.fontRendererObj.getStringWidth(title)) / 2, ty + 5, 0xFF8EC8FF);

        GameSettings gs = this.mc.gameSettings;
        int px = this.width / 2 - 150;
        int py = 30;
        int w = 300;
        int rowH = 20;

        ArrayList<UIElement> list = new ArrayList<>(ui.all());
        int total = list.size();
        int maxVis = Math.min(getMaxVisible(), total);
        widgetScroll = Math.max(0, Math.min(widgetScroll, total - maxVis));

        boolean needsScroll = total > maxVis;
        int listH = 24 + maxVis * rowH + (needsScroll ? 20 : 0);

        // --- WIDGET LIST PANEL ---
        GuiRenderUtils.drawRoundedPanel(px, py, w, listH, 0xEE0D0D15, 0xFF151525, 22, 0xFF2A7FFF);
        this.fontRendererObj.drawStringWithShadow("Configuration des Widgets", px + 10, py + 7, 0xFFAAD4FF);

        int y = py + 24;
        int start = widgetScroll;
        int end = Math.min(start + maxVis, total);
        for (int i = start; i < end; i++) {
            UIElement e = list.get(i);
            boolean on = e.isEnabled();
            boolean hovered = inRect(mouseX, mouseY, px, y, w, rowH);
            
            if (hovered) Gui.drawRect(px + 1, y, px + w - 1, y + rowH, 0x11FFFFFF);
            
            int dotCol = on ? 0xFF44EE77 : 0xFF666666;
            Gui.drawRect(px + 8, y + 8, px + 12, y + 12, dotCol);
            
            String name = friendlyName(e.getId());
            this.fontRendererObj.drawStringWithShadow(name, px + 20, y + 6, hovered ? 0xFFFFFFFF : 0xFFCCCCCC);
            
            drawToggle(px + w - 34, y + 4, on);
            y += rowH;
        }

        if (needsScroll) {
            int btnY = py + 24 + maxVis * rowH;
            Gui.drawRect(px + 1, btnY, px + w - 1, btnY + 1, 0x22FFFFFF);
            
            boolean canUp = widgetScroll > 0;
            boolean canDown = widgetScroll < total - maxVis;
            
            String scrollText = (widgetScroll + 1) + "-" + end + " / " + total;
            this.fontRendererObj.drawString(scrollText, px + (w - fontRendererObj.getStringWidth(scrollText)) / 2, btnY + 6, 0xFF555577);
            
            int upCol = canUp ? (inRect(mouseX, mouseY, px + 10, btnY, 40, 20) ? 0xFFFFFFFF : 0xFFAABBFF) : 0xFF444455;
            this.fontRendererObj.drawString("▲ Préc.", px + 10, btnY + 6, upCol);
            
            int downCol = canDown ? (inRect(mouseX, mouseY, px + w - 50, btnY, 40, 20) ? 0xFFFFFFFF : 0xFFAABBFF) : 0xFF444455;
            this.fontRendererObj.drawString("Suiv. ▼", px + w - 50, btnY + 6, downCol);
        }

        // --- MOVEMENT OPTIONS PANEL ---
        // On place ce panneau juste au dessus des boutons du bas
        int oh = 68;
        int oy = this.height - 60 - oh; 
        GuiRenderUtils.drawRoundedPanel(px, oy, w, oh, 0xEE0D0D15, 0xFF151525, 22, 0xFF2ECC71);
        this.fontRendererObj.drawStringWithShadow("Options de mouvement", px + 10, oy + 7, 0xFF88FFCC);

        int optY = oy + 28;
        // Toggle Sneak
        this.fontRendererObj.drawStringWithShadow("Toggle Sneak", px + 12, optY + 2, 0xFFCCCCCC);
        drawToggle(px + w - 34, optY, gs.toggleSneakEnabled);
        
        optY += 20;
        // Toggle Sprint
        this.fontRendererObj.drawStringWithShadow("Toggle Sprint", px + 12, optY + 2, 0xFFCCCCCC);
        drawToggle(px + w - 34, optY, gs.toggleSprintEnabled);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawToggle(int x, int y, boolean value) {
        String key = x + "," + y;
        float target = value ? 1.0f : 0.0f;
        float current = toggleAnimMap.getOrDefault(key, target);
        current = GuiRenderUtils.lerp(current, target, 0.2f);
        toggleAnimMap.put(key, current);
        GuiRenderUtils.drawSmoothToggle(x, y, value, current);
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        GameSettings gs = this.mc.gameSettings;

        int px = this.width / 2 - 150;
        int py = 30;
        int w = 300;
        int rowH = 20;

        ArrayList<UIElement> list = new ArrayList<>(ui.all());
        int total = list.size();
        int maxVis = Math.min(getMaxVisible(), total);
        widgetScroll = Math.max(0, Math.min(widgetScroll, total - maxVis));
        boolean needsScroll = total > maxVis;
        int listH = 24 + maxVis * rowH + (needsScroll ? 20 : 0);

        // List clicks
        int y = py + 24;
        int start = widgetScroll;
        int end = Math.min(start + maxVis, total);
        for (int i = start; i < end; i++) {
            UIElement e = list.get(i);
            if (inRect(mx, my, px, y, w, rowH)) {
                if (inRect(mx, my, px + w - 34, y + 4, 28, 12)) {
                    e.setEnabled(!e.isEnabled());
                    ui.saveConfig();
                } else {
                    this.mc.displayGuiScreen(new net.minecraft.client.gui.GuiUIEditor(this, e.getId()));
                }
                return;
            }
            y += rowH;
        }

        // Scroll clicks
        if (needsScroll) {
            int btnY = py + 24 + maxVis * rowH;
            if (inRect(mx, my, px + 10, btnY, 40, 20) && widgetScroll > 0) {
                widgetScroll--; return;
            }
            if (inRect(mx, my, px + w - 50, btnY, 40, 20) && widgetScroll < total - maxVis) {
                widgetScroll++; return;
            }
        }

        // Movement options
        int oh = 68;
        int oy = this.height - 60 - oh;
        int optY = oy + 28;
        if (inRect(mx, my, px + w - 34, optY, 28, 12)) {
            gs.toggleSneakEnabled = !gs.toggleSneakEnabled;
            if (!gs.toggleSneakEnabled) gs.isToggleSneakActive = false;
            gs.saveOptions();
            return;
        }
        optY += 20;
        if (inRect(mx, my, px + w - 34, optY, 28, 12)) {
            gs.toggleSprintEnabled = !gs.toggleSprintEnabled;
            if (!gs.toggleSprintEnabled) gs.isToggleSprintActive = false;
            gs.saveOptions();
        }
    }

    private boolean inRect(int mx, int my, int rx, int ry, int rw, int rh) {
        return mx >= rx && my >= ry && mx <= rx + rw && my <= ry + rh;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        lastMouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        lastMouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) {
            int px = this.width / 2 - 150;
            int py = 30;
            int w = 300;
            int listH = 24 + Math.min(getMaxVisible(), ui.all().size()) * 20 + (ui.all().size() > getMaxVisible() ? 20 : 0);
            if (inRect(lastMouseX, lastMouseY, px, py, w, listH)) {
                if (scroll > 0) widgetScroll = Math.max(0, widgetScroll - 1);
                else widgetScroll++;
            }
        }
    }

    private String friendlyName(String id) {
        switch (id) {
            case "fps": return "FPS"; case "ping": return "Ping";
            case "biome": return "Biome"; case "coords": return "Coordonnées";
            case "dir": return "Direction"; case "date": return "Date";
            case "helditem": return "Objet tenu"; case "armor_group": return "Armure";
            case "potions": return "Potions"; case "cps": return "CPS";
            case "toggle_sneak": return "Toggle Sneak"; case "toggle_sprint": return "Toggle Sprint";
            default: 
                String s = id.replace('_', ' ');
                return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }
}
