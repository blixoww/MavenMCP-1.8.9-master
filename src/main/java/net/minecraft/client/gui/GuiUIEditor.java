package net.minecraft.client.gui;

import net.minecraft.client.gui.ui.*;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GuiUIEditor extends GuiScreen {
    private final GuiScreen parent;
    private final UIManager ui = UIManager.getInstance();
    private UIElement selected = null;
    private int dragOffsetX, dragOffsetY;
    private final String preselectId;

    private boolean colorEditorOpen = false;
    private int r = 255, g = 255, b = 255, a = 255;

    private int widgetListX = Integer.MIN_VALUE;
    private int widgetListY = 32;
    private int widgetListW_dyn = 150; 
    private final int widgetListHmax = 240;
    private int widgetListScroll = 0; 
    private String searchFilter = "";
    private boolean searchFocused = false;
    
    private float sidebarAnim = 0.0f;
    private float widgetListAnim = 0.0f;

    private final int[] hbWidgetListClose = new int[4];
    private final List<String> panelOrder = new ArrayList<String>() {{
        add("widgetList");
        add("sidebar");
        add("colorEditor");
    }};

    // Snapping lines state
    private int snapLineX = -1, snapLineY = -1;

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
        this.buttonList.add(new GuiButton(200, this.width / 2 - 100, this.height - 28, 200, 20, I18n.format("gui.done")));
        UIManager.getInstance().setEditorActive(true);
        
        if (this.width < 400) {
            widgetListW_dyn = Math.max(100, this.width - 24);
            widgetListX = 4;
            sidebarW = Math.max(160, this.width - 8);
        } else {
            widgetListW_dyn = GuiRenderUtils.clamp(this.width / 5, 140, 180);
            widgetListX = this.width - widgetListW_dyn - 28;
            sidebarW = 240; 
        }
        widgetListY = 32;
        sidebarX = 10;
        sidebarY = 32;
        colorEditorX = this.width / 2 - 120;
        colorEditorY = this.height / 2 - 80;

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

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        drawGrid(8, 0x05FFFFFF);

        // Draw snapping lines
        if (snapLineX != -1) Gui.drawRect(snapLineX, 0, snapLineX + 1, this.height, 0xAA2A7FFF);
        if (snapLineY != -1) Gui.drawRect(0, snapLineY, this.width, snapLineY + 1, 0xAA2A7FFF);

        for (UIElement e : ui.all()) {
            e.render(mouseX, mouseY, partialTicks);
            if (selected == e) {
                GuiRenderUtils.drawSelectionHalo(e.getX(), e.getY(), e.getWidth(), e.getHeight(), 0xFF2A7FFF);
            }
        }

        sidebarAnim = GuiRenderUtils.lerp(sidebarAnim, (selected != null ? 1.0f : 0.0f), 0.15f);
        widgetListAnim = GuiRenderUtils.lerp(widgetListAnim, (widgetListX > -400 ? 1.0f : 0.0f), 0.15f);

        for (String panel : panelOrder) {
            if ("widgetList".equals(panel) && widgetListAnim > 0.01f) drawWidgetList();
            else if ("sidebar".equals(panel) && sidebarAnim > 0.01f) drawWidgetSidebar();
            else if ("colorEditor".equals(panel) && colorEditorOpen && selected instanceof BaseWidget) drawColorEditor();
        }

        int titleW = 160, titleH = 22;
        int tx = (this.width - titleW) / 2, ty = 4;
        GuiRenderUtils.drawRoundedPanel(tx, ty, titleW, titleH, 0xEE0D0D15, 0xFF151525, 0, 0xFF2A7FFF);
        String title = "EDITEUR D'INTERFACE";
        this.fontRendererObj.drawStringWithShadow(title, tx + (float) (titleW - this.fontRendererObj.getStringWidth(title)) / 2, ty + 7, 0xFF8EC8FF);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawWidgetList() {
        if (widgetListX == Integer.MIN_VALUE) widgetListX = this.width - widgetListW_dyn - 20;
        int px = widgetListX, py = widgetListY;
        int rowH = 20;
        
        List<UIElement> items = ui.all().stream()
                .filter(e -> searchFilter.isEmpty() || friendlyName(e.getId()).toLowerCase().contains(searchFilter.toLowerCase()))
                .collect(Collectors.toList());
                
        int w = widgetListW_dyn + 24;
        int maxH = Math.min(widgetListHmax, this.height - 80);
        int maxVisible = (maxH - 48) / rowH;
        int h = 48 + Math.min(maxVisible, Math.max(1, items.size())) * rowH;

        if (widgetListScroll > Math.max(0, items.size() - maxVisible)) widgetListScroll = Math.max(0, items.size() - maxVisible);

        GuiRenderUtils.drawRoundedPanel(px, py, w, h, 0xEE0D0D15, 0xFF151525, 22, 0xFF2A7FFF);
        this.fontRendererObj.drawStringWithShadow("Widgets", px + 10, py + 7, 0xFFAAD4FF);

        int ccx = px + w - 16, ccy = py + 5, ccs = 11;
        GuiRenderUtils.drawCloseButton(ccx, ccy, ccs, inRect(lastMouseX, lastMouseY, ccx, ccy, ccs, ccs));
        this.fontRendererObj.drawString("✕", ccx + 2, ccy + 1, 0xFFFFFFFF);
        hbWidgetListClose[0] = ccx; hbWidgetListClose[1] = ccy; hbWidgetListClose[2] = ccs; hbWidgetListClose[3] = ccs;

        int sbY = py + 24;
        Gui.drawRect(px + 6, sbY, px + w - 6, sbY + 16, searchFocused ? 0x44FFFFFF : 0x22FFFFFF);
        GuiRenderUtils.drawRectOutline(px + 6, sbY, w - 12, 16, searchFocused ? 0xFF2A7FFF : 0x44FFFFFF);
        if (searchFilter.isEmpty() && !searchFocused) {
            this.fontRendererObj.drawString("Rechercher...", px + 22, sbY + 4, 0x88AAAAAA);
        } else {
            String disp = searchFilter + (searchFocused && (System.currentTimeMillis() / 500 % 2 == 0) ? "_" : "");
            this.fontRendererObj.drawString(disp, px + 22, sbY + 4, 0xFFFFFFFF);
        }
        GuiRenderUtils.drawSearchIcon(px + 10, sbY + 4, 0xFFAAAAAA);

        int y = py + 44;
        if (items.isEmpty()) {
            this.fontRendererObj.drawString("Aucun résultat", px + 10, y + 4, 0xFF666666);
        } else {
            int end = Math.min(widgetListScroll + maxVisible, items.size());
            for (int i = widgetListScroll; i < end; i++) {
                UIElement e = items.get(i);
                boolean isSel = selected == e;
                if (isSel) Gui.drawRect(px + 1, y, px + w - 1, y + rowH, 0x332A7FFF);
                else if (inRect(lastMouseX, lastMouseY, px, y, w, rowH)) Gui.drawRect(px + 1, y, px + w - 1, y + rowH, 0x11FFFFFF);
                
                int dotCol = e.isEnabled() ? 0xFF44EE77 : 0xFF666666;
                Gui.drawRect(px + 8, y + 8, px + 12, y + 12, dotCol);
                
                String name = friendlyName(e.getId());
                this.fontRendererObj.drawStringWithShadow(name, px + 18, y + 6, isSel ? 0xFFFFFFFF : 0xFFCCCCCC);
                
                drawToggle(px + w - 34, y + 4, e.isEnabled());
                y += rowH;
            }
        }
    }

    private void drawWidgetSidebar() {
        if (selected == null) return;
        int px = (int) (sidebarX - (1.0f - sidebarAnim) * 100);
        int py = sidebarY, w = sidebarW;
        
        int h = 135;
        if (selected instanceof BaseWidget) {
            BaseWidget bw = (BaseWidget) selected;
            if (bw instanceof KeyStrokeWidget) h += 40 + ((KeyStrokeWidget) bw).getKeyCount() * 18;
            if (bw instanceof PotionStatusWidget) h += 60;
            if (bw instanceof ArmorGroupWidget) h += 60;
            if (bw instanceof ToggleSneakWidget || bw instanceof ToggleSprintWidget) h += 40;
        }
        h += 24; 
        sidebarH = h;
        
        GuiRenderUtils.drawRoundedPanel(px, py, w, sidebarH, 0xEE0D0D15, 0xFF151525, 24, 0xFF2A7FFF);
        this.fontRendererObj.drawStringWithShadow(friendlyName(selected.getId()), px + 10, py + 8, 0xFFAAD4FF);

        int ccx = px + w - 16, ccy = py + 6, ccs = 11;
        GuiRenderUtils.drawCloseButton(ccx, ccy, ccs, inRect(lastMouseX, lastMouseY, ccx, ccy, ccs, ccs));
        this.fontRendererObj.drawString("✕", ccx + 2, ccy + 1, 0xFFFFFFFF);
        hbSidebarClose[0] = ccx; hbSidebarClose[1] = ccy; hbSidebarClose[2] = ccs; hbSidebarClose[3] = ccs;

        int y = py + 32;
        GuiRenderUtils.drawSectionHeader(this.fontRendererObj, px, y, w, "Général", 0xFF2A7FFF);
        y += 18;

        this.fontRendererObj.drawStringWithShadow("Activé", px + 12, y + 2, 0xFFCCCCCC);
        drawToggle(px + w - 40, y, selected.isEnabled());
        y += 20;

        if (selected instanceof BaseWidget) {
            BaseWidget bw = (BaseWidget) selected;
            this.fontRendererObj.drawStringWithShadow("Mode Rainbow", px + 12, y + 2, 0xFFCCCCCC);
            drawToggle(px + w - 40, y, bw.isRGBMode());
            hbRainbowGeneral[0] = px + w - 40; hbRainbowGeneral[1] = y; hbRainbowGeneral[2] = 28; hbRainbowGeneral[3] = 12;
            y += 20;

            boolean align = Boolean.TRUE.equals(bw.getPropOrDefault("snapGrid", false));
            this.fontRendererObj.drawStringWithShadow("Aligner (Intelligent)", px + 12, y + 2, 0xFFCCCCCC);
            drawToggle(px + w - 40, y, align);
            hbAlignGrid[0] = px + w - 40; hbAlignGrid[1] = y; hbAlignGrid[2] = 28; hbAlignGrid[3] = 12;
            y += 20;

            this.fontRendererObj.drawStringWithShadow("Couleur", px + 12, y + 2, 0xFFCCCCCC);
            int cpX = px + w - 40, cpY = y, cpW = 28, cpH = 12;
            GuiRenderUtils.drawCheckerboard(cpX, cpY, cpW, cpH, 4, 0xFF999999, 0xFF666666);
            Gui.drawRect(cpX, cpY, cpX + cpW, cpY + cpH, bw.getColor());
            GuiRenderUtils.drawRectOutline(cpX, cpY, cpW, cpH, 0xAAFFFFFF);
            hbColorPreview[0] = cpX; hbColorPreview[1] = cpY; hbColorPreview[2] = cpW; hbColorPreview[3] = cpH;
            y += 20;

            // Specifics
            if (bw instanceof KeyStrokeWidget) {
                y += 4; GuiRenderUtils.drawSectionHeader(this.fontRendererObj, px, y, w, "Keystrokes", 0xFF8A2BE2); y += 18;
                KeyStrokeWidget ks = (KeyStrokeWidget) bw;
                for (int i = 0; i < ks.getKeyCount(); i++) {
                    String label = ks.getKeyLabel(i);
                    boolean vis = Boolean.TRUE.equals(bw.getPropOrDefault("showKey" + i, true));
                    this.fontRendererObj.drawStringWithShadow(label, px + 12, y + 2, vis ? 0xFFCCCCCC : 0xFF555555);
                    drawToggle(px + w - 40, y, vis);
                    hbKeyToggle[i][0] = px + w - 40; hbKeyToggle[i][1] = y; hbKeyToggle[i][2] = 28; hbKeyToggle[i][3] = 12;
                    y += 18;
                }
                boolean spaceRainbow = Boolean.TRUE.equals(bw.getPropOrDefault("showSpaceRainbow", false));
                this.fontRendererObj.drawStringWithShadow("Rainbow Espace", px + 12, y + 2, 0xFFCCCCCC);
                drawToggle(px + w - 40, y, spaceRainbow);
                hbSpaceRainbow[0] = px + w - 40; hbSpaceRainbow[1] = y; hbSpaceRainbow[2] = 28; hbSpaceRainbow[3] = 12;
                y += 20;
            } else if (bw instanceof PotionStatusWidget) {
                y += 4; GuiRenderUtils.drawSectionHeader(this.fontRendererObj, px, y, w, "Potions", 0xFF2ECC71); y += 18;
                boolean showDur = Boolean.TRUE.equals(bw.getPropOrDefault("showDuration", true));
                this.fontRendererObj.drawStringWithShadow("Afficher durée", px + 12, y + 2, 0xFFCCCCCC);
                drawToggle(px + w - 40, y, showDur);
                hbPotionDur[0] = px + w - 40; hbPotionDur[1] = y; hbPotionDur[2] = 28; hbPotionDur[3] = 12;
                y += 20;
                boolean showIcons = Boolean.TRUE.equals(bw.getPropOrDefault("showIcons", false));
                this.fontRendererObj.drawStringWithShadow("Afficher icônes", px + 12, y + 2, 0xFFCCCCCC);
                drawToggle(px + w - 40, y, showIcons);
                hbPotionIcons[0] = px + w - 40; hbPotionIcons[1] = y; hbPotionIcons[2] = 28; hbPotionIcons[3] = 12;
                y += 20;
            } else if (bw instanceof ArmorGroupWidget) {
                y += 4; GuiRenderUtils.drawSectionHeader(this.fontRendererObj, px, y, w, "Armure", 0xFFE67E22); y += 18;
                String layout = String.valueOf(bw.getPropOrDefault("layout", "horizontal"));
                this.fontRendererObj.drawStringWithShadow("Disposition Verticale", px + 12, y + 2, 0xFFCCCCCC);
                drawToggle(px + w - 40, y, "vertical".equals(layout));
                hbArmorLayout[0] = px + w - 40; hbArmorLayout[1] = y; hbArmorLayout[2] = 28; hbArmorLayout[3] = 12;
                y += 20;
                boolean dispPct = Boolean.TRUE.equals(bw.getPropOrDefault("displayPercent", true));
                this.fontRendererObj.drawStringWithShadow("Afficher en %", px + 12, y + 2, 0xFFCCCCCC);
                drawToggle(px + w - 40, y, dispPct);
                hbArmorPercent[0] = px + w - 40; hbArmorPercent[1] = y; hbArmorPercent[2] = 28; hbArmorPercent[3] = 12;
                y += 20;
            }
        }

        int rbtnY = py + sidebarH - 20;
        int btnW = (w - 30) / 2;
        // Reset Position
        GuiRenderUtils.drawStyledButton(px + 10, rbtnY, btnW, 14, 0xFF1A1A2A, 0xFF33334A, inRect(lastMouseX, lastMouseY, px + 10, rbtnY, btnW, 14));
        this.fontRendererObj.drawString("Pos. Default", px + 10 + (btnW - fontRendererObj.getStringWidth("Pos. Default")) / 2, rbtnY + 3, 0xFFAAAAAA);
        hbResetPosition[0] = px + 10; hbResetPosition[1] = rbtnY; hbResetPosition[2] = btnW; hbResetPosition[3] = 14;
        
        // Reset Color (Blanc)
        GuiRenderUtils.drawStyledButton(px + 20 + btnW, rbtnY, btnW, 14, 0xFF1A1A2A, 0xFF33334A, inRect(lastMouseX, lastMouseY, px + 20 + btnW, rbtnY, btnW, 14));
        this.fontRendererObj.drawString("Blanc", px + 20 + btnW + (btnW - fontRendererObj.getStringWidth("Blanc")) / 2, rbtnY + 3, 0xFFAAAAAA);
        hbResetColor[0] = px + 20 + btnW; hbResetColor[1] = rbtnY; hbResetColor[2] = btnW; hbResetColor[3] = 14;
    }

    private void drawColorEditor() {
        int px = colorEditorX, py = colorEditorY;
        int w = 240, h = 160;
        cePanelW = w; cePanelH = h;
        GuiRenderUtils.drawRoundedPanel(px, py, w, h, 0xF00D0D15, 0xFF151525, 22, 0xFF9932CC);
        this.fontRendererObj.drawStringWithShadow("Éditeur de couleur", px + 10, py + 7, 0xFFCC88FF);
        int ccx = px + w - 16, ccy = py + 5, ccs = 11;
        GuiRenderUtils.drawCloseButton(ccx, ccy, ccs, inRect(lastMouseX, lastMouseY, ccx, ccy, ccs, ccs));
        this.fontRendererObj.drawString("✕", ccx + 2, ccy + 1, 0xFFFFFFFF);
        hbColorClose[0] = ccx; hbColorClose[1] = ccy; hbColorClose[2] = ccs; hbColorClose[3] = ccs;
        int specX = px + 10, specY = py + 28, specW = 120, specH = 100;
        ceSpecW = specW; ceSpecH = specH;
        for (int sx = 0; sx < specW; sx++) {
            float hue = sx / (float)specW;
            for (int sy = 0; sy < specH; sy++) {
                float val = 1.0f - (sy / (float)specH);
                Gui.drawRect(specX + sx, specY + sy, specX + sx + 1, specY + sy + 1, 0xFF000000 | java.awt.Color.HSBtoRGB(hue, 1.0f, val));
            }
        }
        GuiRenderUtils.drawRectOutline(specX, specY, specW, specH, 0x44FFFFFF);
        int sldX = specX + specW + 15, sldW = w - specW - 35;
        drawChannelSlider(sldX, specY, "R", r, 0xFFEE4444);
        drawChannelSlider(sldX, specY + 22, "G", g, 0xFF44EE44);
        drawChannelSlider(sldX, specY + 44, "B", b, 0xFF4444EE);
        drawChannelSlider(sldX, specY + 66, "A", a, 0xFF888888);
        int pvY = specY + 86;
        GuiRenderUtils.drawCheckerboard(sldX, pvY, sldW, 14, 4, 0xFF999999, 0xFF666666);
        Gui.drawRect(sldX, pvY, sldX + sldW, pvY + 14, (a << 24) | (r << 16) | (g << 8) | b);
        GuiRenderUtils.drawRectOutline(sldX, pvY, sldW, 14, 0xFFFFFFFF);
    }

    private void drawChannelSlider(int x, int y, String label, int val, int color) {
        this.fontRendererObj.drawStringWithShadow(label, x - 10, y + 2, color);
        int sw = cePanelW - ceSpecW - 35;
        Gui.drawRect(x, y + 3, x + sw, y + 9, 0xFF111122);
        int fill = (int)(val / 255f * sw);
        Gui.drawRect(x, y + 3, x + fill, y + 9, color);
        Gui.drawRect(x + fill - 1, y, x + fill + 1, y + 12, 0xFFFFFFFF);
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
        searchFocused = false;
        for (int i = panelOrder.size() - 1; i >= 0; i--) {
            String p = panelOrder.get(i);
            if ("colorEditor".equals(p) && colorEditorOpen && handleColorClick(mx, my)) return;
            if ("sidebar".equals(p) && selected != null && handleSidebarClick(mx, my)) return;
            if ("widgetList".equals(p) && handleWidgetListClick(mx, my)) return;
        }

        for (UIElement e : ui.all()) {
            if (e.isEnabled() && e.containsPoint(mx, my)) {
                selected = e; isDraggingWidget = true;
                dragOffsetX = mx - e.getX(); dragOffsetY = my - e.getY();
                bringToFront("sidebar"); return;
            }
        }
        selected = null;
    }

    private boolean handleWidgetListClick(int mx, int my) {
        int w = widgetListW_dyn + 24;
        List<UIElement> items = ui.all().stream()
                .filter(e -> searchFilter.isEmpty() || friendlyName(e.getId()).toLowerCase().contains(searchFilter.toLowerCase()))
                .collect(Collectors.toList());
        int maxH = Math.min(widgetListHmax, this.height - 80);
        int maxVisible = (maxH - 48) / 20;
        int h = 48 + Math.min(maxVisible, Math.max(1, items.size())) * 20;
        
        if (!inRect(mx, my, widgetListX, widgetListY, w, h)) return false;
        bringToFront("widgetList");
        if (inRect(mx, my, hbWidgetListClose[0], hbWidgetListClose[1], hbWidgetListClose[2], hbWidgetListClose[3])) {
            widgetListX = -500; return true;
        }
        if (inRect(mx, my, widgetListX, widgetListY, w, 22)) {
            widgetListDragging = true; widgetListDragOffsetX = mx; widgetListDragOffsetY = my; return true;
        }
        if (inRect(mx, my, widgetListX + 6, widgetListY + 24, w - 12, 16)) {
            searchFocused = true; return true;
        }
        
        int y = widgetListY + 44;
        for (int i = widgetListScroll; i < Math.min(widgetListScroll + maxVisible, items.size()); i++) {
            UIElement e = items.get(i);
            if (inRect(mx, my, widgetListX + w - 34, y + 4, 28, 12)) {
                e.setEnabled(!e.isEnabled()); ui.saveConfig(); return true;
            }
            if (inRect(mx, my, widgetListX, y, w - 40, 20)) {
                selected = e; bringToFront("sidebar"); return true;
            }
            y += 20;
        }
        return true;
    }

    private boolean handleSidebarClick(int mx, int my) {
        if (!inRect(mx, my, sidebarX, sidebarY, sidebarW, sidebarH)) return false;
        bringToFront("sidebar");
        if (inRect(mx, my, hbSidebarClose[0], hbSidebarClose[1], hbSidebarClose[2], hbSidebarClose[3])) {
            selected = null; return true;
        }
        if (inRect(mx, my, sidebarX, sidebarY, sidebarW, 22)) {
            sidebarDragging = true; sidebarDragOffsetX = mx; sidebarDragOffsetY = my; return true;
        }
        
        int y = sidebarY + 50;
        if (inRect(mx, my, sidebarX + sidebarW - 40, y, 28, 12)) {
            selected.setEnabled(!selected.isEnabled()); ui.saveConfig(); return true;
        }
        
        if (selected instanceof BaseWidget) {
            BaseWidget bw = (BaseWidget) selected;
            y += 20;
            if (inRect(mx, my, hbRainbowGeneral[0], hbRainbowGeneral[1], hbRainbowGeneral[2], hbRainbowGeneral[3])) {
                bw.setRGBMode(!bw.isRGBMode()); ui.saveConfig(); return true;
            }
            y += 20;
            if (inRect(mx, my, hbAlignGrid[0], hbAlignGrid[1], hbAlignGrid[2], hbAlignGrid[3])) {
                boolean cur = Boolean.TRUE.equals(bw.getPropOrDefault("snapGrid", false));
                bw.setProp("snapGrid", !cur); ui.saveConfig(); return true;
            }
            y += 20;
            if (inRect(mx, my, hbColorPreview[0], hbColorPreview[1], hbColorPreview[2], hbColorPreview[3])) {
                colorEditorOpen = true; bringToFront("colorEditor"); 
                int c = bw.getColor();
                a = (c >> 24) & 0xFF; r = (c >> 16) & 0xFF; g = (c >> 8) & 0xFF; b = c & 0xFF;
                return true;
            }
            
            if (bw instanceof KeyStrokeWidget) {
                KeyStrokeWidget ks = (KeyStrokeWidget) bw;
                y += 22; 
                for (int i = 0; i < ks.getKeyCount(); i++) {
                    if (inRect(mx, my, hbKeyToggle[i][0], hbKeyToggle[i][1], hbKeyToggle[i][2], hbKeyToggle[i][3])) {
                        boolean vis = Boolean.TRUE.equals(bw.getPropOrDefault("showKey" + i, true));
                        bw.setProp("showKey" + i, !vis); ui.saveConfig(); return true;
                    }
                    y += 18;
                }
                if (inRect(mx, my, hbSpaceRainbow[0], hbSpaceRainbow[1], hbSpaceRainbow[2], hbSpaceRainbow[3])) {
                    boolean cur = Boolean.TRUE.equals(bw.getPropOrDefault("showSpaceRainbow", false));
                    bw.setProp("showSpaceRainbow", !cur); ui.saveConfig(); return true;
                }
            } else if (bw instanceof PotionStatusWidget) {
                y += 22;
                if (inRect(mx, my, hbPotionDur[0], hbPotionDur[1], hbPotionDur[2], hbPotionDur[3])) {
                    boolean cur = Boolean.TRUE.equals(bw.getPropOrDefault("showDuration", true));
                    bw.setProp("showDuration", !cur); ui.saveConfig(); return true;
                }
                y += 20;
                if (inRect(mx, my, hbPotionIcons[0], hbPotionIcons[1], hbPotionIcons[2], hbPotionIcons[3])) {
                    boolean cur = Boolean.TRUE.equals(bw.getPropOrDefault("showIcons", false));
                    bw.setProp("showIcons", !cur); ui.saveConfig(); return true;
                }
            } else if (bw instanceof ArmorGroupWidget) {
                y += 22;
                if (inRect(mx, my, hbArmorLayout[0], hbArmorLayout[1], hbArmorLayout[2], hbArmorLayout[3])) {
                    String cur = String.valueOf(bw.getPropOrDefault("layout", "horizontal"));
                    bw.setProp("layout", "horizontal".equals(cur) ? "vertical" : "horizontal"); ui.saveConfig(); return true;
                }
                y += 20;
                if (inRect(mx, my, hbArmorPercent[0], hbArmorPercent[1], hbArmorPercent[2], hbArmorPercent[3])) {
                    boolean cur = Boolean.TRUE.equals(bw.getPropOrDefault("displayPercent", true));
                    bw.setProp("displayPercent", !cur); ui.saveConfig(); return true;
                }
            }
        }
        if (inRect(mx, my, hbResetPosition[0], hbResetPosition[1], hbResetPosition[2], hbResetPosition[3])) {
            selected.setPosition(10, 10); ui.saveConfig(); return true;
        }
        if (inRect(mx, my, hbResetColor[0], hbResetColor[1], hbResetColor[2], hbResetColor[3])) {
            selected.setColor(0xFFFFFFFF);
            if (selected instanceof BaseWidget) ((BaseWidget) selected).setRGBMode(false);
            ui.saveConfig(); return true;
        }
        return true;
    }

    private boolean handleColorClick(int mx, int my) {
        if (!inRect(mx, my, colorEditorX, colorEditorY, cePanelW, cePanelH)) return false;
        bringToFront("colorEditor");
        if (inRect(mx, my, hbColorClose[0], hbColorClose[1], hbColorClose[2], hbColorClose[3])) {
            colorEditorOpen = false; return true;
        }
        if (inRect(mx, my, colorEditorX, colorEditorY, cePanelW, 22)) {
            colorEditorDragging = true; colorEditorDragOffsetX = mx; colorEditorDragOffsetY = my; return true;
        }
        int specX = colorEditorX + 10, specY = colorEditorY + 28;
        if (inRect(mx, my, specX, specY, ceSpecW, ceSpecH)) {
            draggingSpectrum = true; updateColorFromSpectrum(mx, my); return true;
        }
        int sldX = specX + ceSpecW + 15, sldW = cePanelW - ceSpecW - 35;
        for (int i = 0; i < 4; i++) {
            if (inRect(mx, my, sldX, specY + i * 22, sldW, 12)) {
                draggingSlider = i; updateColorFromSlider(mx); return true;
            }
        }
        return true;
    }

    private void updateColorFromSpectrum(int mx, int my) {
        float hue = GuiRenderUtils.clamp(mx - (colorEditorX + 10), 0, ceSpecW) / (float)ceSpecW;
        float val = 1.0f - GuiRenderUtils.clamp(my - (colorEditorY + 28), 0, ceSpecH) / (float)ceSpecH;
        int rgb = java.awt.Color.HSBtoRGB(hue, 1.0f, val);
        r = (rgb >> 16) & 0xFF; g = (rgb >> 8) & 0xFF; b = rgb & 0xFF;
        applyColor();
    }

    private void updateColorFromSlider(int mx) {
        int sldX = colorEditorX + 10 + ceSpecW + 15, sldW = cePanelW - ceSpecW - 35;
        int val = (int)(GuiRenderUtils.clamp(mx - sldX, 0, sldW) / (float)sldW * 255);
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
    protected void mouseClickMove(int mx, int my, int btn, long time) {
        if (isDraggingWidget && selected != null) {
            int nx = mx - dragOffsetX;
            int ny = my - dragOffsetY;
            
            snapLineX = -1; snapLineY = -1;

            if (selected instanceof BaseWidget && Boolean.TRUE.equals(((BaseWidget) selected).getPropOrDefault("snapGrid", false)) && !isShiftKeyDown()) {
                int threshold = 4;
                int w = selected.getWidth();
                int h = selected.getHeight();
                
                // Screen Snapping
                if (Math.abs(nx) < threshold) { nx = 0; snapLineX = 0; }
                else if (Math.abs(nx + w - this.width) < threshold) { nx = this.width - w; snapLineX = this.width; }
                else if (Math.abs(nx + w/2 - this.width/2) < threshold) { nx = this.width/2 - w/2; snapLineX = this.width/2; }
                
                if (Math.abs(ny) < threshold) { ny = 0; snapLineY = 0; }
                else if (Math.abs(ny + h - this.height) < threshold) { ny = this.height - h; snapLineY = this.height; }
                else if (Math.abs(ny + h/2 - this.height/2) < threshold) { ny = this.height/2 - h/2; snapLineY = this.height/2; }
                
                // Widget Snapping
                for (UIElement other : ui.all()) {
                    if (other == selected || !other.isEnabled()) continue;
                    int ox = other.getX(), oy = other.getY();
                    int ow = other.getWidth(), oh = other.getHeight();
                    
                    // X-axis alignment
                    if (Math.abs(nx - ox) < threshold) { nx = ox; snapLineX = ox; }
                    else if (Math.abs(nx + w - (ox + ow)) < threshold) { nx = ox + ow - w; snapLineX = ox + ow; }
                    else if (Math.abs(nx - (ox + ow)) < threshold) { nx = ox + ow; snapLineX = ox + ow; }
                    else if (Math.abs(nx + w - ox) < threshold) { nx = ox - w; snapLineX = ox; }
                    else if (Math.abs(nx + w/2 - (ox + ow/2)) < threshold) { nx = ox + ow/2 - w/2; snapLineX = ox + ow/2; }
                    
                    // Y-axis alignment
                    if (Math.abs(ny - oy) < threshold) { ny = oy; snapLineY = oy; }
                    else if (Math.abs(ny + h - (oy + oh)) < threshold) { ny = oy + oh - h; snapLineY = oy + oh; }
                    else if (Math.abs(ny - (oy + oh)) < threshold) { ny = oy + oh; snapLineY = oy + oh; }
                    else if (Math.abs(ny + h - oy) < threshold) { ny = oy - h; snapLineY = oy; }
                    else if (Math.abs(ny + h/2 - (oy + oh/2)) < threshold) { ny = oy + oh/2 - h/2; snapLineY = oy + oh/2; }
                }
            }
            selected.setPosition(nx, ny);
        }
        if (widgetListDragging) {
            widgetListX += mx - widgetListDragOffsetX; widgetListY += my - widgetListDragOffsetY;
            widgetListDragOffsetX = mx; widgetListDragOffsetY = my;
        }
        if (sidebarDragging) {
            sidebarX += mx - sidebarDragOffsetX; sidebarY += my - sidebarDragOffsetY;
            sidebarDragOffsetX = mx; sidebarDragOffsetY = my;
        }
        if (colorEditorDragging) {
            colorEditorX += mx - colorEditorDragOffsetX; colorEditorY += my - colorEditorDragOffsetY;
            colorEditorDragOffsetX = mx; colorEditorDragOffsetY = my;
        }
        if (draggingSpectrum) updateColorFromSpectrum(mx, my);
        if (draggingSlider != -1) updateColorFromSlider(mx);
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        isDraggingWidget = false; widgetListDragging = false; sidebarDragging = false;
        colorEditorDragging = false; draggingSpectrum = false; draggingSlider = -1;
        snapLineX = -1; snapLineY = -1;
        ui.saveConfig();
        super.mouseReleased(mx, my, state);
    }

    private void bringToFront(String name) {
        panelOrder.remove(name); panelOrder.add(name);
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
            case "potions": return "Potions"; case "cps": return "CPS";
            case "toggle_sneak": return "Toggle Sneak"; case "toggle_sprint": return "Toggle Sprint";
            default: return id;
        }
    }

    private void drawGrid(int step, int color) {
        for (int gx = 0; gx < this.width; gx += step) Gui.drawRect(gx, 0, gx + 1, this.height, color);
        for (int gy = 0; gy < this.height; gy += step) Gui.drawRect(0, gy, this.width, gy + 1, color);
    }

    private int lastMouseX, lastMouseY, sidebarX, sidebarY, sidebarW, sidebarH;
    private int colorEditorX, colorEditorY, cePanelW, cePanelH, ceSpecW, ceSpecH;
    private boolean widgetListDragging, sidebarDragging, colorEditorDragging, isDraggingWidget, draggingSpectrum;
    private int widgetListDragOffsetX, widgetListDragOffsetY, sidebarDragOffsetX, sidebarDragOffsetY, colorEditorDragOffsetX, colorEditorDragOffsetY;
    private int draggingSlider = -1;
    private final int[] hbSidebarClose = new int[4], hbColorClose = new int[4], hbColorPreview = new int[4], hbResetPosition = new int[4], hbResetColor = new int[4];
    private final int[] hbPotionDur = new int[4], hbPotionIcons = new int[4], hbArmorLayout = new int[4], hbArmorPercent = new int[4], hbAlignGrid = new int[4], hbRainbowGeneral = new int[4], hbSpaceRainbow = new int[4];
    private final int[][] hbKeyToggle = new int[9][4];
    private final java.util.Map<String, Float> toggleAnimMap = new java.util.HashMap<>();

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        lastMouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        lastMouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0 && inRect(lastMouseX, lastMouseY, widgetListX, widgetListY, widgetListW_dyn + 24, widgetListHmax)) {
            if (scroll > 0) widgetListScroll = Math.max(0, widgetListScroll - 1);
            else widgetListScroll++;
        }
    }
}
