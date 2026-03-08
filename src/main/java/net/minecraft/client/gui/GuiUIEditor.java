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
    private final int widgetListW = 140;
    private final int widgetListHmax = 200;
    private boolean widgetListDragging = false;
    private int widgetListDragOffsetX = 0, widgetListDragOffsetY = 0;
    private int widgetListScroll = 0; // scroll offset pour la liste
    private final int[] hbWidgetListClose = new int[4];
    private final int[] hbCrosshairFixedBtn = new int[4]; // bouton crosshair fixe en bas
    private final List<String> panelOrder = new ArrayList<String>() {{
        add("widgetList");
        add("sidebar");
        add("colorEditor");
        add("crosshairEditor");
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
                bringToFront("sidebar");
            }
        }
        if (this.width < 400) {
            widgetListX = 4;
            colorEditorX = 4;
        } else {
            widgetListX = this.width - 162;
            colorEditorX = this.width - 320;
        }
        widgetListY = 28;
        colorEditorY = 28;
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

        // Petite pastille discrète en haut au centre (ne bloque pas la vue)
        int badgeW = 220, badgeH = 14;
        int badgeX = (this.width - badgeW) / 2, badgeY = 3;
        drawRect(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, 0xAA0D0D1A);
        drawRect(badgeX, badgeY + badgeH, badgeX + badgeW, badgeY + badgeH + 1, 0x882A7FFF);
        this.fontRendererObj.drawString(
                "✦ UI Editor ",
                badgeX + 5, badgeY + 3, 0x88AACCFF);

        // ensure editor preview is active
        UIElement pot = ui.get("potions");
        if (pot instanceof BaseWidget) ((BaseWidget) pot).setProp("editorPreview", Boolean.TRUE);
        UIElement arm = ui.get("armor_group");
        if (arm instanceof BaseWidget) ((BaseWidget) arm).setProp("editorPreview", Boolean.TRUE);

        // Render widgets + halo de sélection
        for (UIElement e : ui.all()) {
            e.render(mouseX, mouseY, partialTicks);
            // Ne pas afficher le halo bleu si le widget est désactivé ou si c'est le crosshair
            if (selected != null && e.getId().equals(selected.getId()) && e.isEnabled() && !(e instanceof net.minecraft.client.gui.ui.CrosshairWidget)) {
                // Halo de sélection bleu pulsant
                long pulse = System.currentTimeMillis() % 1200L;
                float pf = pulse < 600 ? pulse / 600f : (1200f - pulse) / 600f;
                int ha = (int) (40 + 40 * pf);
                drawRect(e.getX() - 3, e.getY() - 3, e.getX() + e.getWidth() + 3, e.getY() + e.getHeight() + 3, (ha << 24) | 0x2A7FFF);
                drawRect(e.getX() - 3, e.getY() - 3, e.getX() + e.getWidth() + 3, e.getY() - 2, 0xCC2A7FFF);
                drawRect(e.getX() - 3, e.getY() + e.getHeight() + 2, e.getX() + e.getWidth() + 3, e.getY() + e.getHeight() + 3, 0xCC2A7FFF);
                drawRect(e.getX() - 3, e.getY() - 3, e.getX() - 2, e.getY() + e.getHeight() + 3, 0xCC2A7FFF);
                drawRect(e.getX() + e.getWidth() + 2, e.getY() - 3, e.getX() + e.getWidth() + 3, e.getY() + e.getHeight() + 3, 0xCC2A7FFF);
            }
        }

        // Panneaux dans l'ordre Z (panelOrder : dernier = dessus)
        for (String panel : panelOrder) {
            if ("widgetList".equals(panel)) drawWidgetList();
            else if ("sidebar".equals(panel) && selected != null) drawWidgetSidebar();
            else if ("colorEditor".equals(panel) && colorEditorOpen && selected instanceof BaseWidget) drawColorEditor();
            else if ("crosshairEditor".equals(panel) && crosshairEditorOpen) drawCrosshairEditor();
        }

        // Bouton Crosshair fixe en bas de l'écran (toujours visible)
        drawCrosshairButton();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawWidgetList() {
        if (widgetListX == Integer.MIN_VALUE && this.width > 0) widgetListX = Math.max(4, this.width - 162);
        int px = widgetListX, py = widgetListY;
        int rowH = 18;
        // Exclure le crosshair de la liste
        java.util.List<UIElement> widgetItems = new java.util.ArrayList<>();
        for (UIElement e : ui.all()) {
            if (!"crosshair".equals(e.getId())) widgetItems.add(e);
        }
        int listCount = widgetItems.size();
        int w = Math.min(widgetListW + 22, this.width - 8);
        // Hauteur max adaptée à l'écran (laisser place au bouton crosshair en bas)
        int maxH = Math.min(widgetListHmax, this.height - 70);
        int maxVisible = Math.max(1, (maxH - 22) / rowH);
        int h = 22 + Math.min(maxVisible, listCount) * rowH;
        // Scroll
        if (widgetListScroll < 0) widgetListScroll = 0;
        if (widgetListScroll > Math.max(0, listCount - maxVisible)) widgetListScroll = Math.max(0, listCount - maxVisible);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        // Fond principal avec bord arrondi simulé
        drawRect(px, py, px + w, py + h, 0xF0101018);
        // Header coloré
        drawRect(px, py, px + w, py + 22, 0xFF151528);
        drawRect(px, py + 21, px + w, py + 22, 0xFF2A7FFF);
        // Titre
        this.fontRendererObj.drawStringWithShadow("Widgets", px + 8, py + 7, 0xFF8EC8FF);
        // Bouton X
        int ccx = px + w - 18, ccy = py + 5, ccs = 12;
        drawRect(ccx, ccy, ccx + ccs, ccy + ccs, 0xBB991111);
        drawRect(ccx, ccy, ccx + ccs, ccy + 1, 0xFF772222);
        this.fontRendererObj.drawString("✕", ccx + 3, ccy + 2, 0xFFFFFFFF);
        hbWidgetListClose[0] = ccx; hbWidgetListClose[1] = ccy; hbWidgetListClose[2] = ccs; hbWidgetListClose[3] = ccs;
        // Indicateur scroll (haut)
        if (widgetListScroll > 0) {
            drawRect(px + w/2 - 8, py + 22, px + w/2 + 8, py + 23, 0xFF2A7FFF);
            this.fontRendererObj.drawString("▲", px + w/2 - 3, py + 23, 0xFF88AAFF);
        }
        // Bordure extérieure bleue
        drawRect(px, py, px + 1, py + h, 0xFF2A7FFF);
        drawRect(px + w - 1, py, px + w, py + h, 0xFF2A7FFF);
        drawRect(px, py + h - 1, px + w, py + h, 0xFF2A7FFF);
        drawRect(px, py, px + w, py + 1, 0xFF2A7FFF);
        GlStateManager.disableBlend();

        int y = py + 22;
        int end = Math.min(widgetListScroll + maxVisible, listCount);
        for (int i = widgetListScroll; i < end; i++) {
            UIElement e = widgetItems.get(i);
            String id = e.getId();
            boolean on = e.isEnabled();
            boolean isSel = selected != null && id.equals(selected.getId());
            // Fond ligne
            if (isSel) drawRect(px + 1, y, px + w - 1, y + rowH, 0x551A6AFF);
            else if (i % 2 == 0) drawRect(px + 1, y, px + w - 1, y + rowH, 0x0A0FFFFF);
            // Séparateur bas
            drawRect(px + 1, y + rowH - 1, px + w - 1, y + rowH, 0x22FFFFFF);
            // Pastille statut
            int dot = on ? 0xFF44EE77 : 0xFF884444;
            drawRect(px + 5, y + 6, px + 9, y + 12, dot);
            // Nom (tronqué si trop long)
            String name = friendlyName(id);
            int nameCol = isSel ? 0xFFAAD4FF : (on ? 0xFFDDDDDD : 0xFF666666);
            this.fontRendererObj.drawString(name, px + 13, y + 5, nameCol);
            // Toggle séparé à droite (avec fond pour le distinguer)
            int tgX = px + w - 34, tgY = y + 3;
            drawRect(tgX - 2, tgY - 1, tgX + 30, tgY + 13, 0x44000000); // fond sombre pour le toggle
            drawToggle(tgX, tgY, on);
            y += rowH;
        }
        // Indicateur scroll (bas)
        if (end < listCount) {
            drawRect(px + w/2 - 8, py + h - 4, px + w/2 + 8, py + h - 3, 0xFF2A7FFF);
            this.fontRendererObj.drawString("▼", px + w/2 - 3, py + h - 14, 0xFF88AAFF);
        }
    }

    /** Bouton Crosshair fixe centré en bas de l'écran de l'éditeur */
    private void drawCrosshairButton() {
        int btnW = 150, btnH = 20;
        int btnX = this.width / 2 - btnW / 2;
        int btnY = this.height - 54; // au-dessus du bouton Done
        boolean hover = crosshairEditorOpen;
        int bg = hover ? 0xFF004488 : 0xCC001833;
        int border = hover ? 0xFF00CCFF : 0xFF2A7FFF;
        drawRect(btnX, btnY, btnX + btnW, btnY + btnH, bg);
        drawRect(btnX, btnY, btnX + btnW, btnY + 1, border);
        drawRect(btnX, btnY + btnH - 1, btnX + btnW, btnY + btnH, border);
        drawRect(btnX, btnY, btnX + 1, btnY + btnH, border);
        drawRect(btnX + btnW - 1, btnY, btnX + btnW, btnY + btnH, border);
        String label = crosshairEditorOpen ? "✦ Crosshair (ouvert)" : "✦ Éditer le Crosshair";
        int lw = this.fontRendererObj.getStringWidth(label);
        this.fontRendererObj.drawStringWithShadow(label, btnX + (btnW - lw) / 2, btnY + 6, hover ? 0xFF88EEFF : 0xFFAADDFF);
        hbCrosshairFixedBtn[0] = btnX; hbCrosshairFixedBtn[1] = btnY; hbCrosshairFixedBtn[2] = btnW; hbCrosshairFixedBtn[3] = btnH;
    }

    // helper to handle clicks on the widget list
    private boolean handleWidgetListClick(int mouseX, int mouseY) {
        int px = widgetListX, py = widgetListY;
        int rowH = 18;
        int w = widgetListW + 22;
        java.util.List<UIElement> widgetItems = new java.util.ArrayList<>();
        for (UIElement e : ui.all()) {
            if (!"crosshair".equals(e.getId())) widgetItems.add(e);
        }
        int listCount = widgetItems.size();
        int maxH = Math.min(widgetListHmax, this.height - 70);
        int maxVisible = Math.max(1, (maxH - 22) / rowH);
        int h = 22 + Math.min(maxVisible, listCount) * rowH;
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
        int y = py + 22;
        int end = Math.min(widgetListScroll + maxVisible, listCount);
        for (int i = widgetListScroll; i < end; i++) {
            if (y + rowH > py + h) break;
            UIElement e = widgetItems.get(i);
            // Clic sur le toggle (zone droite)
            if (inRect(mouseX, mouseY, px + w - 36, y + 2, 32, 14)) {
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
        int w = Math.min(310, this.width - 8);
        int h = 188;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        drawRect(px, py, px + w, py + h, 0xEE0D0D1A);
        drawRect(px, py, px + w, py + 22, 0xFF111130);
        drawRect(px, py + 22, px + w, py + 23, 0xFF9932CC);
        drawRect(px, py, px + 3, py + h, 0xFF9932CC);
        drawRect(px, py, px + w, py + 1, 0xFF9932CC);
        drawRect(px, py + h - 1, px + w, py + h, 0xFF333344);
        drawRect(px + w - 1, py, px + w, py + h, 0xFF333344);
        GlStateManager.disableBlend();

        this.fontRendererObj.drawStringWithShadow("Éditeur de couleur", px + 10, py + 7, 0xFFCC88FF);

        // Bouton fermer
        int ccx = px + w - 18, ccy = py + 5, ccs = 13;
        drawRect(ccx, ccy, ccx + ccs, ccy + ccs, 0xBB991111);
        drawRect(ccx, ccy, ccx + ccs, ccy + 1, 0xFF772222);
        this.fontRendererObj.drawStringWithShadow("✕", ccx + 3, ccy + 2, 0xFFFFFFFF);
        hbColorClose[0] = ccx; hbColorClose[1] = ccy; hbColorClose[2] = ccs; hbColorClose[3] = ccs;

        // Spectre couleur (hue x, valeur y)
        int specX = px + 10, specY = py + 28, specW = 148, specH = 120;
        for (int sx = 0; sx < specW; sx++) {
            float hue = sx / (float) (specW - 1);
            for (int sy = 0; sy < specH; sy++) {
                float val = 1.0f - (sy / (float) (specH - 1));
                int rgb = java.awt.Color.HSBtoRGB(hue, 1.0f, val);
                drawRect(specX + sx, specY + sy, specX + sx + 1, specY + sy + 1, 0xFF000000 | (rgb & 0x00FFFFFF));
            }
        }
        drawRect(specX - 1, specY - 1, specX + specW + 1, specY, 0xFF9932CC);
        drawRect(specX - 1, specY + specH, specX + specW + 1, specY + specH + 1, 0xFF333344);
        drawRect(specX - 1, specY - 1, specX, specY + specH + 1, 0xFF9932CC);
        drawRect(specX + specW, specY - 1, specX + specW + 1, specY + specH + 1, 0xFF333344);
        hbColorPreview[0] = specX; hbColorPreview[1] = specY; hbColorPreview[2] = specW; hbColorPreview[3] = specH;

        // Sliders RGBA
        int sldX = specX + specW + 8;
        drawChannelSlider(sldX, specY,      "R", 0, 100);
        drawChannelSlider(sldX, specY + 22, "G", 1, 100);
        drawChannelSlider(sldX, specY + 44, "B", 2, 100);
        drawChannelSlider(sldX, specY + 66, "A", 3, 100);

        // Prévisualisation couleur
        int previewCol = (a << 24) | (r << 16) | (g << 8) | b;
        drawRect(sldX, specY + 90, sldX + 100, specY + 106, 0xFF333333);
        drawRect(sldX, specY + 90, sldX + 100, specY + 106, previewCol);
        drawRect(sldX, specY + 90, sldX + 100, specY + 91, 0x44FFFFFF);

        // Boutons bas : Blanc + Rainbow
        int btnH = 14;
        int btnY = py + h - btnH - 6;
        int bw = (w - 24) / 2;

        drawRect(px + 10, btnY, px + 10 + bw, btnY + btnH, 0xFF333344);
        drawRect(px + 10, btnY, px + 10 + bw, btnY + 1, 0xFF555566);
        String resetLabel = "Blanc";
        this.fontRendererObj.drawString(resetLabel,
                px + 10 + (bw - this.fontRendererObj.getStringWidth(resetLabel)) / 2, btnY + 3, 0xFFCCCCCC);
        hbPreviewReset[0] = px + 10; hbPreviewReset[1] = btnY; hbPreviewReset[2] = bw; hbPreviewReset[3] = btnH;

        boolean isRainbow = selected instanceof BaseWidget && ((BaseWidget) selected).isRGBMode();
        int rbBg = isRainbow ? 0xFF1A8A4A : 0xFF333344;
        drawRect(px + 14 + bw, btnY, px + 14 + bw * 2, btnY + btnH, rbBg);
        drawRect(px + 14 + bw, btnY, px + 14 + bw * 2, btnY + 1, isRainbow ? 0xFF22CC66 : 0xFF555566);
        String rb = "Rainbow";
        this.fontRendererObj.drawString(rb,
                px + 14 + bw + (bw - this.fontRendererObj.getStringWidth(rb)) / 2, btnY + 3, 0xFFCCCCCC);
        hbColorRainbow[0] = px + 14 + bw; hbColorRainbow[1] = btnY; hbColorRainbow[2] = bw; hbColorRainbow[3] = btnH;
    }

    /** Dessine un slider R/G/B/A à la position donnée */
    private void drawChannelSlider(int x, int y, String label, int channel, int width) {
        // Valeur courante du canal
        int val;
        switch (channel) {
            case 0: val = r; break;
            case 1: val = g; break;
            case 2: val = b; break;
            default: val = a; break;
        }
        // Couleur du label
        int labelCol;
        switch (channel) {
            case 0: labelCol = 0xFFFF6666; break;
            case 1: labelCol = 0xFF66FF66; break;
            case 2: labelCol = 0xFF6666FF; break;
            default: labelCol = 0xFFAAAAAA; break;
        }
        // Fond slider
        drawRect(x, y + 2, x + width, y + 12, 0xFF222233);
        // Remplissage
        int filled = (int) (val / 255.0f * width);
        int fillCol = 0xFF000000 | (channel == 0 ? 0xFF0000 : (channel == 1 ? 0x00FF00 : (channel == 2 ? 0x0000FF : 0x888888)));
        drawRect(x, y + 2, x + filled, y + 12, fillCol);
        // Curseur
        drawRect(x + filled - 1, y + 1, x + filled + 1, y + 13, 0xFFFFFFFF);
        // Label
        this.fontRendererObj.drawString(label, x - 10, y + 3, labelCol);
        // Valeur numérique
        String valStr = String.valueOf(val);
        this.fontRendererObj.drawString(valStr, x + width + 3, y + 3, 0xFFCCCCCC);
    }

    // ── Panneau latéral contextuel ─────────────────────────────────────────────
    private void drawWidgetSidebar() {
        if (!(selected instanceof BaseWidget)) return;
        BaseWidget widget = (BaseWidget) selected;
        if (widget.getPropOrDefault("snapAlign", null) == null) widget.setProp("snapAlign", Boolean.TRUE);
        int px = sidebarX, py = sidebarY, w = sidebarW;

        // Hauteur adaptative
        int lines = 3; // Actif + Aligner + couleur/reset
        if (widget instanceof net.minecraft.client.gui.ui.KeyStrokeWidget) {
            lines += 2; // RGB + section touches
            lines += ((net.minecraft.client.gui.ui.KeyStrokeWidget) widget).getKeyCount();
        }
        if (widget instanceof PotionStatusWidget) lines += 3;
        if (widget instanceof ArmorGroupWidget) lines += 3;
        if (widget instanceof net.minecraft.client.gui.ui.CrosshairWidget) lines += 3; // redirige vers panneau dédié
        if (widget instanceof net.minecraft.client.gui.ui.ToggleSneakWidget
                || widget instanceof net.minecraft.client.gui.ui.ToggleSprintWidget) lines += 5;
        int h = Math.max(160, 48 + lines * 18);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // Fond principal
        drawRect(px, py, px + w, py + h, 0xEE111118);

        // Header avec couleur d'accent selon le type de widget
        int accentColor = 0xFF2A7FFF; // bleu par défaut
        if (widget instanceof net.minecraft.client.gui.ui.KeyStrokeWidget) accentColor = 0xFF8A2BE2;
        if (widget instanceof PotionStatusWidget) accentColor = 0xFF2ECC71;
        if (widget instanceof ArmorGroupWidget) accentColor = 0xFFE67E22;
        if (widget instanceof net.minecraft.client.gui.ui.ToggleSneakWidget
                || widget instanceof net.minecraft.client.gui.ui.ToggleSprintWidget) accentColor = 0xFF00CCAA;

        drawRect(px, py, px + w, py + 22, 0xFF0D0D1A);
        drawRect(px, py + 22, px + w, py + 23, accentColor); // trait d'accent
        // Bande colorée à gauche
        drawRect(px, py, px + 3, py + h, accentColor);

        // Bordure extérieure
        drawRect(px, py, px + w, py + 1, accentColor);
        drawRect(px, py + h - 1, px + w, py + h, 0xFF333344);
        drawRect(px + w - 1, py, px + w, py + h, 0xFF333344);

        GlStateManager.disableBlend();

        // Titre dans le header
        String widgetName = friendlyName(widget.getId());
        this.fontRendererObj.drawStringWithShadow(widgetName, px + 10, py + 7, accentColor);

        // Bouton fermer
        int cx = px + w - 18, cy = py + 5, cs = 13;
        drawRect(cx, cy, cx + cs, cy + cs, 0xBB991111);
        drawRect(cx, cy, cx + cs, cy + 1, 0xFF772222);
        this.fontRendererObj.drawStringWithShadow("✕", cx + 3, cy + 2, 0xFFFFFFFF);
        hbSidebarClose[0] = cx;
        hbSidebarClose[1] = cy;
        hbSidebarClose[2] = cs;
        hbSidebarClose[3] = cs;

        int y = py + 28;

        // ── Section : Général ──────────────────────────────────────────────────
        drawSectionHeader(px, y, w, "Général", accentColor);
        y += 13;

        // Actif
        boolean enabled = widget.isEnabled();
        this.fontRendererObj.drawString("Actif", px + 10, y + 2, 0xBBCCCCCC);
        drawToggle(px + w - 40, y, enabled);
        y += 18;

        // Aligner
        boolean snapAlign = Boolean.TRUE.equals(widget.getPropOrDefault("snapAlign", Boolean.TRUE));
        this.fontRendererObj.drawString("Aligner sur grille", px + 10, y + 2, 0xBBCCCCCC);
        drawToggle(px + w - 40, y, snapAlign);
        hbSnapAlign[0] = px + w - 40;
        hbSnapAlign[1] = y;
        hbSnapAlign[2] = 28;
        hbSnapAlign[3] = 12;
        y += 18;

        // Couleur (preview cliquable + bouton Reset)
        int col = widget.getColor();
        int cpX = px + 10, cpY = y, cpW = w - 20, cpH = 14;
        // Fond de la barre couleur avec damier (pour transparence)
        for (int cx2 = cpX; cx2 < cpX + cpW; cx2 += 4) {
            for (int cy2 = cpY; cy2 < cpY + cpH; cy2 += 4) {
                boolean light = ((cx2 / 4 + cy2 / 4) % 2 == 0);
                drawRect(cx2, cy2, Math.min(cx2 + 4, cpX + cpW), Math.min(cy2 + 4, cpY + cpH),
                        light ? 0xFF999999 : 0xFF666666);
            }
        }
        drawRect(cpX, cpY, cpX + cpW, cpY + cpH, col);
        drawRect(cpX, cpY, cpX + cpW, cpY + 1, 0x55FFFFFF);
        drawRect(cpX, cpY, cpX + 1, cpY + cpH, 0x55FFFFFF);
        this.fontRendererObj.drawString("▶ Couleur", cpX + 4, cpY + 3, 0xFFFFFFFF);
        hbColorPreview[0] = cpX;
        hbColorPreview[1] = cpY;
        hbColorPreview[2] = cpW;
        hbColorPreview[3] = cpH;
        y += 18;

        // Réinitialiser position
        int resetY = y;
        drawRect(px + 10, y - 1, px + w - 10, y + 12, 0x44FFFFFF);
        drawRect(px + 10, y - 1, px + w - 10, y, 0x22FFFFFF);
        this.fontRendererObj.drawString("↺  Réinitialiser position", px + 14, y + 2, 0xFFAAD4FF);
        hbResetColor[0] = px + 10;
        hbResetColor[1] = y - 1;
        hbResetColor[2] = w - 20;
        hbResetColor[3] = 13;
        y += 17;

        // ── Section : KeyStroke ────────────────────────────────────────────────
        if (widget instanceof net.minecraft.client.gui.ui.KeyStrokeWidget) {
            net.minecraft.client.gui.ui.KeyStrokeWidget ks = (net.minecraft.client.gui.ui.KeyStrokeWidget) widget;
            y += 4;
            drawSectionHeader(px, y, w, "Keystroke", accentColor);
            y += 13;

            // RGB mode
            boolean rgb = widget.isRGBMode();
            this.fontRendererObj.drawString("Mode arc-en-ciel", px + 10, y + 2, 0xBBCCCCCC);
            drawToggle(px + w - 40, y, rgb);
            y += 18;

            // Touches visibles
            int keyCount = ks.getKeyCount();
            for (int i = 0; i < keyCount; i++) {
                String lbl = ks.getKeyLabel(i);
                boolean vis = Boolean.TRUE.equals(widget.getPropOrDefault("showKey" + i, Boolean.TRUE));
                // fond alterné
                if (i % 2 == 0) drawRect(px + 4, y - 1, px + w - 4, y + 13, 0x0AFFFFFF);
                drawToggle(px + 10, y, vis);
                this.fontRendererObj.drawString(lbl, px + 44, y + 2, vis ? 0xCCCCCC : 0x555555);
                hbKeyToggle[i][0] = px + 10;
                hbKeyToggle[i][1] = y;
                hbKeyToggle[i][2] = 28;
                hbKeyToggle[i][3] = 12;
                y += 16;
            }
        }

        // ── Section : Potions ──────────────────────────────────────────────────
        if (widget instanceof net.minecraft.client.gui.ui.PotionStatusWidget) {
            y += 4;
            drawSectionHeader(px, y, w, "Potions", accentColor);
            y += 13;
            boolean showDur = Boolean.TRUE.equals(widget.getPropOrDefault("showDuration", Boolean.TRUE));
            this.fontRendererObj.drawString("Afficher durée", px + 10, y + 2, 0xBBCCCCCC);
            drawToggle(px + w - 40, y, showDur);
            hbPotionDur[0] = px + w - 40;
            hbPotionDur[1] = y;
            hbPotionDur[2] = 28;
            hbPotionDur[3] = 12;
            y += 18;
            boolean showIcons = Boolean.TRUE.equals(widget.getPropOrDefault("showIcons", Boolean.FALSE));
            this.fontRendererObj.drawString("Afficher icônes", px + 10, y + 2, 0xBBCCCCCC);
            drawToggle(px + w - 40, y, showIcons);
            hbPotionIcons[0] = px + w - 40;
            hbPotionIcons[1] = y;
            hbPotionIcons[2] = 28;
            hbPotionIcons[3] = 12;
            y += 18;
        }

        // ── Section : Armure ──────────────────────────────────────────────────
        if (widget instanceof net.minecraft.client.gui.ui.ArmorGroupWidget) {
            y += 4;
            drawSectionHeader(px, y, w, "Armure", accentColor);
            y += 13;
            String layout = String.valueOf(widget.getPropOrDefault("layout", "horizontal"));
            boolean isVert = "vertical".equals(layout);
            this.fontRendererObj.drawString("Disposition :", px + 10, y + 2, 0xBBCCCCCC);
            String dispLabel = isVert ? "Verticale ▼" : "Horizontale ▶";
            int dw = this.fontRendererObj.getStringWidth(dispLabel) + 8;
            drawRect(px + w - dw - 6, y - 1, px + w - 6, y + 11, 0x44FFFFFF);
            this.fontRendererObj.drawString(dispLabel, px + w - dw - 2, y + 2, 0xFFAAD4FF);
            hbArmorLayout[0] = px + w - dw - 6;
            hbArmorLayout[1] = y - 1;
            hbArmorLayout[2] = dw + 2;
            hbArmorLayout[3] = 12;
            y += 18;
            boolean dispPct = Boolean.TRUE.equals(widget.getPropOrDefault("displayPercent", Boolean.TRUE));
            this.fontRendererObj.drawString("Afficher %", px + 10, y + 2, 0xBBCCCCCC);
            drawToggle(px + w - 40, y, dispPct);
            hbArmorPercent[0] = px + w - 40;
            hbArmorPercent[1] = y;
            hbArmorPercent[2] = 28;
            hbArmorPercent[3] = 12;
            y += 18;
        }

        // ── Section : Toggle Sneak ─────────────────────────────────────────────
        if (widget instanceof net.minecraft.client.gui.ui.ToggleSneakWidget) {
            y += 4;
            drawSectionHeader(px, y, w, "Toggle Sneak", accentColor);
            y += 13;
            net.minecraft.client.settings.GameSettings gs = Minecraft.getMinecraft().gameSettings;
            boolean sneakEnabled = gs.toggleSneakEnabled;
            this.fontRendererObj.drawString("Fonctionnalité :", px + 10, y + 2, 0xBBCCCCCC);
            drawToggle(px + w - 40, y, sneakEnabled);
            hbArmorLayout[0] = px + w - 40; hbArmorLayout[1] = y; hbArmorLayout[2] = 28; hbArmorLayout[3] = 12;
            y += 18;
            this.fontRendererObj.drawString("\u00a77La touche Sneak classique bascule", px + 10, y + 2, 0x77AAAAAA);
            y += 12;
            this.fontRendererObj.drawString("\u00a77l'état accroupi si activé.", px + 10, y + 2, 0x77AAAAAA);
            y += 14;
            boolean active = gs.isToggleSneakActive;
            this.fontRendererObj.drawString("État actuel :", px + 10, y + 2, 0xBBCCCCCC);
            this.fontRendererObj.drawString(active ? "\u00a7aCROUCH" : "\u00a7cNormal", px + 90, y + 2, 0xFFFFFFFF);
            y += 16;
        }

        // ── Section : Toggle Sprint ────────────────────────────────────────────
        if (widget instanceof net.minecraft.client.gui.ui.ToggleSprintWidget) {
            y += 4;
            drawSectionHeader(px, y, w, "Toggle Sprint", accentColor);
            y += 13;
            net.minecraft.client.settings.GameSettings gs = Minecraft.getMinecraft().gameSettings;
            boolean sprintEnabled = gs.toggleSprintEnabled;
            this.fontRendererObj.drawString("Fonctionnalité :", px + 10, y + 2, 0xBBCCCCCC);
            drawToggle(px + w - 40, y, sprintEnabled);
            hbArmorPercent[0] = px + w - 40; hbArmorPercent[1] = y; hbArmorPercent[2] = 28; hbArmorPercent[3] = 12;
            y += 18;
            this.fontRendererObj.drawString("\u00a77La touche Sprint classique bascule", px + 10, y + 2, 0x77AAAAAA);
            y += 12;
            this.fontRendererObj.drawString("\u00a77le sprint si activé.", px + 10, y + 2, 0x77AAAAAA);
            y += 14;
            boolean active = gs.isToggleSprintActive;
            this.fontRendererObj.drawString("État actuel :", px + 10, y + 2, 0xBBCCCCCC);
            this.fontRendererObj.drawString(active ? "\u00a7aSPRINT" : "\u00a7cNormal", px + 90, y + 2, 0xFFFFFFFF);
            y += 16;
        }

        // ── Section : Crosshair (redirige vers panneau dédié) ─────────────────
        if (widget instanceof net.minecraft.client.gui.ui.CrosshairWidget) {
            y += 4;
            drawSectionHeader(px, y, w, "Crosshair", accentColor);
            y += 13;
            this.fontRendererObj.drawString("\u00a77Cliquer sur le bouton ✦ Crosshair", px + 10, y + 2, 0x77AAAAAA);
            y += 12;
            this.fontRendererObj.drawString("\u00a77dans la liste des widgets pour", px + 10, y + 2, 0x77AAAAAA);
            y += 12;
            this.fontRendererObj.drawString("\u00a77accéder aux options du crosshair.", px + 10, y + 2, 0x77AAAAAA);
            y += 14;
        }

        // Hint en bas
        if (y + 14 < py + h) {
            drawRect(px + 4, y + 4, px + w - 4, y + 5, 0x22FFFFFF);
            this.fontRendererObj.drawString("Glisse la barre de titre pour déplacer", px + 10, y + 8, 0x44AAAAAA);
        }

        sidebarH = h;
    }

    /**
     * Dessine un en-tête de section avec un trait coloré
     */
    private void drawSectionHeader(int px, int y, int w, String label, int accent) {
        drawRect(px + 4, y + 5, px + 4 + 3, y + 5 + 8, accent); // carré coloré
        this.fontRendererObj.drawString(label, px + 12, y + 4, 0xFFCCCCCC);
        drawRect(px + 12 + this.fontRendererObj.getStringWidth(label) + 4, y + 8, px + w - 4, y + 9, 0x22FFFFFF);
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

    // Éditeur Crosshair dédié (panneau flottant séparé)
    private boolean crosshairEditorOpen = false;
    private int crosshairEditorX = Integer.MIN_VALUE, crosshairEditorY = 60;
    private int crosshairEditorH = 400; // hauteur réelle calculée dynamiquement (mise à jour dans drawCrosshairEditor)
    private boolean crosshairEditorDragging = false;
    private int crosshairEditorDragOffX, crosshairEditorDragOffY;
    // Hitboxes éditeur crosshair
    private final int[] hbChEditorClose = new int[4];
    private final int[] hbChStyleVanilla = new int[4];
    private final int[] hbChStyleCS = new int[4];
    private final int[] hbChStyleDot = new int[4];
    private final int[] hbChSizeMinus = new int[4];
    private final int[] hbChSizePlus = new int[4];
    private final int[] hbChThickMinus = new int[4];
    private final int[] hbChThickPlus = new int[4];
    private final int[] hbChColor = new int[4];
    // dragging state for color sliders in crosshair editor (-1 = none, 0=R,1=G,2=B,3=A)
    private int draggingChColor = -1;

    // Color editor movable origin and dragging
    // lazy init similar to widgetListX
    private int colorEditorX = Integer.MIN_VALUE, colorEditorY = 28;
    private boolean colorEditorDragging = false;
    private int colorEditorDragOffsetX = 0, colorEditorDragOffsetY = 0;
    private boolean draggingSpectrum = false;

    private void drawToggle(int x, int y, boolean value) {
        int tw = 28, th = 12;
        // Fond coloré
        int bg = value ? 0xFF1A8A4A : 0xFF444455;
        drawRect(x, y, x + tw, y + th, bg);
        // Contour
        drawRect(x, y, x + tw, y + 1, value ? 0xFF22CC66 : 0xFF555566);
        drawRect(x, y + th - 1, x + tw, y + th, 0xFF111122);
        // Knob blanc
        int kx = value ? x + tw - th : x;
        drawRect(kx, y, kx + th, y + th, 0xFFEEEEEE);
        drawRect(kx, y, kx + th, y + 1, 0xFFFFFFFF); // reflet
        drawRect(kx, y + th - 1, kx + th, y + th, 0xFFBBBBBB); // ombre
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

        int y = py + 28; // après le header

        // Actif toggle (Général section, y+13 pour le header)
        y += 13; // section header
        if (inRect(mouseX, mouseY, px + w - 40, y, 28, 12)) {
            widget.setEnabled(!widget.isEnabled());
            ui.saveConfig();
            return;
        }
        y += 18;

        // Aligner toggle
        if (hbSnapAlign[2] != 0 && inRect(mouseX, mouseY, hbSnapAlign[0], hbSnapAlign[1], hbSnapAlign[2], hbSnapAlign[3])) {
            boolean cur = Boolean.TRUE.equals(widget.getPropOrDefault("snapAlign", Boolean.TRUE));
            widget.setProp("snapAlign", !cur);
            ui.saveConfig();
            return;
        }
        y += 18;

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
            y += 4;
            y += 13; // section header
            // RGB toggle
            if (inRect(mouseX, mouseY, px + w - 40, y, 28, 12)) {
                widget.setRGBMode(!widget.isRGBMode());
                ui.saveConfig();
                return;
            }
            y += 18;
            // Touches
            int keyCount = ks.getKeyCount();
            for (int i = 0; i < keyCount; i++) {
                if (hbKeyToggle[i][2] != 0 && inRect(mouseX, mouseY, hbKeyToggle[i][0], hbKeyToggle[i][1], hbKeyToggle[i][2], hbKeyToggle[i][3])) {
                    boolean vis = Boolean.TRUE.equals(widget.getPropOrDefault("showKey" + i, Boolean.TRUE));
                    widget.setProp("showKey" + i, !vis);
                    ui.saveConfig();
                    return;
                }
                y += 16;
            }
        }

        // ── Potions ────────────────────────────────────────────────────────────
        if (widget instanceof net.minecraft.client.gui.ui.PotionStatusWidget) {
            y += 4; // keep y progression consistent with drawWidgetSidebar
            // Duration toggle
            if (hbPotionDur[2] != 0 && inRect(mouseX, mouseY, hbPotionDur[0], hbPotionDur[1], hbPotionDur[2], hbPotionDur[3])) {
                boolean cur = Boolean.TRUE.equals(widget.getPropOrDefault("showDuration", Boolean.TRUE));
                widget.setProp("showDuration", !cur);
                ui.saveConfig();
                return;
            }
            y += 18;
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
            y += 4;
            // Layout toggle
            if (hbArmorLayout[2] != 0 && inRect(mouseX, mouseY, hbArmorLayout[0], hbArmorLayout[1], hbArmorLayout[2], hbArmorLayout[3])) {
                String cur = String.valueOf(widget.getPropOrDefault("layout", "horizontal"));
                widget.setProp("layout", "horizontal".equals(cur) ? "vertical" : "horizontal");
                ui.saveConfig();
                return;
            }
            y += 18;
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

        // ── Crosshair (la sidebar ne gère plus les options, panneau dédié) ─────
        // Pas de clics crosshair dans la sidebar
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

        // ── 1. Bouton "Éditer le Crosshair" fixe en bas (priorité absolue) ──────
        if (hbCrosshairFixedBtn[2] != 0
                && inRect(mouseX, mouseY, hbCrosshairFixedBtn[0], hbCrosshairFixedBtn[1],
                           hbCrosshairFixedBtn[2], hbCrosshairFixedBtn[3])) {
            crosshairEditorOpen = !crosshairEditorOpen;
            if (crosshairEditorOpen) {
                bringToFront("crosshairEditor");
                net.minecraft.client.settings.GameSettings gs2 = Minecraft.getMinecraft().gameSettings;
                if (gs2.crosshairType == 0) {
                    gs2.crosshairUseVanillaTexture = false;
                    gs2.saveOptions();
                }
            }
            return;
        }

        // ── 2. Panneaux flottants par ordre Z (top en premier) ──────────────────
        for (int i = panelOrder.size() - 1; i >= 0; i--) {
            String p = panelOrder.get(i);

            // ── Éditeur Crosshair ──────────────────────────────────────────────
            if ("crosshairEditor".equals(p) && crosshairEditorOpen) {
                int px = crosshairEditorX, py = crosshairEditorY;
                if (crosshairEditorX == Integer.MIN_VALUE) continue;
                if (!inRect(mouseX, mouseY, px, py, 320, crosshairEditorH)) continue;
                handleCrosshairEditorClick(mouseX, mouseY);
                return;
            }

            // ── Éditeur de couleur ─────────────────────────────────────────────
            if ("colorEditor".equals(p) && colorEditorOpen && selected instanceof BaseWidget) {
                int px = colorEditorX, py = colorEditorY;
                int panelW = Math.min(310, this.width - 8), panelH = 188;
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
                // Spectre
                int specX = px + 10, specY = py + 28, specW = 148, specH = 120;
                if (inRect(mouseX, mouseY, specX, specY, specW, specH)) {
                    float hue = (mouseX - specX) / (float) Math.max(1, specW - 1);
                    float val = 1.0f - (mouseY - specY) / (float) Math.max(1, specH - 1);
                    int rgb = java.awt.Color.HSBtoRGB(hue, 1.0f, val);
                    r = (rgb >> 16) & 0xFF; g = (rgb >> 8) & 0xFF; b = rgb & 0xFF;
                    applyColorToWidget(); draggingSpectrum = true;
                    return;
                }
                // Sliders RGBA
                int sldX = specX + specW + 8, slider_w = 100;
                for (int j = 0; j < 4; j++) {
                    int sy = specY + j * 22;
                    if (inRect(mouseX, mouseY, sldX + 20, sy + 2, slider_w, 10)) {
                        int rel = Math.max(0, Math.min(slider_w, mouseX - (sldX + 20)));
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
                int wlW = widgetListW + 22;
                java.util.List<UIElement> wlItems = new java.util.ArrayList<>();
                for (UIElement e : ui.all()) { if (!"crosshair".equals(e.getId())) wlItems.add(e); }
                int maxH = Math.min(widgetListHmax, this.height - 70);
                int maxVisible = Math.max(1, (maxH - 22) / 18);
                int wlH = 22 + Math.min(maxVisible, wlItems.size()) * 18;
                if (!inRect(mouseX, mouseY, widgetListX, widgetListY, wlW, wlH)) continue;
                if (handleWidgetListClick(mouseX, mouseY)) return;
            }
        }

        // ── 3. Clic sur un widget dans la scène (drag) ──────────────────────────
        // On vérifie d'abord qu'on n'est pas sur un panneau
        boolean onPanel = false;
        if (crosshairEditorOpen && crosshairEditorX != Integer.MIN_VALUE
                && inRect(mouseX, mouseY, crosshairEditorX, crosshairEditorY, 320, crosshairEditorH)) onPanel = true;
        if (!onPanel && colorEditorOpen && selected instanceof BaseWidget) {
            int panelW = Math.min(310, this.width - 8);
            if (inRect(mouseX, mouseY, colorEditorX, colorEditorY, panelW, 188)) onPanel = true;
        }
        if (!onPanel && selected != null && inRect(mouseX, mouseY, sidebarX, sidebarY, sidebarW, sidebarH)) onPanel = true;
        if (!onPanel) {
            int wlW = widgetListW + 22;
            java.util.List<UIElement> wlItems2 = new java.util.ArrayList<>();
            for (UIElement e : ui.all()) { if (!"crosshair".equals(e.getId())) wlItems2.add(e); }
            int maxH2 = Math.min(widgetListHmax, this.height - 70);
            int maxVisible2 = Math.max(1, (maxH2 - 22) / 18);
            int wlH2 = 22 + Math.min(maxVisible2, wlItems2.size()) * 18;
            if (inRect(mouseX, mouseY, widgetListX, widgetListY, wlW, wlH2)) onPanel = true;
        }

        if (!onPanel) {
            for (UIElement e : ui.all()) {
                if (e instanceof net.minecraft.client.gui.ui.CrosshairWidget) continue;
                if (!e.isEnabled()) continue;
                if (e.containsPoint(mouseX, mouseY)) {
                    selected = e;
                    dragOffsetX = mouseX - e.getX();
                    dragOffsetY = mouseY - e.getY();
                    bringToFront("sidebar");
                    if (mouseButton == 0) isDraggingWidget = true;
                    if (mouseButton == 1 && selected instanceof BaseWidget) {
                        BaseWidget bw = (BaseWidget) selected;
                        Object rawObj = bw.getProp("rawColor");
                        if (rawObj instanceof Number) {
                            int raw = ((Number) rawObj).intValue();
                            a = (raw >> 24) & 0xFF; if (a == 0) a = 255;
                            r = (raw >> 16) & 0xFF; g = (raw >> 8) & 0xFF; b = raw & 0xFF;
                        } else {
                            int col = bw.getColor();
                            a = 255; r = (col >> 16) & 0xFF; g = (col >> 8) & 0xFF; b = col & 0xFF;
                        }
                        colorEditorOpen = true; bringToFront("colorEditor");
                    }
                    if (selected instanceof net.minecraft.client.gui.ui.PotionStatusWidget) {
                        ((BaseWidget) selected).setProp("editorPreview", Boolean.TRUE);
                        ((BaseWidget) selected).setProp("previewEffect", "speed");
                    }
                    if (selected instanceof net.minecraft.client.gui.ui.ArmorGroupWidget)
                        ((BaseWidget) selected).setProp("editorPreview", Boolean.TRUE);
                    return;
                }
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        // Déplacement d'un widget : uniquement si on est vraiment en train de dragger un widget
        if (isDraggingWidget && selected != null
                && !(selected instanceof net.minecraft.client.gui.ui.CrosshairWidget)
                && selected.isEnabled()) {
            int newX = mouseX - dragOffsetX;
            int newY = mouseY - dragOffsetY;
            if (selected instanceof BaseWidget) {
                boolean snap = Boolean.TRUE.equals(((BaseWidget) selected).getPropOrDefault("snapAlign", Boolean.TRUE));
                if (snap) {
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
            widgetListX = Math.max(0, Math.min(this.width - widgetListW - 22, widgetListX));
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
            int panelW = Math.min(310, this.width - 8);
            int dx = mouseX - colorEditorDragOffsetX;
            int dy = mouseY - colorEditorDragOffsetY;
            colorEditorX += dx;
            colorEditorY += dy;
            colorEditorX = Math.max(0, Math.min(this.width - panelW, colorEditorX));
            colorEditorY = Math.max(0, Math.min(this.height - 60, colorEditorY));
            colorEditorDragOffsetX = mouseX;
            colorEditorDragOffsetY = mouseY;
        }
        // Déplacement du crosshairEditor
        if (crosshairEditorDragging) {
            int dx = mouseX - crosshairEditorDragOffX;
            int dy = mouseY - crosshairEditorDragOffY;
            crosshairEditorX += dx;
            crosshairEditorY += dy;
            crosshairEditorX = Math.max(0, Math.min(this.width - 320, crosshairEditorX));
            crosshairEditorY = Math.max(0, Math.min(this.height - 60, crosshairEditorY));
            crosshairEditorDragOffX = mouseX;
            crosshairEditorDragOffY = mouseY;
        }
        // dragging taille/épaisseur/gap sliders pour le crosshair editor
        if (crosshairEditorOpen && (draggingChSliderSize || draggingChSliderThick || draggingChSliderGap)) {
            net.minecraft.client.settings.GameSettings gs = Minecraft.getMinecraft().gameSettings;
            gs.crosshairUseVanillaTexture = false;
            if (draggingChSliderSize && hbChSliderSize[2] != 0) {
                int rel = Math.max(0, Math.min(hbChSliderSize[2], mouseX - hbChSliderSize[0]));
                gs.crosshairSize = Math.max(1, Math.min(20, 1 + (int)(rel / (float) hbChSliderSize[2] * 19)));
                gs.saveOptions();
            }
            if (draggingChSliderThick && hbChSliderThick[2] != 0) {
                int rel = Math.max(0, Math.min(hbChSliderThick[2], mouseX - hbChSliderThick[0]));
                gs.crosshairThickness = Math.max(1, Math.min(10, 1 + (int)(rel / (float) hbChSliderThick[2] * 9)));
                gs.saveOptions();
            }
            if (draggingChSliderGap && hbChSliderGap[2] != 0) {
                int rel = Math.max(0, Math.min(hbChSliderGap[2], mouseX - hbChSliderGap[0]));
                gs.crosshairGap = Math.max(0, Math.min(15, (int)(rel / (float) hbChSliderGap[2] * 15)));
                gs.saveOptions();
            }
        }
        // dragging color slider for crosshair editor
        if (crosshairEditorOpen && draggingChColor != -1) {
            net.minecraft.client.settings.GameSettings gs = Minecraft.getMinecraft().gameSettings;
            int px = crosshairEditorX, py = crosshairEditorY;
            int W = 320;
            int y = py + 26;
            y += 16 + 24; // section style + boutons style
            int leftH = 12 + 18;
            if (gs.crosshairType == 1) leftH += 12 + 18 + 12 + 18;
            int pvBottom = y + Math.max(leftH, 70 + 12);
            y = pvBottom;
            y += 16; // section couleur header
            int csldX = px + 30;
            int csldW = W - 36;
            int rel = Math.max(0, Math.min(csldW, mouseX - csldX));
            int val = (int)(rel / (float) csldW * 255);
            int col = gs.crosshairColor;
            if ((col & 0xFF000000) == 0) col |= 0xFF000000;
            int A = (col >> 24) & 0xFF;
            int R = (col >> 16) & 0xFF;
            int G = (col >> 8) & 0xFF;
            int B = col & 0xFF;
            if (draggingChColor == 0) R = val;
            else if (draggingChColor == 1) G = val;
            else if (draggingChColor == 2) B = val;
            else A = val;
            gs.crosshairColor = (A << 24) | (R << 16) | (G << 8) | B;
            gs.crosshairUseVanillaTexture = false;
            gs.saveOptions();
            UIElement cw2 = ui.get("crosshair"); if (cw2 instanceof BaseWidget) ((BaseWidget) cw2).setColor(gs.crosshairColor);
        }
        // dragging color slider for colorEditor
        if (colorEditorOpen && draggingSlider != -1) {
            int px = colorEditorX, py = colorEditorY;
            int specX = px + 10, specW = 148;
            int sldX = specX + specW + 8;
            int slider_w = 100;
            int rel = mouseX - (sldX + 20);
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
        // stop dragging crosshair sliders
        draggingChSliderSize = draggingChSliderThick = draggingChSliderGap = false;
        // stop dragging color sliders in crosshair editor
        if (draggingChColor != -1) draggingChColor = -1;
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
        if (crosshairEditorDragging) crosshairEditorDragging = false;
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
            int wlW = widgetListW + 22;
            java.util.List<UIElement> wlItems = new java.util.ArrayList<>();
            for (UIElement e : ui.all()) { if (!"crosshair".equals(e.getId())) wlItems.add(e); }
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
            // Si c'est le widget crosshair, écrire dans gameSettings avec alpha forcé à 0xFF
            if (selected instanceof net.minecraft.client.gui.ui.CrosshairWidget) {
                // Le crosshair utilise toujours une couleur opaque
                int crosshairRaw = 0xFF000000 | (r << 16) | (g << 8) | b;
                Minecraft.getMinecraft().gameSettings.crosshairColor = crosshairRaw;
                bw.setColor(crosshairRaw);
                Minecraft.getMinecraft().gameSettings.saveOptions();
            }
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
        if ("crosshair".equals(id)) return "Crosshair";
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
        for (int gx = 0; gx < this.width; gx += step)
            drawRect(gx, 0, gx + 1, this.height, color);
        for (int gy = 0; gy < this.height; gy += step)
            drawRect(0, gy, this.width, gy + 1, color);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  ÉDITEUR DE CROSSHAIR — complet, ergonomique, fonctionnel
    // ═══════════════════════════════════════════════════════════════════════════

    // Champs d'état de l'éditeur crosshair
    private int crosshairResetBtnY = -1;
    // Hitboxes pour les sliders intégrés dans le panneau crosshair
    private final int[] hbChSliderSize  = new int[4]; // [x,y,w,h] zone cliquable du slider taille
    private final int[] hbChSliderThick = new int[4]; // idem épaisseur
    private final int[] hbChSliderGap   = new int[4]; // idem gap
    private final int[] hbChRainbow     = new int[4]; // bouton rainbow
    private boolean draggingChSliderSize  = false;
    private boolean draggingChSliderThick = false;
    private boolean draggingChSliderGap   = false;

    private void drawCrosshairEditor() {
        net.minecraft.client.settings.GameSettings gs = Minecraft.getMinecraft().gameSettings;
        if (crosshairEditorX == Integer.MIN_VALUE && this.width > 0)
            crosshairEditorX = Math.max(4, this.width / 2 - 160);
        int px = crosshairEditorX, py = crosshairEditorY;
        final int W = 320, ACCENT = 0xFF00CCFF;

        // ── Calcul hauteur dynamique ─────────────────────────────────────────
        int contentH = 22 + 4; // header + padding
        contentH += 16 + 4;    // Section "Style" header
        contentH += 18 + 6;    // boutons style + gap
        // Aperçu : 70px à droite, les options à gauche → max(options, 70)
        int leftH = 0;
        // Taille (toujours visible)
        leftH += 12 + 14 + 4; // label + slider + gap
        // Épaisseur (CS:GO seulement)
        if (gs.crosshairType == 1) leftH += 12 + 14 + 4;
        // Gap central (CS:GO seulement)
        if (gs.crosshairType == 1) leftH += 12 + 14 + 4;
        int previewH = 70;
        contentH += Math.max(leftH, previewH) + 8;
        // Section Couleur
        contentH += 16 + 4;    // section header
        contentH += 14 + 4;    // slider R
        contentH += 14 + 4;    // slider G
        contentH += 14 + 4;    // slider B
        contentH += 14 + 8;    // slider A + gap
        // Boutons Blanc / Rainbow
        contentH += 18 + 6;
        // Bouton reset
        contentH += 18 + 4;
        int H = contentH + 8;
        crosshairEditorH = H; // stocker pour que mouseClicked utilise la bonne hauteur

        // ── Fond & bordure ───────────────────────────────────────────────────
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        drawRect(px, py, px + W, py + H, 0xF0101018);
        drawRect(px, py, px + W, py + 22, 0xFF080814);
        drawRect(px, py + 22, px + W, py + 23, ACCENT);
        drawRect(px, py, px + 2, py + H, ACCENT);
        drawRect(px, py, px + W, py + 1, ACCENT);
        drawRect(px, py + H - 1, px + W, py + H, 0xFF333344);
        drawRect(px + W - 1, py, px + W, py + H, 0xFF333344);
        GlStateManager.disableBlend();

        // Titre
        this.fontRendererObj.drawStringWithShadow("✦ Éditeur de Crosshair", px + 10, py + 7, ACCENT);

        // Bouton fermer
        int ccx = px + W - 18, ccy = py + 5, ccs = 13;
        drawRect(ccx, ccy, ccx + ccs, ccy + ccs, 0xBB991111);
        drawRect(ccx, ccy, ccx + ccs, ccy + 1, 0xFF772222);
        this.fontRendererObj.drawStringWithShadow("✕", ccx + 3, ccy + 2, 0xFFFFFFFF);
        hbChEditorClose[0] = ccx; hbChEditorClose[1] = ccy; hbChEditorClose[2] = ccs; hbChEditorClose[3] = ccs;

        int y = py + 26;

        // ── Section Style ────────────────────────────────────────────────────
        drawCrosshairSectionHeader(px, y, W, "Style du crosshair", ACCENT);
        y += 16;

        String[] styleLabels = { "Vanilla", "CS:GO", "Point" };
        int btnSW = (W - 20 - 4) / 3;
        for (int i = 0; i < 3; i++) {
            int bx = px + 8 + i * (btnSW + 2);
            boolean sel = gs.crosshairType == i;
            int bg2 = sel ? 0xFF003355 : 0xFF1A1A2A;
            int top = sel ? ACCENT : 0xFF444455;
            drawRect(bx, y, bx + btnSW, y + 18, bg2);
            drawRect(bx, y, bx + btnSW, y + 1, top);
            drawRect(bx, y, bx + 1, y + 18, top);
            drawRect(bx + btnSW - 1, y, bx + btnSW, y + 18, sel ? 0xFF005577 : 0xFF222233);
            String sl = styleLabels[i];
            int slw = this.fontRendererObj.getStringWidth(sl);
            this.fontRendererObj.drawStringWithShadow(sl, bx + (btnSW - slw) / 2, y + 5, sel ? 0xFF88EEFF : 0xFF888899);
            if (i == 0) { hbChStyleVanilla[0] = bx; hbChStyleVanilla[1] = y; hbChStyleVanilla[2] = btnSW; hbChStyleVanilla[3] = 18; }
            if (i == 1) { hbChStyleCS[0] = bx; hbChStyleCS[1] = y; hbChStyleCS[2] = btnSW; hbChStyleCS[3] = 18; }
            if (i == 2) { hbChStyleDot[0] = bx; hbChStyleDot[1] = y; hbChStyleDot[2] = btnSW; hbChStyleDot[3] = 18; }
        }
        y += 24;

        // ── Zone : Aperçu (droite) + Options (gauche) ────────────────────────
        int optW = W - 90; // largeur zone options
        int pvSize = 70;
        int pvX = px + W - pvSize - 8, pvY = y;

        // Fond aperçu
        drawRect(pvX - 1, pvY - 1, pvX + pvSize + 1, pvY + pvSize + 1, ACCENT);
        drawRect(pvX, pvY, pvX + pvSize, pvY + pvSize, 0xFF000000);
        // Label aperçu
        String pvLbl = "Aperçu";
        int pvLw = this.fontRendererObj.getStringWidth(pvLbl);
        this.fontRendererObj.drawString(pvLbl, pvX + (pvSize - pvLw) / 2, pvY + pvSize + 3, 0x77AAAAAA);
        // Dessin crosshair dans l'aperçu (avec rainbow en temps réel si activé)
        int previewColor = gs.crosshairColor;
        if ((previewColor & 0xFF000000) == 0) previewColor |= 0xFF000000;
        if (gs.crosshairRainbow) {
            float hueRainbow = (System.currentTimeMillis() % 3000L) / 3000.0f;
            previewColor = 0xFF000000 | (java.awt.Color.HSBtoRGB(hueRainbow, 1f, 1f) & 0x00FFFFFF);
        }
        drawCrosshairPreviewColor(pvX + pvSize / 2, pvY + pvSize / 2, gs, previewColor);

        // ── Options (gauche de l'aperçu) ─────────────────────────────────────
        int optX = px + 8;
        int optRight = pvX - 8;
        int sliderW = optRight - optX;

        // Taille (toujours)
        drawChSliderLabel(optX, y, "Taille", gs.crosshairSize, 1, 20);
        y += 12;
        drawChSlider(optX, y, sliderW, gs.crosshairSize, 1, 20, ACCENT);
        hbChSliderSize[0] = optX; hbChSliderSize[1] = y; hbChSliderSize[2] = sliderW; hbChSliderSize[3] = 12;
        y += 18;

        // Épaisseur (CS:GO uniquement)
        if (gs.crosshairType == 1) {
            drawChSliderLabel(optX, y, "Épaisseur", gs.crosshairThickness, 1, 10);
            y += 12;
            drawChSlider(optX, y, sliderW, gs.crosshairThickness, 1, 10, ACCENT);
            hbChSliderThick[0] = optX; hbChSliderThick[1] = y; hbChSliderThick[2] = sliderW; hbChSliderThick[3] = 12;
            y += 18;
        } else {
            hbChSliderThick[2] = 0;
        }

        // Gap (CS:GO uniquement)
        if (gs.crosshairType == 1) {
            drawChSliderLabel(optX, y, "Gap central", gs.crosshairGap, 0, 15);
            y += 12;
            drawChSlider(optX, y, sliderW, gs.crosshairGap, 0, 15, ACCENT);
            hbChSliderGap[0] = optX; hbChSliderGap[1] = y; hbChSliderGap[2] = sliderW; hbChSliderGap[3] = 12;
            y += 18;
        } else {
            hbChSliderGap[2] = 0;
        }

        // Aligner y avec la base de l'aperçu si on est encore trop haut
        int pvBottom = pvY + pvSize + 12;
        if (y < pvBottom) y = pvBottom;

        // ── Section Couleur ──────────────────────────────────────────────────
        drawCrosshairSectionHeader(px, y, W, "Couleur", ACCENT);
        y += 16;

        // Sliders R, G, B, A directement dans le panneau
        int csldW = W - 36;
        int csldX = px + 30;

        // R
        drawCrosshairChannelSlider(csldX, y, csldW, "R", 0, 0xFFFF6666);
        y += 18;
        // G
        drawCrosshairChannelSlider(csldX, y, csldW, "G", 1, 0xFF66FF66);
        y += 18;
        // B
        drawCrosshairChannelSlider(csldX, y, csldW, "B", 2, 0xFF6688FF);
        y += 18;
        // A
        drawCrosshairChannelSlider(csldX, y, csldW, "A", 3, 0xFFAAAAAA);
        y += 22;

        // Aperçu couleur actuelle
        int curCol = gs.crosshairColor;
        if ((curCol & 0xFF000000) == 0) curCol |= 0xFF000000;
        int colPreviewX = px + 8, colPreviewW = W - 16, colPreviewH = 12;
        drawRect(colPreviewX, y, colPreviewX + colPreviewW, y + colPreviewH, 0xFF111122);
        drawRect(colPreviewX, y, colPreviewX + colPreviewW, y + colPreviewH, curCol);
        drawRect(colPreviewX, y, colPreviewX + colPreviewW, y + 1, 0x55FFFFFF);
        y += 16;

        // ── Boutons Blanc / Rainbow ──────────────────────────────────────────
        int halfW = (W - 20) / 2;
        // Blanc
        drawRect(px + 8, y, px + 8 + halfW, y + 16, 0xFF1A1A2A);
        drawRect(px + 8, y, px + 8 + halfW, y + 1, 0xFF555566);
        String lblBlanc = "Réinitialiser blanc";
        int lwBlanc = this.fontRendererObj.getStringWidth(lblBlanc);
        this.fontRendererObj.drawString(lblBlanc, px + 8 + (halfW - lwBlanc) / 2, y + 4, 0xFFCCCCCC);
        hbPreviewReset[0] = px + 8; hbPreviewReset[1] = y; hbPreviewReset[2] = halfW; hbPreviewReset[3] = 16;

        // Rainbow
        boolean isRainbow = gs.crosshairRainbow;
        int rbBg = isRainbow ? 0xFF1A3A1A : 0xFF1A1A2A;
        int rbTop = isRainbow ? 0xFF22CC66 : 0xFF555566;
        drawRect(px + 12 + halfW, y, px + 12 + halfW * 2, y + 16, rbBg);
        drawRect(px + 12 + halfW, y, px + 12 + halfW * 2, y + 1, rbTop);
        // Arc-en-ciel animé dans le bouton si actif
        if (isRainbow) {
            for (int rx = 0; rx < halfW - 2; rx++) {
                float h2 = (rx / (float)(halfW - 2) + (System.currentTimeMillis() % 2000L) / 2000.0f) % 1.0f;
                int rc = 0xFF000000 | (java.awt.Color.HSBtoRGB(h2, 1f, 0.8f) & 0x00FFFFFF);
                drawRect(px + 13 + halfW + rx, y + 10, px + 14 + halfW + rx, y + 14, rc);
            }
        }
        String lblRb = isRainbow ? "Rainbow ON" : "Rainbow";
        int lwRb = this.fontRendererObj.getStringWidth(lblRb);
        this.fontRendererObj.drawString(lblRb, px + 12 + halfW + (halfW - lwRb) / 2, y + 4, isRainbow ? 0xFF44FF88 : 0xFFCCCCCC);
        hbChRainbow[0] = px + 12 + halfW; hbChRainbow[1] = y; hbChRainbow[2] = halfW; hbChRainbow[3] = 16;
        y += 22;

        // ── Bouton reset vanilla ─────────────────────────────────────────────
        drawRect(px + 8, y, px + W - 8, y + 18, 0xFF0D0D1A);
        drawRect(px + 8, y, px + W - 8, y + 1, 0xFF444455);
        String resetLbl = "↺  Remettre le crosshair Minecraft par défaut";
        int rlw2 = this.fontRendererObj.getStringWidth(resetLbl);
        if (rlw2 > W - 20) resetLbl = "↺  Crosshair par défaut";
        rlw2 = this.fontRendererObj.getStringWidth(resetLbl);
        this.fontRendererObj.drawString(resetLbl, px + 8 + (W - 16 - rlw2) / 2, y + 5, 0xFF88AABB);
        crosshairResetBtnY = y;
        y += 22;
    }

    /** Label + valeur pour un slider crosshair */
    private void drawChSliderLabel(int x, int y, String name, int val, int min, int max) {
        this.fontRendererObj.drawString(name + ":", x, y, 0xFFCCCCDD);
        String vs = String.valueOf(val);
        int vw = this.fontRendererObj.getStringWidth(vs);
        this.fontRendererObj.drawString(vs, x + 120 - vw, y, 0xFF88EEFF);
    }

    /** Slider horizontal pour les options crosshair (taille, épaisseur, gap) */
    private void drawChSlider(int x, int y, int w, int val, int min, int max, int accent) {
        int range = Math.max(1, max - min);
        int filled = (int)((val - min) / (float) range * w);
        // Fond
        drawRect(x, y + 3, x + w, y + 9, 0xFF111122);
        // Remplissage
        drawRect(x, y + 3, x + filled, y + 9, 0xFF004466);
        // Trait accent
        drawRect(x, y + 3, x + filled, y + 4, accent);
        // Curseur
        int kx = x + filled;
        drawRect(kx - 2, y, kx + 2, y + 12, 0xFFCCEEFF);
        drawRect(kx - 1, y + 1, kx + 1, y + 11, accent);
    }

    /** Slider de canal RGBA directement dans l'éditeur crosshair */
    private void drawCrosshairChannelSlider(int x, int y, int w, String lbl, int channel, int labelCol) {
        net.minecraft.client.settings.GameSettings gs = Minecraft.getMinecraft().gameSettings;
        int col = gs.crosshairColor;
        if ((col & 0xFF000000) == 0) col |= 0xFF000000;
        int val;
        switch (channel) {
            case 0: val = (col >> 16) & 0xFF; break;
            case 1: val = (col >> 8) & 0xFF; break;
            case 2: val = col & 0xFF; break;
            default: val = (col >> 24) & 0xFF; break;
        }
        // Label
        this.fontRendererObj.drawString(lbl, x - 18, y + 2, labelCol);
        // Fond slider
        drawRect(x, y + 2, x + w, y + 10, 0xFF111122);
        // Fond coloré du canal
        int fillCol;
        switch (channel) {
            case 0: fillCol = 0xFF880000; break;
            case 1: fillCol = 0xFF008800; break;
            case 2: fillCol = 0xFF000088; break;
            default: fillCol = 0xFF444444; break;
        }
        int filled = (int)(val / 255.0f * w);
        drawRect(x, y + 2, x + filled, y + 10, fillCol);
        // Curseur
        drawRect(x + filled - 1, y + 1, x + filled + 1, y + 11, 0xFFFFFFFF);
        // Valeur
        String vs = String.valueOf(val);
        this.fontRendererObj.drawString(vs, x + w + 4, y + 2, 0xFFCCCCCC);
    }

    /** Dessine l'aperçu crosshair avec une couleur explicite (pour le rainbow) */
    private void drawCrosshairPreviewColor(int cx, int cy, net.minecraft.client.settings.GameSettings gs, int color) {
        int size = Math.max(1, gs.crosshairSize);
        int thickness = Math.max(1, gs.crosshairThickness);
        int gap = Math.max(0, gs.crosshairGap);
        int type = gs.crosshairType;
        if ((color & 0xFF000000) == 0) color |= 0xFF000000;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        if (type == 0) {
            // Vanilla : croix blanche (+ couleur si modifiée)
            int c0 = (gs.crosshairColor == 0xFFFFFFFF || gs.crosshairColor == 0) ? 0xFFFFFFFF : color;
            drawRect(cx - size, cy - 1, cx + size, cy + 1, c0);
            drawRect(cx - 1, cy - size, cx + 1, cy + size, c0);
        } else if (type == 1) {
            // CS:GO : 4 branches + gap + épaisseur
            int half = Math.max(1, thickness / 2);
            drawRect(cx - half, cy - size - gap, cx + half, cy - gap, color);
            drawRect(cx - half, cy + gap,        cx + half, cy + size + gap, color);
            drawRect(cx - size - gap, cy - half, cx - gap, cy + half, color);
            drawRect(cx + gap,        cy - half, cx + size + gap, cy + half, color);
        } else if (type == 2) {
            // Point
            drawRect(cx - thickness, cy - thickness, cx + thickness, cy + thickness, color);
        }

        GlStateManager.disableBlend();
    }

    private void drawCrosshairPreview(int cx, int cy, net.minecraft.client.settings.GameSettings gs) {
        int col = gs.crosshairColor;
        if ((col & 0xFF000000) == 0) col |= 0xFF000000;
        if (gs.crosshairRainbow) {
            float hueR = (System.currentTimeMillis() % 3000L) / 3000.0f;
            col = 0xFF000000 | (java.awt.Color.HSBtoRGB(hueR, 1f, 1f) & 0x00FFFFFF);
        }
        drawCrosshairPreviewColor(cx, cy, gs, col);
    }

    private void drawCrosshairSectionHeader(int px, int y, int w, String label, int accent) {
        drawRect(px + 6, y + 4, px + 9, y + 12, accent);
        this.fontRendererObj.drawString(label, px + 14, y + 3, 0xFFCCCCDD);
        int tw = this.fontRendererObj.getStringWidth(label);
        drawRect(px + 14 + tw + 4, y + 7, px + w - 6, y + 8, 0x33FFFFFF);
    }

    private void handleCrosshairEditorClick(int mouseX, int mouseY) {
        net.minecraft.client.settings.GameSettings gs = Minecraft.getMinecraft().gameSettings;
        if (crosshairEditorX == Integer.MIN_VALUE) return;
        int px = crosshairEditorX, py = crosshairEditorY;
        final int W = 320;

        // Fermer (priorité maximale)
        if (hbChEditorClose[2] != 0 && inRect(mouseX, mouseY, hbChEditorClose[0], hbChEditorClose[1], hbChEditorClose[2], hbChEditorClose[3])) {
            crosshairEditorOpen = false;
            draggingChSliderSize = draggingChSliderThick = draggingChSliderGap = false;
            return;
        }
        // Drag header (seulement si on n'est pas sur le bouton fermer)
        if (inRect(mouseX, mouseY, px, py, W - 20, 22)) {
            crosshairEditorDragging = true;
            crosshairEditorDragOffX = mouseX;
            crosshairEditorDragOffY = mouseY;
            return;
        }

        bringToFront("crosshairEditor");

        // Bouton reset (remettre le vrai crosshair vanilla Minecraft)
        if (crosshairResetBtnY > py && inRect(mouseX, mouseY, px + 8, crosshairResetBtnY, W - 16, 18)) {
            gs.crosshairType = 0;
            gs.crosshairSize = 5;
            gs.crosshairThickness = 2;
            gs.crosshairGap = 3;
            gs.crosshairColor = 0xFFFFFFFF;
            gs.crosshairRainbow = false;
            gs.crosshairUseVanillaTexture = true; // retour à la texture Minecraft originale
            UIElement cw = ui.get("crosshair"); if (cw != null) cw.setEnabled(false);
            gs.saveOptions();
            return;
        }

        // Style
        if (hbChStyleVanilla[2] != 0 && inRect(mouseX, mouseY, hbChStyleVanilla[0], hbChStyleVanilla[1], hbChStyleVanilla[2], hbChStyleVanilla[3])) {
            gs.crosshairType = 0;
            // Selecting vanilla: enable customizable vanilla crosshair rendering (not the original texture)
            gs.crosshairUseVanillaTexture = false;
            UIElement cw = ui.get("crosshair"); if (cw != null) cw.setEnabled(false);
            gs.saveOptions(); return;
        }
        if (hbChStyleCS[2] != 0 && inRect(mouseX, mouseY, hbChStyleCS[0], hbChStyleCS[1], hbChStyleCS[2], hbChStyleCS[3])) {
            gs.crosshairType = 1;
            // Non-vanilla styles always use widget rendering
            gs.crosshairUseVanillaTexture = false;
            UIElement cw = ui.get("crosshair"); if (cw != null) cw.setEnabled(true);
            gs.saveOptions(); return;
        }
        if (hbChStyleDot[2] != 0 && inRect(mouseX, mouseY, hbChStyleDot[0], hbChStyleDot[1], hbChStyleDot[2], hbChStyleDot[3])) {
            gs.crosshairType = 2;
            gs.crosshairUseVanillaTexture = false;
            UIElement cw = ui.get("crosshair"); if (cw != null) cw.setEnabled(true);
            gs.saveOptions(); return;
        }

        // Slider Taille
        if (hbChSliderSize[2] != 0 && inRect(mouseX, mouseY, hbChSliderSize[0], hbChSliderSize[1], hbChSliderSize[2], hbChSliderSize[3] + 4)) {
            int rel = mouseX - hbChSliderSize[0];
            gs.crosshairSize = Math.max(1, Math.min(20, 1 + (int)(rel / (float) hbChSliderSize[2] * 19)));
            gs.crosshairUseVanillaTexture = false;
            draggingChSliderSize = true;
            gs.saveOptions(); return;
        }
        // Slider Épaisseur
        if (hbChSliderThick[2] != 0 && inRect(mouseX, mouseY, hbChSliderThick[0], hbChSliderThick[1], hbChSliderThick[2], hbChSliderThick[3] + 4)) {
            int rel = mouseX - hbChSliderThick[0];
            gs.crosshairThickness = Math.max(1, Math.min(10, 1 + (int)(rel / (float) hbChSliderThick[2] * 9)));
            gs.crosshairUseVanillaTexture = false;
            draggingChSliderThick = true;
            gs.saveOptions(); return;
        }
        // Slider Gap
        if (hbChSliderGap[2] != 0 && inRect(mouseX, mouseY, hbChSliderGap[0], hbChSliderGap[1], hbChSliderGap[2], hbChSliderGap[3] + 4)) {
            int rel = mouseX - hbChSliderGap[0];
            gs.crosshairGap = Math.max(0, Math.min(15, (int)(rel / (float) hbChSliderGap[2] * 15)));
            gs.crosshairUseVanillaTexture = false;
            draggingChSliderGap = true;
            gs.saveOptions(); return;
        }

        handleCrosshairColorSliderClick(mouseX, mouseY, gs, px, py, W);

        // Bouton Blanc
        if (hbPreviewReset[2] != 0 && inRect(mouseX, mouseY, hbPreviewReset[0], hbPreviewReset[1], hbPreviewReset[2], hbPreviewReset[3])) {
            gs.crosshairColor = 0xFFFFFFFF;
            gs.crosshairUseVanillaTexture = false;
            gs.saveOptions(); return;
        }

        // Bouton Rainbow
        if (hbChRainbow[2] != 0 && inRect(mouseX, mouseY, hbChRainbow[0], hbChRainbow[1], hbChRainbow[2], hbChRainbow[3])) {
            gs.crosshairRainbow = !gs.crosshairRainbow;
            gs.crosshairUseVanillaTexture = false;
            gs.saveOptions(); return;
        }
    }

    /** Gère le clic/drag sur les sliders RGBA du crosshair */
    private void handleCrosshairColorSliderClick(int mouseX, int mouseY, net.minecraft.client.settings.GameSettings gs, int px, int py, int W) {
        // Recalcul de la Y de début des sliders couleur (identique à drawCrosshairEditor)
        int y = py + 26;
        y += 16 + 24; // section style + boutons style
        // options (taille toujours + épaisseur/gap si CS:GO) + aperçu
        int leftH = 12 + 18; // taille
        if (gs.crosshairType == 1) leftH += 12 + 18 + 12 + 18;
        int pvBottom = y + Math.max(leftH, 70 + 12);
        y = pvBottom;
        y += 16; // section couleur header
        int csldX = px + 30;
        int csldW = W - 36;
        for (int ch = 0; ch < 4; ch++) {
            if (inRect(mouseX, mouseY, csldX, y, csldW + 20, 14)) {
                int rel = Math.max(0, Math.min(csldW, mouseX - csldX));
                int val = (int)(rel / (float) csldW * 255);
                int col = gs.crosshairColor;
                if ((col & 0xFF000000) == 0) col |= 0xFF000000;
                int A = (col >> 24) & 0xFF;
                int R = (col >> 16) & 0xFF;
                int G = (col >> 8) & 0xFF;
                int B = col & 0xFF;
                if (ch == 0) R = val;
                else if (ch == 1) G = val;
                else if (ch == 2) B = val;
                else A = val;
                gs.crosshairColor = (A << 24) | (R << 16) | (G << 8) | B;
                gs.crosshairUseVanillaTexture = false;
                gs.saveOptions();
                // Sync widget crosshair si présent
                UIElement cw = ui.get("crosshair");
                if (cw instanceof BaseWidget) ((BaseWidget) cw).setColor(gs.crosshairColor);
                draggingChColor = ch;
                return;
            }
            y += 18;
        }
    }
}
