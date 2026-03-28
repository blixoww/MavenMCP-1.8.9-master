package net.minecraft.client.gui;

import net.minecraft.client.gui.ui.UIManager;
import net.minecraft.client.gui.ui.UIElement;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiUISettings extends GuiScreen {
    private final GuiScreen parent;
    private final UIManager ui = UIManager.getInstance();
    private int scrollOffset = 0;
    private int lastMouseX, lastMouseY;
    
    private final Map<String, Float> toggleAnimMap = new HashMap<>();
    private final Map<String, Float> hoverAnimMap = new HashMap<>();
    
    private final List<SettingRow> rows = new ArrayList<>();

    public GuiUISettings(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        
        // Refresh the list of rows
        rows.clear();
        rows.add(new SettingRow("CONFIGURATION DES WIDGETS", true));
        for (UIElement e : ui.all()) {
            rows.add(new SettingRow(e));
        }
        rows.add(new SettingRow("OPTIONS DE MOUVEMENT", true));
        rows.add(new SettingRow("Toggle Sneak", "sneak"));
        rows.add(new SettingRow("Toggle Sprint", "sprint"));

        int btnW = Math.min(150, this.width / 2 - 20);
        this.buttonList.add(new GuiButton(201, this.width / 2 - btnW - 5, this.height - 26, btnW, 20, "Éditer l'HUD"));
        this.buttonList.add(new GuiButton(200, this.width / 2 + 5, this.height - 26, btnW, 20, I18n.format("gui.done")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 201) {
            this.mc.displayGuiScreen(new GuiUIEditor(this));
        } else if (button.id == 200) {
            this.mc.displayGuiScreen(parent);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        
        // --- TITLE ---
        int titleY = 10;
        String title = "PARAMÈTRES DE L'INTERFACE";
        GlStateManager.pushMatrix();
        GlStateManager.scale(1.2, 1.2, 1.2);
        this.fontRendererObj.drawStringWithShadow(title, (float)((this.width / 1.2 - this.fontRendererObj.getStringWidth(title)) / 2), (float)(titleY / 1.2), 0xFFFFFFFF);
        GlStateManager.popMatrix();

        // --- CALC LAYOUT ---
        int paneW = Math.min(320, this.width - 40);
        int px = (this.width - paneW) / 2;
        int py = 35;
        int bottomLimit = this.height - 35;
        int availableH = bottomLimit - py;
        
        int rowH = 22;
        int maxVisible = availableH / rowH;
        
        // Ensure scroll is in bounds
        scrollOffset = Math.max(0, Math.min(scrollOffset, rows.size() - maxVisible));
        
        // --- MAIN PANEL ---
        int actualListH = Math.min(rows.size(), maxVisible) * rowH + 4;
        GuiRenderUtils.drawRoundedPanel(px, py, paneW, actualListH, 0xCC05050A, 0xFF101015, 0, 0xFF2A7FFF);
        GuiRenderUtils.drawRectOutline(px, py, paneW, actualListH, 0x33FFFFFF);

        int currentY = py + 2;
        int start = scrollOffset;
        int end = Math.min(start + maxVisible, rows.size());
        
        for (int i = start; i < end; i++) {
            SettingRow row = rows.get(i);
            renderRow(row, px, currentY, paneW, rowH, mouseX, mouseY);
            currentY += rowH;
        }

        // Scrollbar if needed
        if (rows.size() > maxVisible) {
            int sbW = 2;
            int sbX = px + paneW - sbW - 2;
            int sbH = actualListH - 4;
            Gui.drawRect(sbX, py + 2, sbX + sbW, py + 2 + sbH, 0x22FFFFFF);
            
            float percentage = (float) maxVisible / rows.size();
            int thumbH = (int) (sbH * percentage);
            int thumbY = (int) (sbH * ((float) scrollOffset / rows.size()));
            Gui.drawRect(sbX, py + 2 + thumbY, sbX + sbW, py + 2 + thumbY + thumbH, 0xFF2A7FFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void renderRow(SettingRow row, int x, int y, int w, int h, int mx, int my) {
        if (row.isHeader) {
            Gui.drawRect(x + 5, y + h - 2, x + w - 5, y + h - 1, 0x22FFFFFF);
            this.fontRendererObj.drawString(row.label, x + 8, y + 7, 0xFF2A7FFF);
            return;
        }

        boolean hovered = inRect(mx, my, x + 2, y, w - 4, h);
        String hKey = "hover_" + row.getId();
        float hAnim = hoverAnimMap.getOrDefault(hKey, 0f);
        hAnim = GuiRenderUtils.lerp(hAnim, hovered ? 1f : 0f, 0.2f);
        hoverAnimMap.put(hKey, hAnim);

        if (hAnim > 0) {
            int alpha = (int) (hAnim * 0x1A);
            Gui.drawRect(x + 4, y + 1, x + w - 4, y + h - 1, (alpha << 24) | 0xFFFFFF);
        }

        boolean enabled = row.isEnabled(this.mc.gameSettings);
        int dotCol = enabled ? 0xFF44EE77 : 0xFFEE4444;
        Gui.drawRect(x + 10, y + 9, x + 13, y + 12, dotCol);
        
        String displayLabel = row.element != null ? friendlyName(row.element.getId()) : row.label;
        this.fontRendererObj.drawStringWithShadow(displayLabel, x + 20, y + 7, hovered ? 0xFFFFFFFF : 0xFFCCCCCC);

        if (row.element != null && hovered) {
            String edit = "CLIC POUR CONFIGURER";
            int ew = fontRendererObj.getStringWidth(edit);
            this.fontRendererObj.drawString(edit, x + w - 45 - ew, y + 8, 0x44FFFFFF);
        }

        drawToggle(x + w - 35, y + 5, enabled);
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
        
        int paneW = Math.min(320, this.width - 40);
        int px = (this.width - paneW) / 2;
        int py = 35;
        int bottomLimit = this.height - 35;
        int availableH = bottomLimit - py;
        int rowH = 22;
        int maxVisible = availableH / rowH;
        
        int currentY = py + 2;
        int start = scrollOffset;
        int end = Math.min(start + maxVisible, rows.size());

        for (int i = start; i < end; i++) {
            SettingRow row = rows.get(i);
            if (!row.isHeader && inRect(mx, my, px, currentY, paneW, rowH)) {
                if (inRect(mx, my, px + paneW - 40, currentY, 35, 15)) {
                    row.toggle(this.mc.gameSettings);
                    if (row.element != null) ui.saveConfig();
                    else this.mc.gameSettings.saveOptions();
                } else if (row.element != null) {
                    this.mc.displayGuiScreen(new GuiUIEditor(this, row.element.getId()));
                }
                return;
            }
            currentY += rowH;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        lastMouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        lastMouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) {
            int paneW = Math.min(320, this.width - 40);
            int px = (this.width - paneW) / 2;
            int py = 35;
            int bottomLimit = this.height - 35;
            int availableH = bottomLimit - py;
            int rowH = 22;
            int maxVisible = availableH / rowH;

            if (inRect(lastMouseX, lastMouseY, px, py, paneW, Math.min(rows.size(), maxVisible) * rowH)) {
                if (scroll > 0) scrollOffset = Math.max(0, scrollOffset - 1);
                else scrollOffset = Math.min(rows.size() - maxVisible, scrollOffset + 1);
            }
        }
    }

    private boolean inRect(int mx, int my, int rx, int ry, int rw, int rh) {
        return mx >= rx && my >= ry && mx <= rx + rw && my <= ry + rh;
    }

    private String friendlyName(String id) {
        switch (id) {
            case "fps": return "FPS"; case "ping": return "Ping";
            case "biome": return "Biome"; case "coords": return "Coordonnées";
            case "dir": return "Direction"; case "date": return "Date";
            case "helditem": return "Objet tenu"; case "armor_group": return "Armure";
            case "potions": return "Effets de Potion"; case "cps": return "CPS";
            case "toggle_sneak": return "Affichage Sneak"; case "toggle_sprint": return "Affichage Sprint";
            case "reach": return "Reach Display";
            default: 
                String s = id.replace('_', ' ');
                return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }

    private static class SettingRow {
        String label;
        boolean isHeader;
        UIElement element;
        String settingKey;

        SettingRow(String label, boolean isHeader) {
            this.label = label;
            this.isHeader = isHeader;
        }

        SettingRow(UIElement element) {
            this.element = element;
            this.label = element.getId();
        }

        SettingRow(String label, String settingKey) {
            this.label = label;
            this.settingKey = settingKey;
        }

        String getId() {
            if (element != null) return element.getId();
            if (settingKey != null) return settingKey;
            return label;
        }

        boolean isEnabled(GameSettings gs) {
            if (element != null) return element.isEnabled();
            if ("sneak".equals(settingKey)) return gs.toggleSneakEnabled;
            if ("sprint".equals(settingKey)) return gs.toggleSprintEnabled;
            return false;
        }

        void toggle(GameSettings gs) {
            if (element != null) element.setEnabled(!element.isEnabled());
            if ("sneak".equals(settingKey)) {
                gs.toggleSneakEnabled = !gs.toggleSneakEnabled;
                if (!gs.toggleSneakEnabled) gs.isToggleSneakActive = false;
            }
            if ("sprint".equals(settingKey)) {
                gs.toggleSprintEnabled = !gs.toggleSprintEnabled;
                if (!gs.toggleSprintEnabled) gs.isToggleSprintActive = false;
            }
        }
    }
}
