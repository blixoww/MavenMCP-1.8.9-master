package net.minecraft.client.waypoint;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMenuButton;
import net.minecraft.client.gui.GuiRenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * GUI pour créer ou éditer un waypoint — style "Red Conflict" (inspiré GuiIngameMenu).
 */
public class GuiWaypointEdit extends GuiScreen {

    private final GuiScreen parent;
    private final Waypoint  editing;
    private final int editIndex;

    private GuiTextField fieldName;
    private GuiTextField fieldX;
    private GuiTextField fieldY;
    private GuiTextField fieldZ;

    private boolean beamEnabled   = true;
    private boolean coordsEnabled = true;
    private boolean labelEnabled  = true;
    private int     selectedColorIndex = 0;
    private Waypoint.TextSize textSize = Waypoint.TextSize.MEDIUM;

    // ── Animation ──────────────────────────────────────────────────────────
    private float animation = 0.0f;
    private long  lastTime  = -1L;

    // ── Palette ────────────────────────────────────────────────────────────
    private static final int[][] COLORS = {
            {255, 255, 255}, {255,  80,  80}, { 80, 255,  80}, { 80, 140, 255},
            {255, 255,  60}, {180,  60, 255}, { 60, 230, 230}, {255, 165,  50},
            {255, 110, 190}, {255, 200, 100}, {150, 255, 150}, {100, 200, 255},
    };
    private static final String[] COLOR_NAMES = {
            "Blanc","Rouge","Vert","Bleu","Jaune","Violet","Cyan","Orange","Rose","Or","Vert cl.","Ciel"
    };

    // ── Style ──────────────────────────────────────────────────────────────
    private static final int ACCENT      = 0xFFDC1E1E;
    private static final int C_BG        = 0xEE0A0D14;
    private static final int C_HEADER    = 0xFF141926;
    private static final int C_TEXT      = 0xFFE0E0E0;
    private static final int C_MUTED     = 0xFF707880;
    private static final int SWATCH_SIZE = 16;
    private static final int SWATCH_GAP  = 4;
    private static final int SWATCHES_PER_ROW = 6;

    // ── Layout (calculé dans initGui) ──────────────────────────────────────
    private int pX, pY, pW, pH;

    public GuiWaypointEdit(GuiScreen parent, Waypoint editing, int editIndex) {
        this.parent    = parent;
        this.editing   = editing;
        this.editIndex = editIndex;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.animation = 0.0f;
        this.lastTime  = -1L;

        // Dimensions du panneau
        pW = Math.min(290, this.width - 20);
        pH = 280;
        pX = (this.width  - pW) / 2;
        pY = (this.height - pH) / 2;

        int cx   = pX + pW / 2;
        int rowH = 20;

        // ── Champ Nom ──────────────────────────────────────────────────────
        int nameY = pY + 50;
        this.fieldName = new GuiTextField(0, this.fontRendererObj, pX + 15, nameY, pW - 30, rowH);
        this.fieldName.setMaxStringLength(32);
        this.fieldName.setFocused(true);

        // ── Champs Coordonnées ─────────────────────────────────────────────
        int coordY = pY + 100;
        int coordW = (pW - 50) / 3;
        this.fieldX = new GuiTextField(1, this.fontRendererObj, pX + 15,                  coordY, coordW, rowH);
        this.fieldY = new GuiTextField(2, this.fontRendererObj, pX + 15 + coordW + 8,     coordY, coordW, rowH);
        this.fieldZ = new GuiTextField(3, this.fontRendererObj, pX + 15 + (coordW + 8)*2, coordY, coordW, rowH);
        this.fieldX.setMaxStringLength(10);
        this.fieldY.setMaxStringLength(10);
        this.fieldZ.setMaxStringLength(10);

        // Valeurs par défaut
        if (editing != null) {
            fieldName.setText(editing.getName());
            fieldX.setText(String.valueOf(editing.getX()));
            fieldY.setText(String.valueOf(editing.getY()));
            fieldZ.setText(String.valueOf(editing.getZ()));
            beamEnabled   = editing.isBeamVisible();
            coordsEnabled = editing.isCoordsVisible();
            labelEnabled  = editing.isLabelVisible();
            textSize      = editing.getTextSize();
            for (int i = 0; i < COLORS.length; i++) {
                if (COLORS[i][0] == editing.getColorR() && COLORS[i][1] == editing.getColorG() && COLORS[i][2] == editing.getColorB()) {
                    selectedColorIndex = i; break;
                }
            }
        } else if (Minecraft.getMinecraft().thePlayer != null) {
            fieldName.setText("Waypoint");
            fieldX.setText(String.valueOf((int) Minecraft.getMinecraft().thePlayer.posX));
            fieldY.setText(String.valueOf((int) Minecraft.getMinecraft().thePlayer.posY));
            fieldZ.setText(String.valueOf((int) Minecraft.getMinecraft().thePlayer.posZ));
        }

        // ── Boutons Options ────────────────────────────────────────────────
        int optY = pY + 140;
        int optW = (pW - 42) / 4;
        this.buttonList.add(new GuiMenuButton(10, pX + 15,               optY, optW, rowH, getBeamText()));
        this.buttonList.add(new GuiMenuButton(11, pX + 15 + (optW+4),   optY, optW, rowH, getCoordsText()));
        this.buttonList.add(new GuiMenuButton(13, pX + 15 + (optW+4)*2, optY, optW, rowH, getLabelText()));
        this.buttonList.add(new GuiMenuButton(12, pX + 15 + (optW+4)*3, optY, optW, rowH, getTextSizeText()));

        // ── Boutons Sauver / Annuler ───────────────────────────────────────
        int btnW = pW / 2 - 12;
        int btnY = pY + pH - 30;
        this.buttonList.add(new GuiMenuButton(20, pX + 8,        btnY, btnW, 22, "§aSAUVEGARDER", true));
        this.buttonList.add(new GuiMenuButton(21, pX + pW/2 + 4, btnY, btnW, 22, "§cANNULER"));
    }

    // ── Textes des boutons options ─────────────────────────────────────────
    private String getBeamText()     { return beamEnabled   ? "Beam §a✔" : "Beam §c✗"; }
    private String getCoordsText()   { return coordsEnabled ? "XYZ §a✔"  : "XYZ §c✗";  }
    private String getLabelText()    { return labelEnabled  ? "Label §a✔": "Label §c✗"; }
    private String getTextSizeText() {
        return (textSize == Waypoint.TextSize.LARGE ? "§a" : textSize == Waypoint.TextSize.MEDIUM ? "§e" : "§7") + textSize.label;
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // ── Animation entrée ──────────────────────────────────────────────
        long now = Minecraft.getSystemTime();
        if (lastTime != -1L) {
            float dt = (now - lastTime) / 1000.0f;
            animation = MathHelper.clamp_float(animation + dt * 4.0f, 0.0f, 1.0f);
        }
        lastTime = now;

        float eased = animation * animation * (3.0f - 2.0f * animation);

        // Fond obscurci animé
        this.drawRect(0, 0, this.width, this.height, (int)(eased * 160) << 24);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, (1.0f - eased) * 12, 0);

        // ── Panneau principal ─────────────────────────────────────────────
        GuiRenderUtils.drawShadow(pX, pY, pW, pH, 10, (int)(eased * 120));
        Gui.drawRect(pX, pY, pX + pW, pY + pH, (int)(eased * 220) << 24 | 0x0A0D14);
        // Ligne accent rouge en haut
        Gui.drawRect(pX, pY, pX + pW, pY + 1, (int)(eased * 255) << 24 | (ACCENT & 0xFFFFFF));
        // Header dégradé
        GuiRenderUtils.drawGradientRect(pX, pY + 1, pX + pW, pY + 32, (int)(eased * 180) << 24 | 0x141926, 0x00141926);
        // Bordure subtile
        GuiRenderUtils.drawRectOutline(pX, pY, pW, pH, (int)(eased * 35) << 24 | 0xFFFFFF);

        // ── Titre ─────────────────────────────────────────────────────────
        int textAlpha = (int)(eased * 255) << 24;
        String t1 = editing == null ? "§c§lNOUVEAU " : "§c§lEDITION ";
        String t2 = "§f§lWAYPOINT";
        int tw = fontRendererObj.getStringWidth(t1) + fontRendererObj.getStringWidth(t2);
        int titleX = pX + pW / 2 - tw / 2;
        fontRendererObj.drawStringWithShadow(t1, titleX, pY + 10, textAlpha | 0xFFFFFF);
        fontRendererObj.drawStringWithShadow(t2, titleX + fontRendererObj.getStringWidth(t1), pY + 10, textAlpha | 0xFFFFFF);
        // Diviseur sous le titre
        int divW = (int)((tw + 16) * eased);
        Gui.drawRect(pX + pW / 2 - divW / 2, pY + 22, pX + pW / 2 + divW / 2, pY + 23, (int)(eased * 45) << 24 | 0xFFFFFF);

        // ── Champ Nom ─────────────────────────────────────────────────────
        fontRendererObj.drawString("§7Nom", pX + 15, pY + 39, textAlpha | (C_MUTED & 0xFFFFFF));
        drawStyledField(fieldName);

        // ── Champs XYZ ────────────────────────────────────────────────────
        fontRendererObj.drawString("§7X", fieldX.xPosition + 2, pY + 89, textAlpha | (C_MUTED & 0xFFFFFF));
        fontRendererObj.drawString("§7Y", fieldY.xPosition + 2, pY + 89, textAlpha | (C_MUTED & 0xFFFFFF));
        fontRendererObj.drawString("§7Z", fieldZ.xPosition + 2, pY + 89, textAlpha | (C_MUTED & 0xFFFFFF));
        drawStyledField(fieldX);
        drawStyledField(fieldY);
        drawStyledField(fieldZ);

        // ── Label Options ─────────────────────────────────────────────────
        fontRendererObj.drawString("§7Options", pX + 15, pY + 130, textAlpha | (C_MUTED & 0xFFFFFF));

        // ── Palette Couleur ───────────────────────────────────────────────
        int palY = pY + 175;
        String colLabel = "§7Couleur : §f" + COLOR_NAMES[selectedColorIndex];
        fontRendererObj.drawString(colLabel, pX + 15, palY - 12, textAlpha | 0xFFFFFF);

        int paletteW = SWATCHES_PER_ROW * (SWATCH_SIZE + SWATCH_GAP) - SWATCH_GAP;
        int paletteX = pX + pW / 2 - paletteW / 2;

        for (int i = 0; i < COLORS.length; i++) {
            int col   = i % SWATCHES_PER_ROW;
            int row   = i / SWATCHES_PER_ROW;
            int sx    = paletteX + col * (SWATCH_SIZE + SWATCH_GAP);
            int sy    = palY + row * (SWATCH_SIZE + SWATCH_GAP);
            int colorInt = 0xFF000000 | (COLORS[i][0] << 16) | (COLORS[i][1] << 8) | COLORS[i][2];
            boolean isHov = mouseX >= sx && mouseX < sx + SWATCH_SIZE && mouseY >= sy && mouseY < sy + SWATCH_SIZE;
            boolean isSel = (i == selectedColorIndex);

            if (isSel) {
                // Halo blanc sélectionné
                Gui.drawRect(sx - 2, sy - 2, sx + SWATCH_SIZE + 2, sy + SWATCH_SIZE + 2, 0xFFFFFFFF);
            } else if (isHov) {
                Gui.drawRect(sx - 1, sy - 1, sx + SWATCH_SIZE + 1, sy + SWATCH_SIZE + 1, 0xFFAAAAAA);
            }
            Gui.drawRect(sx, sy, sx + SWATCH_SIZE, sy + SWATCH_SIZE, colorInt);
        }

        // ── Diviseur avant boutons ────────────────────────────────────────
        Gui.drawRect(pX + 8, pY + pH - 40, pX + pW - 8, pY + pH - 39, (int)(eased * 25) << 24 | 0xFFFFFF);

        // Buttons avec stagger
        if (btnYCache == null || btnYCache.length != this.buttonList.size()) {
            btnYCache = new int[this.buttonList.size()];
        }
        for (int i = 0; i < this.buttonList.size(); i++) {
            GuiButton b = this.buttonList.get(i);
            btnYCache[i] = b.yPosition;
            float stagger = i * 0.12f;
            float ba = MathHelper.clamp_float(animation * 1.5f - stagger, 0f, 1f);
            ba = ba * ba * (3f - 2f * ba);
            b.yPosition += (int)((1f - ba) * 12);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        for (int i = 0; i < this.buttonList.size(); i++)
            this.buttonList.get(i).yPosition = btnYCache[i];

        GlStateManager.popMatrix();
    }

    private int[] btnYCache;

    /** Dessine un champ de texte stylé avec bordure accent si focus. */
    private void drawStyledField(GuiTextField field) {
        int borderColor = field.isFocused() ? (ACCENT | 0xFF000000) : 0x33FFFFFF;
        Gui.drawRect(field.xPosition - 1, field.yPosition - 1,
                field.xPosition + field.getWidth() + 1, field.yPosition + field.getHeight() + 1, borderColor);
        Gui.drawRect(field.xPosition, field.yPosition,
                field.xPosition + field.getWidth(), field.yPosition + field.getHeight(), 0xFF050709);
        field.drawTextBox();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.fieldName.mouseClicked(mouseX, mouseY, mouseButton);
        this.fieldX.mouseClicked(mouseX, mouseY, mouseButton);
        this.fieldY.mouseClicked(mouseX, mouseY, mouseButton);
        this.fieldZ.mouseClicked(mouseX, mouseY, mouseButton);

        // Clic palette
        int paletteW = SWATCHES_PER_ROW * (SWATCH_SIZE + SWATCH_GAP) - SWATCH_GAP;
        int paletteX = pX + pW / 2 - paletteW / 2;
        int palY     = pY + 175;

        for (int i = 0; i < COLORS.length; i++) {
            int col = i % SWATCHES_PER_ROW;
            int row = i / SWATCHES_PER_ROW;
            int sx  = paletteX + col * (SWATCH_SIZE + SWATCH_GAP);
            int sy  = palY + row * (SWATCH_SIZE + SWATCH_GAP);
            if (mouseX >= sx && mouseX < sx + SWATCH_SIZE && mouseY >= sy && mouseY < sy + SWATCH_SIZE) {
                selectedColorIndex = i;
                return;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1)  { this.mc.displayGuiScreen(parent); return; }
        if (keyCode == 15) { // Tab
            if      (fieldName.isFocused()) { fieldName.setFocused(false); fieldX.setFocused(true); }
            else if (fieldX.isFocused())    { fieldX.setFocused(false);    fieldY.setFocused(true); }
            else if (fieldY.isFocused())    { fieldY.setFocused(false);    fieldZ.setFocused(true); }
            else if (fieldZ.isFocused())    { fieldZ.setFocused(false);    fieldName.setFocused(true); }
            return;
        }
        if (keyCode == 28) { saveWaypoint(); return; } // Enter

        this.fieldName.textboxKeyTyped(typedChar, keyCode);
        this.fieldX.textboxKeyTyped(typedChar, keyCode);
        this.fieldY.textboxKeyTyped(typedChar, keyCode);
        this.fieldZ.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 10: beamEnabled   = !beamEnabled;   button.displayString = getBeamText();     break;
            case 11: coordsEnabled = !coordsEnabled; button.displayString = getCoordsText();   break;
            case 13: labelEnabled  = !labelEnabled;  button.displayString = getLabelText();    break;
            case 12: textSize = textSize.next();     button.displayString = getTextSizeText(); break;
            case 20: saveWaypoint(); break;
            case 21: this.mc.displayGuiScreen(parent); break;
        }
    }

    private void saveWaypoint() {
        String name = fieldName.getText().trim();
        if (name.isEmpty()) name = "Waypoint";
        int x = 0, y = 64, z = 0;
        try { x = Integer.parseInt(fieldX.getText().trim()); } catch (NumberFormatException ignored) {}
        try { y = Integer.parseInt(fieldY.getText().trim()); } catch (NumberFormatException ignored) {}
        try { z = Integer.parseInt(fieldZ.getText().trim()); } catch (NumberFormatException ignored) {}
        int[] c = COLORS[selectedColorIndex];

        if (editing != null) {
            editing.setName(name);
            editing.setX(x); editing.setY(y); editing.setZ(z);
            editing.setColor(c[0], c[1], c[2]);
            editing.setBeamVisible(beamEnabled);
            editing.setCoordsVisible(coordsEnabled);
            editing.setLabelVisible(labelEnabled);
            editing.setTextSize(textSize);
            WaypointManager.INSTANCE.save();
        } else {
            Waypoint wp = new Waypoint(name, x, y, z, c[0], c[1], c[2]);
            wp.setBeamVisible(beamEnabled);
            wp.setCoordsVisible(coordsEnabled);
            wp.setLabelVisible(labelEnabled);
            wp.setTextSize(textSize);
            WaypointManager.INSTANCE.addWaypoint(wp);
        }

        this.mc.displayGuiScreen(parent);
    }

    @Override
    public void updateScreen() {
        this.fieldName.updateCursorCounter();
        this.fieldX.updateCursorCounter();
        this.fieldY.updateCursorCounter();
        this.fieldZ.updateCursorCounter();
    }
}
