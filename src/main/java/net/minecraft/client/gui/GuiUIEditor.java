package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ui.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
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
    private static final int ACCENT        = 0xFFE02828;  // Vibrant red
    private static final int ACCENT_LINE   = 0xFFE02828;  // Red top border
    private static final int ACCENT_DIM    = 0xFF6E1212;  // Dim red for details
    private static final int BG_PANEL      = 0xF20A0808;  // Deep dark panel
    private static final int BG_HEADER     = 0xFF0E0606;  // Very dark header
    private static final int BORDER        = 0x2BFFFFFF;
    private static final int TEXT_PRIMARY  = 0xFFF2F2F2;
    private static final int TEXT_SECONDARY = 0xFF8A9AB0;
    private static final int TEXT_MUTED    = 0xFF445060;
    private static final int ACCENT_PURPLE = 0xFFAA44EE;
    private static final int ACCENT_GREEN  = 0xFF2ECC71;
    private static final int ACCENT_ORANGE = 0xFFE8871A;

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
    private int sbScroll = 0;

    // ---- Color editor ----
    private boolean colorEditorOpen = false;
    private int ceX, ceY;
    private static final int CE_W = 240, CE_H = 173;
    private int ceSpecW = 120, ceSpecH = 100;
    private int r = 255, g = 255, b = 255, a = 255;

    // ---- Drag state ----
    private boolean isDraggingWidget, wlDragging, sbDragging, ceDragging, draggingSpectrum;
    private int dragOffsetX, dragOffsetY;
    private int wlDragOX, wlDragOY, sbDragOX, sbDragOY, ceDragOX, ceDragOY;
    private int draggingSlider = -1;
    private int lastMouseX, lastMouseY;

    // ---- Resize state ----
    private boolean isResizingWidget = false;
    private int resizeEdge = 0; // bitmask: 1=left, 2=right, 4=top, 8=bottom
    private int resizeStartX, resizeStartY;
    private int resizeStartW, resizeStartH, resizeStartWidgetX, resizeStartWidgetY;
    private float resizeStartScale;
    private static final int RESIZE_HANDLE_SIZE = 6; // smaller handles to avoid accidental resize

    // ---- Snapping ----
    private int snapLineX = -1, snapLineY = -1;
    // Alignment guide caches (x and y positions of other widgets used for visual guides while dragging)
    private final List<Integer> guideXs = new ArrayList<>();
    private final List<Integer> guideYs = new ArrayList<>();

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
    private final int[] hbCompassWaypoints = new int[4];
    private final int[] hbCompassWpLabels  = new int[4];
    private final int[][] hbKeyToggle = new int[9][4];
    private final int[] hbDoneBtn = new int[4];
    private final int[] hbResetSize = new int[4];
    private final int[] hbScaleSlider = new int[4];

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

        wlW = GuiRenderUtils.clamp(this.width / 5, 140, 190);
        wlX = this.width - wlW - 28;
        wlY = 30;

        sbW = 185;
        sbX = 10;
        sbY = 30;

        ceX = this.width / 2 - CE_W / 2;
        ceY = this.height / 2 - CE_H / 2;
        clampColorEditorPosition();

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
        this.drawRect(0, 0, this.width, this.height, (int)(ease * 150) << 24);
        drawGrid(16, (int)(ease * 6) << 24 | 0xFFFFFF);

        // Snap lines
        if (snapLineX != -1) Gui.drawRect(snapLineX, 0, snapLineX + 1, this.height, 0xCCE02828);
        if (snapLineY != -1) Gui.drawRect(0, snapLineY, this.width, snapLineY + 1, 0xCCE02828);

        // Render all widgets (BACKGROUND LAYER)
        for (UIElement e : ui.all()) {
            e.render(mouseX, mouseY, partialTicks);
            if (selected == e) GuiRenderUtils.drawSelectionHalo(e.getX(), e.getY(), e.getWidth(), e.getHeight(), ACCENT);
        }

        // Draw resize handles on selected widget
        if (selected != null) drawResizeHandles(selected);

        // Panel animations
        sidebarAnim = GuiRenderUtils.lerp(sidebarAnim, selected != null ? 1.0f : 0.0f, 0.15f);
        widgetListAnim = GuiRenderUtils.lerp(widgetListAnim, wlX > -400 ? 1.0f : 0.0f, 0.15f);

        // Render panels in z-order (OVERLAY LAYER)
        for (String panel : panelOrder) {
            if ("widgetList".equals(panel) && widgetListAnim > 0.01f) drawWidgetList(mouseX, mouseY);
            else if ("sidebar".equals(panel) && sidebarAnim > 0.01f) drawSidebar(mouseX, mouseY);
            else if ("colorEditor".equals(panel) && colorEditorOpen && selected instanceof BaseWidget) drawColorEditor(mouseX, mouseY);
        }

        // Done button — compact, outlined style
        int btnW = 96, btnH = 20;
        int btnX = (this.width - btnW) / 2;
        int btnY = this.height - 30;
        boolean btnHover = inRect(mouseX, mouseY, btnX, btnY, btnW, btnH);
        // Background
        Gui.drawRect(btnX, btnY, btnX + btnW, btnY + btnH,
                btnHover ? 0x28FFFFFF : 0x0A000000);
        // Top accent line
        Gui.drawRect(btnX, btnY, btnX + btnW, btnY + 1, btnHover ? ACCENT : ACCENT_DIM);
        // Outline
        GuiRenderUtils.drawRectOutline(btnX, btnY, btnW, btnH, btnHover ? 0x44FFFFFF : 0x28FFFFFF);
        if (btnHover) {
            Gui.drawRect(btnX, btnY + 1, btnX + 2, btnY + btnH - 1, ACCENT);
        }
        // Label
        int btnTw = fontRendererObj.getStringWidth("RETOUR");
        fontRendererObj.drawStringWithShadow("RETOUR", btnX + (btnW - btnTw) / 2, btnY + (btnH - 8) / 2.0f,
                btnHover ? 0xFFFFFFFF : TEXT_SECONDARY);
        setHB(hbDoneBtn, btnX, btnY, btnW, btnH);
    }


    private void drawWidgetList(int mx, int my) {
        int px = wlX, py = wlY;
        int totalW = wlW + 20;
        List<UIElement> items = getFilteredWidgets();
        int maxH = Math.min(WL_MAX_H, this.height - 80);
        int maxVisible = (maxH - 50) / WL_ROW_H;
        int listH = Math.min(maxVisible, Math.max(1, items.size())) * WL_ROW_H;
        int h = 50 + listH;
        wlScroll = GuiRenderUtils.clamp(wlScroll, 0, Math.max(0, items.size() - maxVisible));

        GuiRenderUtils.drawShadow(px, py, totalW, h, 10, 0x80);
        Gui.drawRect(px, py, px + totalW, py + h, BG_PANEL);
        // Top accent line (2px) + subtle glow
        Gui.drawRect(px, py, px + totalW, py + 2, ACCENT_LINE);
        GuiRenderUtils.drawGradientRect(px, py + 2, px + totalW, py + 8, 0x18E02828, 0x00000000);
        GuiRenderUtils.drawGradientRect(px, py + 2, px + totalW, py + 24, BG_HEADER, BG_PANEL);
        Gui.drawRect(px, py + 24, px + totalW, py + 25, 0x28FFFFFF);
        GuiRenderUtils.drawRectOutline(px, py, totalW, h, BORDER);
        // Left accent bar (2px) + title
        Gui.drawRect(px + 7, py + 9, px + 9, py + 19, ACCENT);
        fontRendererObj.drawStringWithShadow("W", px + 14, py + 9, 0xFFFF8888);
        fontRendererObj.drawStringWithShadow("IDGETS", px + 14 + fontRendererObj.getStringWidth("W"), py + 9, TEXT_PRIMARY);

        int ccx = px + totalW - 16, ccy = py + 7, ccs = 10;
        drawMiniClose(ccx, ccy, ccs, inRect(mx, my, ccx, ccy, ccs, ccs));
        setHB(hbWlClose, ccx, ccy, ccs, ccs);

        int searchY = py + 27, searchW = totalW - 16;
        Gui.drawRect(px + 8, searchY, px + 8 + searchW, searchY + 16, searchFocused ? 0x44FFFFFF : 0x16FFFFFF);
        GuiRenderUtils.drawRectOutline(px + 8, searchY, searchW, 16, searchFocused ? ACCENT : 0x2AFFFFFF);
        if (searchFocused) Gui.drawRect(px + 8, searchY, px + 8 + searchW, searchY + 2, ACCENT);
        GuiRenderUtils.drawSearchIcon(px + 12, searchY + 4, TEXT_MUTED);
        if (searchFilter.isEmpty() && !searchFocused) fontRendererObj.drawString("Rechercher...", px + 24, searchY + 4, TEXT_MUTED);
        else fontRendererObj.drawString(searchFilter + (searchFocused && (System.currentTimeMillis() / 500 % 2 == 0) ? "|" : ""), px + 24, searchY + 4, TEXT_PRIMARY);

        int y = py + 47;
        if (items.isEmpty()) fontRendererObj.drawString("Aucun resultat", px + 10, y + 4, TEXT_MUTED);
        else {
            int end = Math.min(wlScroll + maxVisible, items.size());
            for (int i = wlScroll; i < end; i++) {
                UIElement e = items.get(i);
                boolean isSel = selected == e, isHover = inRect(mx, my, px, y, totalW - 6, WL_ROW_H);
                if (isSel) {
                    GuiRenderUtils.drawGradientRect(px+1, y, px+totalW-6, y+WL_ROW_H, 0x28E02828, 0x18E02828);
                    Gui.drawRect(px+1, y, px+4, y+WL_ROW_H, ACCENT);
                    Gui.drawRect(px+4, y, px+5, y+WL_ROW_H, ACCENT_DIM);
                } else if (isHover) {
                    Gui.drawRect(px+1, y, px+totalW-6, y+WL_ROW_H, 0x0EFFFFFF);
                }
                // Status dot — 8×8
                boolean enab = e.isEnabled();
                int dotX2 = px + 10, dotY2 = y + WL_ROW_H / 2 - 4;
                if (enab) {
                    Gui.drawRect(dotX2 - 1, dotY2 - 1, dotX2 + 9, dotY2 + 9, 0x1822EE55);
                    Gui.drawRect(dotX2, dotY2, dotX2 + 8, dotY2 + 8, 0xFF22CC50);
                    Gui.drawRect(dotX2 + 1, dotY2 + 1, dotX2 + 4, dotY2 + 2, 0x44FFFFFF);
                } else {
                    Gui.drawRect(dotX2, dotY2, dotX2 + 8, dotY2 + 8, 0xFF141420);
                    GuiRenderUtils.drawRectOutline(dotX2, dotY2, 8, 8, 0x22FFFFFF);
                }
                fontRendererObj.drawStringWithShadow(friendlyName(e.getId()), px + 22, y + 7, isSel ? TEXT_PRIMARY : (isHover ? 0xFFEEEEEE : TEXT_SECONDARY));
                drawToggle(px + totalW - 40, y + 5, e.isEnabled(), "wl_" + e.getId());
                y += WL_ROW_H;
            }
            if (items.size() > maxVisible) {
                int sbTrackX = px + totalW - 5, sbTrackTop = py + 46;
                Gui.drawRect(sbTrackX, sbTrackTop, sbTrackX + 4, sbTrackTop + listH, 0x10FFFFFF);
                int thumbH = Math.max(12, (int)(listH * ((float)maxVisible / items.size())));
                int thumbY = (int)((listH - thumbH) * ((float) wlScroll / (items.size() - maxVisible)));
                Gui.drawRect(sbTrackX, sbTrackTop + thumbY, sbTrackX + 4, sbTrackTop + thumbY + thumbH, ACCENT);
            }
        }
    }

    private void drawSidebar(int mx, int my) {
        if (selected == null) return;
        int px = (int)(sbX - (1.0f - sidebarAnim) * 80), py = sbY;
        int w = (selected instanceof KeyStrokeWidget) ? 260 : sbW;
        List<SidebarItem> actions = buildSidebarItems();
        int contentH = calculateContentHeight(actions);
        int maxPanelH = this.height - py - 20;
        sbH = Math.min(maxPanelH, contentH + 28);
        if (py + sbH > this.height - 10) py = Math.max(5, this.height - sbH - 10);
        sbScroll = GuiRenderUtils.clamp(sbScroll, 0, Math.max(0, contentH - (sbH - 28)));

        GuiRenderUtils.drawShadow(px, py, w, sbH, 6, 0x60);
        Gui.drawRect(px, py, px + w, py + sbH, BG_PANEL);
        // Top accent line (2px) + subtle glow
        Gui.drawRect(px, py, px + w, py + 2, ACCENT_LINE);
        GuiRenderUtils.drawGradientRect(px, py + 2, px + w, py + 8, 0x18E02828, 0x00000000);
        GuiRenderUtils.drawGradientRect(px, py + 2, px + w, py + 24, BG_HEADER, BG_PANEL);
        Gui.drawRect(px, py + 23, px + w, py + 24, 0x28FFFFFF);
        GuiRenderUtils.drawRectOutline(px, py, w, sbH, BORDER);
        // Left accent bar (2px) + widget name
        Gui.drawRect(px + 7, py + 9, px + 9, py + 19, ACCENT);
        fontRendererObj.drawStringWithShadow(friendlyName(selected.getId()).toUpperCase(), px + 15, py + 9, TEXT_PRIMARY);
        int ccx = px + w - 16, ccy = py + 7, ccs = 10;
        drawMiniClose(ccx, ccy, ccs, inRect(mx, my, ccx, ccy, ccs, ccs));
        setHB(hbSbClose, ccx, ccy, ccs, ccs);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        ScaledResolution sr = new ScaledResolution(mc);
        int f = sr.getScaleFactor();
        GL11.glScissor(px * f, (this.height - py - sbH + 2) * f, w * f, (sbH - 25) * f);
        int curY = py + 28 - sbScroll;
        for (SidebarItem item : actions) { item.draw(px, curY, w, mx, my); curY += item.getHeight(); }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        if (contentH > sbH - 28) {
            int sX = px + w - 3, sY = py + 25, sH = sbH - 30;
            Gui.drawRect(sX, sY, sX + 1, sY + sH, 0x10FFFFFF);
            int tH = Math.max(10, (int)(sH * ((float)(sbH - 28) / contentH)));
            int tY = (int)((sH - tH) * ((float)sbScroll / (contentH - (sbH - 28))));
            Gui.drawRect(sX, sY + tY, sX + 1, sY + tY + tH, ACCENT);
        }
    }

    private int calculateContentHeight(List<SidebarItem> items) {
        int h = 0;
        for (SidebarItem i : items) h += i.getHeight();
        return h + 5;
    }

    private void drawColorEditor(int mx, int my) {
        clampColorEditorPosition();
        int px = ceX, py = ceY;
        GuiRenderUtils.drawShadow(px, py, CE_W, CE_H, 6, 0x60);
        Gui.drawRect(px, py, px + CE_W, py + CE_H, BG_PANEL);
        // Purple top accent (2px) + subtle glow
        Gui.drawRect(px, py, px + CE_W, py + 2, ACCENT_PURPLE);
        GuiRenderUtils.drawGradientRect(px, py + 2, px + CE_W, py + 8, 0x18AA44EE, 0x00000000);
        GuiRenderUtils.drawGradientRect(px, py + 2, px + CE_W, py + 24, BG_HEADER, BG_PANEL);
        Gui.drawRect(px, py + 23, px + CE_W, py + 24, 0x28FFFFFF);
        GuiRenderUtils.drawRectOutline(px, py, CE_W, CE_H, BORDER);
        // Title
        Gui.drawRect(px + 7, py + 9, px + 9, py + 19, ACCENT_PURPLE);
        this.fontRendererObj.drawStringWithShadow("COULEUR", px + 15, py + 9, 0xFFCC88FF);
        int ccx = px + CE_W - 16, ccy = py + 7, ccs = 10;
        drawMiniClose(ccx, ccy, ccs, inRect(mx, my, ccx, ccy, ccs, ccs));
        setHB(hbCeClose, ccx, ccy, ccs, ccs);
        int specX = px + 10, specY = py + 28;
        for (int sx = 0; sx < ceSpecW; sx++) {
            float hue = sx / (float) ceSpecW;
            for (int sy = 0; sy < ceSpecH; sy++) {
                float val = 1.0f - (sy / (float) ceSpecH);
                Gui.drawRect(specX + sx, specY + sy, specX + sx + 1, specY + sy + 1, 0xFF000000 | Color.HSBtoRGB(hue, 1.0f, val));
            }
        }
        GuiRenderUtils.drawRectOutline(specX, specY, ceSpecW, ceSpecH, 0x44FFFFFF);
        // Indicateur de teinte sélectionnée sur le spectre (ligne verticale)
        float selHue = Color.RGBtoHSB(r, g, b, null)[0];
        int hx = specX + (int)(selHue * ceSpecW);
        Gui.drawRect(hx, specY, hx + 1, specY + ceSpecH, 0xBBFFFFFF);

        int sldX = specX + ceSpecW + 15, sldW = CE_W - ceSpecW - 35;
        drawChannelSlider(sldX, specY,      sldW, "R", r, 0xFFEE4444);
        drawChannelSlider(sldX, specY + 22, sldW, "G", g, 0xFF44EE44);
        drawChannelSlider(sldX, specY + 44, sldW, "B", b, 0xFF4444EE);
        drawChannelSlider(sldX, specY + 66, sldW, "A", a, 0xFF888888);
        int pvY = specY + 88;
        GuiRenderUtils.drawCheckerboard(sldX, pvY, sldW, 14, 4, 0xFF888888, 0xFF555555);
        Gui.drawRect(sldX, pvY, sldX + sldW, pvY + 14, (a << 24) | (r << 16) | (g << 8) | b);
        GuiRenderUtils.drawRectOutline(sldX, pvY, sldW, 14, 0x44FFFFFF);
        // Code hex — à l'intérieur du panneau, sous la preview
        String hex = String.format("#%02X%02X%02X", r, g, b);
        int hexW = this.fontRendererObj.getStringWidth(hex);
        this.fontRendererObj.drawString(hex, sldX + (sldW - hexW) / 2, pvY + 17, TEXT_MUTED);
    }

    private void drawChannelSlider(int x, int y, int w, String label, int val, int color) {
        // Label à gauche (lettre R/G/B/A colorée)
        this.fontRendererObj.drawStringWithShadow(label, x - 10, y + 1, color);

        int trackW = colorSliderTrackWidth(w);
        int valueX = x + trackW + 3;

        // Track fond
        Gui.drawRect(x, y + 3, x + trackW, y + 9, 0xFF111111);

        // Fill gradient
        int fill = Math.max(0, (int)(val / 255f * trackW));
        if (fill > 0) {
            GuiRenderUtils.drawGradientRect(x, y + 3, x + fill, y + 9,
                    GuiRenderUtils.colorLerp(0xFF111122, color, 0.25f), color);
        }

        // Curseur blanc
        Gui.drawRect(x + fill - 1, y + 1, x + fill + 2, y + 11, 0xFFEEEEEE);
        Gui.drawRect(x + fill, y + 2, x + fill + 1, y + 10, 0xFFFFFFFF);

        // Valeur numérique dans la zone réservée à droite
        String valStr = String.valueOf(val);
        int vw = this.fontRendererObj.getStringWidth(valStr);
        this.fontRendererObj.drawString(valStr, valueX + Math.max(0, 14 - vw), y + 1, TEXT_MUTED);
    }

    private interface SidebarItem { int getHeight(); void draw(int px, int y, int w, int mx, int my); }
    private List<SidebarItem> buildSidebarItems() {
        List<SidebarItem> items = new ArrayList<>();
        items.add(new HeaderItem("General", ACCENT));
        items.add(new ToggleItem("Active", selected.isEnabled(), null, v -> selected.setEnabled(v)));
        if (selected instanceof BaseWidget) {
            BaseWidget bw = (BaseWidget) selected;
            items.add(new ToggleItem("Mode Rainbow", bw.isRGBMode(), hbRainbow, bw::setRGBMode));
            if (bw instanceof CombatLogWidget) items.add(new ToggleItem("Design circulaire", Boolean.TRUE.equals(bw.getProps().getOrDefault("originalDesign", false)), hbOrigDesign, v -> bw.getProps().put("originalDesign", v)));
            items.add(new ToggleItem("Aligner (Smart)", Boolean.TRUE.equals(bw.getProps().getOrDefault("snapGrid", false)), hbAlignGrid, v -> bw.getProps().put("snapGrid", v)));
            items.add(new ColorItem(bw.getColor()));
            if (bw instanceof KeyStrokeWidget) {
                items.add(new HeaderItem("Keystrokes", ACCENT_PURPLE));
                items.add(new DoubleToggleItem(bw));
            } else if (bw instanceof PotionStatusWidget) {
                items.add(new HeaderItem("Potions", ACCENT_GREEN));
                items.add(new ToggleItem("Afficher duree", Boolean.TRUE.equals(bw.getProps().getOrDefault("showDuration", true)), hbPotionDur, v -> bw.getProps().put("showDuration", v)));
                items.add(new ToggleItem("Afficher icones", Boolean.TRUE.equals(bw.getProps().getOrDefault("showIcons", false)), hbPotionIcons, v -> bw.getProps().put("showIcons", v)));
            } else if (bw instanceof ArmorGroupWidget) {
                items.add(new HeaderItem("Armure", ACCENT_ORANGE));
                items.add(new ToggleItem("Disposition Verticale", "vertical".equals(bw.getProps().getOrDefault("layout", "horizontal")), hbArmorLayout, v -> bw.getProps().put("layout", v ? "vertical" : "horizontal")));
                items.add(new ToggleItem("Afficher en %", Boolean.TRUE.equals(bw.getProps().getOrDefault("displayPercent", true)), hbArmorPercent, v -> bw.getProps().put("displayPercent", v)));
            } else if (bw instanceof CompassWidget) {
                items.add(new HeaderItem("Boussole", ACCENT_GREEN));
                items.add(new ToggleItem("Afficher waypoints", !Boolean.FALSE.equals(bw.getProps().getOrDefault("showWaypoints", Boolean.TRUE)), hbCompassWaypoints, v -> bw.getProps().put("showWaypoints", v)));
            }
            items.add(new HeaderItem("Taille", ACCENT_ORANGE));
            items.add(new ScaleItem(bw));
            items.add(new ButtonItem("Reset Taille", hbResetSize, () -> bw.setScale(1.0f)));
        }
        items.add(new MultiButtonItem());
        return items;
    }

    private class HeaderItem implements SidebarItem {
        String l; int a; HeaderItem(String l, int a) { this.l=l; this.a=a; }
        public int getHeight() { return 16; }
        public void draw(int px, int y, int w, int mx, int my) { Gui.drawRect(px + 8, y + 4, px + 11, y + 11, a); fontRendererObj.drawStringWithShadow(l, px + 15, y + 3, TEXT_PRIMARY); }
    }
    private class ToggleItem implements SidebarItem {
        String l; boolean v; int[] hb; java.util.function.Consumer<Boolean> c;
        ToggleItem(String l, boolean v, int[] hb, java.util.function.Consumer<Boolean> c) { this.l=l; this.v=v; this.hb=hb; this.c=c; }
        public int getHeight() { return 18; }
        public void draw(int px, int y, int w, int mx, int my) {
            boolean hov = inRect(mx, my, px + 4, y, w - 8, 16);
            if (hov) Gui.drawRect(px + 4, y, px + w - 4, y + 16, 0x08FFFFFF);
            fontRendererObj.drawStringWithShadow(l, px + 12, y + 3, hov ? TEXT_PRIMARY : TEXT_SECONDARY);
            drawToggle(px + w - 42, y + 2, v, "sb_" + l);
            if (hb != null) setHB(hb, px + w - 42, y + 2, 28, 12);
        }
    }
    private class DoubleToggleItem implements SidebarItem {
        BaseWidget bw; DoubleToggleItem(BaseWidget bw) { this.bw = bw; }
        private static final int ROW = 14; // lignes compactes pour keystrokes
        public int getHeight() {
            KeyStrokeWidget ks = (KeyStrokeWidget) bw;
            return ((ks.getKeyCount() + 1) / 2) * ROW + ROW; // rangées de touches + rainbow
        }
        public void draw(int px, int y, int w, int mx, int my) {
            KeyStrokeWidget ks = (KeyStrokeWidget) bw;
            int half = w / 2;
            int curY = y;
            for (int i = 0; i < ks.getKeyCount(); i++) {
                int col = i % 2;
                int ox = px + col * half;
                String label = ks.getKeyLabel(i);
                boolean val = Boolean.TRUE.equals(bw.getProps().getOrDefault("showKey" + i, true));
                boolean hov = inRect(mx, my, ox + 4, curY, half - 8, ROW);
                if (hov) Gui.drawRect(ox + 4, curY, ox + half - 4, curY + ROW, 0x08FFFFFF);
                String disp = label;
                if (fontRendererObj.getStringWidth(disp) > half - 45) disp = fontRendererObj.trimStringToWidth(disp, half - 50) + "..";
                fontRendererObj.drawStringWithShadow(disp, ox + 12, curY + 3, hov ? TEXT_PRIMARY : TEXT_SECONDARY);
                drawToggle(ox + half - 42, curY + 1, val, "sb_" + i);
                setHB(hbKeyToggle[i], ox + half - 42, curY + 1, 28, 12);
                if (col == 1) curY += ROW;
            }
            if (ks.getKeyCount() % 2 != 0) curY += ROW;
            boolean spaceVal = Boolean.TRUE.equals(bw.getProps().getOrDefault("showSpaceRainbow", false));
            boolean hovS = inRect(mx, my, px + 4, curY, w - 8, ROW);
            if (hovS) Gui.drawRect(px + 4, curY, px + w - 4, curY + ROW, 0x08FFFFFF);
            fontRendererObj.drawStringWithShadow("Rainbow Espace", px + 12, curY + 3, hovS ? TEXT_PRIMARY : TEXT_SECONDARY);
            drawToggle(px + w - 42, curY + 1, spaceVal, "sb_space");
            setHB(hbSpaceRainbow, px + w - 42, curY + 1, 28, 12);
        }
    }
    private class ColorItem implements SidebarItem {
        int c; ColorItem(int c) { this.c=c; }
        public int getHeight() { return 18; }
        public void draw(int px, int y, int w, int mx, int my) {
            fontRendererObj.drawStringWithShadow("Couleur", px + 12, y + 3, TEXT_SECONDARY);
            int cpX = px + w - 42, cpW = 28, cpH = 12;
            GuiRenderUtils.drawCheckerboard(cpX, y + 1, cpW, cpH, 4, 0xFF999999, 0xFF666666);
            Gui.drawRect(cpX, y + 1, cpX + cpW, y + 1 + cpH, c);
            GuiRenderUtils.drawRectOutline(cpX, y + 1, cpW, cpH, 0x66FFFFFF);
            setHB(hbColorPreview, cpX, y + 1, cpW, cpH);
        }
    }
    private class ScaleItem implements SidebarItem {
        BaseWidget bw; ScaleItem(BaseWidget bw) { this.bw=bw; }
        public int getHeight() { return 38; }
        public void draw(int px, int y, int w, int mx, int my) {
            fontRendererObj.drawStringWithShadow("Echelle: " + String.format("%.2f", bw.getScale()), px + 12, y + 2, TEXT_SECONDARY);
            int sX = px + 12, sW = w - 24, sH = 6;
            Gui.drawRect(sX, y + 14, sX + sW, y + 14 + sH, 0xFF111111);
            int fill = (int)(((bw.getScale() - 0.5f) / 1.5f) * sW);
            GuiRenderUtils.drawGradientRect(sX, y + 14, sX + fill, y + 14 + sH, 0xFFE67E22, 0xFFFFCC88);
            Gui.drawRect(sX + fill - 1, y + 13, sX + fill + 1, y + 15 + sH, 0xFFEEEEEE);
            setHB(hbScaleSlider, sX, y + 14, sW, sH);
            fontRendererObj.drawStringWithShadow(bw.getWidth() + "x" + bw.getHeight(), px + w - 12 - fontRendererObj.getStringWidth(bw.getWidth() + "x" + bw.getHeight()), y + 2, TEXT_MUTED);
        }
    }
    private class ButtonItem implements SidebarItem {
        String l; int[] hb; Runnable r; ButtonItem(String l, int[] hb, Runnable r) { this.l=l; this.hb=hb; this.r=r; }
        public int getHeight() { return 22; }
        public void draw(int px, int y, int w, int mx, int my) {
            boolean hov = inRect(mx, my, px + 10, y + 2, w - 20, 16);
            drawStyledButton(px + 10, y + 2, w - 20, 16, l, 0xFF141010, hov);
            setHB(hb, px + 10, y + 2, w - 20, 16);
        }
    }
    private class MultiButtonItem implements SidebarItem {
        public int getHeight() { return 30; }
        public void draw(int px, int y, int w, int mx, int my) {
            int bw = (w - 26) / 2;
            boolean hR = inRect(mx, my, px + 8, y + 6, bw, 16);
            drawStyledButton(px + 8, y + 6, bw, 16, "Reset Pos.", 0xFF141010, hR);
            setHB(hbResetPos, px + 8, y + 6, bw, 16);
            boolean hW = inRect(mx, my, px + 18 + bw, y + 6, bw, 16);
            drawStyledButton(px + 18 + bw, y + 6, bw, 16, "Blanc", 0xFF141010, hW);
            setHB(hbResetColor, px + 18 + bw, y + 6, bw, 16);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        lastMouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        lastMouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) {
            if (inRect(lastMouseX, lastMouseY, wlX, wlY, wlW + 20, WL_MAX_H)) { wlScroll += (scroll > 0 ? -1 : 1); }
            else if (inRect(lastMouseX, lastMouseY, (int)(sbX - (1.0f - sidebarAnim) * 80), sbY, (selected instanceof KeyStrokeWidget ? 260 : sbW), sbH)) { sbScroll += (scroll > 0 ? -12 : 12); }
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        if (btn != 0) return;
        searchFocused = false;
        for (int i = panelOrder.size() - 1; i >= 0; i--) {
            String p = panelOrder.get(i);
            if ("colorEditor".equals(p) && colorEditorOpen && handleColorClick(mx, my)) return;
            if ("sidebar".equals(p) && selected != null && handleSidebarClick(mx, my)) return;
            if ("widgetList".equals(p) && handleWidgetListClick(mx, my)) return;
        }
        if (inRect(mx, my, hbDoneBtn[0], hbDoneBtn[1], hbDoneBtn[2], hbDoneBtn[3])) { this.mc.displayGuiScreen(parent); return; }
        
        // Determine clicked widget (topmost) first
        UIElement clicked = null;
        List<UIElement> all = new ArrayList<>(ui.all());
        for (int i = all.size() - 1; i >= 0; i--) {
            UIElement e = all.get(i);
            if (!e.isEnabled()) continue;
            if (e.containsPoint(mx, my)) { clicked = e; break; }
        }
        if (clicked != null) {
            // If clicking on a resize handle of this widget, start resizing (select it first)
            int edge = getResizeEdge(mx, my, clicked);
            if (edge != 0) {
                selected = clicked;
                isResizingWidget = true; resizeEdge = edge;
                resizeStartX = mx; resizeStartY = my;
                resizeStartW = selected.getWidth(); resizeStartH = selected.getHeight();
                resizeStartWidgetX = selected.getX(); resizeStartWidgetY = selected.getY();
                if (selected instanceof BaseWidget) resizeStartScale = ((BaseWidget)selected).getScale();
                bringToFront("sidebar"); sbScroll = 0; return;
            }
            // Otherwise select and start dragging
            selected = clicked; isDraggingWidget = true;
            dragOffsetX = mx - selected.getX(); dragOffsetY = my - selected.getY();
            bringToFront("sidebar"); sbScroll = 0; return;
         }
        selected = null;
    }

    private boolean handleSidebarClick(int mx, int my) {
        int px = (int)(sbX - (1.0f - sidebarAnim) * 80);
        int w = (selected instanceof KeyStrokeWidget) ? 260 : sbW;
        if (!inRect(mx, my, px, sbY, w, sbH)) return false;
        bringToFront("sidebar");
        if (inRect(mx, my, hbSbClose[0], hbSbClose[1], hbSbClose[2], hbSbClose[3])) { selected = null; return true; }
        if (inRect(mx, my, px, sbY, w, 22)) { sbDragging = true; sbDragOX = mx; sbDragOY = my; return true; }

        int sY = sbScroll;
        if (inRect(mx, my + sY, px + w - 42, sbY + 44, 28, 12)) { selected.setEnabled(!selected.isEnabled()); ui.saveConfig(); return true; }
        if (selected instanceof BaseWidget) {
            BaseWidget bw = (BaseWidget) selected;
            if (clickHB(mx, my + sY, hbRainbow)) { bw.setRGBMode(!bw.isRGBMode()); ui.saveConfig(); return true; }
            if (bw instanceof CombatLogWidget && clickHB(mx, my + sY, hbOrigDesign)) { bw.getProps().put("originalDesign", !Boolean.TRUE.equals(bw.getProps().getOrDefault("originalDesign", false))); ui.saveConfig(); return true; }
            if (clickHB(mx, my + sY, hbAlignGrid)) { bw.getProps().put("snapGrid", !Boolean.TRUE.equals(bw.getProps().getOrDefault("snapGrid", false))); ui.saveConfig(); return true; }
            if (clickHB(mx, my + sY, hbColorPreview)) { colorEditorOpen = true; bringToFront("colorEditor"); int c = bw.getColor(); a=(c>>24)&0xFF; r=(c>>16)&0xFF; g=(c>>8)&0xFF; b=c&0xFF; return true; }
            if (bw instanceof KeyStrokeWidget) {
                for(int i=0; i<9; i++) if(clickHB(mx, my + sY, hbKeyToggle[i])) { bw.getProps().put("showKey"+i, !Boolean.TRUE.equals(bw.getProps().getOrDefault("showKey"+i, true))); ui.saveConfig(); return true; }
                if(clickHB(mx, my + sY, hbSpaceRainbow)) { bw.getProps().put("showSpaceRainbow", !Boolean.TRUE.equals(bw.getProps().getOrDefault("showSpaceRainbow", false))); ui.saveConfig(); return true; }
            } else if (bw instanceof PotionStatusWidget) {
                if(clickHB(mx, my + sY, hbPotionDur)) { bw.getProps().put("showDuration", !Boolean.TRUE.equals(bw.getProps().getOrDefault("showDuration", true))); ui.saveConfig(); return true; }
                if(clickHB(mx, my + sY, hbPotionIcons)) { bw.getProps().put("showIcons", !Boolean.TRUE.equals(bw.getProps().getOrDefault("showIcons", false))); ui.saveConfig(); return true; }
            } else if (bw instanceof ArmorGroupWidget) {
                if(clickHB(mx, my + sY, hbArmorLayout)) { bw.getProps().put("layout", "vertical".equals(bw.getProps().getOrDefault("layout", "horizontal")) ? "horizontal" : "vertical"); ui.saveConfig(); return true; }
                if(clickHB(mx, my + sY, hbArmorPercent)) { bw.getProps().put("displayPercent", !Boolean.TRUE.equals(bw.getProps().getOrDefault("displayPercent", true))); ui.saveConfig(); return true; }
            } else if (bw instanceof CompassWidget) {
                if(clickHB(mx, my + sY, hbCompassWaypoints)) { bw.getProps().put("showWaypoints", Boolean.FALSE.equals(bw.getProps().getOrDefault("showWaypoints", Boolean.TRUE))); ui.saveConfig(); return true; }
            }
            if(clickHB(mx, my + sY, hbScaleSlider)) { draggingSlider=100; updateScaleFromSlider(mx); return true; }
            if(clickHB(mx, my + sY, hbResetSize)) { bw.setScale(1.0f); ui.saveConfig(); return true; }
        }
        if(clickHB(mx, my + sY, hbResetPos)) { selected.setPosition(10, 10); ui.saveConfig(); return true; }
        if(clickHB(mx, my + sY, hbResetColor)) { selected.setColor(0xFFFFFFFF); if(selected instanceof BaseWidget) ((BaseWidget)selected).setRGBMode(false); ui.saveConfig(); return true; }
        return true;
    }

    private boolean clickHB(int mx, int my, int[] hb) { return mx >= hb[0] && mx < hb[0] + hb[2] && my >= hb[1] && my < hb[1] + hb[3]; }

    private boolean handleWidgetListClick(int mx, int my) {
        int totalW = wlW + 20; List<UIElement> items = getFilteredWidgets();
        int maxH = Math.min(WL_MAX_H, this.height - 80), maxVisible = (maxH - 50) / WL_ROW_H, listH = Math.min(maxVisible, Math.max(1, items.size())) * WL_ROW_H, h = 50 + listH;
        if (!inRect(mx, my, wlX, wlY, totalW, h)) return false;
        bringToFront("widgetList");
        if (inRect(mx, my, hbWlClose[0], hbWlClose[1], hbWlClose[2], hbWlClose[3])) { wlX = -500; return true; }
        if (inRect(mx, my, wlX, wlY, totalW, 22)) { wlDragging = true; wlDragOX = mx; wlDragOY = my; return true; }
        if (inRect(mx, my, wlX + 8, wlY + 26, totalW - 16, 16)) { searchFocused = true; return true; }
        int y = wlY + 46;
        for (int i = wlScroll; i < Math.min(wlScroll + maxVisible, items.size()); i++) {
            UIElement e = items.get(i);
            if (inRect(mx, my, wlX + totalW - 40, y + 5, 28, 12)) { e.setEnabled(!e.isEnabled()); ui.saveConfig(); return true; }
            if (inRect(mx, my, wlX, y, totalW - 6, WL_ROW_H)) { selected = e; bringToFront("sidebar"); sbScroll = 0; return true; }
            y += WL_ROW_H;
        }
        return true;
    }

    private void updateWlScrollFromMouse(int my) {
        List<UIElement> items = getFilteredWidgets();
        int maxH = Math.min(WL_MAX_H, this.height - 80), maxVisible = (maxH - 50) / WL_ROW_H, listH = Math.min(maxVisible, Math.max(1, items.size())) * WL_ROW_H;
        float ratio = (float)(my - (wlY + 46)) / (float)listH;
        wlScroll = GuiRenderUtils.clamp((int)(ratio * items.size() - maxVisible / 2.0f), 0, Math.max(0, items.size() - maxVisible));
    }

    @Override
    protected void mouseClickMove(int mx, int my, int btn, long time) {
        if (isDraggingWidget && selected != null) {
            int nx = mx - dragOffsetX, ny = my - dragOffsetY;
            snapLineX = -1; snapLineY = -1;
            guideXs.clear(); guideYs.clear();
            // Smart alignment: snap to other widgets' left/center/right and top/center/bottom
            if (selected instanceof BaseWidget && Boolean.TRUE.equals(((BaseWidget) selected).getPropOrDefault("snapGrid", false)) && !isShiftKeyDown()) {
                int threshold = 6;
                int sw = this.width, sh = this.height;
                int w = selected.getWidth(), h = selected.getHeight();
                // candidate positions of the moving widget
                int candLeft = nx;
                int candCenterX = nx + w/2;
                int candRight = nx + w;
                int candTop = ny;
                int candCenterY = ny + h/2;
                int candBottom = ny + h;

                // Iterate other widgets
                for (UIElement other : ui.all()) {
                    if (other == selected || !other.isEnabled()) continue;
                    int ox = other.getX(), oy = other.getY(), ow = other.getWidth(), oh = other.getHeight();
                    int oLeft = ox, oCenterX = ox + ow/2, oRight = ox + ow;
                    int oTop = oy, oCenterY = oy + oh/2, oBottom = oy + oh;

                    // collect guides for visual assistance
                    if (!guideXs.contains(oLeft)) guideXs.add(oLeft);
                    if (!guideXs.contains(oCenterX)) guideXs.add(oCenterX);
                    if (!guideXs.contains(oRight)) guideXs.add(oRight);
                    if (!guideYs.contains(oTop)) guideYs.add(oTop);
                    if (!guideYs.contains(oCenterY)) guideYs.add(oCenterY);
                    if (!guideYs.contains(oBottom)) guideYs.add(oBottom);

                    // horizontal snaps
                    if (Math.abs(candLeft - oLeft) <= threshold) { nx = oLeft; snapLineX = oLeft; }
                    else if (Math.abs(candLeft - oCenterX) <= threshold) { nx = oCenterX; snapLineX = oCenterX; }
                    else if (Math.abs(candLeft - oRight) <= threshold) { nx = oRight; snapLineX = oRight; }

                    if (Math.abs(candCenterX - oLeft) <= threshold) { nx = oLeft - w/2; snapLineX = oLeft; }
                    else if (Math.abs(candCenterX - oCenterX) <= threshold) { nx = oCenterX - w/2; snapLineX = oCenterX; }
                    else if (Math.abs(candCenterX - oRight) <= threshold) { nx = oRight - w/2; snapLineX = oRight; }

                    if (Math.abs(candRight - oLeft) <= threshold) { nx = oLeft - w; snapLineX = oLeft; }
                    else if (Math.abs(candRight - oCenterX) <= threshold) { nx = oCenterX - w; snapLineX = oCenterX; }
                    else if (Math.abs(candRight - oRight) <= threshold) { nx = oRight - w; snapLineX = oRight; }

                    // vertical snaps
                    if (Math.abs(candTop - oTop) <= threshold) { ny = oTop; snapLineY = oTop; }
                    else if (Math.abs(candTop - oCenterY) <= threshold) { ny = oCenterY; snapLineY = oCenterY; }
                    else if (Math.abs(candTop - oBottom) <= threshold) { ny = oBottom; snapLineY = oBottom; }

                    if (Math.abs(candCenterY - oTop) <= threshold) { ny = oTop - h/2; snapLineY = oTop; }
                    else if (Math.abs(candCenterY - oCenterY) <= threshold) { ny = oCenterY - h/2; snapLineY = oCenterY; }
                    else if (Math.abs(candCenterY - oBottom) <= threshold) { ny = oBottom - h/2; snapLineY = oBottom; }

                    if (Math.abs(candBottom - oTop) <= threshold) { ny = oTop - h; snapLineY = oTop; }
                    else if (Math.abs(candBottom - oCenterY) <= threshold) { ny = oCenterY - h; snapLineY = oCenterY; }
                    else if (Math.abs(candBottom - oBottom) <= threshold) { ny = oBottom - h; snapLineY = oBottom; }
                }

                // Also keep simple snap to screen edges
                if (Math.abs(nx) <= threshold) { nx = 0; snapLineX = 0; }
                if (Math.abs(nx + w - sw) <= threshold) { nx = sw - w; snapLineX = sw; }
                if (Math.abs(ny) <= threshold) { ny = 0; snapLineY = 0; }
                if (Math.abs(ny + h - sh) <= threshold) { ny = sh - h; snapLineY = sh; }
            }
            selected.setPosition(nx, ny);
        }
        if (isResizingWidget && selected instanceof BaseWidget) {
            BaseWidget bw = (BaseWidget) selected;
            int dx = mx - resizeStartX, dy = my - resizeStartY;
            boolean hor = (resizeEdge & (1 | 2)) != 0;
            boolean ver = (resizeEdge & (4 | 8)) != 0;
            float delta;
            if (hor && ver) delta = (float) Math.max(Math.abs(dx), Math.abs(dy));
            else if (hor) delta = (float) dx;
            else delta = (float) dy;
            float newScale = MathHelper.clamp_float(resizeStartScale + delta / 100.0f, 0.5f, 2.0f);
            bw.setScale(newScale);
            // If resizing from left/top, we need to adjust position so opposite edge remains anchored
            if ((resizeEdge & 1) != 0) { // left
                int newW = bw.getWidth();
                int diff = newW - resizeStartW;
                bw.setPosition(resizeStartWidgetX - diff, bw.getY());
            }
            if ((resizeEdge & 4) != 0) { // top
                int newH = bw.getHeight();
                int diff = newH - resizeStartH;
                bw.setPosition(bw.getX(), resizeStartWidgetY - diff);
            }
        }
        if (wlDragging) { wlX += mx - wlDragOX; wlY += my - wlDragOY; wlDragOX = mx; wlDragOY = my; }
        if (sbDragging) { sbX += mx - sbDragOX; sbY += my - sbDragOY; sbDragOX = mx; sbDragOY = my; }
        if (ceDragging) {
            ceX += mx - ceDragOX;
            ceY += my - ceDragOY;
            ceDragOX = mx;
            ceDragOY = my;
            clampColorEditorPosition();
        }
        if (draggingSpectrum) updateColorFromSpectrum(mx, my);
        if (draggingSlider != -1) { if (draggingSlider == 100) updateScaleFromSlider(mx); else updateColorFromSlider(mx); }
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        isDraggingWidget = false; isResizingWidget = false; wlDragging = false; sbDragging = false; ceDragging = false; draggingSpectrum = false; draggingSlider = -1;
        snapLineX = -1; snapLineY = -1; ui.saveConfig(); super.mouseReleased(mx, my, state);
    }

    private void updateScaleFromSlider(int mx) {
        if (!(selected instanceof BaseWidget)) return;
        int sx = hbScaleSlider[0], sw = hbScaleSlider[2];
        ((BaseWidget)selected).setScale(0.5f + ((float)GuiRenderUtils.clamp(mx - sx, 0, sw) / sw) * 1.5f);
    }

    private boolean handleColorClick(int mx, int my) {
        if (!inRect(mx, my, ceX, ceY, CE_W, CE_H)) return false;
        bringToFront("colorEditor");
        if (inRect(mx, my, hbCeClose[0], hbCeClose[1], hbCeClose[2], hbCeClose[3])) { colorEditorOpen = false; return true; }
        if (inRect(mx, my, ceX, ceY, CE_W, 22)) { ceDragging = true; ceDragOX = mx; ceDragOY = my; return true; }
        int specX = ceX + 10, specY = ceY + 28;
        if (inRect(mx, my, specX, specY, ceSpecW, ceSpecH)) { draggingSpectrum = true; updateColorFromSpectrum(mx, my); return true; }
        int sldX = specX + ceSpecW + 15, sldW = CE_W - ceSpecW - 35;
        for (int i = 0; i < 4; i++) if (inRect(mx, my, sldX, specY + i * 22, sldW, 12)) { draggingSlider = i; updateColorFromSlider(mx); return true; }
        return true;
    }

    private void updateColorFromSpectrum(int mx, int my) {
        float hue = GuiRenderUtils.clamp(mx - (ceX + 10), 0, ceSpecW) / (float) ceSpecW;
        float val = 1.0f - GuiRenderUtils.clamp(my - (ceY + 28), 0, ceSpecH) / (float) ceSpecH;
        int rgb = Color.HSBtoRGB(hue, 1.0f, val);
        r = (rgb >> 16) & 0xFF; g = (rgb >> 8) & 0xFF; b = rgb & 0xFF; applyColor();
    }
    private void updateColorFromSlider(int mx) {
        int sldX = ceX + 10 + ceSpecW + 15;
        int sldW = CE_W - ceSpecW - 35;
        int trackW = colorSliderTrackWidth(sldW);
        int val = (int)((float)GuiRenderUtils.clamp(mx - sldX, 0, trackW) / trackW * 255);
        if (draggingSlider == 0) r = val; else if (draggingSlider == 1) g = val; else if (draggingSlider == 2) b = val; else if (draggingSlider == 3) a = val;
        applyColor();
    }

    private int colorSliderTrackWidth(int totalSliderWidth) {
        // Reserve ~17px a droite pour la valeur numerique (0-255).
        return Math.max(24, totalSliderWidth - 17);
    }

    private void clampColorEditorPosition() {
        ceX = GuiRenderUtils.clamp(ceX, 2, Math.max(2, this.width - CE_W - 2));
        ceY = GuiRenderUtils.clamp(ceY, 2, Math.max(2, this.height - CE_H - 2));
    }

    private void applyColor() {
        if (selected instanceof BaseWidget) {
            ((BaseWidget) selected).setColor((a << 24) | (r << 16) | (g << 8) | b);
            ui.saveConfig();
        }
    }

    private List<UIElement> getFilteredWidgets() { return ui.all().stream().filter(e -> searchFilter.isEmpty() || friendlyName(e.getId()).toLowerCase().contains(searchFilter.toLowerCase())).collect(Collectors.toList()); }
    private void bringToFront(String name) { panelOrder.remove(name); panelOrder.add(name); }
    private boolean inRect(int mx, int my, int rx, int ry, int rw, int rh) { return mx >= rx && my >= ry && mx < rx + rw && my < ry + rh; }
    private void setHB(int[] hb, int x, int y, int w, int h) { hb[0] = x; hb[1] = y; hb[2] = w; hb[3] = h; }
    private String friendlyName(String id) {
        switch (id) {
            case "fps": return "FPS"; case "ping": return "Ping"; case "biome": return "Biome"; case "coords": return "Coordonnees";
            case "dir": return "Direction"; case "date": return "Date"; case "helditem": return "Objet tenu";
            case "armor_group": return "Armure"; case "potions": return "Potions"; case "cps": return "CPS";
            case "toggle_sneak": return "Toggle Sneak"; case "toggle_sprint": return "Toggle Sprint";
            case "combatlog": return "Combat Tag"; case "keystrokes": return "Keystrokes"; case "Keystrokes": return "Keystrokes";
            case "reach": return "Reach Display"; case "Reach": return "Reach Display";
            case "compass": return "Boussole HUD";
            default: String s = id.replace('_', ' '); return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }
    private void drawGrid(int step, int color) { for (int gx = 0; gx < this.width; gx += step) Gui.drawRect(gx, 0, gx + 1, this.height, color); for (int gy = 0; gy < this.height; gy += step) Gui.drawRect(0, gy, this.width, gy + 1, color); }
    private void drawResizeHandles(UIElement e) {
        int wx = e.getX(), wy = e.getY(), ww = e.getWidth(), wh = e.getHeight(), hs = 6;
        // Only draw corner handles to reduce accidental resize
        int[][] corners = { {wx-hs/2, wy-hs/2}, {wx+ww-hs/2, wy-hs/2}, {wx+ww-hs/2, wy+wh-hs/2}, {wx-hs/2, wy+wh-hs/2} };
        for (int[] hpos : corners) { Gui.drawRect(hpos[0], hpos[1], hpos[0] + hs, hpos[1] + hs, 0xFFFFFFFF); GuiRenderUtils.drawRectOutline(hpos[0], hpos[1], hs, hs, ACCENT); }
    }
    private int getResizeEdge(int mx, int my, UIElement e) {
        // Precise detection: only treat as resize when clicking on one of the 8 small handles
        int wx = e.getX(), wy = e.getY(), ww = e.getWidth(), wh = e.getHeight();
        int s = RESIZE_HANDLE_SIZE;
        // handle positions: TL, TC, TR, RT, BR, BC, BL, LT (clockwise)
        int[][] handles = new int[][]{
            {wx - s/2, wy - s/2}, {wx + ww/2 - s/2, wy - s/2}, {wx + ww - s/2, wy - s/2},
            {wx + ww - s/2, wy + wh/2 - s/2}, {wx + ww - s/2, wy + wh - s/2}, {wx + ww/2 - s/2, wy + wh - s/2},
            {wx - s/2, wy + wh - s/2}, {wx - s/2, wy + wh/2 - s/2}
        };
        int edge = 0;
        for (int i = 0; i < handles.length; i++) {
            int hx = handles[i][0], hy = handles[i][1];
            // Only consider corner handles (0,2,4,6) to avoid accidental resizes on edges
            if (i != 0 && i != 2 && i != 4 && i != 6) continue;
            if (mx >= hx && mx <= hx + s && my >= hy && my <= hy + s) {
                switch (i) {
                    case 0: edge |= 1 | 4; break; // top-left
                    case 2: edge |= 2 | 4; break; // top-right
                    case 4: edge |= 2 | 8; break; // bottom-right
                    case 6: edge |= 1 | 8; break; // bottom-left
                    case 7: edge |= 1; break;     // left-center (handles array index 7 won't be hit because we limited to corners above)
                }
                break;
            }
        }
        return edge;
    }
    private void drawStyledButton(int x, int y, int w, int h, String text, int bgColor, boolean hovered) {
        // Outline-style secondary button
        int bg = hovered ? 0x1EFFFFFF : 0x0A000000;
        Gui.drawRect(x, y, x + w, y + h, bg);
        GuiRenderUtils.drawRectOutline(x, y, w, h, hovered ? 0x44FFFFFF : 0x28FFFFFF);
        if (hovered) {
            Gui.drawRect(x, y + 1, x + 2, y + h - 1, ACCENT);
            GuiRenderUtils.drawGradientRect(x + 1, y + 1, x + w - 1, y + 3, 0x16FFFFFF, 0x00000000);
        }
        fontRendererObj.drawStringWithShadow(text, x + (w - fontRendererObj.getStringWidth(text)) / 2.0f, y + (h - 8) / 2.0f,
                hovered ? TEXT_PRIMARY : TEXT_SECONDARY);
    }
    private void drawMiniClose(int x, int y, int size, boolean hovered) {
        // Red fill close button with clear X
        int bg = hovered ? 0xDDEE2222 : 0x44661111;
        Gui.drawRect(x, y, x + size, y + size, bg);
        GuiRenderUtils.drawRectOutline(x, y, size, size, hovered ? 0x66FFFFFF : 0x22FFFFFF);
        if (hovered) GuiRenderUtils.drawGradientRect(x + 1, y + 1, x + size - 1, y + 3, 0x28FFFFFF, 0x00FFFFFF);
        // Bigger, more readable X char
        fontRendererObj.drawString("x", x + 2, y + 1, hovered ? 0xFFFFFFFF : 0xAABBBBBB);
    }
    private void drawToggle(int x, int y, boolean value, String id) {
        float target = value ? 1.0f : 0.0f;
        float current = toggleAnimMap.getOrDefault(id, target);
        current = GuiRenderUtils.lerp(current, target, 0.18f);
        toggleAnimMap.put(id, current);
        GuiRenderUtils.drawSmoothToggle(x, y, value, current);
    }
    @Override protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (searchFocused) { if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) searchFocused = false; else if (keyCode == Keyboard.KEY_BACK) { if (!searchFilter.isEmpty()) searchFilter = searchFilter.substring(0, searchFilter.length() - 1); } else if (typedChar >= 32) searchFilter += typedChar; return; }
        super.keyTyped(typedChar, keyCode);
    }
}
