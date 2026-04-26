package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.macro.MacroManager;
import net.minecraft.client.macro.MacroManager.Macro;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GuiMacros extends GuiScreen {
    
    // ── Palette de couleurs "Red Conflict" (style GuiIngameMenu) ─────────────
    private static final int C_BG           = 0xEE0A0D14;
    private static final int C_HEADER       = 0xFF141926;
    private static final int C_ACCENT       = 0xFFDC1E1E;
    private static final int C_ACCENT_DIM   = 0xFF992222;
    private static final int C_TEXT         = 0xFFE0E0E0;
    private static final int C_TEXT_MUTED   = 0xFF707880;
    private static final int C_DANGER       = 0xFFEE4444;
    private static final int C_SUCCESS      = 0xFF44EE88;

    private final GuiScreen parent;
    private final MacroManager mgr = MacroManager.INSTANCE;

    // Dimensions dynamiques
    private int px, py, pw, ph;

    // Formulaire
    private GuiTextField fieldCommand;
    private GuiTextField fieldSearch;
    private int pendingKeyCode = 0;
    private boolean listeningKey = false;
    private Integer editingIndex = null;

    // Scroll & Filtrage
    private int scrollOffset = 0;
    private static final int ROW_H = 36;
    private int listY, listH;
    private String searchFilter = "";

    // ── Animation entrée (style GuiIngameMenu) ────────────────────────────
    private float animation = 0.0f;
    private long  animLastTime = -1L;

    // Hover Animations State
    private final float[] rowHovers = new float[25];
    private float addBtnHover = 0f;
    private float cancelBtnHover = 0f;
    private float assignBtnHover = 0f;

    // Hitboxes
    private final int[] hbClose = new int[4], hbAdd = new int[4], hbAssign = new int[4], hbCancel = new int[4];
    private final int[][] hbEdit = new int[25][4], hbDel = new int[25][4], hbCard = new int[25][4];

    public GuiMacros(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        animation = 0.0f;
        animLastTime = -1L;

        // --- Calcul RESPONSIVE ---
        // Le GUI prend 70% de l'écran en largeur/hauteur, mais reste entre 320 et 420px de large
        pw = GuiRenderUtils.clamp((int)(width * 0.7f), 320, 420);
        ph = GuiRenderUtils.clamp((int)(height * 0.7f), 240, 320);
        
        // Si l'écran est vraiment trop petit, on prend tout l'espace moins une marge
        if (width < pw + 20) pw = width - 20;
        if (height < ph + 20) ph = height - 20;

        px = (width - pw) / 2;
        py = (height - ph) / 2;

        listY = py + 65;
        listH = ph - 145; // La hauteur de la liste s'adapte à la hauteur du GUI

        // Positionnement relatif des champs
        int searchW = Math.min(115, pw / 3);
        this.fieldSearch = new GuiTextField(1, fontRendererObj, px + pw - searchW - 15, py + 40, searchW, 16);
        this.fieldSearch.setMaxStringLength(32);
        this.fieldSearch.setText(searchFilter);

        int cmdW = pw - 145; // Le champ de commande s'élargit avec le GUI
        this.fieldCommand = new GuiTextField(0, fontRendererObj, px + 25, py + ph - 65, cmdW, 20);
        this.fieldCommand.setMaxStringLength(128);
        this.fieldCommand.setFocused(true);
        
        if (editingIndex != null) {
            Macro m = mgr.getMacros().get(editingIndex);
            fieldCommand.setText(m.getCommand());
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        // ── Animation entrée style GuiIngameMenu ──────────────────────────
        long now = Minecraft.getSystemTime();
        if (animLastTime != -1L) {
            float dt = (now - animLastTime) / 1000.0f;
            animation = MathHelper.clamp_float(animation + dt * 4.0f, 0.0f, 1.0f);
        }
        animLastTime = now;

        float eased = animation * animation * (3.0f - 2.0f * animation);

        // Fond obscurci animé
        this.drawRect(0, 0, this.width, this.height, (int)(eased * 155) << 24);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, (1.0f - eased) * 10, 0);

        // ── Fenêtre Principale ────────────────────────────────────────────────
        GuiRenderUtils.drawRoundedPanel(px, py, pw, ph, C_BG, C_HEADER, 32, C_ACCENT);
        
        // Titre & Compteur (Adaptation si trop étroit)
        if (pw > 250) {
            fontRendererObj.drawStringWithShadow("§lMACROS", px + 15, py + 11, 0xFFFFFFFF);
            String countInfo = mgr.getCount() + " / " + MacroManager.MAX_MACROS;
            fontRendererObj.drawString(countInfo, px + pw - 35 - fontRendererObj.getStringWidth(countInfo), py + 11, C_TEXT_MUTED);
        }

        // Bouton Fermer
        int cx = px + pw - 18, cy = py + 10;
        boolean chovClose = in(mx, my, cx - 2, cy - 2, 12, 12);
        fontRendererObj.drawString("✕", cx, cy, chovClose ? C_DANGER : 0x88FFFFFF);
        hb(hbClose, cx - 2, cy - 2, 12, 12);

        // ── Barre de recherche ──────────────────────────────────────────────
        if (pw > 300) fontRendererObj.drawString("Recherche:", px + 15, py + 44, C_TEXT_MUTED);
        
        Gui.drawRect(fieldSearch.xPosition - 1, fieldSearch.yPosition - 1, fieldSearch.xPosition + fieldSearch.width + 1, fieldSearch.yPosition + fieldSearch.getHeight() + 1, fieldSearch.isFocused() ? C_ACCENT : 0x33FFFFFF);
        Gui.drawRect(fieldSearch.xPosition, fieldSearch.yPosition, fieldSearch.xPosition + fieldSearch.width, fieldSearch.yPosition + fieldSearch.getHeight(), 0xFF05070A);
        fieldSearch.drawTextBox();
        if (fieldSearch.getText().isEmpty() && !fieldSearch.isFocused())
            fontRendererObj.drawString("...", fieldSearch.xPosition + 4, fieldSearch.yPosition + 4, 0xFF444444);

        // ── Liste des Macros filtrée ─────────────────────────────────────────
        List<Macro> allMacros = mgr.getMacros();
        List<Macro> filtered = allMacros.stream()
                .filter(m -> searchFilter.isEmpty() || m.getCommand().toLowerCase().contains(searchFilter.toLowerCase()))
                .collect(Collectors.toList());
                
        int visible = listH / ROW_H;
        if (filtered.isEmpty()) {
            String msg = searchFilter.isEmpty() ? "§8Aucune macro." : "§8Aucun résultat.";
            drawCentered(msg, px + pw / 2, listY + listH / 2 - 4, 0);
        } else {
            for (int i = 0; i < Math.min(visible, filtered.size() - scrollOffset); i++) {
                int idx = i + scrollOffset;
                if (i >= rowHovers.length) break; // Sécurité

                Macro m = filtered.get(idx);
                int rowY = listY + i * ROW_H;
                
                boolean hovCard = in(mx, my, px + 12, rowY, pw - 24, ROW_H - 6);
                rowHovers[i] = GuiRenderUtils.lerp(rowHovers[i], hovCard ? 1.0f : 0.0f, 0.2f);
                
                int rowColor = GuiRenderUtils.colorLerp(0x0AFFFFFF, 0x1AFFFFFF, rowHovers[i]);
                if (editingIndex != null && allMacros.indexOf(m) == editingIndex) rowColor = 0x33007FFF;
                
                Gui.drawRect(px + 12, rowY, px + pw - 12, rowY + ROW_H - 6, rowColor);
                if (rowHovers[i] > 0.01f) GuiRenderUtils.drawRectOutline(px + 12, rowY, pw - 24, ROW_H - 6, (int)(rowHovers[i] * 0x33) << 24 | (C_ACCENT & 0xFFFFFF));
                hb(hbCard[i], px + 12, rowY, pw - 24, ROW_H - 6);

                // Badge Touche (Adaptation largeur)
                String keyStr = m.getKeyDisplayString().toUpperCase();
                int kw = fontRendererObj.getStringWidth(keyStr) + 8;
                Gui.drawRect(px + 18, rowY + 7, px + 18 + kw, rowY + 19, 0xFF1A1F2B);
                GuiRenderUtils.drawRectOutline(px + 18, rowY + 7, kw, 12, C_ACCENT_DIM);
                fontRendererObj.drawString(keyStr, px + 18 + 4, rowY + 9, 0xFFFFAA00);

                // Commande (Tronquée dynamiquement)
                String cmd = m.getCommand();
                int maxCmdW = pw - kw - 110;
                if (fontRendererObj.getStringWidth(cmd) > maxCmdW)
                    cmd = fontRendererObj.trimStringToWidth(cmd, maxCmdW) + "...";
                fontRendererObj.drawString(cmd, px + 28 + kw, rowY + 9, C_TEXT);

                // Actions
                int bx = px + pw - 65;
                drawMiniAction(mx, my, bx, rowY + 6, "✎", C_SUCCESS, hbEdit[i]);
                drawMiniAction(mx, my, bx + 22, rowY + 6, "✕", C_DANGER, hbDel[i]);
            }
        }

        // Scrollbar responsive
        if (filtered.size() > visible) {
            int sbX = px + pw - 8, sbH = listH;
            Gui.drawRect(sbX, listY, sbX + 2, listY + sbH, 0x11FFFFFF);
            int thumbH = Math.max(15, sbH * visible / filtered.size());
            int thumbY = listY + (sbH - thumbH) * scrollOffset / (filtered.size() - visible);
            Gui.drawRect(sbX, thumbY, sbX + 2, thumbY + thumbH, C_ACCENT);
        }

        // ── Zone de Configuration ────────────────────────────────────────────
        int confY = py + ph - 78;
        Gui.drawRect(px + 12, confY, px + pw - 12, confY + 1, 0x11FFFFFF);
        
        if (ph > 200) {
            String formTitle = editingIndex == null ? "NOUVELLE MACRO" : "MODIFIER #" + (editingIndex + 1);
            fontRendererObj.drawString(formTitle, px + 20, confY + 2, C_TEXT_MUTED);
        }

        // Champ Commande
        Gui.drawRect(fieldCommand.xPosition - 1, fieldCommand.yPosition - 1, fieldCommand.xPosition + fieldCommand.width + 1, fieldCommand.yPosition + 21, fieldCommand.isFocused() ? C_ACCENT : 0x33FFFFFF);
        Gui.drawRect(fieldCommand.xPosition, fieldCommand.yPosition, fieldCommand.xPosition + fieldCommand.width, fieldCommand.yPosition + 20, 0xFF05070A);
        fieldCommand.drawTextBox();

        // Bouton Assignation
        int tx = px + pw - 100, ty = py + ph - 65;
        boolean thovAssign = in(mx, my, tx, ty, 75, 20);
        assignBtnHover = GuiRenderUtils.lerp(assignBtnHover, thovAssign ? 1.0f : 0.0f, 0.2f);
        
        int assignColor = listeningKey ? C_ACCENT : (pendingKeyCode != 0 ? 0xFF1A2635 : 0xFF151515);
        GuiRenderUtils.drawStyledButton(tx, ty, 75, 20, assignColor, C_ACCENT, thovAssign || listeningKey);
        
        String keyLabel = listeningKey ? "???" : (pendingKeyCode == 0 ? "TOUCHE" : new Macro(pendingKeyCode, "").getKeyDisplayString().toUpperCase());
        drawCentered(keyLabel, tx + 37, ty + 6, listeningKey ? -1 : (pendingKeyCode == 0 ? 0xFF888888 : 0xFFFFAA00));
        hb(hbAssign, tx, ty, 75, 20);

        // Boutons Action
        boolean canSave = !fieldCommand.getText().trim().isEmpty() && pendingKeyCode != 0;
        int btnW = (pw - 50) / 2;
        
        int vbx = px + 20, vby = py + ph - 35;
        boolean vhov = canSave && in(mx, my, vbx, vby, btnW, 22);
        addBtnHover = GuiRenderUtils.lerp(addBtnHover, vhov ? 1.0f : 0.0f, 0.2f);
        int vCol = canSave ? GuiRenderUtils.colorLerp(0xFF0066CC, 0xFF007FFF, addBtnHover) : 0xFF222222;
        GuiRenderUtils.drawStyledButton(vbx, vby, btnW, 22, vCol, C_ACCENT, vhov);
        drawCentered(editingIndex == null ? "AJOUTER" : "SAUVEGARDER", vbx + btnW / 2, vby + 7, canSave ? -1 : 0xFF666666);
        hb(hbAdd, vbx, vby, btnW, 22);

        int abx = px + pw - 20 - btnW, aby = py + ph - 35;
        boolean ahov = in(mx, my, abx, aby, btnW, 22);
        cancelBtnHover = GuiRenderUtils.lerp(cancelBtnHover, ahov ? 1.0f : 0.0f, 0.2f);
        int aCol = GuiRenderUtils.colorLerp(0xFF251515, 0xFF452222, cancelBtnHover);
        GuiRenderUtils.drawStyledButton(abx, aby, btnW, 22, aCol, C_DANGER, ahov);
        drawCentered(editingIndex == null ? "EFFACER" : "ANNULER", abx + btnW / 2, aby + 7, -1);
        hb(hbCancel, abx, aby, btnW, 22);

        GlStateManager.popMatrix();
        super.drawScreen(mx, my, pt);
        if (listeningKey) drawTooltip(mx, my, "Appuyez sur une touche...");
    }

    private void drawMiniAction(int mx, int my, int x, int y, String icon, int hovCol, int[] hbT) {
        boolean hov = in(mx, my, x, y, 18, 18);
        if (hov) Gui.drawRect(x, y, x + 18, y + 18, 0x22FFFFFF);
        fontRendererObj.drawString(icon, x + 5, y + 5, hov ? hovCol : 0x88FFFFFF);
        hb(hbT, x, y, 18, 18);
    }

    private void drawTooltip(int mx, int my, String text) {
        List<String> list = new ArrayList<>();
        list.add(text);
        GuiRenderUtils.drawTooltip(mx, my, list);
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        if (listeningKey) {
            pendingKeyCode = -(btn + 1);
            listeningKey = false;
            return;
        }
        if (ok(hbClose) && in(mx, my, hbClose)) { mc.displayGuiScreen(parent); return; }
        if (ok(hbAssign) && in(mx, my, hbAssign)) { listeningKey = !listeningKey; return; }
        if (ok(hbAdd) && in(mx, my, hbAdd)) {
            String cmd = fieldCommand.getText().trim();
            if (!cmd.isEmpty() && pendingKeyCode != 0) {
                if (editingIndex != null) mgr.setMacro(editingIndex, new Macro(pendingKeyCode, cmd));
                else if (mgr.canAddMacro()) mgr.addMacro(new Macro(pendingKeyCode, cmd));
                resetForm();
            }
            return;
        }
        if (ok(hbCancel) && in(mx, my, hbCancel)) { resetForm(); return; }

        List<Macro> filtered = mgr.getMacros().stream()
                .filter(m -> searchFilter.isEmpty() || m.getCommand().toLowerCase().contains(searchFilter.toLowerCase()))
                .collect(Collectors.toList());

        for (int i = 0; i < hbEdit.length; i++) {
            int idxInFiltered = i + scrollOffset;
            if (idxInFiltered >= filtered.size()) continue;
            Macro m = filtered.get(idxInFiltered);
            int realIdx = mgr.getMacros().indexOf(m);

            if (ok(hbEdit[i]) && in(mx, my, hbEdit[i])) {
                editingIndex = realIdx;
                fieldCommand.setText(m.getCommand());
                pendingKeyCode = m.getKeyCode();
                return;
            }
            if (ok(hbDel[i]) && in(mx, my, hbDel[i])) {
                mgr.removeMacro(realIdx);
                return;
            }
            if (ok(hbCard[i]) && in(mx, my, hbCard[i])) {
                editingIndex = realIdx;
                fieldCommand.setText(m.getCommand());
                pendingKeyCode = m.getKeyCode();
                return;
            }
        }
        fieldSearch.mouseClicked(mx, my, btn);
        fieldCommand.mouseClicked(mx, my, btn);
        super.mouseClicked(mx, my, btn);
    }

    @Override
    protected void keyTyped(char c, int k) throws IOException {
        if (listeningKey) {
            if (k == 1) { listeningKey = false; return; }
            if (k != 0) pendingKeyCode = k;
            listeningKey = false;
            return;
        }
        if (fieldSearch.isFocused()) {
            fieldSearch.textboxKeyTyped(c, k);
            searchFilter = fieldSearch.getText();
            scrollOffset = 0;
            if (k == 28) fieldSearch.setFocused(false);
            return;
        }
        if (k == 1) { 
            if (editingIndex != null) { resetForm(); return; }
            mc.displayGuiScreen(parent); 
            return; 
        }
        fieldCommand.textboxKeyTyped(c, k);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            int visible = listH / ROW_H;
            int total = (int) mgr.getMacros().stream().filter(m -> searchFilter.isEmpty() || m.getCommand().toLowerCase().contains(searchFilter.toLowerCase())).count();
            int max = Math.max(0, total - visible);
            if (wheel < 0) scrollOffset = Math.min(scrollOffset + 1, max);
            else scrollOffset = Math.max(scrollOffset - 1, 0);
        }
    }

    @Override
    public void updateScreen() {
        fieldSearch.updateCursorCounter();
        fieldCommand.updateCursorCounter();
    }

    private void resetForm() {
        fieldCommand.setText("");
        pendingKeyCode = 0;
        editingIndex = null;
        listeningKey = false;
    }

    private void drawCentered(String s, int cx, int y, int col) {
        fontRendererObj.drawStringWithShadow(s, cx - fontRendererObj.getStringWidth(s) / 2f, y, col);
    }
    private void hb(int[] h, int x, int y, int w, int wh) { h[0]=x; h[1]=y; h[2]=w; h[3]=wh; }
    private boolean ok(int[] h) { return h != null && h[2] > 0; }
    private boolean in(int mx, int my, int x, int y, int w, int h) { return mx>=x && my>=y && mx<=x+w && my<=y+h; }
    private boolean in(int mx, int my, int[] h) { return mx>=h[0] && my>=h[1] && mx<=h[0]+h[2] && my<=h[1]+h[3]; }
}
