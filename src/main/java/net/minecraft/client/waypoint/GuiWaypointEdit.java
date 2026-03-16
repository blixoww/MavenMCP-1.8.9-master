package net.minecraft.client.waypoint;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import java.io.IOException;

/**
 * GUI pour créer ou éditer un waypoint.
 */
public class GuiWaypointEdit extends GuiScreen {

    private final GuiScreen parent;
    private final Waypoint  editing;

    private GuiTextField fieldName;
    private GuiTextField fieldX;
    private GuiTextField fieldY;
    private GuiTextField fieldZ;

    private boolean beamEnabled   = true;
    private boolean coordsEnabled = true;
    private int     selectedColorIndex = 0;
    private Waypoint.TextSize textSize = Waypoint.TextSize.MEDIUM;

    private static final int[][] COLORS = {
            {255, 255, 255}, {255,  80,  80}, { 80, 255,  80}, { 80, 140, 255},
            {255, 255,  60}, {180,  60, 255}, { 60, 230, 230}, {255, 165,  50},
            {255, 110, 190}, {255, 200, 100}, {150, 255, 150}, {100, 200, 255},
    };
    private static final String[] COLOR_NAMES = {
            "Blanc","Rouge","Vert","Bleu","Jaune","Violet","Cyan","Orange","Rose","Or","Vert cl.","Ciel"
    };

    private static final int SWATCH_SIZE      = 16;
    private static final int SWATCH_GAP       = 4;
    private static final int SWATCHES_PER_ROW = 6;

    // ── Positions Y fixes ──
    private static final int Y_TITLE          = 15;
    private static final int Y_SEP1           = 27;
    private static final int Y_LABEL_NOM      = 35;
    private static final int Y_FIELD_NOM      = 45;
    private static final int Y_LABEL_COORDS   = 70;
    private static final int Y_FIELD_COORDS   = 80;
    private static final int Y_LABEL_COLOR    = 107;
    private static final int Y_PALETTE_START  = 120;
    private static final int PALETTE_ROWS     = (COLORS.length + SWATCHES_PER_ROW - 1) / SWATCHES_PER_ROW;
    private static final int Y_AFTER_PALETTE  = Y_PALETTE_START + PALETTE_ROWS * (SWATCH_SIZE + SWATCH_GAP);
    private static final int Y_PREVIEW        = Y_AFTER_PALETTE + 8;
    private static final int Y_SEP2           = Y_PREVIEW + 20;
    private static final int Y_BTN_ROW1       = Y_SEP2 + 10;
    private static final int Y_BTN_ROW2       = Y_BTN_ROW1 + 24;
    private static final int Y_BTN_ROW3       = Y_BTN_ROW2 + 24;
    private static final int Y_BTN_ROW4       = Y_BTN_ROW3 + 24;

    public GuiWaypointEdit(GuiScreen parent, Waypoint editing, int editIndex) {
        this.parent  = parent;
        this.editing = editing;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int cx = this.width / 2;

        this.fieldName = new GuiTextField(10, this.fontRendererObj, cx - 80, Y_FIELD_NOM, 160, 18);
        this.fieldName.setMaxStringLength(32);
        this.fieldName.setFocused(true);

        this.fieldX = new GuiTextField(11, this.fontRendererObj, cx - 80, Y_FIELD_COORDS, 48, 18);
        this.fieldX.setMaxStringLength(8);
        this.fieldY = new GuiTextField(12, this.fontRendererObj, cx - 24, Y_FIELD_COORDS, 48, 18);
        this.fieldY.setMaxStringLength(8);
        this.fieldZ = new GuiTextField(13, this.fontRendererObj, cx + 32, Y_FIELD_COORDS, 48, 18);
        this.fieldZ.setMaxStringLength(8);

        if (editing != null) {
            fieldName.setText(editing.getName());
            fieldX.setText(String.valueOf(editing.getX()));
            fieldY.setText(String.valueOf(editing.getY()));
            fieldZ.setText(String.valueOf(editing.getZ()));
            beamEnabled   = editing.isBeamVisible();
            coordsEnabled = editing.isCoordsVisible();
            textSize      = editing.getTextSize();
            for (int i = 0; i < COLORS.length; i++) {
                if (COLORS[i][0] == editing.getColorR()
                        && COLORS[i][1] == editing.getColorG()
                        && COLORS[i][2] == editing.getColorB()) {
                    selectedColorIndex = i; break;
                }
            }
        } else if (Minecraft.getMinecraft().thePlayer != null) {
            fieldName.setText("Waypoint");
            fieldX.setText(String.valueOf((int) Minecraft.getMinecraft().thePlayer.posX));
            fieldY.setText(String.valueOf((int) Minecraft.getMinecraft().thePlayer.posY));
            fieldZ.setText(String.valueOf((int) Minecraft.getMinecraft().thePlayer.posZ));
        }

        this.buttonList.add(new GuiButton(1, cx - 83, Y_BTN_ROW1,      80, 20, getBeamText()));
        this.buttonList.add(new GuiButton(2, cx +  3, Y_BTN_ROW1,      80, 20, getCoordsText()));
        this.buttonList.add(new GuiButton(7, cx - 83, Y_BTN_ROW2,     166, 20, getTextSizeText()));
        this.buttonList.add(new GuiButton(5, cx - 83, Y_BTN_ROW3,     166, 20, "§aSauvegarder"));
        this.buttonList.add(new GuiButton(6, cx - 83, Y_BTN_ROW4,     166, 20, "§cAnnuler"));
    }

    private String getBeamText()     { return beamEnabled   ? "§b★ Beam: ON"  : "§8★ Beam: OFF"; }
    private String getCoordsText()   { return coordsEnabled ? "§eXYZ: ON"     : "§8XYZ: OFF"; }
    private String getTextSizeText() {
        String col = textSize == Waypoint.TextSize.LARGE  ? "§a"
                : textSize == Waypoint.TextSize.MEDIUM ? "§e" : "§7";
        return "§fTaille texte: " + col + textSize.label;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int cx = this.width / 2;

        // ── Titre centré ───────────────────────────────────────────────────
        this.drawCenteredString(this.fontRendererObj,
                editing != null ? "§l§6✎ Modifier un waypoint" : "§l§6✚ Nouveau waypoint",
                cx, Y_TITLE, 0xFFFFFF);
        drawHorizontalLine(cx - 90, cx + 90, Y_SEP1, 0x55FFFFFF);

        // ── Nom ────────────────────────────────────────────────────────────
        drawSectionLabel("Nom", cx - 80, Y_LABEL_NOM);
        this.fieldName.drawTextBox();

        // ── Coords ─────────────────────────────────────────────────────────
        drawSectionLabel("Coordonnées", cx - 80, Y_LABEL_COORDS);

        this.fieldX.drawTextBox();
        this.fieldY.drawTextBox();
        this.fieldZ.drawTextBox();

        // ── Palette couleur ────────────────────────────────────────────────
        drawSectionLabel("Couleur du beam", cx - 80, Y_LABEL_COLOR);

        int paletteW  = SWATCHES_PER_ROW * (SWATCH_SIZE + SWATCH_GAP) - SWATCH_GAP;
        int paletteX0 = cx - paletteW / 2;

        for (int i = 0; i < COLORS.length; i++) {
            int sc  = i % SWATCHES_PER_ROW;
            int row = i / SWATCHES_PER_ROW;
            int sx  = paletteX0 + sc  * (SWATCH_SIZE + SWATCH_GAP);
            int sy  = Y_PALETTE_START + row * (SWATCH_SIZE + SWATCH_GAP);
            int sx2 = sx + SWATCH_SIZE;
            int sy2 = sy + SWATCH_SIZE;
            int rgb = 0xFF000000 | (COLORS[i][0] << 16) | (COLORS[i][1] << 8) | COLORS[i][2];

            boolean sel = (i == selectedColorIndex);
            boolean hov = mouseX >= sx && mouseX <= sx2 && mouseY >= sy && mouseY <= sy2;

            drawRect(sx - 1, sy - 1, sx2 + 1, sy2 + 1,
                    sel ? 0xFFFFFFFF : hov ? 0xAAFFFFFF : 0x44FFFFFF);
            drawRect(sx, sy, sx2, sy2, rgb);

            if (sel) {
                this.fontRendererObj.drawString("§f✔", sx + 4, sy + 4, 0xFFFFFF);
            }
        }

        // ── Aperçu couleur sélectionnée ────────────────────────────────────
        int[] swatch = COLORS[selectedColorIndex];
        int previewRgb = 0xFF000000 | (swatch[0] << 16) | (swatch[1] << 8) | swatch[2];
        drawRect(cx - 80, Y_PREVIEW, cx + 80, Y_PREVIEW + 13, 0x55000000);
        drawRect(cx - 78, Y_PREVIEW + 1, cx - 65, Y_PREVIEW + 12, 0xFF111111);
        drawRect(cx - 77, Y_PREVIEW + 2, cx - 66, Y_PREVIEW + 11, previewRgb);
        this.fontRendererObj.drawString(
                "§7Couleur : §f" + COLOR_NAMES[selectedColorIndex],
                cx - 62, Y_PREVIEW + 3, 0xFFFFFF);

        drawHorizontalLine(cx - 90, cx + 90, Y_SEP2, 0x44FFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /** Label de section : petit texte gris au-dessus d'un élément. */
    private void drawSectionLabel(String text, int x, int y) {
        this.fontRendererObj.drawString("§7" + text, x, y, 0xAAAAAA);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.fieldName.mouseClicked(mouseX, mouseY, mouseButton);
        this.fieldX.mouseClicked(mouseX, mouseY, mouseButton);
        this.fieldY.mouseClicked(mouseX, mouseY, mouseButton);
        this.fieldZ.mouseClicked(mouseX, mouseY, mouseButton);

        int paletteW  = SWATCHES_PER_ROW * (SWATCH_SIZE + SWATCH_GAP) - SWATCH_GAP;
        int paletteX0 = this.width / 2 - paletteW / 2;
        for (int i = 0; i < COLORS.length; i++) {
            int sc  = i % SWATCHES_PER_ROW;
            int row = i / SWATCHES_PER_ROW;
            int sx  = paletteX0 + sc  * (SWATCH_SIZE + SWATCH_GAP);
            int sy  = Y_PALETTE_START + row * (SWATCH_SIZE + SWATCH_GAP);
            if (mouseX >= sx && mouseX <= sx + SWATCH_SIZE
                    && mouseY >= sy && mouseY <= sy + SWATCH_SIZE) {
                selectedColorIndex = i;
                return;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { this.mc.displayGuiScreen(parent); return; }
        this.fieldName.textboxKeyTyped(typedChar, keyCode);
        this.fieldX.textboxKeyTyped(typedChar, keyCode);
        this.fieldY.textboxKeyTyped(typedChar, keyCode);
        this.fieldZ.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 1: beamEnabled   = !beamEnabled;   button.displayString = getBeamText();   break;
            case 2: coordsEnabled = !coordsEnabled; button.displayString = getCoordsText(); break;
            case 5: saveWaypoint(); break;
            case 6: this.mc.displayGuiScreen(parent); break;
            case 7: textSize = textSize.next(); button.displayString = getTextSizeText(); break;
        }
    }

    private void saveWaypoint() {
        String name = fieldName.getText().trim();
        if (name.isEmpty()) name = "Waypoint";
        int x, y, z;
        try {
            x = Integer.parseInt(fieldX.getText().trim());
            y = Integer.parseInt(fieldY.getText().trim());
            z = Integer.parseInt(fieldZ.getText().trim());
        } catch (NumberFormatException e) { return; }

        int[] c = COLORS[selectedColorIndex];
        if (editing != null) {
            editing.setName(name); editing.setX(x); editing.setY(y); editing.setZ(z);
            editing.setColor(c[0], c[1], c[2]);
            editing.setBeamVisible(beamEnabled);
            editing.setCoordsVisible(coordsEnabled);
            editing.setTextSize(textSize);
            WaypointManager.INSTANCE.save();
        } else {
            Waypoint wp = new Waypoint(name, x, y, z, c[0], c[1], c[2]);
            wp.setBeamVisible(beamEnabled);
            wp.setCoordsVisible(coordsEnabled);
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

    @Override
    public boolean doesGuiPauseGame() { return false; }
}