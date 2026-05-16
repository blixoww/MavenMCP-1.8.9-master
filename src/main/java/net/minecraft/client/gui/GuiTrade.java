package net.minecraft.client.gui;

import net.minecraft.client.custompackets.data.PlayerData;
import net.minecraft.client.custompackets.handler.PlayerDataHandler;
import net.minecraft.client.custompackets.handler.TradePacketHandler;
import net.minecraft.client.gui.ui.UITheme;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.List;

/**
 * GuiTrade — Interface d'échange, redesign sobre.
 *
 *  • Panel compact pour que les grilles aient une présence visuelle correcte
 *  • Slots "dark transparent" : juste un voile noir + bord subtil
 *  • Pas de glyphes Unicode (flèches) qui rendent mal en MC font
 *  • Argent inline : [−] [input] [+]  (clic droit = ×10)
 *  • Tout l'état authoritative vient de {@link TradePacketHandler} (temps réel)
 */
public class GuiTrade extends GuiScreen {

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final int C_OVERLAY  = 0xC8060410;
    private static final int C_BG       = 0xF20A0810;
    private static final int C_HEADER   = 0xFF060309;
    private static final int C_CARD     = 0xFF0E0B16;
    private static final int C_INPUT    = 0xFF050309;
    private static final int C_BORDER   = 0xFF1C1428;
    private static final int C_BORDER2  = 0xFF120A1C;
    // Slots "dark transparent" — viennent par-dessus le card bg
    private static final int C_SLOT_T   = 0x55000000;
    private static final int C_SLOT_TD  = 0x33000000;  // dim (partenaire)
    private static final int C_SLOT_TH  = 0x44FFFFFF;  // hover
    private static final int C_EDGE_HI  = 0x12FFFFFF;
    private static final int C_EDGE_LO  = 0x40000000;
    // Texte
    private static final int C_TEXT     = 0xFFEDE8F2;
    private static final int C_SOFT     = 0xFFB0A4BE;
    private static final int C_MUTED    = 0xFF6A5F7A;
    private static final int C_LABEL    = 0xFF8A7E9A;
    // Couleurs sémantiques
    private static final int C_GOLD     = 0xFFFFC548;
    private static final int C_OK       = 0xFF4FCB7B;
    private static final int C_WAIT     = 0xFFE6BE3F;
    private static final int C_OVER     = 0xFFEE4444;

    // ── Grille ───────────────────────────────────────────────────────────────
    private static final int SLOT     = 18;
    private static final int GRID_C   = 5;
    private static final int GRID_R   = 3;
    private static final int GRID_W   = GRID_C * SLOT;   // 90
    private static final int GRID_H   = GRID_R * SLOT;   // 54
    private static final int MAX_OFF  = 15;

    // ── État ─────────────────────────────────────────────────────────────────
    private long openMs;
    private long myMoney;
    private long playerBalance;
    private int  myPB;
    private int  playerPB;
    private GuiTextField moneyField;
    private GuiTextField pbField;
    private boolean fieldInit;

    // ── Hover ────────────────────────────────────────────────────────────────
    private int hovMySlot = -1, hovPtSlot = -1, hovInvSlot = -1;
    private boolean hovConfirm, hovCancel, hovClose;
    private boolean hovMinus, hovPlus;
    private boolean hovPBMinus, hovPBPlus, hovPBLabel;

    // ── Layout ───────────────────────────────────────────────────────────────
    private int px, py, pw, ph;
    private int headerH = 22, subtitleY;
    private int cardY, cardH, cardW;
    private int leftCardX, rightCardX;
    private int leftGridX, rightGridX, gridY;
    private int moneyY;
    private int pbRowY;
    private int pbMinusX, pbPlusX, pbValueX, pbValueW;
    private int statusY;
    private int actionsY, actionsH = 16;
    private int bConfX, bConfY, bConfW, bCancX, bCancY, bCancW;
    private int closeX, closeY, closeS = 11;
    private int sepY;
    private int invX, invLblY, invMainY, hotbarY;
    private int minusX, plusX, mBtnW = 13;
    private int fieldX, fieldY, fieldW;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void initGui() {
        openMs = System.currentTimeMillis();
        myMoney = TradePacketHandler.playerOfferedMoney;
        myPB    = TradePacketHandler.playerOfferedPB;
        fieldInit = false;
        moneyField = null;
        pbField    = null;
        Keyboard.enableRepeatEvents(true);
        PlayerData pd = PlayerDataHandler.getCachedData();
        if (pd != null) { playerBalance = pd.getBalance(); playerPB = pd.getPb(); }
        PlayerDataHandler.setListener(d -> {
            playerBalance = d.getBalance();
            playerPB      = d.getPb();
            // Re-cap si le solde a baissé en dessous des offres en cours
            if (myMoney > playerBalance) {
                myMoney = playerBalance;
                if (moneyField != null) {
                    moneyField.setText(myMoney == 0 ? "" : String.valueOf(myMoney));
                }
                TradePacketHandler.sendMoneyOffer(myMoney);
            }
            if (myPB > playerPB) {
                myPB = playerPB;
                if (pbField != null) {
                    pbField.setText(myPB == 0 ? "" : String.valueOf(myPB));
                }
                TradePacketHandler.sendPBOffer(myPB);
            }
        });
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    // ── Input ────────────────────────────────────────────────────────────────

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) {
            TradePacketHandler.sendCancel();
            mc.displayGuiScreen(null);
            return;
        }
        if (moneyField != null && moneyField.isFocused()) {
            String before = moneyField.getText();
            if (moneyField.textboxKeyTyped(c, key)) {
                String after = moneyField.getText();
                if (!after.equals(before)) {
                    // Validation : la saisie ne doit pas dépasser le solde
                    long v;
                    String trimmed = after.trim();
                    if (trimmed.isEmpty()) {
                        v = 0L;
                    } else {
                        try { v = Long.parseLong(trimmed); }
                        catch (NumberFormatException e) {
                            // Non numérique → revert
                            moneyField.setText(before);
                            return;
                        }
                    }
                    if (v < 0 || v > playerBalance) {
                        // Dépasse le solde → on bloque la saisie
                        moneyField.setText(before);
                        return;
                    }
                    commitMoney();
                }
                return;
            }
        }
        if (pbField != null && pbField.isFocused()) {
            String before = pbField.getText();
            if (pbField.textboxKeyTyped(c, key)) {
                String after = pbField.getText();
                if (!after.equals(before)) {
                    int v;
                    String trimmed = after.trim();
                    if (trimmed.isEmpty()) {
                        v = 0;
                    } else {
                        try { v = Integer.parseInt(trimmed); }
                        catch (NumberFormatException e) {
                            pbField.setText(before);
                            return;
                        }
                    }
                    if (v < 0 || v > playerPB) {
                        pbField.setText(before);
                        return;
                    }
                    commitPB();
                }
                return;
            }
        }
        if (key >= Keyboard.KEY_1 && key <= Keyboard.KEY_9) {
            ItemStack it = mc.thePlayer.inventory.mainInventory[key - Keyboard.KEY_1];
            if (it != null) TradePacketHandler.sendOffer(key - Keyboard.KEY_1);
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        if (moneyField != null) moneyField.mouseClicked(mx, my, btn);
        if (pbField    != null) pbField.mouseClicked(mx, my, btn);

        // ± : gauche = ±100, droit = ±1000
        if (hovMinus) { adjustMoney(btn == 1 ? -1000 : -100); return; }
        if (hovPlus)  { adjustMoney(btn == 1 ?  1000 :  100); return; }
        // ± PB : gauche = ±10, droit = ±100
        if (hovPBMinus) { adjustPB(btn == 1 ? -100 : -10); return; }
        if (hovPBPlus)  { adjustPB(btn == 1 ?  100 :  10); return; }
        if (btn != 0) return;

        if (hovClose || hovCancel) {
            TradePacketHandler.sendCancel();
            mc.displayGuiScreen(null);
            return;
        }
        if (hovConfirm && !TradePacketHandler.myConfirmed && myMoney <= playerBalance) {
            TradePacketHandler.sendConfirm();
            return;
        }
        if (hovMySlot >= 0 && hovMySlot < TradePacketHandler.myOfferCount) {
            TradePacketHandler.sendTake(hovMySlot);
            return;
        }
        if (hovInvSlot >= 0) {
            ItemStack it = mc.thePlayer.inventory.mainInventory[hovInvSlot];
            if (it != null) TradePacketHandler.sendOffer(hovInvSlot);
        }
    }

    // ── Rendu principal ───────────────────────────────────────────────────────

    @Override
    public void drawScreen(int mx, int my, float pt) {
        layout();
        syncField();
        updateHover(mx, my);

        float t = openMs > 0 ? Math.min(1f, (System.currentTimeMillis() - openMs) / 220f) : 1f;
        float e = t * t * (3f - 2f * t);

        drawRect(0, 0, width, height, aScale(C_OVERLAY, e));

        GlStateManager.pushMatrix();
        float cx = px + pw * .5f, cy = py + ph * .5f;
        GlStateManager.translate(cx, cy + (1f - e) * 8f, 0);
        GlStateManager.scale(.97f + e * .03f, .97f + e * .03f, 1f);
        GlStateManager.translate(-cx, -cy, 0);

        drawPanel();
        drawHeader();
        drawSubtitle();
        drawCard(leftCardX,  cardY, cardW, cardH, "VOTRE OFFRE",
                TradePacketHandler.myOffer, TradePacketHandler.myOfferCount, hovMySlot,
                true, TradePacketHandler.myConfirmed,
                TradePacketHandler.playerOfferedMoney, leftGridX);
        drawCard(rightCardX, cardY, cardW, cardH, partnerTitle(),
                TradePacketHandler.partnerOffer, TradePacketHandler.partnerOfferCount, -1,
                false, TradePacketHandler.partnerConfirmed,
                TradePacketHandler.partnerOfferedMoney, rightGridX);
        drawActions();
        drawSeparator();
        drawInventory();

        GlStateManager.popMatrix();
        drawTooltip(mx, my);
    }

    // ── Layout ───────────────────────────────────────────────────────────────

    private void layout() {
        pw = Math.min(width - 20, 280);
        ph = Math.min(height - 12, 282); // +16 pour la ligne PB
        px = (width  - pw) / 2;
        py = (height - ph) / 2;

        subtitleY = py + headerH + 4;

        // Cartes : 2 colonnes serrées
        int outerPad = 10;
        int gap = 8;
        cardW = (pw - outerPad * 2 - gap) / 2;  // ~126
        cardH = 118; // +16 pour la ligne PB
        cardY = subtitleY + 14;
        leftCardX  = px + outerPad;
        rightCardX = leftCardX + cardW + gap;

        // Grille centrée dans la carte
        leftGridX  = leftCardX  + (cardW - GRID_W) / 2;
        rightGridX = rightCardX + (cardW - GRID_W) / 2;
        gridY      = cardY + 14;

        moneyY  = gridY + GRID_H + 5;
        pbRowY  = moneyY + 15;
        statusY = pbRowY + 15;

        // Actions
        actionsY = cardY + cardH + 8;
        int bConfWi = 100, bCancWi = 72, btnGap = 6;
        bConfW = bConfWi; bCancW = bCancWi;
        int total = bConfWi + btnGap + bCancWi;
        bConfX = px + (pw - total) / 2;
        bConfY = actionsY;
        bCancX = bConfX + bConfWi + btnGap;
        bCancY = actionsY;

        // Bouton fermer
        closeX = px + pw - 16;
        closeY = py + 5;

        // Inventaire
        sepY     = actionsY + actionsH + 8;
        invLblY  = sepY + 4;
        invMainY = invLblY + 11;
        hotbarY  = invMainY + 3 * SLOT + 3;
        invX     = px + (pw - 9 * SLOT) / 2;
    }

    private void syncField() {
        // [−] [______input______] [+]  centrés dans la carte gauche
        int rowW = GRID_W;
        int x0 = leftGridX;
        minusX = x0;
        plusX  = x0 + rowW - mBtnW;
        fieldX = minusX + mBtnW + 2;
        fieldY = moneyY + 2;
        fieldW = plusX - fieldX - 2;

        // Coordonnées de la ligne PB (mêmes X que la ligne argent)
        pbMinusX = minusX;
        pbPlusX  = plusX;
        pbValueX = pbMinusX + mBtnW + 2;
        pbValueW = pbPlusX - pbValueX - 2;
        int pbFieldX = pbValueX;
        int pbFieldY = pbRowY + 2;
        int pbFieldW = pbValueW;

        if (!fieldInit) {
            moneyField = new GuiTextField(99, fontRendererObj, fieldX, fieldY, fieldW, 10);
            moneyField.setMaxStringLength(10);
            moneyField.setText(myMoney == 0 ? "" : String.valueOf(myMoney));
            moneyField.setEnableBackgroundDrawing(false);
            pbField = new GuiTextField(98, fontRendererObj, pbFieldX, pbFieldY, pbFieldW, 10);
            pbField.setMaxStringLength(10);
            pbField.setText(myPB == 0 ? "" : String.valueOf(myPB));
            pbField.setEnableBackgroundDrawing(false);
            fieldInit = true;
        } else {
            moneyField.xPosition = fieldX;
            moneyField.yPosition = fieldY;
            moneyField.width     = fieldW;
            if (pbField != null) {
                pbField.xPosition = pbFieldX;
                pbField.yPosition = pbFieldY;
                pbField.width     = pbFieldW;
            }
        }
    }

    private void updateHover(int mx, int my) {
        hovMySlot  = hitGrid(mx, my, leftGridX);
        hovPtSlot  = hitGrid(mx, my, rightGridX);
        hovInvSlot = hitInv(mx, my);
        hovConfirm = in(mx, my, bConfX, bConfY, bConfW, actionsH);
        hovCancel  = in(mx, my, bCancX, bCancY, bCancW, actionsH);
        hovClose   = in(mx, my, closeX, closeY, closeS, closeS);
        hovMinus   = in(mx, my, minusX, moneyY, mBtnW, 13);
        hovPlus    = in(mx, my, plusX,  moneyY, mBtnW, 13);
        hovPBMinus = in(mx, my, pbMinusX, pbRowY, mBtnW, 13);
        hovPBPlus  = in(mx, my, pbPlusX,  pbRowY, mBtnW, 13);
        hovPBLabel = in(mx, my, pbValueX, pbRowY, pbValueW, 13);
    }

    // ── Sous-routines ────────────────────────────────────────────────────────

    private void drawPanel() {
        GuiRenderUtils.drawShadow(px, py, pw, ph, 6, 0x88);
        drawRect(px, py, px + pw, py + ph, C_BG);
        // ligne accent en haut
        drawRect(px, py, px + pw, py + 1, UITheme.getPrimary());
        // header solide
        drawRect(px, py + 1, px + pw, py + headerH, C_HEADER);
        // séparateur sous header
        drawRect(px, py + headerH, px + pw, py + headerH + 1, aScale(UITheme.getPrimary(), .35f));
        // bordure périmètre
        drawRect(px, py + ph - 1, px + pw, py + ph, C_BORDER);
        drawRect(px, py, px + 1, py + ph, C_BORDER);
        drawRect(px + pw - 1, py, px + pw, py + ph, C_BORDER);
    }

    private void drawHeader() {
        // Titre : É en accent + reste en blanc, sans icône
        String prefix = "É";
        String rest   = "CHANGE";
        int wP = fontRendererObj.getStringWidth(prefix);
        int wR = fontRendererObj.getStringWidth(rest);
        int tx = px + (pw - wP - wR) / 2;
        int ty = py + 7;
        fontRendererObj.drawStringWithShadow("§l" + prefix, tx,      ty, UITheme.getPrimary());
        fontRendererObj.drawStringWithShadow("§l" + rest,   tx + wP, ty, C_TEXT);

        // Solde joueur — pastille discrète à gauche
        String bal = "§7Solde §6" + fmtMoney(playerBalance) + "$";
        fontRendererObj.drawString(bal, px + 8, ty + 1, C_TEXT);

        // Bouton fermer
        int bg = hovClose ? 0xFFCC2A2A : 0x55000000;
        drawRect(closeX, closeY, closeX + closeS, closeY + closeS, bg);
        if (!hovClose) {
            GuiRenderUtils.drawRectOutline(closeX, closeY, closeS, closeS, 0xFF3A2030);
        }
        // X en pixels
        int cx2 = closeX + closeS / 2, cy2 = closeY + closeS / 2;
        for (int i = -2; i <= 2; i++) {
            drawRect(cx2 + i, cy2 + i, cx2 + i + 1, cy2 + i + 1, 0xFFFFFFFF);
            drawRect(cx2 + i, cy2 - i, cx2 + i + 1, cy2 - i + 1, 0xFFFFFFFF);
        }
    }

    private void drawSubtitle() {
        String me = mc.thePlayer != null ? mc.thePlayer.getName() : "Vous";
        int maxW = (pw - 60) / 2;
        String meT = fontRendererObj.trimStringToWidth(me, maxW);
        String ptT = fontRendererObj.trimStringToWidth(TradePacketHandler.partnerName, maxW);
        String sep = "  ·  ";  // point médian, propre en MC font
        int wM = fontRendererObj.getStringWidth(meT);
        int wS = fontRendererObj.getStringWidth(sep);
        int wP = fontRendererObj.getStringWidth(ptT);
        int sx = px + (pw - wM - wS - wP) / 2;
        fontRendererObj.drawString(meT, sx,                 subtitleY, C_SOFT);
        fontRendererObj.drawString(sep, sx + wM,            subtitleY, UITheme.primary(0xCC));
        fontRendererObj.drawString(ptT, sx + wM + wS,       subtitleY, C_SOFT);
    }

    private String partnerTitle() {
        int maxW = cardW - 50;
        if (maxW < 20) maxW = 20;
        return fontRendererObj.trimStringToWidth(TradePacketHandler.partnerName.toUpperCase(), maxW);
    }

    private void drawCard(int cx, int cy, int cw, int ch, String title,
                          ItemStack[] items, int count, int hover,
                          boolean isMine, boolean confirmed,
                          long money, int gridX) {
        // Corps de carte (subtil, juste un voile)
        drawRect(cx, cy, cx + cw, cy + ch, C_CARD);
        // Ligne accent fine en haut
        drawRect(cx, cy, cx + cw, cy + 1, isMine ? UITheme.primary(0x88) : aScale(C_OK, .50f));
        // Bordure subtile
        GuiRenderUtils.drawRectOutline(cx, cy, cw, ch, C_BORDER2);

        // Titre + compteur
        fontRendererObj.drawString(title, cx + 6, cy + 4, isMine ? C_TEXT : C_SOFT);
        String cnt = count + "/" + MAX_OFF;
        int wc = fontRendererObj.getStringWidth(cnt);
        fontRendererObj.drawString(cnt, cx + cw - wc - 6, cy + 4, C_MUTED);

        // Grille
        drawGrid(gridX, items, count, hover, isMine);

        // Empty hint
        if (count == 0) {
            String hint = "";
            int hw = fontRendererObj.getStringWidth(hint);
            fontRendererObj.drawString(hint,
                    gridX + (GRID_W - hw) / 2,
                    gridY + GRID_H / 2 - 4, C_MUTED);
        }

        // Ligne argent
        if (isMine) drawMyMoneyRow();
        else        drawPartnerMoney(gridX, money);

        // Ligne PB
        if (isMine) drawMyPBRow();
        else        drawPartnerPB(gridX, TradePacketHandler.partnerOfferedPB);

        // Status badge (sobre, juste pastille + texte)
        drawStatusInline(gridX, statusY, confirmed);
    }

    private void drawMyPBRow() {
        int rowH = 13;
        boolean over = myPB > playerPB;
        boolean focused = pbField != null && pbField.isFocused();
        drawSmallBtn(pbMinusX, pbRowY, mBtnW, rowH, "−", hovPBMinus, false);
        drawSmallBtn(pbPlusX,  pbRowY, mBtnW, rowH, "+", hovPBPlus,  true);
        int fbgX = pbValueX - 2, fbgY = pbRowY, fbgW = pbValueW + 4;
        drawRect(fbgX, fbgY, fbgX + fbgW, fbgY + rowH, C_INPUT);
        GuiRenderUtils.drawRectOutline(fbgX, fbgY, fbgW, rowH,
                over ? C_OVER : (focused ? UITheme.primary(0xBB) : C_BORDER2));
        if (pbField != null) pbField.drawTextBox();
        if (pbField != null && !focused && pbField.getText().isEmpty()) {
            fontRendererObj.drawString("§7PB 0", pbValueX, pbRowY + 2, C_MUTED);
        }
    }

    private void drawPartnerPB(int gx, int pb) {
        int rowH = 13;
        drawRect(gx, pbRowY, gx + GRID_W, pbRowY + rowH, C_INPUT);
        GuiRenderUtils.drawRectOutline(gx, pbRowY, GRID_W, rowH, C_BORDER2);
        String s = pb > 0 ? "PB " + fmtPB(pb) : "PB —";
        int sw = fontRendererObj.getStringWidth(s);
        fontRendererObj.drawString(s, gx + (GRID_W - sw) / 2, pbRowY + (rowH - 8) / 2 + 1,
                pb > 0 ? 0xFFFFEE55 : C_MUTED);
    }

    private void drawGrid(int gx, ItemStack[] items, int count, int hov, boolean inter) {
        for (int r = 0; r < GRID_R; r++) {
            for (int c = 0; c < GRID_C; c++) {
                int idx = r * GRID_C + c;
                int sx = gx + c * SLOT, sy = gridY + r * SLOT;
                boolean h = inter && hov == idx;
                drawSlot(sx, sy, !inter ? C_SLOT_TD : C_SLOT_T, h, inter);
                if (idx < count && items[idx] != null) {
                    renderItem(items[idx], sx + 1, sy + 1);
                }
            }
        }
    }

    /** Slot dark transparent — juste un voile + bord subtil. */
    private void drawSlot(int x, int y, int bg, boolean hover, boolean interactive) {
        drawRect(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, bg);
        // Edges subtils
        drawRect(x, y, x + SLOT, y + 1, C_EDGE_HI);
        drawRect(x, y, x + 1, y + SLOT, C_EDGE_HI);
        drawRect(x, y + SLOT - 1, x + SLOT, y + SLOT, C_EDGE_LO);
        drawRect(x + SLOT - 1, y, x + SLOT, y + SLOT, C_EDGE_LO);
        if (hover && interactive) {
            // Surimpression accent
            drawRect(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, C_SLOT_TH);
            drawRect(x, y,                x + SLOT, y + 1,            UITheme.primary(0xAA));
            drawRect(x, y + SLOT - 1,     x + SLOT, y + SLOT,         UITheme.primary(0x66));
            drawRect(x, y,                x + 1,    y + SLOT,         UITheme.primary(0x88));
            drawRect(x + SLOT - 1, y,     x + SLOT, y + SLOT,         UITheme.primary(0x55));
        }
    }

    private void drawMyMoneyRow() {
        boolean over = myMoney > playerBalance;
        boolean focused = moneyField != null && moneyField.isFocused();
        int rowH = 13;

        // − bouton
        drawSmallBtn(minusX, moneyY, mBtnW, rowH, "−", hovMinus, false);
        // + bouton
        drawSmallBtn(plusX,  moneyY, mBtnW, rowH, "+", hovPlus,  true);

        // Champ d'input central
        int fbgX = fieldX - 2, fbgY = moneyY, fbgW = fieldW + 4;
        drawRect(fbgX, fbgY, fbgX + fbgW, fbgY + rowH, C_INPUT);
        GuiRenderUtils.drawRectOutline(fbgX, fbgY, fbgW, rowH,
                over ? C_OVER : (focused ? UITheme.primary(0xBB) : C_BORDER2));

        if (moneyField != null) moneyField.drawTextBox();
        if (moneyField != null && !focused && moneyField.getText().isEmpty()) {
            fontRendererObj.drawString("§7$ 0", fieldX, fieldY, C_MUTED);
        }
    }

    private void drawSmallBtn(int x, int y, int w, int h, String label, boolean hov, boolean positive) {
        int bg = hov ? (positive ? 0xFF1F4A2B : 0xFF4A1F1F)
                     : 0xFF110A1C;
        drawRect(x, y, x + w, y + h, bg);
        GuiRenderUtils.drawRectOutline(x, y, w, h,
                hov ? (positive ? C_OK : C_OVER) : C_BORDER);
        int tw = fontRendererObj.getStringWidth(label);
        fontRendererObj.drawString(label, x + (w - tw) / 2, y + (h - 8) / 2 + 1,
                hov ? 0xFFFFFFFF : C_SOFT);
    }

    private void drawPartnerMoney(int gx, long money) {
        int rowH = 13;
        // Cadre input read-only
        drawRect(gx, moneyY, gx + GRID_W, moneyY + rowH, C_INPUT);
        GuiRenderUtils.drawRectOutline(gx, moneyY, GRID_W, rowH, C_BORDER2);
        // $ + valeur centrés
        String s = money > 0 ? "$ " + fmtMoney(money) : "$ —";
        int sw = fontRendererObj.getStringWidth(s);
        fontRendererObj.drawString(s,
                gx + (GRID_W - sw) / 2,
                moneyY + (rowH - 8) / 2 + 1,
                money > 0 ? C_GOLD : C_MUTED);
    }

    private void drawStatusInline(int gx, int sy, boolean confirmed) {
        int accent = confirmed ? C_OK : C_WAIT;
        // Pastille
        int dx = gx, dy = sy + 2;
        drawRect(dx - 1, dy - 1, dx + 5, dy + 5, aScale(accent, .22f));
        drawRect(dx, dy, dx + 4, dy + 4, accent);
        // Texte
        String label = confirmed ? "PRÊT" : "EN COURS";
        fontRendererObj.drawString(label, gx + 8, sy, accent);
    }

    private void drawActions() {
        boolean conf = TradePacketHandler.myConfirmed;

        // CONFIRMER
        int bgC = conf ? 0xFF1A4A28 : (hovConfirm ? 0xFF205830 : 0xFF14321D);
        drawRect(bConfX, bConfY, bConfX + bConfW, bConfY + actionsH, bgC);
        GuiRenderUtils.drawRectOutline(bConfX, bConfY, bConfW, actionsH,
                hovConfirm || conf ? C_OK : aScale(C_OK, .35f));
        drawRect(bConfX, bConfY, bConfX + bConfW, bConfY + 1, aScale(C_OK, hovConfirm || conf ? .80f : .50f));
        String labelC = conf ? "CONFIRMÉ" : "CONFIRMER";
        int wC = fontRendererObj.getStringWidth(labelC);
        fontRendererObj.drawStringWithShadow(labelC,
                bConfX + (bConfW - wC) / 2, bConfY + (actionsH - 8) / 2 + 1,
                conf ? C_OK : 0xFFE5F8E8);

        // ANNULER
        int bgA = hovCancel ? 0xFF541818 : 0xFF321010;
        drawRect(bCancX, bCancY, bCancX + bCancW, bCancY + actionsH, bgA);
        GuiRenderUtils.drawRectOutline(bCancX, bCancY, bCancW, actionsH,
                hovCancel ? UITheme.getPrimary() : UITheme.primary(0x55));
        drawRect(bCancX, bCancY, bCancX + bCancW, bCancY + 1, UITheme.primary(hovCancel ? 0xCC : 0x77));
        String labelA = "ANNULER";
        int wA = fontRendererObj.getStringWidth(labelA);
        fontRendererObj.drawStringWithShadow(labelA,
                bCancX + (bCancW - wA) / 2, bCancY + (actionsH - 8) / 2 + 1, 0xFFFFDDDD);
    }

    private void drawSeparator() {
        int x1 = px + 16, x2 = px + pw - 16, mid = (x1 + x2) / 2;
        GuiRenderUtils.drawGradientRect(x1, sepY, mid, sepY + 1, 0x00000000, 0x33FFFFFF);
        GuiRenderUtils.drawGradientRect(mid, sepY, x2, sepY + 1, 0x33FFFFFF, 0x00000000);

        String lbl = "INVENTAIRE";
        int lw = fontRendererObj.getStringWidth(lbl);
        int tlx = px + (pw - lw) / 2 - 4;
        drawRect(tlx, invLblY - 1, tlx + lw + 8, invLblY + 9, C_BG);
        fontRendererObj.drawString(lbl, tlx + 4, invLblY, C_LABEL);
    }

    private void drawInventory() {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                int slot = 9 + r * 9 + c;
                int sx = invX + c * SLOT, sy = invMainY + r * SLOT;
                boolean h = hovInvSlot == slot;
                drawSlot(sx, sy, C_SLOT_T, h, true);
                ItemStack it = mc.thePlayer.inventory.mainInventory[slot];
                if (it != null) renderItem(it, sx + 1, sy + 1);
            }
        }
        int sel = mc.thePlayer.inventory.currentItem;
        for (int c = 0; c < 9; c++) {
            int sx = invX + c * SLOT;
            boolean h = hovInvSlot == c;
            drawSlot(sx, hotbarY, C_SLOT_T, h, true);
            ItemStack it = mc.thePlayer.inventory.mainInventory[c];
            if (it != null) renderItem(it, sx + 1, hotbarY + 1);
        }
        // Hotbar selected : bordure or
        int ssx = invX + sel * SLOT;
        drawRect(ssx, hotbarY,                  ssx + SLOT, hotbarY + 1,           C_GOLD);
        drawRect(ssx, hotbarY + SLOT - 1,       ssx + SLOT, hotbarY + SLOT,        C_GOLD);
        drawRect(ssx, hotbarY,                  ssx + 1,    hotbarY + SLOT,        C_GOLD);
        drawRect(ssx + SLOT - 1, hotbarY,       ssx + SLOT, hotbarY + SLOT,        C_GOLD);
    }

    private void drawTooltip(int mx, int my) {
        ItemStack it = null;
        if      (hovMySlot  >= 0 && hovMySlot  < TradePacketHandler.myOfferCount)
            it = TradePacketHandler.myOffer[hovMySlot];
        else if (hovPtSlot  >= 0 && hovPtSlot  < TradePacketHandler.partnerOfferCount)
            it = TradePacketHandler.partnerOffer[hovPtSlot];
        else if (hovInvSlot >= 0)
            it = mc.thePlayer.inventory.mainInventory[hovInvSlot];
        if (it == null) return;
        try {
            List<String> tt = it.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips);
            if (!tt.isEmpty()) drawHoveringText(tt, mx, my);
        } catch (Exception ignored) {}
    }

    // ── Render item ──────────────────────────────────────────────────────────

    private void renderItem(ItemStack item, int x, int y) {
        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        mc.getRenderItem().renderItemAndEffectIntoGUI(item, x, y);
        mc.getRenderItem().renderItemOverlayIntoGUI(fontRendererObj, item, x, y, null);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableDepth();
    }

    // ── Monnaie ──────────────────────────────────────────────────────────────

    private void adjustMoney(long delta) {
        long v = Math.max(0L, Math.min(playerBalance, myMoney + delta));
        if (v == myMoney) return;
        myMoney = v;
        if (moneyField != null) moneyField.setText(v == 0 ? "" : String.valueOf(v));
        TradePacketHandler.sendMoneyOffer(myMoney);
    }

    private void commitMoney() {
        if (moneyField == null) return;
        String txt = moneyField.getText().trim();
        long v;
        if (txt.isEmpty()) v = 0L;
        else {
            try { v = Math.max(0L, Long.parseLong(txt)); }
            catch (NumberFormatException e) { return; }
        }
        // Défensif : cap par le solde (la validation keyTyped est censée empêcher ça)
        v = Math.min(v, playerBalance);
        if (v == myMoney) return;
        myMoney = v;
        TradePacketHandler.sendMoneyOffer(myMoney);
    }

    private static String fmtMoney(long v) {
        if (v >= 1_000_000L) return String.format("%.1fM", v / 1_000_000.0);
        if (v >= 10_000L)    return String.format("%.1fk", v / 1_000.0);
        return String.valueOf(v);
    }

    // ── PB ───────────────────────────────────────────────────────────────────

    private void adjustPB(int delta) {
        int v = Math.max(0, Math.min(playerPB, myPB + delta));
        if (v == myPB) return;
        myPB = v;
        if (pbField != null) pbField.setText(v == 0 ? "" : String.valueOf(v));
        TradePacketHandler.sendPBOffer(myPB);
    }

    private void commitPB() {
        if (pbField == null) return;
        String txt = pbField.getText().trim();
        int v;
        if (txt.isEmpty()) v = 0;
        else {
            try { v = Math.max(0, Integer.parseInt(txt)); }
            catch (NumberFormatException e) { return; }
        }
        v = Math.min(v, playerPB);
        if (v == myPB) return;
        myPB = v;
        TradePacketHandler.sendPBOffer(myPB);
    }

    private static String fmtPB(int v) {
        if (v >= 1_000_000)  return String.format("%.1fM", v / 1_000_000.0);
        if (v >= 10_000)     return String.format("%.1fk", v / 1_000.0);
        return String.valueOf(v);
    }

    // ── Hit-test ─────────────────────────────────────────────────────────────

    private int hitGrid(int mx, int my, int gx) {
        if (mx < gx || my < gridY || mx >= gx + GRID_W || my >= gridY + GRID_H) return -1;
        return ((my - gridY) / SLOT) * GRID_C + (mx - gx) / SLOT;
    }

    private int hitInv(int mx, int my) {
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) {
            int sx = invX + c * SLOT, sy = invMainY + r * SLOT;
            if (mx >= sx && mx < sx + SLOT && my >= sy && my < sy + SLOT) return 9 + r * 9 + c;
        }
        for (int c = 0; c < 9; c++) {
            int sx = invX + c * SLOT;
            if (mx >= sx && mx < sx + SLOT && my >= hotbarY && my < hotbarY + SLOT) return c;
        }
        return -1;
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ── Util ─────────────────────────────────────────────────────────────────

    private static int aScale(int argb, float f) {
        int a = Math.max(0, Math.min(255, (int)(((argb >>> 24) & 0xFF) * f)));
        return (a << 24) | (argb & 0x00FFFFFF);
    }
}
