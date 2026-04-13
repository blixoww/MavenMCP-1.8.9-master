package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ui.UIManager;
import net.minecraft.client.gui.ui.UIElement;
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
    private int headerH = 28;
    private int rowH = 24;
    private int footerH = 50; // Increased to fit two rows of buttons
    private int contentTop, contentBottom, contentH;
    private int maxScroll;

    // Colors
    private static final int ACCENT = 0xFF2A7FFF;
    private static final int BG_PANEL = 0xF0101018;
    private static final int BG_HEADER = 0xFF141420;
    private static final int BORDER = 0x18FFFFFF;
    private static final int TEXT_PRIMARY = 0xFFEEEEEE;
    private static final int TEXT_SECONDARY = 0xFFAAAAAA;
    private static final int TEXT_MUTED = 0xFF666666;

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
        }
        rows.add(new SettingRow("MOUVEMENT", true));
        rows.add(new SettingRow("Toggle Sneak", "sneak"));
        rows.add(new SettingRow("Toggle Sprint", "sprint"));

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
        GuiRenderUtils.drawShadow(panelX, panelY, panelW, panelH, 10, (int)(ease * 100));
        Gui.drawRect(panelX, panelY, panelX + panelW, panelY + panelH, BG_PANEL);

        // Top accent line
        Gui.drawRect(panelX, panelY, panelX + panelW, panelY + 1, ACCENT);

        // Header
        Gui.drawRect(panelX, panelY + 1, panelX + panelW, panelY + headerH, BG_HEADER);
        Gui.drawRect(panelX, panelY + headerH, panelX + panelW, panelY + headerH + 1, BORDER);

        String title = "INTERFACE";
        int titleW = this.fontRendererObj.getStringWidth(title);
        this.fontRendererObj.drawStringWithShadow(title,
                panelX + (panelW - titleW) / 2.0f, panelY + 10, TEXT_PRIMARY);

        // Subtle icon hint on each side of title
        int iconCol = 0x33FFFFFF;
        int lineY = panelY + 14;
        Gui.drawRect(panelX + 10, lineY, panelX + (panelW - titleW) / 2 - 8, lineY + 1, iconCol);
        Gui.drawRect(panelX + (panelW + titleW) / 2 + 8, lineY, panelX + panelW - 10, lineY + 1, iconCol);

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

        // Scroll indicators (fade gradients)
        if (scrollOffset > 2) {
            GuiRenderUtils.drawGradientRect(panelX + 1, contentTop, panelX + panelW - 1, contentTop + 12, 0xCC101018, 0x00101018);
        }
        if (scrollOffset < maxScroll - 2) {
            GuiRenderUtils.drawGradientRect(panelX + 1, contentBottom - 12, panelX + panelW - 1, contentBottom, 0x00101018, 0xCC101018);
        }

        // Scrollbar
        if (maxScroll > 0) {
            int sbW = 4;
            int sbX = panelX + panelW - sbW - 2;
            int sbTrackH = contentH - 4;
            Gui.drawRect(sbX, contentTop + 2, sbX + sbW, contentTop + 2 + sbTrackH, 0x15FFFFFF);

            float scrollRatio = scrollOffset / (float) maxScroll;
            float thumbRatio = (float) contentH / (contentH + maxScroll);
            int thumbH = Math.max(12, (int)(sbTrackH * thumbRatio));
            int thumbY = (int)((sbTrackH - thumbH) * scrollRatio);
            Gui.drawRect(sbX, contentTop + 2 + thumbY, sbX + sbW, contentTop + 2 + thumbY + thumbH, ACCENT);
        }

        // Footer separator
        Gui.drawRect(panelX, contentBottom, panelX + panelW, contentBottom + 1, BORDER);

        // Footer buttons
        int btnW = (panelW - 20);
        int btnH = 16;
        int gap = 4;
        
        // Row 1: HUD Editor
        int btnY1 = contentBottom + 6;
        boolean hoverEdit = inRect(mouseX, mouseY, panelX + 10, btnY1, btnW, btnH);
        drawFooterButton(panelX + 10, btnY1, btnW, btnH, "Editeur HUD", ACCENT, hoverEdit);

        // Row 2: Profiles & Done
        int btnY2 = btnY1 + btnH + gap;
        int halfW = (btnW - gap) / 2;
        
        boolean hoverProf = inRect(mouseX, mouseY, panelX + 10, btnY2, halfW, btnH);
        // Changed color to a clearer blue-green to stand out
        drawFooterButton(panelX + 10, btnY2, halfW, btnH, "Profils HUD", 0xFF2ECC71, hoverProf);
        
        boolean hoverDone = inRect(mouseX, mouseY, panelX + 10 + halfW + gap, btnY2, halfW, btnH);
        drawFooterButton(panelX + 10 + halfW + gap, btnY2, halfW, btnH, I18n.format("gui.done"), 0xFF333344, hoverDone);

        // Panel outline
        GuiRenderUtils.drawRectOutline(panelX, panelY, panelW, panelH, BORDER);

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
        // Section header with accent bar
        int barW = 3;
        Gui.drawRect(x + 10, y + 6, x + 10 + barW, y + 15, ACCENT);
        this.fontRendererObj.drawStringWithShadow(row.label, x + 18, y + 7, ACCENT);

        int textEnd = x + 18 + this.fontRendererObj.getStringWidth(row.label) + 8;
        GuiRenderUtils.drawGradientRect(textEnd, y + 11, x + w - 14, y + 12,
                (0x33 << 24) | (ACCENT & 0xFFFFFF), 0x00000000);
    }

    private void drawSettingRow(SettingRow row, int x, int y, int w, int h, int mx, int my) {
        boolean hovered = inRect(mx, my, x + 2, y, w - 8, h) // Reduced width to avoid scrollbar area
                && my >= contentTop && my < contentBottom;

        // Hover animation
        String hKey = "h_" + row.getId();
        float hAnim = hoverAnimMap.getOrDefault(hKey, 0f);
        hAnim = GuiRenderUtils.lerp(hAnim, hovered ? 1f : 0f, 0.18f);
        hoverAnimMap.put(hKey, hAnim);

        // Hover background
        if (hAnim > 0.01f) {
            int alpha = (int)(hAnim * 0x18);
            Gui.drawRect(x + 4, y + 1, x + w - 8, y + h - 1, (alpha << 24) | 0xFFFFFF);
        }

        boolean enabled = row.isEnabled(this.mc.gameSettings);

        // Status dot with glow
        int dotX = x + 14;
        int dotY = y + h / 2 - 2;
        int dotCol = enabled ? 0xFF44DD66 : 0xFF444455;
        if (enabled && hAnim > 0.01f) {
            // Subtle glow around enabled dot on hover
            Gui.drawRect(dotX - 1, dotY - 1, dotX + 5, dotY + 5, (int)(hAnim * 0x20) << 24 | 0x44DD66);
        }
        Gui.drawRect(dotX, dotY, dotX + 4, dotY + 4, dotCol);

        // Label
        String displayLabel = row.element != null ? friendlyName(row.element.getId()) : row.label;
        int labelCol = GuiRenderUtils.colorLerp(
                enabled ? TEXT_SECONDARY : TEXT_MUTED,
                TEXT_PRIMARY,
                hAnim
        );
        this.fontRendererObj.drawStringWithShadow(displayLabel, x + 24, y + (h - 8) / 2, labelCol);

        // Click hint on hover (for widget rows)
        if (row.element != null && hAnim > 0.3f) {
            int hintAlpha = (int)(Math.min(1f, (hAnim - 0.3f) / 0.7f) * 0x55);
            String hint = ">";
            int hw = fontRendererObj.getStringWidth(hint);
            this.fontRendererObj.drawString(hint, x + w - 48 - hw, y + (h - 8) / 2, (hintAlpha << 24) | 0xFFFFFF);
        }

        // Toggle switch
        drawToggle(x + w - 42, y + (h - 12) / 2, enabled, row.getId());
    }

    private void drawToggle(int x, int y, boolean value, String id) {
        String key = "t_" + id;
        float target = value ? 1.0f : 0.0f;
        float current = toggleAnimMap.getOrDefault(key, target);
        current = GuiRenderUtils.lerp(current, target, 0.18f);
        toggleAnimMap.put(key, current);
        GuiRenderUtils.drawSmoothToggle(x, y, value, current);
    }

    private void drawFooterButton(int x, int y, int w, int h, String text, int baseColor, boolean hovered) {
        int bg = hovered ? GuiRenderUtils.colorLerp(baseColor, 0xFFFFFFFF, 0.15f) : baseColor;
        Gui.drawRect(x, y, x + w, y + h, bg);
        GuiRenderUtils.drawRectOutline(x, y, w, h, hovered ? 0x44FFFFFF : 0x22FFFFFF);

        if (hovered) {
            GuiRenderUtils.drawGradientRect(x + 1, y + 1, x + w - 1, y + 3, 0x22FFFFFF, 0x00FFFFFF);
        }

        int tw = this.fontRendererObj.getStringWidth(text);
        this.fontRendererObj.drawStringWithShadow(text,
                x + (w - tw) / 2.0f, y + (h - 8) / 2.0f,
                hovered ? 0xFFFFFFFF : 0xFFCCCCCC);
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        if (btn != 0) return;

        // Footer layout logic
        int btnW = (panelW - 20);
        int btnH = 16;
        int gap = 4;
        int btnY1 = contentBottom + 6;
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
            case "fps": return "FPS";
            case "ping": return "Ping";
            case "biome": return "Biome";
            case "coords": return "Coordonnees";
            case "dir": return "Direction";
            case "date": return "Date";
            case "helditem": return "Objet tenu";
            case "armor_group": return "Armure";
            case "potions": return "Effets de Potion";
            case "cps": return "CPS";
            case "toggle_sneak": return "Affichage Sneak";
            case "toggle_sprint": return "Affichage Sprint";
            case "reach": return "Reach Display";
            case "combatlog": return "Combat Tag";
            case "keystrokes": return "Keystrokes";
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
            }
            if ("sprint".equals(settingKey)) {
                gs.toggleSprintEnabled = !gs.toggleSprintEnabled;
                if (!gs.toggleSprintEnabled) gs.isToggleSprintActive = false;
            }
            try {
                gs.saveOptions();
            } catch (Exception ignored) {
            }
        }
    }
}
