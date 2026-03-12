package net.minecraft.client.gui;

import net.minecraft.client.gui.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.renderer.GlStateManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiUIEditor extends GuiScreen {
    private final GuiScreen parent;
    private final UIManager ui = UIManager.getInstance();
    private UIElement selected = null;
    private int dragOffsetX, dragOffsetY;
    private final String preselectId;

    private boolean colorEditorOpen = false;
    private int r = 255, g = 255, b = 255, a = 255;

    private int widgetListX = Integer.MIN_VALUE;
    private int widgetListY = 28;
    private int widgetListW_dyn = 140; // proportional width, recalculated in initGui
    private final int widgetListHmax = 200;
    private boolean widgetListDragging = false;
    private int widgetListDragOffsetX = 0, widgetListDragOffsetY = 0;
    private int widgetListScroll = 0; // scroll offset pour la liste
    private final int[] hbWidgetListClose = new int[4];
    private final List<String> panelOrder = new ArrayList<String>() {{
        add("widgetList");
        add("sidebar");
        add("colorEditor");
    }};

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
        this.buttonList.add(new GuiButton(200, Math.max(4, this.width / 2 - 100), Math.max(4, this.height - 28), Math.min(200, this.width - 8), 20, I18n.format("gui.done")));
        UIManager.getInstance().setEditorActive(true);
        UIElement pot = ui.get("potions");
        if (pot instanceof BaseWidget) {
            ((BaseWidget) pot).setProp("editorPreview", Boolean.TRUE);
            ((BaseWidget) pot).setProp("previewEffect", "speed");
        }
        UIElement arm = ui.get("armor_group");
        if (arm instanceof BaseWidget) ((BaseWidget) arm).setProp("editorPreview", Boolean.TRUE);
        UIElement cps = ui.get("cps");
        if (cps instanceof BaseWidget) {
            ((BaseWidget) cps).setProp("showBackground", Boolean.FALSE);
            ((BaseWidget) cps).setProp("background", Boolean.FALSE);
        }

        if (this.preselectId != null) {
            UIElement e = ui.get(this.preselectId);
            if (e != null) {
                this.selected = e;
                if (selected instanceof net.minecraft.client.gui.ui.PotionStatusWidget) {
                    ((BaseWidget) selected).setProp("editorPreview", Boolean.TRUE);
                    ((BaseWidget) selected).setProp("previewEffect", "speed");
                }
                if (selected instanceof net.minecraft.client.gui.ui.ArmorGroupWidget)
                    ((BaseWidget) selected).setProp("editorPreview", Boolean.TRUE);
                if (selected instanceof net.minecraft.client.gui.ui.HeldItemDurabilityWidget)
                    ((BaseWidget) selected).setProp("editorPreview", Boolean.TRUE);
                bringToFront("sidebar");
            }
        }
        if (this.width < 400) {
            widgetListW_dyn = Math.max(100, this.width - 12);
            widgetListX = 4;
            colorEditorX = 4;
            sidebarW = Math.max(160, this.width - 8);
        } else if (this.width < 600) {
            widgetListW_dyn = Math.max(120, this.width / 4);
            widgetListX = this.width - widgetListW_dyn - 8;
            colorEditorX = 4;
            sidebarW = GuiRenderUtils.clamp(this.width / 3, 180, 260);
        } else {
            widgetListW_dyn = GuiRenderUtils.clamp(this.width / 5, 140, 180);
            widgetListX = this.width - widgetListW_dyn - 8;
            colorEditorX = Math.max(4, this.width - GuiRenderUtils.clamp(this.width / 3, 280, 340));
            sidebarW = GuiRenderUtils.clamp(this.width / 4, 220, 280);
        }
        widgetListY = 28;
        colorEditorY = 28;
        sidebarX = Math.max(4, 20);
        sidebarY = Math.max(28, 40);
        // Force all widgets to update proportional positions on resize
        for (UIElement el : ui.all()) {
            if (el instanceof BaseWidget) {
                ((BaseWidget) el).updateAbsolutePosition();
            }
        }
    }

    @Override
    public void onGuiClosed() {
        UIManager.getInstance().setEditorActive(false);
        UIElement pot = ui.get("potions");
        if (pot instanceof BaseWidget) {
            ((BaseWidget) pot).setProp("editorPreview", Boolean.FALSE);
            ((BaseWidget) pot).setProp("previewEffect", null);
        }
        UIElement arm = ui.get("armor_group");
        if (arm instanceof BaseWidget) ((BaseWidget) arm).setProp("editorPreview", Boolean.FALSE);
        ui.saveConfig();
        super.onGuiClosed();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 200) {
            UIElement pot = ui.get("potions");
            if (pot instanceof BaseWidget) {
                ((BaseWidget) pot).setProp("editorPreview", Boolean.FALSE);
                ((BaseWidget) pot).setProp("previewEffect", null);
            }
            UIElement arm = ui.get("armor_group");
            if (arm instanceof BaseWidget) ((BaseWidget) arm).setProp("editorPreview", Boolean.FALSE);
            UIManager.getInstance().setEditorActive(false);
            ui.saveConfig();
            this.mc.displayGuiScreen(parent);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        drawGrid(8, 0x0F000000);

        // Badge discret en haut au centre (amélioré avec shadow)
        int badgeW = Math.min(220, this.width - 20), badgeH = 16;
        int badgeX = (this.width - badgeW) / 2, badgeY = 3;
        GuiRenderUtils.drawShadow(badgeX, badgeY, badgeW, badgeH, 3, 0x40);
        drawRect(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, 0xCC0D0D1A);
        drawRect(badgeX, badgeY + badgeH, badgeX + badgeW, badgeY + badgeH + 1, 0xAA2A7FFF);
        drawRect(badgeX, badgeY, badgeX + badgeW, badgeY + 1, 0x442A7FFF);
        this.fontRendererObj.drawStringWithShadow(
                "\u2726 UI Editor ",
                badgeX + 5, badgeY + 4, 0xAA8EC8FF);

        // ensure editor preview is active
        UIElement pot = ui.get("potions");
        if (pot instanceof BaseWidget) ((BaseWidget) pot).setProp("editorPreview", Boolean.TRUE);
        UIElement arm = ui.get("armor_group");
        if (arm instanceof BaseWidget) ((BaseWidget) arm).setProp("editorPreview", Boolean.TRUE);

        // Render widgets + halo de sélection
        for (UIElement e : ui.all()) {
            e.render(mouseX, mouseY, partialTicks);
        }

        // Panneaux dans l'ordre Z (panelOrder : dernier = dessus)
        for (String panel : panelOrder) {
            if ("widgetList".equals(panel)) drawWidgetList();
            else if ("sidebar".equals(panel) && selected != null) drawWidgetSidebar();
            else if ("colorEditor".equals(panel) && colorEditorOpen && selected instanceof BaseWidget) drawColorEditor();
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawWidgetList() {
        if (widgetListX == Integer.MIN_VALUE && this.width > 0) widgetListX = Math.max(4, this.width - widgetListW_dyn - 8);
        int px = widgetListX, py = widgetListY;
        int rowH = 20;
        // Widgets list
        java.util.List<UIElement> widgetItems = new java.util.ArrayList<>();
        for (UIElement e : ui.all()) {
            widgetItems.add(e);
        }
        int listCount = widgetItems.size();
        int w = Math.min(widgetListW_dyn + 22, this.width - 8);
        // Hauteur max adaptée à l'écran
        int maxH = Math.min(widgetListHmax, this.height - 70);
        int maxVisible = Math.max(1, (maxH - 24) / rowH);
        int h = 24 + Math.min(maxVisible, listCount) * rowH;
        // Scroll
        if (widgetListScroll < 0) widgetListScroll = 0;
        if (widgetListScroll > Math.max(0, listCount - maxVisible)) widgetListScroll = Math.max(0, listCount - maxVisible);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // Panel with shadow + rounded feel
        int ACCENT = 0xFF2A7FFF;
        GuiRenderUtils.drawRoundedPanel(px, py, w, h, 0xF0101018, 0xFF151528, 22, ACCENT);

        // Titre
        this.fontRendererObj.drawStringWithShadow("\u2726 Widgets", px + 10, py + 7, 0xFF8EC8FF);

        // Widget count badge
        String countBadge = String.valueOf(listCount);
        int cbw = this.fontRendererObj.getStringWidth(countBadge) + 6;
        drawRect(px + w - 38 - cbw, py + 6, px + w - 38, py + 16, 0x44FFFFFF);
        this.fontRendererObj.drawString(countBadge, px + w - 38 - cbw + 3, py + 7, 0xFF88AACC);

        // Bouton X (styled)
        int ccx = px + w - 18, ccy = py + 5, ccs = 13;
        boolean closeHover = false; // Would need mouse pos, simplified
        GuiRenderUtils.drawCloseButton(ccx, ccy, ccs, closeHover);
        this.fontRendererObj.drawStringWithShadow("\u2715", ccx + 3, ccy + 2, 0xFFFFFFFF);
        hbWidgetListClose[0] = ccx; hbWidgetListClose[1] = ccy; hbWidgetListClose[2] = ccs; hbWidgetListClose[3] = ccs;

        // Scroll indicator (top)
        if (widgetListScroll > 0) {
            GuiRenderUtils.drawGradientRect(px + 2, py + 22, px + w - 2, py + 26, 0x442A7FFF, 0x00000000);
            this.fontRendererObj.drawString("\u25B2", px + w / 2 - 3, py + 23, 0xFF5588CC);
        }

        GlStateManager.disableBlend();

        int y = py + 24;
        int end = Math.min(widgetListScroll + maxVisible, listCount);
        for (int i = widgetListScroll; i < end; i++) {
            UIElement e = widgetItems.get(i);
            String id = e.getId();
            boolean on = e.isEnabled();
            boolean isSel = selected != null && id.equals(selected.getId());

            // Row background
            if (isSel) {
                drawRect(px + 2, y, px + w - 2, y + rowH, 0x44001A66);
                drawRect(px + 2, y, px + 3, y + rowH, ACCENT); // left accent stripe on selected
            } else if (i % 2 == 0) {
                drawRect(px + 2, y, px + w - 2, y + rowH, 0x08FFFFFF);
            }

            // Bottom separator
            drawRect(px + 8, y + rowH - 1, px + w - 8, y + rowH, 0x11FFFFFF);

            // Status dot with glow effect
            int dotColor = on ? 0xFF44EE77 : 0xFF664444;
            if (on) {
                drawRect(px + 5, y + 7, px + 11, y + 13, 0x2044EE77); // glow
            }
            drawRect(px + 6, y + 8, px + 10, y + 12, dotColor);

            // Name
            String name = friendlyName(id);
            int nameCol = isSel ? 0xFFAAD4FF : (on ? 0xFFDDDDDD : 0xFF555555);
            this.fontRendererObj.drawStringWithShadow(name, px + 14, y + 6, nameCol);

            // Toggle with background
            int tgX = px + w - 36, tgY = y + 4;
            drawRect(tgX - 3, tgY - 2, tgX + 31, tgY + 14, 0x33000000);
            drawToggle(tgX, tgY, on);
            y += rowH;
        }

        // Scroll indicator (bottom)
        if (end < listCount) {
            GuiRenderUtils.drawGradientRect(px + 2, py + h - 6, px + w - 2, py + h - 2, 0x00000000, 0x442A7FFF);
            this.fontRendererObj.drawString("\u25BC", px + w / 2 - 3, py + h - 14, 0xFF5588CC);
        }
    }


    // helper to handle clicks on the widget list
    private boolean handleWidgetListClick(int mouseX, int mouseY) {
        int px = widgetListX, py = widgetListY;
        int rowH = 20;
        int w = widgetListW_dyn + 22;
        java.util.List<UIElement> widgetItems = new java.util.ArrayList<>();
        for (UIElement e : ui.all()) {
            widgetItems.add(e);
        }
        int listCount = widgetItems.size();
        int maxH = Math.min(widgetListHmax, this.height - 70);
        int maxVisible = Math.max(1, (maxH - 24) / rowH);
        int h = 24 + Math.min(maxVisible, listCount) * rowH;
        if (!inRect(mouseX, mouseY, px, py, w, h)) return false;
        bringToFront("widgetList");

        // Bouton fermer
        if (hbWidgetListClose[2] != 0 && inRect(mouseX, mouseY, hbWidgetListClose[0], hbWidgetListClose[1], hbWidgetListClose[2], hbWidgetListClose[3])) {
            widgetListX = this.width + 20;
            return true;
        }
        // Drag header
        if (inRect(mouseX, mouseY, px, py, w, 22)) {
            widgetListDragging = true;
            widgetListDragOffsetX = mouseX;
            widgetListDragOffsetY = mouseY;
            return true;
        }
        int y = py + 24;
        int end = Math.min(widgetListScroll + maxVisible, listCount);
        for (int i = widgetListScroll; i < end; i++) {
            if (y + rowH > py + h) break;
            UIElement e = widgetItems.get(i);
            // Clic sur le toggle (zone droite)
            if (inRect(mouseX, mouseY, px + w - 36, y + 4, 32, 14)) { // Corrigé : y + 4 pour correspondre au rendu
                e.setEnabled(!e.isEnabled());
                ui.saveConfig();
                return true;
            }
            // Clic sur le nom → sélectionner
            if (inRect(mouseX, mouseY, px + 4, y, w - 40, rowH)) {
                selected = e;
                if (selected instanceof net.minecraft.client.gui.ui.PotionStatusWidget) {
                    ((BaseWidget) selected).setProp("editorPreview", Boolean.TRUE);
                    ((BaseWidget) selected).setProp("previewEffect", "speed");
                }
                if (selected instanceof net.minecraft.client.gui.ui.ArmorGroupWidget)
                    ((BaseWidget) selected).setProp("editorPreview", Boolean.TRUE);
                if (selected instanceof net.minecraft.client.gui.ui.HeldItemDurabilityWidget)
                    ((BaseWidget) selected).setProp("editorPreview", Boolean.TRUE);
                UIManager.getInstance().setEditorActive(true);
                bringToFront("sidebar");
                return true;
            }
            y += rowH;
        }
        return true;
    }

    private void drawColorEditor() {
        if (colorEditorX == Integer.MIN_VALUE && this.width > 0) colorEditorX = Math.max(4, this.width - 320);
        int px = colorEditorX, py = colorEditorY;
        // Proportional sizing: scale with screen but enforce min/max
        int w = GuiRenderUtils.clamp(this.width * 2 / 5, 220, 340);
        w = Math.min(w, this.width - 8);
        int specW = GuiRenderUtils.clamp(w - 120, 80, 160);
        int specH = GuiRenderUtils.clamp(this.height / 3, 60, 130);
        int h = specH + 78; // 28 top + specH + margin + buttons
        // Store for click handlers
        ceSpecW = specW; ceSpecH = specH; cePanelW = w; cePanelH = h;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // Panel with shadow + rounded corners
        int COLOR_ACCENT = 0xFF9932CC;
        GuiRenderUtils.drawRoundedPanel(px, py, w, h, 0xEE0D0D1A, 0xFF111130, 22, COLOR_ACCENT);

        GlStateManager.disableBlend();

        this.fontRendererObj.drawStringWithShadow("\u2726 \u00c9diteur de couleur", px + 10, py + 7, 0xFFCC88FF);

        // Bouton fermer (styled)
        int ccx = px + w - 18, ccy = py + 5, ccs = 13;
        GuiRenderUtils.drawCloseButton(ccx, ccy, ccs, false);
        this.fontRendererObj.drawStringWithShadow("\u2715", ccx + 3, ccy + 2, 0xFFFFFFFF);
        hbColorClose[0] = ccx; hbColorClose[1] = ccy; hbColorClose[2] = ccs; hbColorClose[3] = ccs;

        // Spectre couleur (hue x, valeur y)
        int specX = px + 10, specY = py + 28;
        for (int sx = 0; sx < specW; sx++) {
            float hue = sx / (float) (specW - 1);
            for (int sy = 0; sy < specH; sy++) {
                float val = 1.0f - (sy / (float) (specH - 1));
                int rgb = java.awt.Color.HSBtoRGB(hue, 1.0f, val);
                drawRect(specX + sx, specY + sy, specX + sx + 1, specY + sy + 1, 0xFF000000 | (rgb & 0x00FFFFFF));
            }
        }
        // Spectrum border with accent
        drawRect(specX - 1, specY - 1, specX + specW + 1, specY, COLOR_ACCENT);
        drawRect(specX - 1, specY + specH, specX + specW + 1, specY + specH + 1, 0xFF333344);
        drawRect(specX - 1, specY - 1, specX, specY + specH + 1, COLOR_ACCENT);
        drawRect(specX + specW, specY - 1, specX + specW + 1, specY + specH + 1, 0xFF333344);

        // Color indicator on spectrum showing current color
        float[] hsb = java.awt.Color.RGBtoHSB(r, g, b, null);
        int cursorX = specX + (int)(hsb[0] * (specW - 1));
        int cursorY = specY + (int)((1.0f - hsb[2]) * (specH - 1));
        // Draw color cursor on spectrum
        drawRect(cursorX - 4, cursorY, cursorX + 5, cursorY + 1, 0xCCFFFFFF);
        drawRect(cursorX, cursorY - 4, cursorX + 1, cursorY + 5, 0xCCFFFFFF);
        // Outer ring
        drawRect(cursorX - 3, cursorY - 1, cursorX - 2, cursorY + 2, 0x88FFFFFF);
        drawRect(cursorX + 3, cursorY - 1, cursorX + 4, cursorY + 2, 0x88FFFFFF);

        hbColorPreview[0] = specX; hbColorPreview[1] = specY; hbColorPreview[2] = specW; hbColorPreview[3] = specH;

        // Sliders RGBA (improved with handle) — evenly spaced within spectrum height
        int sldX = specX + specW + 10;
        int sldW = w - specW - 28;
        int sldSpacing = Math.max(18, specH / 5);
        ceSldSpacing = sldSpacing;
        drawImprovedChannelSlider(sldX, specY,                "R", 0, sldW);
        drawImprovedChannelSlider(sldX, specY + sldSpacing,   "G", 1, sldW);
        drawImprovedChannelSlider(sldX, specY + sldSpacing*2, "B", 2, sldW);
        drawImprovedChannelSlider(sldX, specY + sldSpacing*3, "A", 3, sldW);

        // Prévisualisation couleur
        int previewCol = (a << 24) | (r << 16) | (g << 8) | b;
        int pvY = specY + sldSpacing * 4 + 4;
        GuiRenderUtils.drawCheckerboard(sldX, pvY, sldW, 16, 4, 0xFF999999, 0xFF666666);
        drawRect(sldX, pvY, sldX + sldW, pvY + 16, previewCol);
        drawRect(sldX, pvY, sldX + sldW, pvY + 1, 0x44FFFFFF);

        // Boutons bas : Blanc + Rainbow
        int btnH = 16;
        int btnY = py + h - btnH - 6;
        int bw = (w - 24) / 2;

        GuiRenderUtils.drawStyledButton(px + 10, btnY, bw, btnH, 0xFF1A1A2A, 0xFF555566, false);
        String resetLabel = "Blanc";
        this.fontRendererObj.drawStringWithShadow(resetLabel,
                px + 10 + (bw - this.fontRendererObj.getStringWidth(resetLabel)) / 2, btnY + 4, 0xFFCCCCCC);
        hbPreviewReset[0] = px + 10; hbPreviewReset[1] = btnY; hbPreviewReset[2] = bw; hbPreviewReset[3] = btnH;

        boolean isRainbow = selected instanceof BaseWidget && ((BaseWidget) selected).isRGBMode();
        int rbBg = isRainbow ? 0xFF1A8A4A : 0xFF1A1A2A;
        int rbBorder = isRainbow ? 0xFF22CC66 : 0xFF555566;
        GuiRenderUtils.drawStyledButton(px + 14 + bw, btnY, bw, btnH, rbBg, rbBorder, isRainbow);
        String rb = isRainbow ? "Rainbow ON" : "Rainbow";
        this.fontRendererObj.drawStringWithShadow(rb,
                px + 14 + bw + (bw - this.fontRendererObj.getStringWidth(rb)) / 2, btnY + 4,
                isRainbow ? 0xFF44FF88 : 0xFFCCCCCC);
        hbColorRainbow[0] = px + 14 + bw; hbColorRainbow[1] = btnY; hbColorRainbow[2] = bw; hbColorRainbow[3] = btnH;
    }

    /** Dessine un slider R/G/B/A amélioré avec handle visible */
    private void drawImprovedChannelSlider(int x, int y, String label, int channel, int width) {
        int val;
        switch (channel) {
            case 0: val = r; break;
            case 1: val = g; break;
            case 2: val = b; break;
            default: val = a; break;
        }
        int labelCol;
        switch (channel) {
            case 0: labelCol = 0xFFFF6666; break;
            case 1: labelCol = 0xFF66FF66; break;
            case 2: labelCol = 0xFF6666FF; break;
            default: labelCol = 0xFFAAAAAA; break;
        }
        // Label
        this.fontRendererObj.drawStringWithShadow(label, x - 12, y + 3, labelCol);
        // Track background
        drawRect(x, y + 3, x + width, y + 13, 0xFF1A1A2A);
        // Fill
        int filled = (int)(val / 255.0f * width);
        int fillCol;
        switch (channel) {
            case 0: fillCol = 0xFF880000; break;
            case 1: fillCol = 0xFF008800; break;
            case 2: fillCol = 0xFF000088; break;
            default: fillCol = 0xFF555555; break;
        }
        drawRect(x, y + 3, x + filled, y + 13, fillCol);
        // Top accent line on fill
        drawRect(x, y + 3, x + filled, y + 4, labelCol);
        // Handle (wider, more visible)
        int hx = x + filled;
        drawRect(hx - 2, y + 1, hx + 2, y + 15, 0xFFEEEEEE);
        drawRect(hx - 1, y + 2, hx + 1, y + 14, labelCol);
        // Value
        String valStr = String.valueOf(val);
        this.fontRendererObj.drawStringWithShadow(valStr, x + width + 4, y + 3, 0xFFCCCCCC);
    }

    // ── Panneau latéral contextuel ─────────────────────────────────────────────
    private void drawWidgetSidebar() {
        if (!(selected instanceof BaseWidget)) return;
        BaseWidget widget = (BaseWidget) selected;
        if (widget.getPropOrDefault("snapAlign", null) == null) widget.setProp("snapAlign", Boolean.TRUE);
        int px = sidebarX, py = sidebarY, w = sidebarW;

        // Hauteur adaptative
        int lines = 3;
        if (widget instanceof net.minecraft.client.gui.ui.KeyStrokeWidget) {
            lines += 2;
            lines += ((net.minecraft.client.gui.ui.KeyStrokeWidget) widget).getKeyCount();
        }
        if (widget instanceof PotionStatusWidget) lines += 3;
        if (widget instanceof ArmorGroupWidget) lines += 3;
        if (widget instanceof ToggleSneakWidget
                || widget instanceof ToggleSprintWidget) lines += 5;
        int h = Math.max(160, 52 + lines * 20);

        // Accent color per widget type
        int accentColor = 0xFF2A7FFF;
        if (widget instanceof KeyStrokeWidget) accentColor = 0xFF8A2BE2;
        if (widget instanceof PotionStatusWidget) accentColor = 0xFF2ECC71;
        if (widget instanceof ArmorGroupWidget) accentColor = 0xFFE67E22;
        if (widget instanceof ToggleSneakWidget
                || widget instanceof ToggleSprintWidget) accentColor = 0xFF00CCAA;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // Panel with rounded corners + shadow
        GuiRenderUtils.drawRoundedPanel(px, py, w, h, 0xEE111118, 0xFF0D0D1A, 24, accentColor);

        GlStateManager.disableBlend();

        // Titre dans le header
        String widgetName = friendlyName(widget.getId());
        this.fontRendererObj.drawStringWithShadow("\u2726 " + widgetName, px + 10, py + 8, accentColor);

        // Bouton fermer (styled)
        int cx = px + w - 18, cy = py + 6, cs = 13;
        GuiRenderUtils.drawCloseButton(cx, cy, cs, false);
        this.fontRendererObj.drawStringWithShadow("\u2715", cx + 3, cy + 2, 0xFFFFFFFF);
        hbSidebarClose[0] = cx;
        hbSidebarClose[1] = cy;
        hbSidebarClose[2] = cs;
        hbSidebarClose[3] = cs;

        int y = py + 30;

        // ── Section : Général ──────────────────────────────────────────────────
        drawSectionHeader(px, y, w, "G\u00e9n\u00e9ral", accentColor);
        y += 16;

        // Actif
        boolean enabled = widget.isEnabled();
        this.fontRendererObj.drawStringWithShadow("Actif", px + 12, y + 2, 0xBBCCCCCC);
        drawToggle(px + w - 40, y, enabled);
        y += 20;

        // Aligner
        boolean snapAlign = Boolean.TRUE.equals(widget.getPropOrDefault("snapAlign", Boolean.TRUE));
        this.fontRendererObj.drawStringWithShadow("Aligner sur grille", px + 12, y + 2, 0xBBCCCCCC);
        drawToggle(px + w - 40, y, snapAlign);
        hbSnapAlign[0] = px + w - 40;
        hbSnapAlign[1] = y;
        hbSnapAlign[2] = 28;
        hbSnapAlign[3] = 12;
        y += 18;

        // Couleur (preview cliquable + bouton Reset)
        int col = widget.getColor();
        int cpX = px + 10, cpY = y, cpW = w - 20, cpH = 16;
        // Checkerboard background for transparency
        GuiRenderUtils.drawCheckerboard(cpX, cpY, cpW, cpH, 4, 0xFF999999, 0xFF666666);
        drawRect(cpX, cpY, cpX + cpW, cpY + cpH, col);
        // Border
        drawRect(cpX, cpY, cpX + cpW, cpY + 1, 0x55FFFFFF);
        drawRect(cpX, cpY, cpX + 1, cpY + cpH, 0x55FFFFFF);
        drawRect(cpX, cpY + cpH - 1, cpX + cpW, cpY + cpH, 0x33000000);
        this.fontRendererObj.drawStringWithShadow("\u25B6 Couleur", cpX + 4, cpY + 4, 0xFFFFFFFF);
        hbColorPreview[0] = cpX;
        hbColorPreview[1] = cpY;
        hbColorPreview[2] = cpW;
        hbColorPreview[3] = cpH;
        y += 18;

        // Réinitialiser position
        GuiRenderUtils.drawStyledButton(px + 10, y - 1, w - 20, 14, 0xFF0D0D1A, 0xFF2A4466, false);
        this.fontRendererObj.drawStringWithShadow("\u21BA  R\u00e9initialiser position", px + 14, y + 2, 0xFFAAD4FF);
        hbResetColor[0] = px + 10;
        hbResetColor[1] = y - 1;
        hbResetColor[2] = w - 20;
        hbResetColor[3] = 13;
        y += 17;

        // ── Section : KeyStroke ────────────────────────────────────────────────
        if (widget instanceof net.minecraft.client.gui.ui.KeyStrokeWidget) {
            net.minecraft.client.gui.ui.KeyStrokeWidget ks = (net.minecraft.client.gui.ui.KeyStrokeWidget) widget;
            y += 6;
            drawSectionHeader(px, y, w, "Keystroke", accentColor);
            y += 16;

            // RGB mode
            boolean rgb = widget.isRGBMode();
            this.fontRendererObj.drawStringWithShadow("Mode arc-en-ciel", px + 12, y + 2, 0xBBCCCCCC);
            drawToggle(px + w - 40, y, rgb);
            y += 20;

            // Touches visibles
            int keyCount = ks.getKeyCount();
            for (int i = 0; i < keyCount; i++) {
                String lbl = ks.getKeyLabel(i);
                boolean vis = Boolean.TRUE.equals(widget.getPropOrDefault("showKey" + i, Boolean.TRUE));
                // fond alterné
                if (i % 2 == 0) drawRect(px + 4, y - 1, px + w - 4, y + 14, 0x08FFFFFF);
                drawToggle(px + 12, y, vis);
                this.fontRendererObj.drawStringWithShadow(lbl, px + 46, y + 2, vis ? 0xFFCCCCCC : 0xFF555555);
                hbKeyToggle[i][0] = px + 12;
                hbKeyToggle[i][1] = y;
                hbKeyToggle[i][2] = 28;
                hbKeyToggle[i][3] = 12;
                y += 17;
            }
        }

        // ── Section : Potions ──────────────────────────────────────────────────
        if (widget instanceof net.minecraft.client.gui.ui.PotionStatusWidget) {
            y += 6;
            drawSectionHeader(px, y, w, "Potions", accentColor);
            y += 16;
            boolean showDur = Boolean.TRUE.equals(widget.getPropOrDefault("showDuration", Boolean.TRUE));
            this.fontRendererObj.drawStringWithShadow("Afficher dur\u00e9e", px + 12, y + 2, 0xBBCCCCCC);
            drawToggle(px + w - 40, y, showDur);
            hbPotionDur[0] = px + w - 40;
            hbPotionDur[1] = y;
            hbPotionDur[2] = 28;
            hbPotionDur[3] = 12;
            y += 20;
            boolean showIcons = Boolean.TRUE.equals(widget.getPropOrDefault("showIcons", Boolean.FALSE));
            this.fontRendererObj.drawStringWithShadow("Afficher ic\u00f4nes", px + 12, y + 2, 0xBBCCCCCC);
            drawToggle(px + w - 40, y, showIcons);
            hbPotionIcons[0] = px + w - 40;
            hbPotionIcons[1] = y;
            hbPotionIcons[2] = 28;
            hbPotionIcons[3] = 12;
            y += 20;
        }

        // ── Section : Armure ──────────────────────────────────────────────────
        if (widget instanceof net.minecraft.client.gui.ui.ArmorGroupWidget) {
            y += 6;
            drawSectionHeader(px, y, w, "Armure", accentColor);
            y += 16;
            String layout = String.valueOf(widget.getPropOrDefault("layout", "horizontal"));
            boolean isVert = "vertical".equals(layout);
            this.fontRendererObj.drawStringWithShadow("Disposition :", px + 12, y + 2, 0xBBCCCCCC);
            String dispLabel = isVert ? "Verticale \u25BC" : "Horizontale \u25B6";
            int dw = this.fontRendererObj.getStringWidth(dispLabel) + 8;
            GuiRenderUtils.drawStyledButton(px + w - dw - 6, y - 1, dw + 2, 13, 0xFF0D0D1A, 0xFF2A4466, false);
            this.fontRendererObj.drawStringWithShadow(dispLabel, px + w - dw - 2, y + 2, 0xFFAAD4FF);
            hbArmorLayout[0] = px + w - dw - 6;
            hbArmorLayout[1] = y - 1;
            hbArmorLayout[2] = dw + 2;
            hbArmorLayout[3] = 12;
            y += 20;
            boolean dispPct = Boolean.TRUE.equals(widget.getPropOrDefault("displayPercent", Boolean.TRUE));
            this.fontRendererObj.drawStringWithShadow("Afficher %", px + 12, y + 2, 0xBBCCCCCC);
            drawToggle(px + w - 40, y, dispPct);
            hbArmorPercent[0] = px + w - 40;
            hbArmorPercent[1] = y;
            hbArmorPercent[2] = 28;
            hbArmorPercent[3] = 12;
            y += 20;
        }

        // ── Section : Toggle Sneak ─────────────────────────────────────────────
        if (widget instanceof net.minecraft.client.gui.ui.ToggleSneakWidget) {
            y += 6;
            drawSectionHeader(px, y, w, "Toggle Sneak", accentColor);
            y += 16;
            net.minecraft.client.settings.GameSettings gs = Minecraft.getMinecraft().gameSettings;
            boolean sneakEnabled = gs.toggleSneakEnabled;
            this.fontRendererObj.drawStringWithShadow("Fonctionnalit\u00e9 :", px + 12, y + 2, 0xBBCCCCCC);
            drawToggle(px + w - 40, y, sneakEnabled);
            hbArmorLayout[0] = px + w - 40; hbArmorLayout[1] = y; hbArmorLayout[2] = 28; hbArmorLayout[3] = 12;
            y += 20;
            this.fontRendererObj.drawStringWithShadow("\u00a78La touche Sneak classique bascule", px + 12, y + 2, 0x77AAAAAA);
            y += 12;
            this.fontRendererObj.drawStringWithShadow("\u00a78l'\u00e9tat accroupi si activ\u00e9.", px + 12, y + 2, 0x77AAAAAA);
            y += 14;
            boolean active = gs.isToggleSneakActive;
            this.fontRendererObj.drawStringWithShadow("\u00c9tat actuel :", px + 12, y + 2, 0xBBCCCCCC);
            this.fontRendererObj.drawStringWithShadow(active ? "\u00a7aCROUCH" : "\u00a7cNormal", px + 94, y + 2, 0xFFFFFFFF);
            y += 18;
        }

        // ── Section : Toggle Sprint ────────────────────────────────────────────
        if (widget instanceof net.minecraft.client.gui.ui.ToggleSprintWidget) {
            y += 6;
            drawSectionHeader(px, y, w, "Toggle Sprint", accentColor);
            y += 16;
            net.minecraft.client.settings.GameSettings gs = Minecraft.getMinecraft().gameSettings;
            boolean sprintEnabled = gs.toggleSprintEnabled;
            this.fontRendererObj.drawStringWithShadow("Fonctionnalit\u00e9 :", px + 12, y + 2, 0xBBCCCCCC);
            drawToggle(px + w - 40, y, sprintEnabled);
            hbArmorPercent[0] = px + w - 40; hbArmorPercent[1] = y; hbArmorPercent[2] = 28; hbArmorPercent[3] = 12;
            y += 20;
            this.fontRendererObj.drawStringWithShadow("\u00a78La touche Sprint classique bascule", px + 12, y + 2, 0x77AAAAAA);
            y += 12;
            this.fontRendererObj.drawStringWithShadow("\u00a78le sprint si activ\u00e9.", px + 12, y + 2, 0x77AAAAAA);
            y += 14;
            boolean active = gs.isToggleSprintActive;
            this.fontRendererObj.drawStringWithShadow("\u00c9tat actuel :", px + 12, y + 2, 0xBBCCCCCC);
            this.fontRendererObj.drawStringWithShadow(active ? "\u00a7aSPRINT" : "\u00a7cNormal", px + 94, y + 2, 0xFFFFFFFF);
            y += 18;
        }


        // Hint en bas
        if (y + 14 < py + h) {
            GuiRenderUtils.drawGradientRect(px + 4, y + 4, px + w - 4, y + 5, 0x00000000, 0x22FFFFFF);
            this.fontRendererObj.drawStringWithShadow("\u00a78Glisse le header pour d\u00e9placer", px + 10, y + 8, 0x44AAAAAA);
        }

        sidebarH = h;
    }

    /**
     * Dessine un en-tête de section avec un carré coloré + trait dégradé
     */
    private void drawSectionHeader(int px, int y, int w, String label, int accent) {
        GuiRenderUtils.drawSectionHeader(this.fontRendererObj, px, y, w, label, accent);
    }

    // Sidebar position and dragging state (allows moving the sidebar)
    private int sidebarX = 20, sidebarY = 40, sidebarW = 260, sidebarH = 220;
    private boolean sidebarDragging = false;
    private int sidebarDragOffsetX = 0, sidebarDragOffsetY = 0;
    private boolean isDraggingWidget = false;
    private int draggingSlider = -1; // 0=R,1=G,2=B,3=A

    private final int[] hbColorPreview = new int[4];
    private final int[] hbResetColor = new int[4];
    private final int[] hbColorRainbow = new int[4];
    private final int[] hbPotionDur = new int[4];
    private final int[] hbPotionIcons = new int[4];
    private final int[] hbArmorLayout = new int[4];
    private final int[] hbArmorPercent = new int[4];
    private final int[] hbSidebarClose = new int[4];
    private final int[] hbColorClose = new int[4];
    private final int[] hbPreviewReset = new int[4];
    private final int[] hbSnapAlign = new int[4];
    // keystroke widget toggles (support up to 9 keys)
    private final int[][] hbKeyToggle = new int[9][4];

    // Color editor movable origin and dragging
    // lazy init similar to widgetListX
    private int colorEditorX = Integer.MIN_VALUE, colorEditorY = 28;
    private boolean colorEditorDragging = false;
    private int colorEditorDragOffsetX = 0, colorEditorDragOffsetY = 0;
    private boolean draggingSpectrum = false;
    // Dynamic color editor dimensions (set in drawColorEditor, used in click handlers)
    private int ceSpecW = 148, ceSpecH = 120, cePanelW = 310, cePanelH = 194, ceSldSpacing = 24;

    // Toggle animation tracking
    private final java.util.Map<String, Float> toggleAnimMap = new java.util.HashMap<>();

    private void drawToggle(int x, int y, boolean value) {
        // Compute a unique key from position for animation tracking
        String key = x + "," + y;
        float target = value ? 1.0f : 0.0f;
        float current = toggleAnimMap.getOrDefault(key, target);
        current = GuiRenderUtils.lerp(current, target, 0.18f);
        if (Math.abs(current - target) < 0.02f) current = target;
        toggleAnimMap.put(key, current);
        GuiRenderUtils.drawSmoothToggle(x, y, value, current);
    }

    // helper pour vérifier si un point est dans un rectangle
    private boolean inRect(int mx, int my, int rx, int ry, int rw, int rh) {
        return mx >= rx && my >= ry && mx <= rx + rw && my <= ry + rh;
    }

    // handle clicks in the sidebar panel (single, consolidated)
    private void handleSidebarClick(int mouseX, int mouseY) {
        if (!(selected instanceof BaseWidget)) return;
        bringToFront("sidebar");
        BaseWidget widget = (BaseWidget) selected;
        int px = sidebarX, py = sidebarY, w = sidebarW;
        if (!inRect(mouseX, mouseY, px, py, w, sidebarH)) return;

        int y = py + 30; // après le header (matches drawWidgetSidebar y = py + 30)

        // Actif toggle (Général section)
        y += 16; // section header height (matches drawSectionHeader + y += 16)
        if (inRect(mouseX, mouseY, px + w - 40, y, 28, 12)) {
            widget.setEnabled(!widget.isEnabled());
            ui.saveConfig();
            return;
        }
        y += 20;

        // Aligner toggle
        if (hbSnapAlign[2] != 0 && inRect(mouseX, mouseY, hbSnapAlign[0], hbSnapAlign[1], hbSnapAlign[2], hbSnapAlign[3])) {
            boolean cur = Boolean.TRUE.equals(widget.getPropOrDefault("snapAlign", Boolean.TRUE));
            widget.setProp("snapAlign", !cur);
            ui.saveConfig();
            return;
        }
        y += 20;

        // Couleur (ouvre l'éditeur)
        if (hbColorPreview[2] != 0 && inRect(mouseX, mouseY, hbColorPreview[0], hbColorPreview[1], hbColorPreview[2], hbColorPreview[3])) {
            int col = widget.getColor();
            a = (col >> 24) & 0xFF;
            if (a == 0) a = 255;
            r = (col >> 16) & 0xFF;
            g = (col >> 8) & 0xFF;
            b = col & 0xFF;
            colorEditorOpen = true;
            bringToFront("colorEditor");
            return;
        }
        y += 18;

        // Réinitialiser position
        if (hbResetColor[2] != 0 && inRect(mouseX, mouseY, hbResetColor[0], hbResetColor[1], hbResetColor[2], hbResetColor[3])) {
            widget.setPosition(10, 10);
            ui.saveConfig();
            return;
        }
        y += 17;

        // ── KeyStroke ──────────────────────────────────────────────────────────
        if (widget instanceof net.minecraft.client.gui.ui.KeyStrokeWidget) {
            net.minecraft.client.gui.ui.KeyStrokeWidget ks = (net.minecraft.client.gui.ui.KeyStrokeWidget) widget;
            y += 6;
            y += 16; // section header
            // RGB toggle
            if (inRect(mouseX, mouseY, px + w - 40, y, 28, 12)) {
                widget.setRGBMode(!widget.isRGBMode());
                ui.saveConfig();
                return;
            }
            y += 20;
            // Touches
            int keyCount = ks.getKeyCount();
            for (int i = 0; i < keyCount; i++) {
                if (hbKeyToggle[i][2] != 0 && inRect(mouseX, mouseY, hbKeyToggle[i][0], hbKeyToggle[i][1], hbKeyToggle[i][2], hbKeyToggle[i][3])) {
                    boolean vis = Boolean.TRUE.equals(widget.getPropOrDefault("showKey" + i, Boolean.TRUE));
                    widget.setProp("showKey" + i, !vis);
                    ui.saveConfig();
                    return;
                }
                y += 17;
            }
        }

        // ── Potions ────────────────────────────────────────────────────────────
        if (widget instanceof net.minecraft.client.gui.ui.PotionStatusWidget) {
            y += 6; // keep y progression consistent with drawWidgetSidebar
            // Duration toggle
            if (hbPotionDur[2] != 0 && inRect(mouseX, mouseY, hbPotionDur[0], hbPotionDur[1], hbPotionDur[2], hbPotionDur[3])) {
                boolean cur = Boolean.TRUE.equals(widget.getPropOrDefault("showDuration", Boolean.TRUE));
                widget.setProp("showDuration", !cur);
                ui.saveConfig();
                return;
            }
            y += 20;
            // Icons toggle
            if (hbPotionIcons[2] != 0 && inRect(mouseX, mouseY, hbPotionIcons[0], hbPotionIcons[1], hbPotionIcons[2], hbPotionIcons[3])) {
                boolean cur = Boolean.TRUE.equals(widget.getPropOrDefault("showIcons", Boolean.FALSE));
                widget.setProp("showIcons", !cur);
                ui.saveConfig();
                return;
            }
        }

        // ── Armure ─────────────────────────────────────────────────────────────
        if (widget instanceof net.minecraft.client.gui.ui.ArmorGroupWidget) {
            y += 6;
            // Layout toggle
            if (hbArmorLayout[2] != 0 && inRect(mouseX, mouseY, hbArmorLayout[0], hbArmorLayout[1], hbArmorLayout[2], hbArmorLayout[3])) {
                String cur = String.valueOf(widget.getPropOrDefault("layout", "horizontal"));
                widget.setProp("layout", "horizontal".equals(cur) ? "vertical" : "horizontal");
                ui.saveConfig();
                return;
            }
            y += 20;
            // Percent toggle
            if (hbArmorPercent[2] != 0 && inRect(mouseX, mouseY, hbArmorPercent[0], hbArmorPercent[1], hbArmorPercent[2], hbArmorPercent[3])) {
                boolean cur = Boolean.TRUE.equals(widget.getPropOrDefault("displayPercent", Boolean.TRUE));
                widget.setProp("displayPercent", !cur);
                ui.saveConfig();
                return;
            }
        }

        // ── Toggle Sneak ───────────────────────────────────────────────────────
        if (widget instanceof net.minecraft.client.gui.ui.ToggleSneakWidget) {
            // hbArmorLayout réutilisé pour toggle fonctionnalité sneak
            if (hbArmorLayout[2] != 0 && inRect(mouseX, mouseY, hbArmorLayout[0], hbArmorLayout[1], hbArmorLayout[2], hbArmorLayout[3])) {
                net.minecraft.client.settings.GameSettings gs = Minecraft.getMinecraft().gameSettings;
                gs.toggleSneakEnabled = !gs.toggleSneakEnabled;
                if (!gs.toggleSneakEnabled) gs.isToggleSneakActive = false;
                gs.saveOptions();
                return;
            }
        }

        // ── Toggle Sprint ──────────────────────────────────────────────────────
        if (widget instanceof net.minecraft.client.gui.ui.ToggleSprintWidget) {
            // hbArmorPercent réutilisé pour toggle fonctionnalité sprint
            if (hbArmorPercent[2] != 0 && inRect(mouseX, mouseY, hbArmorPercent[0], hbArmorPercent[1], hbArmorPercent[2], hbArmorPercent[3])) {
                net.minecraft.client.settings.GameSettings gs = Minecraft.getMinecraft().gameSettings;
                gs.toggleSprintEnabled = !gs.toggleSprintEnabled;
                if (!gs.toggleSprintEnabled) gs.isToggleSprintActive = false;
                gs.saveOptions();
                return;
            }
        }
    }

    private void bringToFront(String name) {
        try {
            panelOrder.remove(name);
        } catch (Throwable ignored) {
        }
        panelOrder.add(name);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        // ── Panneaux flottants par ordre Z (top en premier) ──────────────────
        for (int i = panelOrder.size() - 1; i >= 0; i--) {
            String p = panelOrder.get(i);

            // ── Éditeur de couleur ─────────────────────────────────────────────
            if ("colorEditor".equals(p) && colorEditorOpen && selected instanceof BaseWidget) {
                int px = colorEditorX, py = colorEditorY;
                int panelW = cePanelW, panelH = cePanelH;
                if (!inRect(mouseX, mouseY, px, py, panelW, panelH)) continue;
                bringToFront("colorEditor");
                // Fermer
                if (hbColorClose[2] != 0 && inRect(mouseX, mouseY, hbColorClose[0], hbColorClose[1], hbColorClose[2], hbColorClose[3])) {
                    colorEditorOpen = false; draggingSpectrum = false; draggingSlider = -1; colorEditorDragging = false;
                    return;
                }
                // Drag header
                if (inRect(mouseX, mouseY, px, py, panelW, 22)) {
                    colorEditorDragging = true; colorEditorDragOffsetX = mouseX; colorEditorDragOffsetY = mouseY;
                    return;
                }
                // Spectre — use dynamic stored sizes
                int specX = px + 10, specY = py + 28, specW = ceSpecW, specH = ceSpecH;
                if (inRect(mouseX, mouseY, specX, specY, specW, specH)) {
                    float hue = (mouseX - specX) / (float) Math.max(1, specW - 1);
                    float val = 1.0f - (mouseY - specY) / (float) Math.max(1, specH - 1);
                    int rgb = java.awt.Color.HSBtoRGB(hue, 1.0f, val);
                    r = (rgb >> 16) & 0xFF; g = (rgb >> 8) & 0xFF; b = rgb & 0xFF;
                    applyColorToWidget(); draggingSpectrum = true;
                    return;
                }
                // Sliders RGBA — use dynamic stored positions
                int sldX = specX + specW + 10;
                int slider_w = cePanelW - specW - 28;
                for (int j = 0; j < 4; j++) {
                    int sy = specY + j * ceSldSpacing;
                    if (inRect(mouseX, mouseY, sldX, sy, slider_w, 16)) {
                        int rel = Math.max(0, Math.min(slider_w, mouseX - sldX));
                        int val = (int)(rel / (float) slider_w * 255f);
                        if (j == 0) r = val; else if (j == 1) g = val; else if (j == 2) b = val; else a = val;
                        applyColorToWidget(); draggingSlider = j;
                        return;
                    }
                }
                // Bouton Blanc
                if (hbPreviewReset[2] != 0 && inRect(mouseX, mouseY, hbPreviewReset[0], hbPreviewReset[1], hbPreviewReset[2], hbPreviewReset[3])) {
                    r = 255; g = 255; b = 255; a = 255; applyColorToWidget(); ui.saveConfig();
                    return;
                }
                // Bouton Rainbow
                if (hbColorRainbow[2] != 0 && inRect(mouseX, mouseY, hbColorRainbow[0], hbColorRainbow[1], hbColorRainbow[2], hbColorRainbow[3])) {
                    BaseWidget bw = (BaseWidget) selected;
                    bw.setRGBMode(!bw.isRGBMode()); bw.setProp("rgbMode", bw.isRGBMode()); ui.saveConfig();
                    return;
                }
                return; // absorbé
            }

            // ── Sidebar ────────────────────────────────────────────────────────
            if ("sidebar".equals(p) && selected != null) {
                if (!inRect(mouseX, mouseY, sidebarX, sidebarY, sidebarW, sidebarH)) continue;
                bringToFront("sidebar");
                // Fermer
                if (hbSidebarClose[2] != 0 && inRect(mouseX, mouseY, hbSidebarClose[0], hbSidebarClose[1], hbSidebarClose[2], hbSidebarClose[3])) {
                    if (selected instanceof net.minecraft.client.gui.ui.PotionStatusWidget) {
                        ((BaseWidget) selected).setProp("editorPreview", Boolean.FALSE);
                        ((BaseWidget) selected).setProp("previewEffect", null);
                    }
                    if (selected instanceof net.minecraft.client.gui.ui.ArmorGroupWidget)
                        ((BaseWidget) selected).setProp("editorPreview", Boolean.FALSE);
                    if (selected instanceof net.minecraft.client.gui.ui.HeldItemDurabilityWidget)
                        ((BaseWidget) selected).setProp("editorPreview", Boolean.FALSE);
                    selected = null; colorEditorOpen = false; sidebarDragging = false;
                    return;
                }
                // Drag header
                if (inRect(mouseX, mouseY, sidebarX, sidebarY, sidebarW, 22)) {
                    sidebarDragging = true; sidebarDragOffsetX = mouseX; sidebarDragOffsetY = mouseY;
                    return;
                }
                handleSidebarClick(mouseX, mouseY);
                return;
            }

            // ── Widget List ────────────────────────────────────────────────────
            if ("widgetList".equals(p)) {
                int wlW = widgetListW_dyn + 22;
                java.util.List<UIElement> wlItems = new java.util.ArrayList<>();
                for (UIElement e : ui.all()) { wlItems.add(e); }
                int maxH = Math.min(widgetListHmax, this.height - 70);
                int maxVisible = Math.max(1, (maxH - 22) / 18);
                int wlH = 22 + Math.min(maxVisible, wlItems.size()) * 18;
                if (!inRect(mouseX, mouseY, widgetListX, widgetListY, wlW, wlH)) continue;
                if (handleWidgetListClick(mouseX, mouseY)) return;
            }
        }

        // ── Clic sur un widget dans la scène (drag) ──────────────────────────
        // On vérifie d'abord qu'on n'est pas sur un panneau
        boolean onPanel = false;
        if (colorEditorOpen && selected instanceof BaseWidget) {
            if (inRect(mouseX, mouseY, colorEditorX, colorEditorY, cePanelW, cePanelH)) onPanel = true;
        }
        if (!onPanel && selected != null && inRect(mouseX, mouseY, sidebarX, sidebarY, sidebarW, sidebarH)) onPanel = true;
        if (!onPanel) {
            int wlW = widgetListW_dyn + 22;
            java.util.List<UIElement> wlItems2 = new java.util.ArrayList<>();
            for (UIElement e : ui.all()) { wlItems2.add(e); }
            int maxH2 = Math.min(widgetListHmax, this.height - 70);
            int maxVisible2 = Math.max(1, (maxH2 - 22) / 18);
            int wlH2 = 22 + Math.min(maxVisible2, wlItems2.size()) * 18;
            if (inRect(mouseX, mouseY, widgetListX, widgetListY, wlW, wlH2)) onPanel = true;
        }

        // ── Clic sur un widget dans la scène → démarrer le drag ──────────────
        if (!onPanel && mouseButton == 0) {
            // Chercher le widget le plus "au-dessus" (dernier dans la liste) sous la souris
            UIElement hit = null;
            for (UIElement e : ui.all()) {
                if (e.isEnabled() && e.containsPoint(mouseX, mouseY)) {
                    hit = e;
                }
            }
            if (hit != null) {
                selected = hit;
                isDraggingWidget = true;
                dragOffsetX = mouseX - hit.getX();
                dragOffsetY = mouseY - hit.getY();
                if (selected instanceof net.minecraft.client.gui.ui.PotionStatusWidget) {
                    ((BaseWidget) selected).setProp("editorPreview", Boolean.TRUE);
                    ((BaseWidget) selected).setProp("previewEffect", "speed");
                }
                if (selected instanceof net.minecraft.client.gui.ui.ArmorGroupWidget)
                    ((BaseWidget) selected).setProp("editorPreview", Boolean.TRUE);
                bringToFront("sidebar");
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        // Déplacement d'un widget : uniquement si on est vraiment en train de dragger un widget
        if (isDraggingWidget && selected != null) {
            int newX = mouseX - dragOffsetX;
            int newY = mouseY - dragOffsetY;
            // Clamper aux bords de l'écran
            newX = Math.max(0, Math.min(this.width - selected.getWidth(), newX));
            newY = Math.max(0, Math.min(this.height - selected.getHeight(), newY));
            // Snap magnétique aux voisins si activé
            if (selected instanceof BaseWidget) {
                boolean snapAlign = Boolean.TRUE.equals(((BaseWidget) selected).getPropOrDefault("snapAlign", Boolean.TRUE));
                if (snapAlign) {
                    int[] snapped = snapToNeighbors(selected, newX, newY);
                    newX = snapped[0];
                    newY = snapped[1];
                }
            }
            selected.setPosition(newX, newY);
        }

        // Déplacement de la widgetList
        if (widgetListDragging) {
            int dx = mouseX - widgetListDragOffsetX;
            int dy = mouseY - widgetListDragOffsetY;
            widgetListX += dx;
            widgetListY += dy;
            widgetListX = Math.max(0, Math.min(this.width - widgetListW_dyn - 22, widgetListX));
            widgetListY = Math.max(0, Math.min(this.height - 60, widgetListY));
            widgetListDragOffsetX = mouseX;
            widgetListDragOffsetY = mouseY;
        }
        // Déplacement de la sidebar
        if (sidebarDragging) {
            int dx = mouseX - sidebarDragOffsetX;
            int dy = mouseY - sidebarDragOffsetY;
            sidebarX += dx;
            sidebarY += dy;
            sidebarX = Math.max(0, Math.min(this.width - sidebarW, sidebarX));
            sidebarY = Math.max(0, Math.min(this.height - sidebarH, sidebarY));
            sidebarDragOffsetX = mouseX;
            sidebarDragOffsetY = mouseY;
        }
        // Déplacement du colorEditor
        if (colorEditorDragging) {
            int dx = mouseX - colorEditorDragOffsetX;
            int dy = mouseY - colorEditorDragOffsetY;
            colorEditorX += dx;
            colorEditorY += dy;
            colorEditorX = Math.max(0, Math.min(this.width - cePanelW, colorEditorX));
            colorEditorY = Math.max(0, Math.min(this.height - 60, colorEditorY));
            colorEditorDragOffsetX = mouseX;
            colorEditorDragOffsetY = mouseY;
        }
        // Dragging on the color spectrum (hold mouse to change color in real time)
        if (draggingSpectrum && colorEditorOpen) {
            int px = colorEditorX, py = colorEditorY;
            int specX = px + 10, specY = py + 28, specW = ceSpecW, specH = ceSpecH;
            int clampedX = Math.max(specX, Math.min(specX + specW - 1, mouseX));
            int clampedY = Math.max(specY, Math.min(specY + specH - 1, mouseY));
            float hue = (clampedX - specX) / (float) Math.max(1, specW - 1);
            float val = 1.0f - (clampedY - specY) / (float) Math.max(1, specH - 1);
            int rgb = java.awt.Color.HSBtoRGB(hue, 1.0f, val);
            r = (rgb >> 16) & 0xFF; g = (rgb >> 8) & 0xFF; b = rgb & 0xFF;
            applyColorToWidget();
        }
        // dragging color slider for colorEditor
        if (colorEditorOpen && draggingSlider != -1) {
            int px = colorEditorX;
            int specX = px + 10;
            int sldX = specX + ceSpecW + 10;
            int slider_w = cePanelW - ceSpecW - 28;
            int rel = mouseX - sldX;
            rel = Math.max(0, Math.min(slider_w, rel));
            int val = (int) (rel / (float) slider_w * 255.0f);
            if (draggingSlider == 0) r = val;
            else if (draggingSlider == 1) g = val;
            else if (draggingSlider == 2) b = val;
            else if (draggingSlider == 3) a = val;
            applyColorToWidget();
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (selected != null && state == 0) {
            ui.saveConfig();
        }
        // stop dragging slider
        if (draggingSlider != -1) draggingSlider = -1;
        // stop dragging spectrum
        if (draggingSpectrum) draggingSpectrum = false;
        // stop dragging sidebar
        if (sidebarDragging) sidebarDragging = false;
        // stop dragging widget
        if (isDraggingWidget) {
            isDraggingWidget = false;
            ui.saveConfig();
        }
        // stop dragging color editor or widget list
        if (colorEditorDragging) colorEditorDragging = false;
        if (widgetListDragging) widgetListDragging = false;
        // if we released and there's no selected widget, ensure potion preview disabled everywhere
        if (selected == null) {
            UIElement pot = ui.get("potions");
            if (pot instanceof BaseWidget) {
                ((BaseWidget) pot).setProp("editorPreview", Boolean.FALSE);
                ((BaseWidget) pot).setProp("previewEffect", null);
            }
            UIElement arm = ui.get("armor_group");
            if (arm instanceof BaseWidget) ((BaseWidget) arm).setProp("editorPreview", Boolean.FALSE);
            // ensure editorActive is reset if editor fully closed
            UIManager.getInstance().setEditorActive(false);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int scroll = org.lwjgl.input.Mouse.getEventDWheel();
        if (scroll != 0) {
            int mouseX = org.lwjgl.input.Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - org.lwjgl.input.Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            int wlW = widgetListW_dyn + 22;
            java.util.List<UIElement> wlItems = new java.util.ArrayList<>();
            for (UIElement e : ui.all()) { wlItems.add(e); }
            int maxH = Math.min(widgetListHmax, this.height - 70);
            int maxVisible = Math.max(1, (maxH - 22) / 18);
            int wlH = 22 + Math.min(maxVisible, wlItems.size()) * 18;
            if (inRect(mouseX, mouseY, widgetListX, widgetListY, wlW, wlH)) {
                int maxScroll = Math.max(0, wlItems.size() - maxVisible);
                if (scroll > 0) widgetListScroll = Math.max(0, widgetListScroll - 1);
                else widgetListScroll = Math.min(maxScroll, widgetListScroll + 1);
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        if (colorEditorOpen && selected instanceof BaseWidget) {
            // left/right arrows to adjust selected component
            if (keyCode == 203) // left arrow
            {
                cycleColor(-5);
            } else if (keyCode == 205) // right arrow
            {
                cycleColor(5);
            }
        }
    }

    private void cycleColor(int delta) {
        r = Math.max(0, Math.min(255, r + delta));
        applyColorToWidget();
    }

    private void applyColorToWidget() {
        if (selected instanceof BaseWidget) {
            BaseWidget bw = (BaseWidget) selected;
            int raw = (a << 24) | (r << 16) | (g << 8) | b;
            bw.setProp("rawColor", raw);
            bw.setColor(raw);
            ui.saveConfig();
        }
    }

    // Map internal widget ids to friendly French names (same mapping as settings)
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

    private int[] snapToNeighbors(UIElement moving, int x, int y) {
        int snap = 5;
        int mw = Math.max(0, getSnapWidth(moving));
        int mh = Math.max(0, getSnapHeight(moving));
        if (mw == 0 && mh == 0) return new int[]{x, y};
        int sx = x, sy = y;
        for (UIElement e : ui.all()) {
            if (e == moving || !e.isEnabled()) continue;
            int ew = Math.max(0, getSnapWidth(e));
            int eh = Math.max(0, getSnapHeight(e));
            if (ew == 0 && eh == 0) continue;
            int ex = e.getX();
            int ey = e.getY();
            if (Math.abs(sx - ex) <= snap) sx = ex;
            if (Math.abs((sx + mw) - (ex + ew)) <= snap) sx = ex + ew - mw;
            if (Math.abs(sy - ey) <= snap) sy = ey;
            if (Math.abs((sy + mh) - (ey + eh)) <= snap) sy = ey + eh - mh;
        }
        return new int[]{sx, sy};
    }

    private int getSnapWidth(UIElement e) {
        if (e instanceof SimpleTextWidget) {
            SimpleTextWidget stw = (SimpleTextWidget) e;
            return stw.getTextWidth(Minecraft.getMinecraft().fontRendererObj);
        }
        return e.getWidth();
    }

    private int getSnapHeight(UIElement e) {
        if (e instanceof SimpleTextWidget) {
            return 12;
        }
        return e.getHeight();
    }

    private void drawGrid(int step, int color) {
        if (step <= 2) return;
        // Adaptive grid: larger step on small screens
        int actualStep = this.width < 400 ? step * 2 : step;
        int mainColor = color;
        int accentColor = (color & 0x00FFFFFF) | (((color >> 24) & 0xFF) * 2 << 24); // slightly brighter every 4th
        for (int gx = 0; gx < this.width; gx += actualStep) {
            int c = (gx % (actualStep * 4) == 0) ? accentColor : mainColor;
            drawRect(gx, 0, gx + 1, this.height, c);
        }
        for (int gy = 0; gy < this.height; gy += actualStep) {
            int c = (gy % (actualStep * 4) == 0) ? accentColor : mainColor;
            drawRect(0, gy, this.width, gy + 1, c);
        }
    }
}
