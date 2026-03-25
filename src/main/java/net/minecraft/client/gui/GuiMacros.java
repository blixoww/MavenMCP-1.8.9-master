package net.minecraft.client.gui;

import net.minecraft.client.macro.MacroManager;
import net.minecraft.client.macro.MacroManager.Macro;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

public class GuiMacros extends GuiScreen {
    
    // ── Palette de couleurs "Origins" ────────────────────────────────────────
    private static final int C_BG       = 0xF2040912;
    private static final int C_BORDER   = 0xFF1A3A6A;
    private static final int C_ACCENT   = 0xFF2277EE;
    private static final int C_ACCENT2  = 0xFF55AAFF;
    private static final int C_HEADER   = 0xFF030B16;
    private static final int C_GOLD     = 0xFFFFD700;
    private static final int C_GREEN    = 0xFF33CC77;
    private static final int C_RED      = 0xFFEE3333;
    private static final int C_GRAY     = 0xFF778899;
    private static final int C_WHITE    = 0xFFFFFFFF;
    private static final int C_PANEL    = 0x44000000;

    private final GuiScreen parent;
    private final MacroManager mgr = MacroManager.INSTANCE;

    // Dimensions
    private int px, py, pw, ph;
    
    // Formulaire
    private GuiTextField fieldCommand;
    private int pendingKeyCode = 0;
    private boolean listeningKey = false;
    private Integer editingIndex = null;

    // Scroll
    private int scrollOffset = 0;
    private static final int ROW_H = 30;
    private int listY, listH;

    // Hitboxes pour les clics sans GuiButton
    private final int[] hbClose   = new int[4];
    private final int[] hbAdd     = new int[4];
    private final int[] hbAssign  = new int[4];
    private final int[] hbCancel  = new int[4];
    private final int[][] hbEdit  = new int[20][4];
    private final int[][] hbDel   = new int[20][4];

    public GuiMacros(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        
        // Taille fixe plus "pro"
        pw = 400;
        ph = 300;
        px = (width - pw) / 2;
        py = (height - ph) / 2;

        listY = py + 40;
        listH = ph - 110;

        // On utilise la hauteur standard (20) pour le GuiTextField car le champ est privé
        this.fieldCommand = new GuiTextField(0, fontRendererObj, px + 20, py + ph - 62, pw - 130, 20);
        this.fieldCommand.setMaxStringLength(128);
        this.fieldCommand.setFocused(true);
        
        if (editingIndex != null) {
            Macro m = mgr.getMacros().get(editingIndex);
            fieldCommand.setText(m.getCommand());
        }
        
        // Réinitialisation des hitboxes pour éviter les résidus
        for(int i=0; i<20; i++) {
            hbEdit[i][2] = 0; hbDel[i][2] = 0;
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        GlStateManager.enableBlend();

        // ── Fenêtre Principale ────────────────────────────────────────────────
        drawRect(px + 4, py + 4, px + pw + 4, py + ph + 4, 0x55000000); // Ombre
        drawRect(px, py, px + pw, py + ph, C_BG);
        drawBorder(px, py, pw, ph, C_BORDER);
        
        // Header
        drawRect(px, py, px + pw, py + 26, C_HEADER);
        drawRect(px, py + 25, px + pw, py + 26, C_ACCENT);
        fontRendererObj.drawStringWithShadow("§lGESTION DES MACROS", px + 15, py + 9, C_WHITE);
        
        // Bouton Fermer (X)
        int cx = px + pw - 20, cy = py + 6;
        boolean chov = in(mx, my, cx, cy, 14, 14);
        drawRect(cx, cy, cx + 14, cy + 14, chov ? 0xFFBB3333 : 0x44AA3333);
        fontRendererObj.drawString("x", cx + 4, cy + 2, chov ? C_WHITE : 0xFFCC7777);
        hb(hbClose, cx, cy, 14, 14);

        // ── Liste des Macros ──────────────────────────────────────────────────
        List<Macro> macros = mgr.getMacros();
        int visible = listH / ROW_H;
        
        if (macros.isEmpty()) {
            drawCentered("§8Aucune macro configurée.", px + pw / 2, listY + listH / 2 - 4, 0);
        } else {
            for (int i = 0; i < visible && (i + scrollOffset) < macros.size(); i++) {
                int idx = i + scrollOffset;
                Macro m = macros.get(idx);
                int rowY = listY + i * ROW_H;
                boolean hov = in(mx, my, px + 10, rowY, pw - 20, ROW_H - 4);
                boolean isEditing = editingIndex != null && editingIndex == idx;

                // Fond de ligne
                drawRect(px + 10, rowY, px + pw - 10, rowY + ROW_H - 4, isEditing ? 0x442277EE : (hov ? 0x22FFFFFF : 0x11FFFFFF));
                if (isEditing) drawRect(px + 10, rowY, px + 12, rowY + ROW_H - 4, C_ACCENT);

                // Touche (Badge)
                String keyStr = m.getKeyDisplayString();
                int kw = fontRendererObj.getStringWidth(keyStr) + 10;
                drawRect(px + 18, rowY + 6, px + 18 + kw, rowY + 18, 0xFF152545);
                drawBorder(px + 18, rowY + 6, kw, 12, C_ACCENT);
                fontRendererObj.drawString(keyStr, px + 18 + 5, rowY + 8, C_GOLD);

                // Commande
                String cmd = m.getCommand();
                int maxW = pw - kw - 120;
                if (fontRendererObj.getStringWidth(cmd) > maxW)
                    cmd = fontRendererObj.trimStringToWidth(cmd, maxW) + "...";
                fontRendererObj.drawString(cmd, px + 25 + kw, rowY + 8, C_WHITE);

                // Boutons d'action (mini)
                int bx = px + pw - 85;
                miniBtn(mx, my, bx, rowY + 4, 30, 14, "§aEdt", 0xFF154515, C_GREEN, hbEdit[i]);
                miniBtn(mx, my, bx + 35, rowY + 4, 30, 14, "§cSup", 0xFF451515, C_RED, hbDel[i]);
            }
        }

        // Scrollbar stylisée
        if (macros.size() > visible) {
            int sbX = px + pw - 8, sbH = listH;
            drawRect(sbX, listY, sbX + 3, listY + sbH, 0x22FFFFFF);
            int thumbH = Math.max(10, sbH * visible / macros.size());
            int thumbY = listY + (sbH - thumbH) * scrollOffset / (macros.size() - visible);
            drawRect(sbX, thumbY, sbX + 3, thumbY + thumbH, C_ACCENT);
        }

        // ── Zone d'Ajout / Edition ───────────────────────────────────────────
        int formY = py + ph - 68;
        drawRect(px + 10, formY, px + pw - 10, py + ph - 10, C_PANEL);
        drawRect(px + 10, formY, px + pw - 10, formY + 1, 0x44FFFFFF);

        fontRendererObj.drawString(editingIndex == null ? "§7AJOUTER UNE MACRO" : "§bEDITION MACRO #" + (editingIndex + 1), px + 20, formY + 8, 0);

        // Champ Commande - On évite l'accès au champ privé height
        drawRect(fieldCommand.xPosition - 2, fieldCommand.yPosition - 2, fieldCommand.xPosition + fieldCommand.width + 2, fieldCommand.yPosition + 22, 0xFF05101A);
        drawBorder(fieldCommand.xPosition - 2, fieldCommand.yPosition - 2, fieldCommand.width + 4, 24, fieldCommand.isFocused() ? C_ACCENT : 0xFF1A2A45);
        fieldCommand.drawTextBox();
        if (fieldCommand.getText().isEmpty() && !fieldCommand.isFocused())
            fontRendererObj.drawString("§8/commande ou texte...", fieldCommand.xPosition + 4, fieldCommand.yPosition + 6, 0);

        // Bouton Touche
        int tx = px + pw - 100, ty = py + ph - 62;
        boolean thov = in(mx, my, tx, ty, 80, 20);
        drawRect(tx, ty, tx + 80, ty + 20, listeningKey ? 0xFF225588 : (thov ? 0xFF152545 : 0xFF0A1020));
        drawBorder(tx, ty, 80, 20, listeningKey ? C_WHITE : (thov ? C_ACCENT2 : C_BORDER));
        String keyLabel = listeningKey ? "§b???" : (pendingKeyCode == 0 ? "§7Touche" : "§e" + new Macro(pendingKeyCode, "").getKeyDisplayString());
        drawCentered(keyLabel, tx + 40, ty + 6, 0);
        hb(hbAssign, tx, ty, 80, 20);

        // Boutons Action Formulaire
        boolean canSave = !fieldCommand.getText().trim().isEmpty() && pendingKeyCode != 0;
        int btnW = (pw - 40) / 2 - 5;
        
        // Bouton Valider
        int vbx = px + 20, vby = py + ph - 35;
        boolean vhov = canSave && in(mx, my, vbx, vby, btnW, 18);
        drawRect(vbx, vby, vbx + btnW, vby + 18, canSave ? (vhov ? 0xFF228844 : 0xFF154525) : 0xFF333333);
        drawBorder(vbx, vby, btnW, 18, canSave ? (vhov ? C_GREEN : 0xFF226644) : 0xFF444444);
        drawCentered(editingIndex == null ? "§lAJOUTER" : "§lSAUVEGARDER", vbx + btnW / 2, vby + 5, canSave ? C_WHITE : C_GRAY);
        hb(hbAdd, vbx, vby, btnW, 18);

        // Bouton Annuler / Clear
        int abx = px + pw - 20 - btnW, aby = py + ph - 35;
        boolean ahov = in(mx, my, abx, aby, btnW, 18);
        drawRect(abx, aby, abx + btnW, aby + 18, ahov ? 0xFF882222 : 0xFF451515);
        drawBorder(abx, aby, btnW, 18, ahov ? C_RED : 0xFF662222);
        drawCentered(editingIndex == null ? "§lEFFACER" : "§lANNULER", abx + btnW / 2, aby + 5, C_WHITE);
        hb(hbCancel, abx, aby, btnW, 18);

        super.drawScreen(mx, my, pt);
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

        // Clics sur la liste
        for (int i = 0; i < hbEdit.length; i++) {
            if (ok(hbEdit[i]) && in(mx, my, hbEdit[i])) {
                int idx = i + scrollOffset;
                if (idx < mgr.getMacros().size()) {
                    Macro m = mgr.getMacros().get(idx);
                    editingIndex = idx;
                    fieldCommand.setText(m.getCommand());
                    pendingKeyCode = m.getKeyCode();
                }
                return;
            }
            if (ok(hbDel[i]) && in(mx, my, hbDel[i])) {
                int idx = i + scrollOffset;
                if (idx < mgr.getMacros().size()) mgr.removeMacro(idx);
                return;
            }
        }

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
        if (k == 1) { mc.displayGuiScreen(parent); return; }
        fieldCommand.textboxKeyTyped(c, k);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            int visible = listH / ROW_H;
            int max = Math.max(0, mgr.getCount() - visible);
            if (wheel < 0) scrollOffset = Math.min(scrollOffset + 1, max);
            else scrollOffset = Math.max(scrollOffset - 1, 0);
        }
    }

    @Override
    public void updateScreen() {
        fieldCommand.updateCursorCounter();
    }

    private void resetForm() {
        fieldCommand.setText("");
        pendingKeyCode = 0;
        editingIndex = null;
        listeningKey = false;
    }

    // ── Utilitaires de Rendu ────────────────────────────────────────────────
    private void drawBorder(int x, int y, int w, int h, int c) {
        drawRect(x, y, x+w, y+1, c); drawRect(x, y+h-1, x+w, y+h, c);
        drawRect(x, y, x+1, y+h, c); drawRect(x+w-1, y, x+w, y+h, c);
    }
    private void drawCentered(String s, int cx, int y, int col) {
        fontRendererObj.drawStringWithShadow(s, cx - fontRendererObj.getStringWidth(s) / 2f, y, col);
    }
    private void miniBtn(int mx, int my, int x, int y, int w, int h, String l, int bg, int tc, int[] hbT) {
        boolean hov = in(mx, my, x, y, w, h);
        drawRect(x, y, x+w, y+h, hov ? bg : 0x44000000);
        drawBorder(x, y, w, h, hov ? tc : 0x44FFFFFF);
        fontRendererObj.drawString(l, x + (w - fontRendererObj.getStringWidth(l)) / 2, y + 3, 0xFFFFFFFF);
        hb(hbT, x, y, w, h);
    }
    private void hb(int[] h, int x, int y, int w, int wh) { h[0]=x; h[1]=y; h[2]=w; h[3]=wh; }
    private boolean ok(int[] h) { return h[2] > 0; }
    private boolean in(int mx, int my, int x, int y, int w, int h) { return mx>=x && my>=y && mx<=x+w && my<=y+h; }
    private boolean in(int mx, int my, int[] h) { return mx>=h[0] && my>=h[1] && mx<=h[0]+h[2] && my<=h[1]+h[3]; }
}
