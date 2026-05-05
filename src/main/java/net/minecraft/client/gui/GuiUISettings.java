package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ui.UIManager;
import net.minecraft.client.gui.ui.UIElement;
import net.minecraft.client.gui.ui.UITheme;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interface de configuration des widgets HUD et options de mouvement.
 * Design sobre et moderne, coherent avec le style du client.
 */
public class GuiUISettings extends GuiScreen {

    private final GuiScreen parent;
    private final UIManager ui = UIManager.getInstance();

    // Scroll state
    private float scrollOffset = 0;
    private float scrollTarget = 0;

    // Drag-to-scroll state
    private boolean isDraggingScrollbar = false;

    // Animation maps
    private final Map<String, Float> toggleAnimMap = new HashMap<>();
    private final Map<String, Float> hoverAnimMap = new HashMap<>();

    // Open/close animation
    private float openAnim = 0.0f;
    private long lastTime = -1L;

    // Data
    private final List<SettingRow> rows = new ArrayList<>();

    // Layout cache
    private int panelX, panelY, panelW, panelH;
    private int headerH = 35;
    private int rowH = 28;
    private int footerH = 68;
    private int contentTop, contentBottom, contentH;
    private int maxScroll;

    // Colors
    private static final int ACCENT         = 0xFFE02828;  // Vibrant red — PvP theme
    private static final int ACCENT_LINE    = 0xFFE02828;  // Hot red top line
    private static final int ACCENT_DIM     = 0xFF6E1212;  // Dim red for details
    private static final int BG_PANEL       = 0xF20A0808;  // Deep dark panel
    private static final int BG_HEADER      = 0xFF0E0606;  // Very dark header
    private static final int BORDER         = 0x2BFFFFFF;  // Slightly more visible
    private static final int TEXT_PRIMARY   = 0xFFF2F2F2;
    private static final int TEXT_SECONDARY = 0xFF8A9AB0;  // Cool blue-grey
    private static final int TEXT_MUTED     = 0xFF445060;

    public GuiUISettings(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.lastTime = Minecraft.getSystemTime();
        this.openAnim = 0.0f;

        // Build rows
        rows.clear();
        rows.add(new SettingRow("WIDGETS", true));
        for (UIElement e : ui.all()) {
            rows.add(new SettingRow(e));
            if ("reach".equalsIgnoreCase(e.getId())) {
                rows.add(new SettingRow("Toggle Sneak", "sneak"));
                rows.add(new SettingRow("Toggle Sprint", "sprint"));
            }
        }

        // Layout
        panelW = MathHelper.clamp_int(this.width - 60, 260, 360);
        panelH = MathHelper.clamp_int(this.height - 40, 200, 420);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        contentTop = panelY + headerH;
        contentBottom = panelY + panelH - footerH;
        contentH = contentBottom - contentTop;

        int totalContentH = 0;
        for (SettingRow row : rows) {
            totalContentH += row.isHeader ? 20 : rowH;
        }
        maxScroll = Math.max(0, totalContentH - contentH);
        scrollTarget = MathHelper.clamp_float(scrollTarget, 0, maxScroll);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Animate opening
        long now = Minecraft.getSystemTime();
        if (lastTime > 0) {
            float dt = (now - lastTime) / 1000.0f;
            openAnim = MathHelper.clamp_float(openAnim + dt * 5.0f, 0.0f, 1.0f);
        }
        lastTime = now;

        float ease = openAnim * openAnim * (3.0f - 2.0f * openAnim);

        // Dimmed background
        this.drawRect(0, 0, this.width, this.height, (int)(ease * 160) << 24);

        // Smooth scroll
        scrollOffset = GuiRenderUtils.lerp(scrollOffset, scrollTarget, 0.25f);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, (1.0f - ease) * 10, 0);

        // Panel shadow + background
        GuiRenderUtils.drawShadow(panelX, panelY, panelW, panelH, 8, (int)(ease * 90));
        Gui.drawRect(panelX, panelY, panelX + panelW, panelY + panelH, BG_PANEL);

        // Top accent line (2px) + subtle glow
        Gui.drawRect(panelX, panelY, panelX + panelW, panelY + 2, UITheme.getPrimary());
        GuiRenderUtils.drawGradientRect(panelX, panelY + 2, panelX + panelW, panelY + 8, UITheme.primary(0x18), 0x00000000);

        // Header gradient
        GuiRenderUtils.drawGradientRect(panelX, panelY + 2, panelX + panelW, panelY + headerH, BG_HEADER, BG_PANEL);
        // Separator
        Gui.drawRect(panelX, panelY + headerH, panelX + panelW, panelY + headerH + 1, 0x30FFFFFF);

        // Two-tone title — first letter primary, rest secondary
        String fullTitle = "HUD SETTINGS";
        int tw1 = fontRendererObj.getStringWidth(fullTitle.substring(0, 1));
        int titleW = fontRendererObj.getStringWidth(fullTitle);
        int ttx = panelX + (panelW - titleW) / 2;
        int tty = panelY + (headerH - 8) / 2;
        fontRendererObj.drawStringWithShadow(fullTitle.substring(0, 1), ttx, tty, UITheme.getPrimary());
        fontRendererObj.drawStringWithShadow(fullTitle.substring(1), ttx + tw1, tty, UITheme.getSecondary());


        // Content area with scissor
        ScaledResolution sr = new ScaledResolution(mc);
        int factor = sr.getScaleFactor();
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glScissor(
                panelX * factor,
                (this.height - contentBottom) * factor,
                panelW * factor,
                contentH * factor
        );

        drawContent(mouseX, mouseY);

        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);

        // Scroll fade indicators
        if (scrollOffset > 2) {
            GuiRenderUtils.drawGradientRect(panelX + 1, contentTop, panelX + panelW - 1, contentTop + 14, 0xCC101018, 0x00101018);
        }
        if (scrollOffset < maxScroll - 2) {
            GuiRenderUtils.drawGradientRect(panelX + 1, contentBottom - 14, panelX + panelW - 1, contentBottom, 0x00101018, 0xCC101018);
        }

        // Scrollbar (6 px wide, accent-colored thumb with highlight)
        if (maxScroll > 0) {
            int sbW = 5;
            int sbX = panelX + panelW - sbW - 3;
            int sbTrackH = contentH - 4;
            Gui.drawRect(sbX, contentTop + 2, sbX + sbW, contentTop + 2 + sbTrackH, 0x0FFFFFFF);

            float scrollRatio = scrollOffset / (float) maxScroll;
            float thumbRatio = (float) contentH / (contentH + maxScroll);
            int thumbH = Math.max(16, (int)(sbTrackH * thumbRatio));
            int thumbY = (int)((sbTrackH - thumbH) * scrollRatio);
            // Thumb body
            Gui.drawRect(sbX, contentTop + 2 + thumbY, sbX + sbW, contentTop + 2 + thumbY + thumbH, UITheme.getPrimaryDim());
            // Thumb top highlight
            Gui.drawRect(sbX, contentTop + 2 + thumbY, sbX + sbW, contentTop + 3 + thumbY, UITheme.primary(0x88));
        }

        // Footer separator
        Gui.drawRect(panelX, contentBottom, panelX + panelW, contentBottom + 1, 0x30FFFFFF);

        // Footer buttons
        int btnW = (panelW - 20);
        int btnH = 22;
        int gap  = 6;

        // Row 1: HUD Editor — primary action, full width (RED CTA)
        int btnY1 = contentBottom + 8;
        boolean hoverEdit = inRect(mouseX, mouseY, panelX + 10, btnY1, btnW, btnH);
        drawFooterButton(panelX + 10, btnY1, btnW, btnH, "EDITEUR HUD", 0xFF1E0808, hoverEdit, true);

        // Row 2: Profiles & Done — half width each
        int btnY2 = btnY1 + btnH + gap;
        int halfW = (btnW - gap) / 2;

        boolean hoverProf = inRect(mouseX, mouseY, panelX + 10, btnY2, halfW, btnH);
        drawFooterButton(panelX + 10, btnY2, halfW, btnH, "PROFILS HUD", 0xFF141010, hoverProf, false);

        boolean hoverDone = inRect(mouseX, mouseY, panelX + 10 + halfW + gap, btnY2, halfW, btnH);
        drawFooterButton(panelX + 10 + halfW + gap, btnY2, halfW, btnH, "RETOUR", 0xFF141010, hoverDone, false);

        // Panel outline
        GuiRenderUtils.drawRectOutline(panelX, panelY, panelW, panelH, UITheme.primary(0x2B));

        GlStateManager.popMatrix();
    }

    private void drawContent(int mouseX, int mouseY) {
        int y = contentTop + 4 - (int) scrollOffset;

        for (int i = 0; i < rows.size(); i++) {
            SettingRow row = rows.get(i);
            int rh = row.isHeader ? 20 : rowH;

            // Skip if fully outside visible area (with some margin)
            if (y + rh > contentTop - 10 && y < contentBottom + 10) {
                if (row.isHeader) {
                    drawHeaderRow(row, panelX, y, panelW);
                } else {
                    drawSettingRow(row, panelX, y, panelW, rowH, mouseX, mouseY);
                }
            }
            y += rh;
        }
    }

    private void drawHeaderRow(SettingRow row, int x, int y, int w) {
        // Full-width gradient band
        GuiRenderUtils.drawGradientRect(x + 2, y + 2, x + w - 6, y + 18, 0x18FFFFFF, 0x08FFFFFF);
        GuiRenderUtils.drawGradientRect(x + 2, y + 2, x + w - 6, y + 4, UITheme.primary(0x28), 0x00000000);
        // Left accent stripe (3px wide)
        Gui.drawRect(x + 8, y + 4, x + 12, y + 16, UITheme.getPrimary());
        Gui.drawRect(x + 12, y + 5, x + 13, y + 15, UITheme.getPrimaryDim());
        // Label: first letter primary, rest secondary
        String lbl = row.label;
        fontRendererObj.drawStringWithShadow(lbl.substring(0, 1), x + 18, y + 6, UITheme.getPrimary());
        if (lbl.length() > 1) fontRendererObj.drawStringWithShadow(lbl.substring(1), x + 18 + fontRendererObj.getStringWidth(lbl.substring(0, 1)), y + 6, UITheme.getSecondary());
        // Fading separator line after text
        int textEnd = x + 18 + fontRendererObj.getStringWidth(lbl) + 6;
        GuiRenderUtils.drawGradientRect(textEnd, y + 10, x + w - 14, y + 11,
                UITheme.primary(0x55), 0x00000000);
    }

    private void drawSettingRow(SettingRow row, int x, int y, int w, int h, int mx, int my) {
        boolean hovered = inRect(mx, my, x + 2, y, w - 8, h) // Reduced width to avoid scrollbar area
                && my >= contentTop && my < contentBottom;

        // Hover animation
        String hKey = "h_" + row.getId();
        float hAnim = hoverAnimMap.getOrDefault(hKey, 0f);
        hAnim = GuiRenderUtils.lerp(hAnim, hovered ? 1f : 0f, 0.18f);
        hoverAnimMap.put(hKey, hAnim);

        boolean enabled = row.isEnabled(this.mc.gameSettings);

        // Hover background (orange-ish warm tint)
        if (hAnim > 0.01f) {
            int alpha = (int)(hAnim * 0x16);
            Gui.drawRect(x + 4, y + 1, x + w - 8, y + h - 1, (alpha << 24) | 0xFFDDCC);
        }

        // Left edge status bar (only when enabled)
        if (enabled) {
            int barAlpha = (int)(0x55 + hAnim * 0x44);
            Gui.drawRect(x + 4, y + 4, x + 6, y + h - 4, (barAlpha << 24) | (UITheme.getPrimary() & 0xFFFFFF));
        }

        // Status indicator — 8×8 with glow when active
        int dotX = x + 12;
        int dotY = y + h / 2 - 4;
        if (enabled) {
            // Outer glow halo
            int glowA = (int)(0x18 + hAnim * 0x28);
            Gui.drawRect(dotX - 2, dotY - 2, dotX + 10, dotY + 10, (glowA << 24) | 0x22EE55);
            // Main dot — vibrant green
            Gui.drawRect(dotX, dotY, dotX + 8, dotY + 8, 0xFF22CC50);
            // Top-left shine
            Gui.drawRect(dotX + 1, dotY + 1, dotX + 4, dotY + 2, 0x55FFFFFF);
        } else {
            Gui.drawRect(dotX, dotY, dotX + 8, dotY + 8, 0xFF141420);
            GuiRenderUtils.drawRectOutline(dotX, dotY, 8, 8, 0x25FFFFFF);
        }

        // Label
        String displayLabel = row.element != null ? friendlyName(row.element.getId()) : row.label;
        int labelCol = GuiRenderUtils.colorLerp(
                enabled ? TEXT_SECONDARY : TEXT_MUTED,
                TEXT_PRIMARY,
                hAnim
        );
        this.fontRendererObj.drawStringWithShadow(displayLabel, x + 27, y + (h - 8) / 2, labelCol);

        // Arrow hint on hover (widget rows only)
        if (row.element != null && hAnim > 0.2f) {
            int hintAlpha = (int)(Math.min(1f, (hAnim - 0.2f) / 0.8f) * 0x80);
            this.fontRendererObj.drawString(">", x + w - 52, y + (h - 8) / 2, (hintAlpha << 24) | 0xFFDDCC);
        }

        // Toggle switch
        drawToggle(x + w - 41, y + (h - 12) / 2, enabled, row.getId());
    }

    private void drawToggle(int x, int y, boolean value, String id) {
        String key = "t_" + id;
        float target = value ? 1.0f : 0.0f;
        float current = toggleAnimMap.getOrDefault(key, target);
        current = GuiRenderUtils.lerp(current, target, 0.18f);
        toggleAnimMap.put(key, current);
        GuiRenderUtils.drawSmoothToggle(x, y, value, current);
    }

    private void drawFooterButton(int x, int y, int w, int h, String text, int baseColor, boolean hovered, boolean isPrimary) {
        if (isPrimary) {
            // Primary CTA → filled dark, accent top border
            Gui.drawRect(x, y, x + w, y + h, hovered ? 0xFF1A1A2A : 0xFF101018);
            Gui.drawRect(x, y, x + w, y + 1, hovered ? UITheme.getPrimary() : UITheme.getPrimaryDim());
            GuiRenderUtils.drawRectOutline(x, y, w, h, UITheme.primary(hovered ? 0x44 : 0x33));
            int tw = this.fontRendererObj.getStringWidth(text);
            this.fontRendererObj.drawStringWithShadow(text, x + (w - tw) / 2, y + (h - 8) / 2.0f,
                    hovered ? UITheme.getSecondary() : UITheme.primary(0xCC));
        } else {
            // Secondary → outlined style
            int bg = hovered ? 0x1EFFFFFF : 0x0A000000;
            Gui.drawRect(x, y, x + w, y + h, bg);
            GuiRenderUtils.drawRectOutline(x, y, w, h, hovered ? 0x44FFFFFF : 0x28FFFFFF);
            if (hovered) {
                // Left accent bar appears on hover
                Gui.drawRect(x, y + 1, x + 2, y + h - 1, UITheme.getPrimary());
                GuiRenderUtils.drawGradientRect(x + 1, y + 1, x + w - 1, y + 3, 0x16FFFFFF, 0x00000000);
            }
            // Label
            int tw = this.fontRendererObj.getStringWidth(text);
            this.fontRendererObj.drawStringWithShadow(text, x + (w - tw) / 2, y + (h - 8) / 2.0f,
                    hovered ? 0xFFFFFFFF : 0xFFCCCCDD);
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        if (btn != 0) return;

        // Footer layout logic — must match drawScreen values exactly
        int btnW = (panelW - 20);
        int btnH = 22;
        int gap  = 6;
        int btnY1 = contentBottom + 8;
        int btnY2 = btnY1 + btnH + gap;
        int halfW = (btnW - gap) / 2;

        // HUD Editor
        if (inRect(mx, my, panelX + 10, btnY1, btnW, btnH)) {
            this.mc.displayGuiScreen(new GuiUIEditor(this));
            return;
        }
        // HUD Profiles
        if (inRect(mx, my, panelX + 10, btnY2, halfW, btnH)) {
            this.mc.displayGuiScreen(new GuiHudProfiles(this));
            return;
        }
        // Done
        if (inRect(mx, my, panelX + 10 + halfW + gap, btnY2, halfW, btnH)) {
            this.mc.displayGuiScreen(parent);
            return;
        }
        
        // Scrollbar interaction (priority)
        if (maxScroll > 0) {
            int sbW = 4;
            int sbX = panelX + panelW - sbW - 2;
            if (mx >= sbX && mx <= sbX + sbW && my >= contentTop && my < contentBottom) {
                isDraggingScrollbar = true;
                updateScrollFromMouse(my);
                return;
            }
        }

        // Content rows
        if (!inRect(mx, my, panelX, contentTop, panelW - 8, contentH)) return; // Exclude scrollbar area

        int y = contentTop + 4 - (int) scrollOffset;
        for (int i = 0; i < rows.size(); i++) {
            SettingRow row = rows.get(i);
            int rh = row.isHeader ? 20 : rowH;

            if (!row.isHeader && inRect(mx, my, panelX, y, panelW - 8, rh)) {
                // Toggle area (right side)
                if (inRect(mx, my, panelX + panelW - 46, y, 40, rh)) {
                    row.toggle(this.mc.gameSettings);
                    if (row.element != null) ui.saveConfig();
                    else this.mc.gameSettings.saveOptions();
                    return;
                }
                // Click on widget row -> open editor
                if (row.element != null) {
                    this.mc.displayGuiScreen(new GuiUIEditor(this, row.element.getId()));
                    return;
                }
            }
            y += rh;
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int btn, long time) {
        if (isDraggingScrollbar) {
            updateScrollFromMouse(my);
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        isDraggingScrollbar = false;
        super.mouseReleased(mx, my, state);
    }

    private void updateScrollFromMouse(int my) {
        float ratio = (float)(my - contentTop) / (float)contentH;
        scrollTarget = ratio * maxScroll;
        scrollTarget = MathHelper.clamp_float(scrollTarget, 0, maxScroll);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) {
            int mx = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int my = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            if (inRect(mx, my, panelX, panelY, panelW, panelH)) {
                scrollTarget += scroll > 0 ? -24 : 24;
                scrollTarget = MathHelper.clamp_float(scrollTarget, 0, maxScroll);
            }
        }
    }

    @Override
    protected void keyTyped(char c, int keyCode) throws IOException {
        if (keyCode == 1) {
            this.mc.displayGuiScreen(parent);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private boolean inRect(int mx, int my, int rx, int ry, int rw, int rh) {
        return mx >= rx && my >= ry && mx < rx + rw && my < ry + rh;
    }

    private String friendlyName(String id) {
        switch (id) {
            case "fps":             return "FPS";
            case "ping":            return "Ping";
            case "biome":           return "Biome";
            case "coords":          return "Coordonnees";
            case "dir":             return "Direction";
            case "date":            return "Date";
            case "helditem":        return "Objet tenu";
            case "armor_group":     return "Armure";
            case "potions":         return "Effets de Potion";
            case "cps":             return "CPS";
            case "toggle_sneak":    return "Affichage Sneak";
            case "toggle_sprint":   return "Affichage Sprint";
            case "auto_armor":      return "Auto Armor";
            case "combatlog":       return "Combat Tag";
            case "keystrokes":
            case "Keystrokes":      return "Keystrokes";
            case "reach":
            case "Reach":           return "Reach Display";
            case "compass":         return "Boussole HUD";
            case "player_healthbar":return "Barre de vie joueurs";
            case "faction_zone":    return "Zone Faction";
            default:
                String s = id.replace('_', ' ');
                return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }

    // ---- Data model ----

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
                // Synchroniser l'état enabled du widget HUD pour garder la cohérence
                UIElement sneakWidget = UIManager.getInstance().get("toggle_sneak");
                if (sneakWidget != null) {
                    // On appelle directement super.setEnabled via l'interface pour éviter
                    // une boucle — on utilise UIElement.setEnabled qui est la méthode de base
                    // (ToggleSneakWidget.setEnabled est déjà synchro dans l'autre sens)
                    sneakWidget.setEnabled(gs.toggleSneakEnabled);
                }
            }
            if ("sprint".equals(settingKey)) {
                gs.toggleSprintEnabled = !gs.toggleSprintEnabled;
                if (!gs.toggleSprintEnabled) gs.isToggleSprintActive = false;
                // Synchroniser l'état enabled du widget HUD
                UIElement sprintWidget = UIManager.getInstance().get("toggle_sprint");
                if (sprintWidget != null) {
                    sprintWidget.setEnabled(gs.toggleSprintEnabled);
                }
            }
            try {
                gs.saveOptions();
            } catch (Exception ignored) {
            }
        }
    }
}
