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
        int available = this.height - 40 - 80 - 100; // Marge pour titre, options et boutons
        return Math.max(4, available / 22);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        
        // --- TITLE ---
        int titleW = 160, titleH = 22;
        int tx = (this.width - titleW) / 2, ty = 6;
        GuiRenderUtils.drawRoundedPanel(tx, ty, titleW, titleH, 0xEE0D0D15, 0xFF151525, 0, 0xFF2A7FFF);
        String title = "UI SETTINGS";
        this.fontRendererObj.drawStringWithShadow(title, tx + (titleW - this.fontRendererObj.getStringWidth(title)) / 2, ty + 7, 0xFF8EC8FF);

        GameSettings gs = this.mc.gameSettings;
        int px = this.width / 2 - 160;
        int w = 320;
        int rowH = 22;

        ArrayList<UIElement> list = new ArrayList<>(ui.all());
        int total = list.size();
        int maxVis = Math.min(getMaxVisible(), total);
        widgetScroll = Math.max(0, Math.min(widgetScroll, total - maxVis));

        // --- WIDGETS PANEL ---
        int py = 35;
        int listH = 28 + maxVis * rowH + (total > maxVis ? 22 : 8);
        GuiRenderUtils.drawRoundedPanel(px, py, w, listH, 0xEE0D0D15, 0xFF151525, 26, 0xFF2A7FFF);
        GuiRenderUtils.drawSectionHeader(this.fontRendererObj, px, py + 4, w, "Configuration des Widgets", 0xFF2A7FFF);

        int y = py + 28;
        int start = widgetScroll;
        int end = Math.min(start + maxVis, total);
        for (int i = start; i < end; i++) {
            UIElement e = list.get(i);
            boolean on = e.isEnabled();
            boolean hovered = inRect(mouseX, mouseY, px + 4, y, w - 8, rowH);
            
            if (hovered) Gui.drawRect(px + 4, y, px + w - 4, y + rowH, 0x11FFFFFF);
            
            int dotCol = on ? 0xFF44EE77 : 0xFF666666;
            Gui.drawRect(px + 12, y + 9, px + 16, y + 13, dotCol);
            
            String name = friendlyName(e.getId());
            this.fontRendererObj.drawStringWithShadow(name, px + 24, y + 7, hovered ? 0xFFFFFFFF : 0xFFCCCCCC);
            
            // Edit icon hint
            if (hovered) {
                String hint = "Click to edit details";
                int hw = fontRendererObj.getStringWidth(hint);
                this.fontRendererObj.drawString(hint, px + w - 45 - hw, y + 7, 0x55FFFFFF);
            }
            
            drawToggle(px + w - 40, y + 5, on);
            y += rowH;
        }

        if (total > maxVis) {
            int scrollY = py + 28 + maxVis * rowH;
            Gui.drawRect(px + 10, scrollY + 4, px + w - 10, scrollY + 5, 0x11FFFFFF);
            String scrollText = (widgetScroll + 1) + "-" + end + " / " + total;
            this.fontRendererObj.drawString(scrollText, px + (w - fontRendererObj.getStringWidth(scrollText)) / 2, scrollY + 8, 0xFF555577);
            
            boolean canUp = widgetScroll > 0;
            boolean canDown = widgetScroll < total - maxVis;
            if (canUp) this.fontRendererObj.drawString("▲", px + 15, scrollY + 8, inRect(mouseX, mouseY, px + 10, scrollY + 4, 20, 14) ? 0xFFFFFFFF : 0xFF7777AA);
            if (canDown) this.fontRendererObj.drawString("▼", px + w - 25, scrollY + 8, inRect(mouseX, mouseY, px + w - 30, scrollY + 4, 20, 14) ? 0xFFFFFFFF : 0xFF7777AA);
        }

        // --- MOVEMENT PANEL ---
        int oh = 76;
        int oy = py + listH + 8;
        GuiRenderUtils.drawRoundedPanel(px, oy, w, oh, 0xEE0D0D15, 0xFF151525, 26, 0xFF2ECC71);
        GuiRenderUtils.drawSectionHeader(this.fontRendererObj, px, oy + 4, w, "Options de mouvement", 0xFF2ECC71);

        int optY = oy + 32;
        // Toggle Sneak
        this.fontRendererObj.drawStringWithShadow("Toggle Sneak", px + 12, optY + 2, 0xFFCCCCCC);
        drawToggle(px + w - 40, optY, gs.toggleSneakEnabled);
        
        optY += 22;
        // Toggle Sprint
        this.fontRendererObj.drawStringWithShadow("Toggle Sprint", px + 12, optY + 2, 0xFFCCCCCC);
        drawToggle(px + w - 40, optY, gs.toggleSprintEnabled);

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
        int px = this.width / 2 - 160;
        int w = 320;
        int rowH = 22;
        ArrayList<UIElement> list = new ArrayList<>(ui.all());
        int total = list.size();
        int maxVis = Math.min(getMaxVisible(), total);
        
        int py = 35;
        int y = py + 28;
        for (int i = widgetScroll; i < Math.min(widgetScroll + maxVis, total); i++) {
            UIElement e = list.get(i);
            if (inRect(mx, my, px, y, w, rowH)) {
                if (inRect(mx, my, px + w - 40, y, 35, 15)) {
                    e.setEnabled(!e.isEnabled());
                    ui.saveConfig();
                } else {
                    this.mc.displayGuiScreen(new GuiUIEditor(this, e.getId()));
                }
                return;
            }
            y += rowH;
        }

        if (total > maxVis) {
            int scrollY = py + 28 + maxVis * rowH;
            if (inRect(mx, my, px + 10, scrollY + 4, 30, 14) && widgetScroll > 0) widgetScroll--;
            if (inRect(mx, my, px + w - 40, scrollY + 4, 30, 14) && widgetScroll < total - maxVis) widgetScroll++;
        }

        int listH = 28 + maxVis * rowH + (total > maxVis ? 22 : 8);
        int oy = py + listH + 8;
        int optY = oy + 32;
        if (inRect(mx, my, px + w - 40, optY, 35, 15)) {
            gs.toggleSneakEnabled = !gs.toggleSneakEnabled;
            if (!gs.toggleSneakEnabled) gs.isToggleSneakActive = false;
            gs.saveOptions();
        }
        optY += 22;
        if (inRect(mx, my, px + w - 40, optY, 35, 15)) {
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
            int px = this.width / 2 - 160, py = 35, w = 320;
            int total = ui.all().size();
            int maxVis = Math.min(getMaxVisible(), total);
            int listH = 28 + maxVis * 22 + (total > maxVis ? 22 : 8);
            if (inRect(lastMouseX, lastMouseY, px, py, w, listH)) {
                if (scroll > 0) widgetScroll = Math.max(0, widgetScroll - 1);
                else widgetScroll = Math.min(total - maxVis, widgetScroll + 1);
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
