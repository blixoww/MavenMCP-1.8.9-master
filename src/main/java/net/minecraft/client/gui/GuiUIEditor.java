package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ui.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HUD Editor - Drag widgets, configure properties, color picker.
 * Clean, panel-based UI with smooth animations.
 */
public class GuiUIEditor extends GuiScreen {

    // ---- Colors ----
    private static final int ACCENT = 0xFF2A7FFF;
    private static final int BG_PANEL = 0xF0101018;
    private static final int BG_HEADER = 0xFF141420;
    private static final int BORDER = 0x18FFFFFF;
    private static final int TEXT_PRIMARY = 0xFFEEEEEE;
    private static final int TEXT_SECONDARY = 0xFFAAAAAA;
    private static final int TEXT_MUTED = 0xFF555555;
    private static final int ACCENT_PURPLE = 0xFF9932CC;
    private static final int ACCENT_GREEN = 0xFF2ECC71;
    private static final int ACCENT_ORANGE = 0xFFE67E22;

    // ---- Core state ----
    private final GuiScreen parent;
    private final UIManager ui = UIManager.getInstance();
    private UIElement selected = null;
    private final String preselectId;

    // ---- Animation ----
    private float openAnim = 0.0f;
    private float sidebarAnim = 0.0f;
    private float widgetListAnim = 0.0f;
    private long lastTime = -1L;
    private final Map<String, Float> toggleAnimMap = new HashMap<>();
    private final Map<String, Float> hoverAnimMap = new HashMap<>();

    // ---- Widget list panel ----
    private int wlX, wlY, wlW;
    private static final int WL_ROW_H = 22;
    private static final int WL_MAX_H = 260;
    private int wlScroll = 0;
    private boolean wlScrollDragging = false;
    private String searchFilter = "";
    private boolean searchFocused = false;

    // ---- Sidebar panel ----
    private int sbX, sbY, sbW, sbH;

    // ---- Color editor ----
    private boolean colorEditorOpen = false;
    private int ceX, ceY;
    private static final int CE_W = 240, CE_H = 165;
    private int ceSpecW = 120, ceSpecH = 100;
    private int r = 255, g = 255, b = 255, a = 255;

    // ---- Drag state ----
    private boolean isDraggingWidget, wlDragging, sbDragging, ceDragging, draggingSpectrum;
    private int dragOffsetX, dragOffsetY;
    private int wlDragOX, wlDragOY, sbDragOX, sbDragOY, ceDragOX, ceDragOY;
    private int draggingSlider = -1;
    private int lastMouseX, lastMouseY;

    // ---- Snapping ----
    private int snapLineX = -1, snapLineY = -1;

    // ---- Panel ordering ----
    private final List<String> panelOrder = new ArrayList<String>() {{
        add("widgetList");
        add("sidebar");
        add("colorEditor");
    }};

    // ---- Hitboxes (x, y, w, h) ----
    private final int[] hbWlClose = new int[4];
    private final int[] hbSbClose = new int[4];
    private final int[] hbCeClose = new int[4];
    private final int[] hbColorPreview = new int[4];
    private final int[] hbResetPos = new int[4];
    private final int[] hbResetColor = new int[4];
    private final int[] hbRainbow = new int[4];
    private final int[] hbOrigDesign = new int[4];
    private final int[] hbAlignGrid = new int[4];
    private final int[] hbSpaceRainbow = new int[4];
    private final int[] hbPotionDur = new int[4];
    private final int[] hbPotionIcons = new int[4];
    private final int[] hbArmorLayout = new int[4];
    private final int[] hbArmorPercent = new int[4];
    private final int[][] hbKeyToggle = new int[9][4];

    public GuiUIEditor(GuiScreen parent) {
        this(parent, null);
    }

    public GuiUIEditor(GuiScreen parent, String preselectId) {
        this.parent = parent;
        this.preselectId = preselectId;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.lastTime = Minecraft.getSystemTime();
        this.openAnim = 0.0f;
        UIManager.getInstance().setEditorActive(true);

        // Widget list layout
        wlW = GuiRenderUtils.clamp(this.width / 5, 140, 190);
        wlX = this.width - wlW - 28;
        wlY = 30;

        // Sidebar layout
        sbW = 185;
        sbX = 10;
        sbY = 30;

        // Color editor layout
        ceX = this.width / 2 - CE_W / 2;
        ceY = this.height / 2 - CE_H / 2;

        if (this.preselectId != null) {
            UIElement e = ui.get(this.preselectId);
            if (e != null) {
                this.selected = e;
                bringToFront("sidebar");
            }
        }

        for (UIElement el : ui.all()) {
            if (el instanceof BaseWidget) {
                ((BaseWidget) el).updateAbsolutePosition();
            }
        }
        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void onGuiClosed() {
        UIManager.getInstance().setEditorActive(false);
        ui.saveConfig();
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 200) {
            this.mc.displayGuiScreen(parent);
        }
    }

    // ========================================================================
    // RENDERING
    // ========================================================================

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = Minecraft.getSystemTime();
        if (lastTime > 0) {
            float dt = (now - lastTime) / 1000.0f;
            openAnim = MathHelper.clamp_float(openAnim + dt * 5.0f, 0.0f, 1.0f);
        }
        lastTime = now;

        float ease = openAnim * openAnim * (3.0f - 2.0f * openAnim);

        // Background
        this.drawRect(0, 0, this.width, this.height, (int)(ease * 140) << 24);
        drawGrid(16, (int)(ease * 5) << 24 | 0xFFFFFF);

        // Snap lines
        if (snapLineX != -1) {
            Gui.drawRect(snapLineX, 0, snapLineX + 1, this.height, 0x882A7FFF);
        }
        if (snapLineY != -1) {
            Gui.drawRect(0, snapLineY, this.width, snapLineY + 1, 0x882A7FFF);
        }

        // Render all widgets
        for (UIElement e : ui.all()) {
            e.render(mouseX, mouseY, partialTicks);
            if (selected == e) {
                GuiRenderUtils.drawSelectionHalo(e.getX(), e.getY(), e.getWidth(), e.getHeight(), ACCENT);
            }
        }

        // Panel animations
        sidebarAnim = GuiRenderUtils.lerp(sidebarAnim, selected != null ? 1.0f : 0.0f, 0.15f);
        widgetListAnim = GuiRenderUtils.lerp(widgetListAnim, wlX > -400 ? 1.0f : 0.0f, 0.15f);

        // Render panels in z-order
        for (String panel : panelOrder) {
            if ("widgetList".equals(panel) && widgetListAnim > 0.01f) drawWidgetList(mouseX, mouseY);
            else if ("sidebar".equals(panel) && sidebarAnim > 0.01f) drawSidebar(mouseX, mouseY);
            else if ("colorEditor".equals(panel) && colorEditorOpen && selected instanceof BaseWidget) drawColorEditor(mouseX, mouseY);
        }

        // Title bar
        drawTitleBar(ease);

        // Done button
        int btnW = 100, btnH = 18;
        int btnX = (this.width - btnW) / 2;
        int btnY = this.height - 28;
        boolean btnHover = inRect(mouseX, mouseY, btnX, btnY, btnW, btnH);
        drawStyledButton(btnX, btnY, btnW, btnH, I18n.format("gui.done"), 0xFF1A1A28, btnHover);
    }

    private void drawTitleBar(float ease) {
        int tw = 170, th = 22;
        int tx = (this.width - tw) / 2, ty = 3;
        GuiRenderUtils.drawShadow(tx, ty, tw, th, 4, (int)(ease * 60));
        Gui.drawRect(tx, ty, tx + tw, ty + th, 0xEE0D0D15);
        Gui.drawRect(tx, ty, tx + tw, ty + 1, ACCENT);
        Gui.drawRect(tx, ty + th, tx + tw, ty + th + 1, BORDER);

        String title = "EDITEUR D'INTERFACE";
        int titleW = this.fontRendererObj.getStringWidth(title);
        this.fontRendererObj.drawStringWithShadow(title, tx + (tw - titleW) / 2.0f, ty + 7, 0xFF8EC8FF);
    }

    // ---- Widget List Panel ----

    private void drawWidgetList(int mx, int my) {
        int px = wlX, py = wlY;
        int totalW = wlW + 20;

        List<UIElement> items = getFilteredWidgets();

        int maxH = Math.min(WL_MAX_H, this.height - 80);
        int maxVisible = (maxH - 50) / WL_ROW_H;
        int listH = Math.min(maxVisible, Math.max(1, items.size())) * WL_ROW_H;
        int h = 50 + listH;

        wlScroll = GuiRenderUtils.clamp(wlScroll, 0, Math.max(0, items.size() - maxVisible));

        // Panel
        GuiRenderUtils.drawShadow(px, py, totalW, h, 6, 0x60);
        Gui.drawRect(px, py, px + totalW, py + h, BG_PANEL);
        Gui.drawRect(px, py, px + totalW, py + 1, ACCENT);
        Gui.drawRect(px, py + 1, px + totalW, py + 22, BG_HEADER);
        Gui.drawRect(px, py + 22, px + totalW, py + 23, BORDER);
        GuiRenderUtils.drawRectOutline(px, py, totalW, h, BORDER);

        // Header
        this.fontRendererObj.drawStringWithShadow("Widgets", px + 10, py + 7, 0xFFAAD4FF);

        // Close button
        int ccx = px + totalW - 16, ccy = py + 6, ccs = 10;
        boolean closeHover = inRect(mx, my, ccx, ccy, ccs, ccs);
        drawMiniClose(ccx, ccy, ccs, closeHover);
        setHB(hbWlClose, ccx, ccy, ccs, ccs);

        // Search bar
        int searchY = py + 26;
        int searchW = totalW - 16;
        Gui.drawRect(px + 8, searchY, px + 8 + searchW, searchY + 16, searchFocused ? 0x33FFFFFF : 0x18FFFFFF);
        GuiRenderUtils.drawRectOutline(px + 8, searchY, searchW, 16, searchFocused ? ACCENT : 0x22FFFFFF);
        GuiRenderUtils.drawSearchIcon(px + 12, searchY + 4, TEXT_MUTED);

        if (searchFilter.isEmpty() && !searchFocused) {
            this.fontRendererObj.drawString("Rechercher...", px + 24, searchY + 4, TEXT_MUTED);
        } else {
            String disp = searchFilter + (searchFocused && (System.currentTimeMillis() / 500 % 2 == 0) ? "|" : "");
            this.fontRendererObj.drawString(disp, px + 24, searchY + 4, TEXT_PRIMARY);
        }

        // List items
        int y = py + 46;
        if (items.isEmpty()) {
            this.fontRendererObj.drawString("Aucun resultat", px + 10, y + 4, TEXT_MUTED);
        } else {
            int end = Math.min(wlScroll + maxVisible, items.size());
            for (int i = wlScroll; i < end; i++) {
                UIElement e = items.get(i);
                boolean isSel = selected == e;
                boolean isHover = inRect(mx, my, px, y, totalW - 6, WL_ROW_H); // Adjusted width to exclude scrollbar area

                // Row background
                if (isSel) {
                    Gui.drawRect(px + 1, y, px + totalW - 6, y + WL_ROW_H, 0x222A7FFF);
                    Gui.drawRect(px + 1, y, px + 3, y + WL_ROW_H, ACCENT);
                } else if (isHover) {
                    Gui.drawRect(px + 1, y, px + totalW - 6, y + WL_ROW_H, 0x0DFFFFFF);
                }

                // Status dot
                int dotCol = e.isEnabled() ? 0xFF44DD66 : TEXT_MUTED;
                Gui.drawRect(px + 10, y + 9, px + 14, y + 13, dotCol);

                // Name
                String name = friendlyName(e.getId());
                int nameCol = isSel ? TEXT_PRIMARY : (isHover ? 0xFFDDDDDD : TEXT_SECONDARY);
                this.fontRendererObj.drawStringWithShadow(name, px + 20, y + 7, nameCol);

                // Toggle
                drawToggle(px + totalW - 40, y + 5, e.isEnabled(), "wl_" + e.getId());
                y += WL_ROW_H;
            }

            // Scrollbar
            if (items.size() > maxVisible) {
                int sbTrackX = px + totalW - 5;
                int sbTrackTop = py + 46;
                int sbTrackH = listH;
                Gui.drawRect(sbTrackX, sbTrackTop, sbTrackX + 4, sbTrackTop + sbTrackH, 0x10FFFFFF);

                float ratio = (float) maxVisible / items.size();
                int thumbH = Math.max(12, (int)(sbTrackH * ratio));
                int thumbY = (int)((sbTrackH - thumbH) * ((float) wlScroll / Math.max(1, items.size() - maxVisible)));
                Gui.drawRect(sbTrackX, sbTrackTop + thumbY, sbTrackX + 4, sbTrackTop + thumbY + thumbH, ACCENT);
            }
        }
    }

    // ---- Sidebar Panel ----

    private void drawSidebar(int mx, int my) {
        if (selected == null) return;

        // Calculate height dynamically
        int h = computeSidebarHeight();
        sbH = h;

        int px = (int)(sbX - (1.0f - sidebarAnim) * 80);
        int py = sbY;
        int w = sbW;

        // Panel
        GuiRenderUtils.drawShadow(px, py, w, h, 6, 0x60);
        Gui.drawRect(px, py, px + w, py + h, BG_PANEL);
        Gui.drawRect(px, py, px + w, py + 1, ACCENT);
        Gui.drawRect(px, py + 1, px + w, py + 22, BG_HEADER);
        Gui.drawRect(px, py + 22, px + w, py + 23, BORDER);
        GuiRenderUtils.drawRectOutline(px, py, w, h, BORDER);

        // Title
        String name = friendlyName(selected.getId());
        this.fontRendererObj.drawStringWithShadow(name, px + 10, py + 7, 0xFFAAD4FF);

        // Close
        int ccx = px + w - 16, ccy = py + 6, ccs = 10;
        drawMiniClose(ccx, ccy, ccs, inRect(mx, my, ccx, ccy, ccs, ccs));
        setHB(hbSbClose, ccx, ccy, ccs, ccs);

        int y = py + 28;

        // Section: General
        y = drawSectionHeader(px, y, w, "General", ACCENT);

        // Enabled toggle
        y = drawPropertyToggle(px, y, w, "Active", selected.isEnabled(), mx, my, null);

        if (selected instanceof BaseWidget) {
            BaseWidget bw = (BaseWidget) selected;

            // Rainbow
            y = drawPropertyToggle(px, y, w, "Mode Rainbow", bw.isRGBMode(), mx, my, hbRainbow);

            // Original design (CombatLog)
            if (bw instanceof CombatLogWidget) {
                boolean orig = Boolean.TRUE.equals(bw.getPropOrDefault("originalDesign", false));
                y = drawPropertyToggle(px, y, w, "Design circulaire", orig, mx, my, hbOrigDesign);
            }

            // Snap grid
            boolean align = Boolean.TRUE.equals(bw.getPropOrDefault("snapGrid", false));
            y = drawPropertyToggle(px, y, w, "Aligner (Smart)", align, mx, my, hbAlignGrid);

            // Color preview
            this.fontRendererObj.drawStringWithShadow("Couleur", px + 12, y + 3, TEXT_SECONDARY);
            int cpX = px + w - 42, cpW = 28, cpH = 12;
            GuiRenderUtils.drawCheckerboard(cpX, y + 1, cpW, cpH, 4, 0xFF999999, 0xFF666666);
            Gui.drawRect(cpX, y + 1, cpX + cpW, y + 1 + cpH, bw.getColor());
            GuiRenderUtils.drawRectOutline(cpX, y + 1, cpW, cpH, 0x66FFFFFF);
            setHB(hbColorPreview, cpX, y + 1, cpW, cpH);
            y += 18;

            // Widget-specific sections
            if (bw instanceof KeyStrokeWidget) {
                y = drawKeystrokeSection(px, y, w, bw, mx, my);
            } else if (bw instanceof PotionStatusWidget) {
                y = drawPotionSection(px, y, w, bw, mx, my);
            } else if (bw instanceof ArmorGroupWidget) {
                y = drawArmorSection(px, y, w, bw, mx, my);
            }
        }

        // Bottom buttons
        y += 6;
        int btnW = (w - 26) / 2;
        int btnH = 16;

        boolean hoverReset = inRect(mx, my, px + 8, y, btnW, btnH);
        drawStyledButton(px + 8, y, btnW, btnH, "Reset Pos.", 0xFF1A1A28, hoverReset);
        setHB(hbResetPos, px + 8, y, btnW, btnH);

        boolean hoverWhite = inRect(mx, my, px + 18 + btnW, y, btnW, btnH);
        drawStyledButton(px + 18 + btnW, y, btnW, btnH, "Blanc", 0xFF1A1A28, hoverWhite);
        setHB(hbResetColor, px + 18 + btnW, y, btnW, btnH);
    }

    private int computeSidebarHeight() {
        int h = 28; // header
        h += 16;    // section header "General"
        h += 18;    // Enabled
        if (selected instanceof BaseWidget) {
            h += 18;  // Rainbow
            BaseWidget bw = (BaseWidget) selected;
            if (bw instanceof CombatLogWidget) h += 18;
            h += 18;  // Align
            h += 18;  // Color
            if (bw instanceof KeyStrokeWidget) {
                h += 16 + ((KeyStrokeWidget) bw).getKeyCount() * 18 + 18;
            } else if (bw instanceof PotionStatusWidget) {
                h += 16 + 18 + 18;
            } else if (bw instanceof ArmorGroupWidget) {
                h += 16 + 18 + 18;
            }
        }
        h += 30; // buttons + padding
        return h;
    }

    private int drawSectionHeader(int px, int y, int w, String label, int accent) {
        Gui.drawRect(px + 8, y + 4, px + 11, y + 11, accent);
        this.fontRendererObj.drawStringWithShadow(label, px + 15, y + 3, TEXT_PRIMARY);
        int textEnd = px + 15 + this.fontRendererObj.getStringWidth(label) + 6;
        GuiRenderUtils.drawGradientRect(textEnd, y + 7, px + w - 8, y + 8, (0x33 << 24) | (accent & 0xFFFFFF), 0);
        return y + 16;
    }

    private int drawPropertyToggle(int px, int y, int w, String label, boolean value, int mx, int my, int[] hitbox) {
        boolean hover = inRect(mx, my, px + 4, y, w - 8, 16);
        if (hover) {
            Gui.drawRect(px + 4, y, px + w - 4, y + 16, 0x08FFFFFF);
        }
        this.fontRendererObj.drawStringWithShadow(label, px + 12, y + 3, hover ? TEXT_PRIMARY : TEXT_SECONDARY);
        drawToggle(px + w - 42, y + 2, value, "sb_" + label);
        if (hitbox != null) {
            setHB(hitbox, px + w - 42, y + 2, 28, 12);
        }
        return y + 18;
    }

    private int drawKeystrokeSection(int px, int y, int w, BaseWidget bw, int mx, int my) {
        y = drawSectionHeader(px, y + 2, w, "Keystrokes", ACCENT_PURPLE);
        KeyStrokeWidget ks = (KeyStrokeWidget) bw;
        for (int i = 0; i < ks.getKeyCount(); i++) {
            String label = ks.getKeyLabel(i);
            boolean vis = Boolean.TRUE.equals(bw.getPropOrDefault("showKey" + i, true));
            boolean hover = inRect(mx, my, px + 4, y, w - 8, 16);
            if (hover) Gui.drawRect(px + 4, y, px + w - 4, y + 16, 0x08FFFFFF);
            this.fontRendererObj.drawStringWithShadow(label, px + 12, y + 3, vis ? TEXT_SECONDARY : TEXT_MUTED);
            drawToggle(px + w - 42, y + 2, vis, "key_" + i);
            setHB(hbKeyToggle[i], px + w - 42, y + 2, 28, 12);
            y += 18;
        }
        boolean spaceRainbow = Boolean.TRUE.equals(bw.getPropOrDefault("showSpaceRainbow", false));
        y = drawPropertyToggle(px, y, w, "Rainbow Espace", spaceRainbow, mx, my, hbSpaceRainbow);
        return y;
    }

    private int drawPotionSection(int px, int y, int w, BaseWidget bw, int mx, int my) {
        y = drawSectionHeader(px, y + 2, w, "Potions", ACCENT_GREEN);
        boolean showDur = Boolean.TRUE.equals(bw.getPropOrDefault("showDuration", true));
        y = drawPropertyToggle(px, y, w, "Afficher duree", showDur, mx, my, hbPotionDur);
        boolean showIcons = Boolean.TRUE.equals(bw.getPropOrDefault("showIcons", false));
        y = drawPropertyToggle(px, y, w, "Afficher icones", showIcons, mx, my, hbPotionIcons);
        return y;
    }

    private int drawArmorSection(int px, int y, int w, BaseWidget bw, int mx, int my) {
        y = drawSectionHeader(px, y + 2, w, "Armure", ACCENT_ORANGE);
        String layout = String.valueOf(bw.getPropOrDefault("layout", "horizontal"));
        y = drawPropertyToggle(px, y, w, "Disposition Verticale", "vertical".equals(layout), mx, my, hbArmorLayout);
        boolean dispPct = Boolean.TRUE.equals(bw.getPropOrDefault("displayPercent", true));
        y = drawPropertyToggle(px, y, w, "Afficher en %", dispPct, mx, my, hbArmorPercent);
        return y;
    }

    // ---- Color Editor Panel ----

    private void drawColorEditor(int mx, int my) {
        int px = ceX, py = ceY;

        // Panel
        GuiRenderUtils.drawShadow(px, py, CE_W, CE_H, 8, 0x70);
        Gui.drawRect(px, py, px + CE_W, py + CE_H, BG_PANEL);
        Gui.drawRect(px, py, px + CE_W, py + 1, ACCENT_PURPLE);
        Gui.drawRect(px, py + 1, px + CE_W, py + 22, BG_HEADER);
        Gui.drawRect(px, py + 22, px + CE_W, py + 23, BORDER);
        GuiRenderUtils.drawRectOutline(px, py, CE_W, CE_H, BORDER);

        this.fontRendererObj.drawStringWithShadow("Editeur de couleur", px + 10, py + 7, 0xFFCC88FF);

        // Close
        int ccx = px + CE_W - 16, ccy = py + 6, ccs = 10;
        drawMiniClose(ccx, ccy, ccs, inRect(mx, my, ccx, ccy, ccs, ccs));
        setHB(hbCeClose, ccx, ccy, ccs, ccs);

        // Spectrum
        int specX = px + 10, specY = py + 28;
        for (int sx = 0; sx < ceSpecW; sx++) {
            float hue = sx / (float) ceSpecW;
            for (int sy = 0; sy < ceSpecH; sy++) {
                float val = 1.0f - (sy / (float) ceSpecH);
                Gui.drawRect(specX + sx, specY + sy, specX + sx + 1, specY + sy + 1,
                        0xFF000000 | java.awt.Color.HSBtoRGB(hue, 1.0f, val));
            }
        }
        GuiRenderUtils.drawRectOutline(specX, specY, ceSpecW, ceSpecH, 0x33FFFFFF);

        // RGBA sliders
        int sldX = specX + ceSpecW + 15;
        int sldW = CE_W - ceSpecW - 35;
        drawChannelSlider(sldX, specY, sldW, "R", r, 0xFFEE4444);
        drawChannelSlider(sldX, specY + 22, sldW, "G", g, 0xFF44EE44);
        drawChannelSlider(sldX, specY + 44, sldW, "B", b, 0xFF4444EE);
        drawChannelSlider(sldX, specY + 66, sldW, "A", a, 0xFF888888);

        // Preview
        int pvY = specY + 88;
        GuiRenderUtils.drawCheckerboard(sldX, pvY, sldW, 14, 4, 0xFF999999, 0xFF666666);
        Gui.drawRect(sldX, pvY, sldX + sldW, pvY + 14, (a << 24) | (r << 16) | (g << 8) | b);
        GuiRenderUtils.drawRectOutline(sldX, pvY, sldW, 14, 0x66FFFFFF);

        // Hex display
        String hex = String.format("#%02X%02X%02X", r, g, b);
        this.fontRendererObj.drawString(hex, px + 10, py + CE_H - 14, TEXT_MUTED);
    }

    private void drawChannelSlider(int x, int y, int w, String label, int val, int color) {
        this.fontRendererObj.drawStringWithShadow(label, x - 10, y + 1, color);
        Gui.drawRect(x, y + 3, x + w, y + 9, 0xFF111122);
        int fill = (int)(val / 255f * w);
        GuiRenderUtils.drawGradientRect(x, y + 3, x + fill, y + 9,
                GuiRenderUtils.colorLerp(0xFF111122, color, 0.3f), color);
        // Thumb
        Gui.drawRect(x + fill - 1, y + 1, x + fill + 1, y + 11, 0xFFEEEEEE);
        // Value text
        String valStr = String.valueOf(val);
        this.fontRendererObj.drawString(valStr, x + w + 3, y + 1, TEXT_MUTED);
    }

    // ---- Shared drawing helpers ----

    private void drawToggle(int x, int y, boolean value, String id) {
        float target = value ? 1.0f : 0.0f;
        float current = toggleAnimMap.getOrDefault(id, target);
        current = GuiRenderUtils.lerp(current, target, 0.18f);
        toggleAnimMap.put(id, current);
        GuiRenderUtils.drawSmoothToggle(x, y, value, current);
    }

    private void drawMiniClose(int x, int y, int size, boolean hovered) {
        int bg = hovered ? 0xCCCC3333 : 0x44662222;
        Gui.drawRect(x, y, x + size, y + size, bg);
        GuiRenderUtils.drawRectOutline(x, y, size, size, 0x22FFFFFF);
        // X icon
        int col = hovered ? 0xFFFFFFFF : 0xAABBBBBB;
        this.fontRendererObj.drawString("x", x + 2, y + 1, col);
    }

    private void drawStyledButton(int x, int y, int w, int h, String text, int bgColor, boolean hovered) {
        int bg = hovered ? GuiRenderUtils.colorLerp(bgColor, 0xFFFFFFFF, 0.12f) : bgColor;
        Gui.drawRect(x, y, x + w, y + h, bg);
        GuiRenderUtils.drawRectOutline(x, y, w, h, hovered ? 0x33FFFFFF : 0x1AFFFFFF);
        if (hovered) {
            GuiRenderUtils.drawGradientRect(x + 1, y + 1, x + w - 1, y + 3, 0x18FFFFFF, 0x00000000);
        }
        int tw = this.fontRendererObj.getStringWidth(text);
        this.fontRendererObj.drawStringWithShadow(text,
                x + (w - tw) / 2.0f, y + (h - 8) / 2.0f,
                hovered ? TEXT_PRIMARY : TEXT_SECONDARY);
    }

    private void drawGrid(int step, int color) {
        for (int gx = 0; gx < this.width; gx += step) Gui.drawRect(gx, 0, gx + 1, this.height, color);
        for (int gy = 0; gy < this.height; gy += step) Gui.drawRect(0, gy, this.width, gy + 1, color);
    }

    // ========================================================================
    // INPUT HANDLING
    // ========================================================================

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        if (btn != 0) return;
        searchFocused = false;

        // Done button
        int dBtnW = 100, dBtnH = 18;
        int dBtnX = (this.width - dBtnW) / 2, dBtnY = this.height - 28;
        if (inRect(mx, my, dBtnX, dBtnY, dBtnW, dBtnH)) {
            this.mc.displayGuiScreen(parent);
            return;
        }

        // Check panels in reverse z-order
        for (int i = panelOrder.size() - 1; i >= 0; i--) {
            String p = panelOrder.get(i);
            if ("colorEditor".equals(p) && colorEditorOpen && handleColorClick(mx, my)) return;
            if ("sidebar".equals(p) && selected != null && handleSidebarClick(mx, my)) return;
            if ("widgetList".equals(p) && handleWidgetListClick(mx, my)) return;
        }

        // Click on a widget in the viewport
        for (UIElement e : ui.all()) {
            if (e.isEnabled() && e.containsPoint(mx, my)) {
                selected = e;
                isDraggingWidget = true;
                dragOffsetX = mx - e.getX();
                dragOffsetY = my - e.getY();
                bringToFront("sidebar");
                return;
            }
        }
        selected = null;
    }

    private boolean handleWidgetListClick(int mx, int my) {
        int totalW = wlW + 20;
        List<UIElement> items = getFilteredWidgets();
        int maxH = Math.min(WL_MAX_H, this.height - 80);
        int maxVisible = (maxH - 50) / WL_ROW_H;
        int listH = Math.min(maxVisible, Math.max(1, items.size())) * WL_ROW_H;
        int h = 50 + listH;

        if (!inRect(mx, my, wlX, wlY, totalW, h)) return false;
        bringToFront("widgetList");

        // Close
        if (inRect(mx, my, hbWlClose[0], hbWlClose[1], hbWlClose[2], hbWlClose[3])) {
            wlX = -500;
            return true;
        }
        
        // Scrollbar dragging (priority)
        if (items.size() > maxVisible) {
            int sbTrackX = wlX + totalW - 5;
            if (mx >= sbTrackX && mx <= sbTrackX + 4 && my >= wlY + 46 && my <= wlY + 46 + listH) {
                wlScrollDragging = true;
                updateWlScrollFromMouse(my);
                return true;
            }
        }

        // Header drag
        if (inRect(mx, my, wlX, wlY, totalW, 22)) {
            wlDragging = true;
            wlDragOX = mx;
            wlDragOY = my;
            return true;
        }
        // Search bar
        if (inRect(mx, my, wlX + 8, wlY + 26, totalW - 16, 16)) {
            searchFocused = true;
            return true;
        }
        // List items
        int y = wlY + 46;
        for (int i = wlScroll; i < Math.min(wlScroll + maxVisible, items.size()); i++) {
            UIElement e = items.get(i);
            // Toggle
            if (inRect(mx, my, wlX + totalW - 40, y + 5, 28, 12)) {
                e.setEnabled(!e.isEnabled());
                ui.saveConfig();
                return true;
            }
            // Selection (if not clicking scrollbar area)
            if (inRect(mx, my, wlX, y, totalW - 6, WL_ROW_H)) {
                selected = e;
                bringToFront("sidebar");
                return true;
            }
            y += WL_ROW_H;
        }
        return true;
    }

    private void updateWlScrollFromMouse(int my) {
        List<UIElement> items = getFilteredWidgets();
        int maxH = Math.min(WL_MAX_H, this.height - 80);
        int maxVisible = (maxH - 50) / WL_ROW_H;
        int listH = Math.min(maxVisible, Math.max(1, items.size())) * WL_ROW_H;
        
        float ratio = (float)(my - (wlY + 46)) / (float)listH;
        wlScroll = (int)(ratio * items.size() - maxVisible / 2.0f);
        wlScroll = GuiRenderUtils.clamp(wlScroll, 0, Math.max(0, items.size() - maxVisible));
    }

    private boolean handleSidebarClick(int mx, int my) {
        int px = (int)(sbX - (1.0f - sidebarAnim) * 80);
        if (!inRect(mx, my, px, sbY, sbW, sbH)) return false;
        bringToFront("sidebar");

        // Close
        if (inRect(mx, my, hbSbClose[0], hbSbClose[1], hbSbClose[2], hbSbClose[3])) {
            selected = null;
            return true;
        }
        // Header drag
        if (inRect(mx, my, px, sbY, sbW, 22)) {
            sbDragging = true;
            sbDragOX = mx;
            sbDragOY = my;
            return true;
        }

        // Enabled toggle (first toggle after section header)
        int y = sbY + 28 + 16; // after header + section header
        if (inRect(mx, my, px + sbW - 42, y + 2, 28, 12)) {
            selected.setEnabled(!selected.isEnabled());
            ui.saveConfig();
            return true;
        }

        if (selected instanceof BaseWidget) {
            BaseWidget bw = (BaseWidget) selected;

            // Rainbow
            if (inRect(mx, my, hbRainbow[0], hbRainbow[1], hbRainbow[2], hbRainbow[3])) {
                bw.setRGBMode(!bw.isRGBMode());
                ui.saveConfig();
                return true;
            }
            // Original design
            if (bw instanceof CombatLogWidget && inRect(mx, my, hbOrigDesign[0], hbOrigDesign[1], hbOrigDesign[2], hbOrigDesign[3])) {
                boolean cur = Boolean.TRUE.equals(bw.getPropOrDefault("originalDesign", false));
                bw.setProp("originalDesign", !cur);
                ui.saveConfig();
                return true;
            }
            // Align grid
            if (inRect(mx, my, hbAlignGrid[0], hbAlignGrid[1], hbAlignGrid[2], hbAlignGrid[3])) {
                boolean cur = Boolean.TRUE.equals(bw.getPropOrDefault("snapGrid", false));
                bw.setProp("snapGrid", !cur);
                ui.saveConfig();
                return true;
            }
            // Color preview
            if (inRect(mx, my, hbColorPreview[0], hbColorPreview[1], hbColorPreview[2], hbColorPreview[3])) {
                colorEditorOpen = true;
                bringToFront("colorEditor");
                int c = bw.getColor();
                a = (c >> 24) & 0xFF;
                r = (c >> 16) & 0xFF;
                g = (c >> 8) & 0xFF;
                b = c & 0xFF;
                return true;
            }

            // Widget-specific toggles
            if (bw instanceof KeyStrokeWidget) {
                KeyStrokeWidget ks = (KeyStrokeWidget) bw;
                for (int i = 0; i < ks.getKeyCount(); i++) {
                    if (inRect(mx, my, hbKeyToggle[i][0], hbKeyToggle[i][1], hbKeyToggle[i][2], hbKeyToggle[i][3])) {
                        boolean vis = Boolean.TRUE.equals(bw.getPropOrDefault("showKey" + i, true));
                        bw.setProp("showKey" + i, !vis);
                        ui.saveConfig();
                        return true;
                    }
                }
                if (inRect(mx, my, hbSpaceRainbow[0], hbSpaceRainbow[1], hbSpaceRainbow[2], hbSpaceRainbow[3])) {
                    boolean cur = Boolean.TRUE.equals(bw.getPropOrDefault("showSpaceRainbow", false));
                    bw.setProp("showSpaceRainbow", !cur);
                    ui.saveConfig();
                    return true;
                }
            } else if (bw instanceof PotionStatusWidget) {
                if (inRect(mx, my, hbPotionDur[0], hbPotionDur[1], hbPotionDur[2], hbPotionDur[3])) {
                    boolean cur = Boolean.TRUE.equals(bw.getPropOrDefault("showDuration", true));
                    bw.setProp("showDuration", !cur);
                    ui.saveConfig();
                    return true;
                }
                if (inRect(mx, my, hbPotionIcons[0], hbPotionIcons[1], hbPotionIcons[2], hbPotionIcons[3])) {
                    boolean cur = Boolean.TRUE.equals(bw.getPropOrDefault("showIcons", false));
                    bw.setProp("showIcons", !cur);
                    ui.saveConfig();
                    return true;
                }
            } else if (bw instanceof ArmorGroupWidget) {
                if (inRect(mx, my, hbArmorLayout[0], hbArmorLayout[1], hbArmorLayout[2], hbArmorLayout[3])) {
                    String cur = String.valueOf(bw.getPropOrDefault("layout", "horizontal"));
                    bw.setProp("layout", "horizontal".equals(cur) ? "vertical" : "horizontal");
                    ui.saveConfig();
                    return true;
                }
                if (inRect(mx, my, hbArmorPercent[0], hbArmorPercent[1], hbArmorPercent[2], hbArmorPercent[3])) {
                    boolean cur = Boolean.TRUE.equals(bw.getPropOrDefault("displayPercent", true));
                    bw.setProp("displayPercent", !cur);
                    ui.saveConfig();
                    return true;
                }
            }
        }

        // Reset buttons
        if (inRect(mx, my, hbResetPos[0], hbResetPos[1], hbResetPos[2], hbResetPos[3])) {
            selected.setPosition(10, 10);
            ui.saveConfig();
            return true;
        }
        if (inRect(mx, my, hbResetColor[0], hbResetColor[1], hbResetColor[2], hbResetColor[3])) {
            selected.setColor(0xFFFFFFFF);
            if (selected instanceof BaseWidget) ((BaseWidget) selected).setRGBMode(false);
            ui.saveConfig();
            return true;
        }
        return true;
    }

    private boolean handleColorClick(int mx, int my) {
        if (!inRect(mx, my, ceX, ceY, CE_W, CE_H)) return false;
        bringToFront("colorEditor");

        if (inRect(mx, my, hbCeClose[0], hbCeClose[1], hbCeClose[2], hbCeClose[3])) {
            colorEditorOpen = false;
            return true;
        }
        if (inRect(mx, my, ceX, ceY, CE_W, 22)) {
            ceDragging = true;
            ceDragOX = mx;
            ceDragOY = my;
            return true;
        }

        int specX = ceX + 10, specY = ceY + 28;
        if (inRect(mx, my, specX, specY, ceSpecW, ceSpecH)) {
            draggingSpectrum = true;
            updateColorFromSpectrum(mx, my);
            return true;
        }

        int sldX = specX + ceSpecW + 15, sldW = CE_W - ceSpecW - 35;
        for (int i = 0; i < 4; i++) {
            if (inRect(mx, my, sldX, specY + i * 22, sldW, 12)) {
                draggingSlider = i;
                updateColorFromSlider(mx);
                return true;
            }
        }
        return true;
    }

    @Override
    protected void mouseClickMove(int mx, int my, int btn, long time) {
        if (isDraggingWidget && selected != null) {
            int nx = mx - dragOffsetX;
            int ny = my - dragOffsetY;
            snapLineX = -1;
            snapLineY = -1;

            if (selected instanceof BaseWidget
                    && Boolean.TRUE.equals(((BaseWidget) selected).getPropOrDefault("snapGrid", false))
                    && !isShiftKeyDown()) {
                int threshold = 4;
                int w = selected.getWidth();
                int h = selected.getHeight();

                // Screen snapping
                if (Math.abs(nx) < threshold) { nx = 0; snapLineX = 0; }
                else if (Math.abs(nx + w - this.width) < threshold) { nx = this.width - w; snapLineX = this.width; }
                else if (Math.abs(nx + w / 2 - this.width / 2) < threshold) { nx = this.width / 2 - w / 2; snapLineX = this.width / 2; }

                if (Math.abs(ny) < threshold) { ny = 0; snapLineY = 0; }
                else if (Math.abs(ny + h - this.height) < threshold) { ny = this.height - h; snapLineY = this.height; }
                else if (Math.abs(ny + h / 2 - this.height / 2) < threshold) { ny = this.height / 2 - h / 2; snapLineY = this.height / 2; }

                // Widget snapping
                for (UIElement other : ui.all()) {
                    if (other == selected || !other.isEnabled()) continue;
                    int ox = other.getX(), oy = other.getY();
                    int ow = other.getWidth(), oh = other.getHeight();

                    if (Math.abs(nx - ox) < threshold) { nx = ox; snapLineX = ox; }
                    else if (Math.abs(nx + w - (ox + ow)) < threshold) { nx = ox + ow - w; snapLineX = ox + ow; }
                    else if (Math.abs(nx - (ox + ow)) < threshold) { nx = ox + ow; snapLineX = ox + ow; }
                    else if (Math.abs(nx + w - ox) < threshold) { nx = ox - w; snapLineX = ox; }
                    else if (Math.abs(nx + w / 2 - (ox + ow / 2)) < threshold) { nx = ox + ow / 2 - w / 2; snapLineX = ox + ow / 2; }

                    if (Math.abs(ny - oy) < threshold) { ny = oy; snapLineY = oy; }
                    else if (Math.abs(ny + h - (oy + oh)) < threshold) { ny = oy + oh - h; snapLineY = oy + oh; }
                    else if (Math.abs(ny - (oy + oh)) < threshold) { ny = oy + oh; snapLineY = oy + oh; }
                    else if (Math.abs(ny + h - oy) < threshold) { ny = oy - h; snapLineY = oy; }
                    else if (Math.abs(ny + h / 2 - (oy + oh / 2)) < threshold) { ny = oy + oh / 2 - h / 2; snapLineY = oy + oh / 2; }
                }
            }
            selected.setPosition(nx, ny);
        }

        if (wlDragging) {
            wlX += mx - wlDragOX;
            wlY += my - wlDragOY;
            wlDragOX = mx;
            wlDragOY = my;
        }
        if (wlScrollDragging) {
            updateWlScrollFromMouse(my);
        }
        if (sbDragging) {
            sbX += mx - sbDragOX;
            sbY += my - sbDragOY;
            sbDragOX = mx;
            sbDragOY = my;
        }
        if (ceDragging) {
            ceX += mx - ceDragOX;
            ceY += my - ceDragOY;
            ceDragOX = mx;
            ceDragOY = my;
        }
        if (draggingSpectrum) updateColorFromSpectrum(mx, my);
        if (draggingSlider != -1) updateColorFromSlider(mx);
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        isDraggingWidget = false;
        wlDragging = false;
        wlScrollDragging = false;
        sbDragging = false;
        ceDragging = false;
        draggingSpectrum = false;
        draggingSlider = -1;
        snapLineX = -1;
        snapLineY = -1;
        ui.saveConfig();
        super.mouseReleased(mx, my, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (searchFocused) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                searchFocused = false;
            } else if (keyCode == Keyboard.KEY_BACK) {
                if (!searchFilter.isEmpty()) searchFilter = searchFilter.substring(0, searchFilter.length() - 1);
            } else if (keyCode == Keyboard.KEY_RETURN) {
                searchFocused = false;
            } else if (typedChar >= 32) {
                searchFilter += typedChar;
            }
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        lastMouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        lastMouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0 && inRect(lastMouseX, lastMouseY, wlX, wlY, wlW + 20, WL_MAX_H)) {
            if (scroll > 0) wlScroll = Math.max(0, wlScroll - 1);
            else wlScroll++;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    // ========================================================================
    // COLOR LOGIC
    // ========================================================================

    private void updateColorFromSpectrum(int mx, int my) {
        float hue = GuiRenderUtils.clamp(mx - (ceX + 10), 0, ceSpecW) / (float) ceSpecW;
        float val = 1.0f - GuiRenderUtils.clamp(my - (ceY + 28), 0, ceSpecH) / (float) ceSpecH;
        int rgb = java.awt.Color.HSBtoRGB(hue, 1.0f, val);
        r = (rgb >> 16) & 0xFF;
        g = (rgb >> 8) & 0xFF;
        b = rgb & 0xFF;
        applyColor();
    }

    private void updateColorFromSlider(int mx) {
        int sldX = ceX + 10 + ceSpecW + 15;
        int sldW = CE_W - ceSpecW - 35;
        int val = (int)(GuiRenderUtils.clamp(mx - sldX, 0, sldW) / (float) sldW * 255);
        if (draggingSlider == 0) r = val;
        else if (draggingSlider == 1) g = val;
        else if (draggingSlider == 2) b = val;
        else if (draggingSlider == 3) a = val;
        applyColor();
    }

    private void applyColor() {
        if (selected instanceof BaseWidget) {
            ((BaseWidget) selected).setColor((a << 24) | (r << 16) | (g << 8) | b);
            ui.saveConfig();
        }
    }

    // ========================================================================
    // UTILITIES
    // ========================================================================

    private List<UIElement> getFilteredWidgets() {
        return ui.all().stream()
                .filter(e -> searchFilter.isEmpty()
                        || friendlyName(e.getId()).toLowerCase().contains(searchFilter.toLowerCase()))
                .collect(Collectors.toList());
    }

    private void bringToFront(String name) {
        panelOrder.remove(name);
        panelOrder.add(name);
    }

    private boolean inRect(int mx, int my, int rx, int ry, int rw, int rh) {
        return mx >= rx && my >= ry && mx < rx + rw && my < ry + rh;
    }

    private void setHB(int[] hb, int x, int y, int w, int h) {
        hb[0] = x;
        hb[1] = y;
        hb[2] = w;
        hb[3] = h;
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
            case "potions": return "Potions";
            case "cps": return "CPS";
            case "toggle_sneak": return "Toggle Sneak";
            case "toggle_sprint": return "Toggle Sprint";
            case "combatlog": return "Combat Tag";
            case "keystrokes": return "Keystrokes";
            case "reach": return "Reach Display";
            default:
                String s = id.replace('_', ' ');
                return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }
}
