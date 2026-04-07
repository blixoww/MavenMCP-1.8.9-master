package net.optifine.gui;

import java.awt.Color;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMenuButton;
import net.minecraft.client.gui.GuiOptionButton;
import net.minecraft.client.gui.GuiRenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.MathHelper;

/**
 * Base commune RED CONFLICT pour tous les sous-menus OptiFine.
 * Fournit le header animé, le fond panel sombre, les boutons stylisés
 * et le layout 2-colonnes centré.
 */
public abstract class GuiOFSettingsBase extends GuiScreen {

    protected GuiScreen prevScreen;
    protected String title = "";
    protected GameSettings settings;

    // Style RED CONFLICT
    private float animation = 0.0f;
    private long lastTime  = -1L;
    protected static final int ACCENT = new Color(220, 30, 30).getRGB();

    // Layout
    protected static final int BTN_W   = 150;
    protected static final int BTN_H   = 20;
    protected static final int COL_GAP = 10;
    protected static final int BTN_GAP = 3;
    protected static final int HEADER_H = 36;

    /** ID du bouton Done */
    protected static final int BTN_DONE = 200;

    // ── Sous-classes doivent implémenter ────────────────────────────────────

    /** Retourne le tableau d'options à afficher dans la grille. */
    protected abstract GameSettings.Options[] getOptions();

    /** Titre i18n du menu. */
    protected abstract String buildTitle();

    /** Actions spécifiques à la sous-classe (appelé après les options génériques). */
    protected void onActionPerformed(GuiButton btn) throws IOException {}

    /** Nombre de lignes de boutons supplémentaires au-dessus du Done (défaut 0). */
    protected int extraButtonRows() { return 0; }

    /** Boutons supplémentaires ajoutés sous le Done (ex: All On / All Off). */
    protected void addExtraButtons(int doneX, int doneY, int doneW) {}

    // ── initGui ─────────────────────────────────────────────────────────────

    @Override
    public void initGui() {
        this.title = buildTitle();
        this.buttonList.clear();
        this.lastTime = Minecraft.getSystemTime();

        GameSettings.Options[] opts = getOptions();
        int cx       = this.width  / 2;
        int colLeft  = cx - BTN_W - COL_GAP / 2;
        int colRight = cx + COL_GAP / 2;

        // pY : haut du panel de contenu
        // section label : 8px texte + 4px top-pad + 6px gap bas = 18px
        // startY : premier bouton situé 18px sous le haut du panel
        int pY     = HEADER_H + 8;
        int startY = pY + 18 + 4;          // panel top + label height + small gap

        for (int i = 0; i < opts.length; i++) {
            GameSettings.Options opt = opts[i];
            int col = i % 2;
            int row = i / 2;
            int bx  = (col == 0) ? colLeft : colRight;
            int by  = startY + row * (BTN_H + BTN_GAP);

            if (opt.getEnumFloat())
                this.buttonList.add(new GuiOptionSliderOF(opt.returnEnumOrdinal(), bx, by, opt));
            else
                this.buttonList.add(new GuiOptionButtonOF(opt.returnEnumOrdinal(), bx, by, opt,
                        this.settings.getKeyBinding(opt)));
        }

        int rows      = (opts.length + 1) / 2;
        int gridEndY  = startY + rows * (BTN_H + BTN_GAP);
        // Espace pour les extra-boutons : chaque ligne occupe BTN_H + BTN_GAP
        int extraRows = extraButtonRows();
        int doneY     = gridEndY + 8 + extraRows * (BTN_H + BTN_GAP);
        int doneW     = BTN_W * 2 + COL_GAP;

        // Les extra-boutons se placent entre la fin de grille et le Done
        addExtraButtons(colLeft, doneY, doneW);

        this.buttonList.add(new GuiMenuButton(BTN_DONE, colLeft, doneY, doneW, BTN_H,
                I18n.format("gui.done"), true));
    }

    // ── actionPerformed ─────────────────────────────────────────────────────

    @Override
    protected void actionPerformed(GuiButton btn) throws IOException {
        if (!btn.enabled) return;

        if (btn.id < BTN_DONE && btn instanceof GuiOptionButton) {
            this.settings.setOptionValue(((GuiOptionButton) btn).returnEnumOptions(), 1);
            btn.displayString = this.settings.getKeyBinding(
                    GameSettings.Options.getEnumOptions(btn.id));
        }

        if (btn.id == BTN_DONE) {
            this.mc.gameSettings.saveOptions();
            this.mc.displayGuiScreen(this.prevScreen);
            return;
        }

        onActionPerformed(btn);
    }

    // ── drawScreen ──────────────────────────────────────────────────────────

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Animation slide-in
        long now = Minecraft.getSystemTime();
        if (lastTime >= 0)
            animation = MathHelper.clamp_float(animation + (now - lastTime) / 1000.0f * 5.0f, 0.0f, 1.0f);
        lastTime = now;
        float e = animation * animation * (3.0f - 2.0f * animation);

        // Fond : gradient du monde visible derrière, puis léger overlay sombre
        this.drawDefaultBackground();
        Gui.drawRect(0, 0, this.width, this.height, (int)(e * 140) << 24 | 0x050505);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, (1.0f - e) * 10, 0);

        int textAlpha = (int)(e * 255) << 24;
        int cx = this.width / 2;

        // ── Header bar (full width) ──────────────────────────────────────────
        Gui.drawRect(0, 0, this.width, HEADER_H, (int)(e * 220) << 24 | 0x0C0C0C);
        Gui.drawRect(0, 0, this.width, 1, textAlpha | (ACCENT & 0xFFFFFF));
        Gui.drawRect(0, HEADER_H - 1, this.width, HEADER_H, (int)(e * 40) << 24 | 0xFFFFFF);

        // Titre "RED CONFLICT | <sous-titre>"
        String t1  = "§c§lRED ", t2 = "§f§lCONFLICT", sep = " §8| §7";
        int w1 = fr(t1), w2 = fr(t2), w3 = fr(sep);
        int tX = cx - (w1 + w2 + w3 + fr(title)) / 2;
        int tY = (HEADER_H - 8) / 2;
        this.fontRendererObj.drawStringWithShadow(t1,   tX,                tY, textAlpha | 0xFFFFFF);
        this.fontRendererObj.drawStringWithShadow(t2,   tX + w1,           tY, textAlpha | 0xFFFFFF);
        this.fontRendererObj.drawStringWithShadow(sep,  tX + w1 + w2,      tY, textAlpha | 0xFFFFFF);
        this.fontRendererObj.drawStringWithShadow(title,tX + w1 + w2 + w3, tY, textAlpha | 0xFFFFFF);

        // ── Content panel ────────────────────────────────────────────────────
        GameSettings.Options[] opts = getOptions();
        int rows      = (opts.length + 1) / 2;
        int extraRows = extraButtonRows();
        int pX = cx - BTN_W - COL_GAP / 2 - 6;
        int pW = BTN_W * 2 + COL_GAP + 12;
        int pY = HEADER_H + 8;   // doit correspondre à initGui
        // label (18px) + gap (4px) + grille + gap (8px) + extra rows + done + padding bas (8px)
        int pH = 18 + 4
               + rows * (BTN_H + BTN_GAP)
               + 8
               + extraRows * (BTN_H + BTN_GAP)
               + BTN_H + 8;
        GuiRenderUtils.drawShadow(pX, pY, pW, pH, 5, (int)(e * 80));
        Gui.drawRect(pX, pY, pX + pW, pY + pH, (int)(e * 190) << 24 | 0x080808);
        Gui.drawRect(pX, pY, pX + pW, pY + 1, textAlpha | (ACCENT & 0xFFFFFF));
        GuiRenderUtils.drawRectOutline(pX, pY, pW, pH, (int)(e * 30) << 24 | 0xFFFFFF);

        // Section header inside panel
        GuiRenderUtils.drawSectionHeader(this.fontRendererObj, pX + 6, pY + 4, pW - 12, title, ACCENT);

        // ── Boutons ───────────────────────────────────────────────────────────
        super.drawScreen(mouseX, mouseY, partialTicks);

        GlStateManager.popMatrix();
    }

    // ── Tooltip (sous-classes avec tooltip doivent override) ────────────────
    // (les sous-classes qui ont un TooltipManager l'appellent dans onDraw)

    protected int fr(String s) {
        return this.fontRendererObj.getStringWidth(s);
    }
}

