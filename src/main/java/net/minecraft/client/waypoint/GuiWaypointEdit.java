package net.minecraft.client.waypoint;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * GUI pour créer ou éditer un waypoint.
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

    private static final int SWATCH_SIZE      = 20; // Plus grand pour mieux voir
    private static final int SWATCH_GAP       = 6;
    private static final int SWATCHES_PER_ROW = 6;

    public GuiWaypointEdit(GuiScreen parent, Waypoint editing, int editIndex) {
        this.parent  = parent;
        this.editing = editing;
        this.editIndex = editIndex;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        int cx = this.width / 2;
        int top = 40;

        // Champs de texte
        this.fieldName = new GuiTextField(0, this.fontRendererObj, cx - 100, top + 20, 200, 20);
        this.fieldName.setMaxStringLength(32);
        this.fieldName.setFocused(true);

        int coordsY = top + 65;
        this.fieldX = new GuiTextField(1, this.fontRendererObj, cx - 100, coordsY, 60, 20);
        this.fieldY = new GuiTextField(2, this.fontRendererObj, cx - 30, coordsY, 60, 20);
        this.fieldZ = new GuiTextField(3, this.fontRendererObj, cx + 40, coordsY, 60, 20);
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
            textSize      = editing.getTextSize();
            // Retrouver l'index couleur
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

        // Boutons Options (placés plus bas)
        int optY = top + 100;
        this.buttonList.add(new GuiButton(10, cx - 100, optY, 64, 20, getBeamText()));
        this.buttonList.add(new GuiButton(11, cx - 32, optY, 64, 20, getCoordsText()));
        this.buttonList.add(new GuiButton(12, cx + 36, optY, 64, 20, getTextSizeText()));

        // Boutons Sauver / Annuler (tout en bas)
        this.buttonList.add(new GuiButton(20, cx - 102, this.height - 40, 100, 20, "\u00a7aSauvegarder"));
        this.buttonList.add(new GuiButton(21, cx + 2, this.height - 40, 100, 20, "\u00a7cAnnuler"));
    }

    private String getBeamText()     { return beamEnabled   ? "\u00a7bBeam: ON"  : "\u00a77Beam: OFF"; }
    private String getCoordsText()   { return coordsEnabled ? "\u00a7eXYZ: ON"     : "\u00a77XYZ: OFF"; }
    private String getTextSizeText() {
        return (textSize == Waypoint.TextSize.LARGE ? "\u00a7a" : textSize == Waypoint.TextSize.MEDIUM ? "\u00a7e" : "\u00a77") + textSize.label;
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int cx = this.width / 2;
        int top = 40;

        // Titre
        this.drawCenteredString(this.fontRendererObj, editing == null ? "\u00a7l\u00a7a+ Nouveau Waypoint" : "\u00a7l\u00a76Edition du Waypoint", cx, 15, 0xFFFFFF);
        drawHorizontalLine(cx - 120, cx + 120, 30, 0x55FFFFFF);

        // Labels champs
        this.drawString(this.fontRendererObj, "\u00a77Nom du point", cx - 100, top + 8, 0xAAAAAA);
        this.fieldName.drawTextBox();

        this.drawString(this.fontRendererObj, "\u00a77Coordonnées (X, Y, Z)", cx - 100, top + 53, 0xAAAAAA);
        this.fieldX.drawTextBox();
        this.fieldY.drawTextBox();
        this.fieldZ.drawTextBox();

        // Label Options
        this.drawString(this.fontRendererObj, "\u00a77Options d'affichage", cx - 100, top + 90, 0xAAAAAA);
        // (Les boutons options sont dessinés par super.drawScreen)

        // Palette Couleurs
        int palY = top + 135;
        this.drawCenteredString(this.fontRendererObj, "\u00a77Couleur : \u00a7f" + COLOR_NAMES[selectedColorIndex], cx, palY - 12, 0xFFFFFF);

        int paletteW = SWATCHES_PER_ROW * (SWATCH_SIZE + SWATCH_GAP) - SWATCH_GAP;
        int paletteX = cx - paletteW / 2;
        
        for (int i = 0; i < COLORS.length; i++) {
            int col = i % SWATCHES_PER_ROW;
            int row = i / SWATCHES_PER_ROW;
            int x = paletteX + col * (SWATCH_SIZE + SWATCH_GAP);
            int y = palY + row * (SWATCH_SIZE + SWATCH_GAP);
            
            int colorInt = 0xFF000000 | (COLORS[i][0] << 16) | (COLORS[i][1] << 8) | COLORS[i][2];
            
            boolean isHover = mouseX >= x && mouseX < x + SWATCH_SIZE && mouseY >= y && mouseY < y + SWATCH_SIZE;
            boolean isSelected = (i == selectedColorIndex);

            // Bordure
            if (isSelected) {
                drawRect(x - 2, y - 2, x + SWATCH_SIZE + 2, y + SWATCH_SIZE + 2, 0xFFFFFFFF);
            } else if (isHover) {
                drawRect(x - 1, y - 1, x + SWATCH_SIZE + 1, y + SWATCH_SIZE + 1, 0xFFAAAAAA);
            }

            drawRect(x, y, x + SWATCH_SIZE, y + SWATCH_SIZE, colorInt);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.fieldName.mouseClicked(mouseX, mouseY, mouseButton);
        this.fieldX.mouseClicked(mouseX, mouseY, mouseButton);
        this.fieldY.mouseClicked(mouseX, mouseY, mouseButton);
        this.fieldZ.mouseClicked(mouseX, mouseY, mouseButton);

        // Gestion clic palette
        int cx = this.width / 2;
        int top = 40;
        int palY = top + 135;
        int paletteW = SWATCHES_PER_ROW * (SWATCH_SIZE + SWATCH_GAP) - SWATCH_GAP;
        int paletteX = cx - paletteW / 2;

        for (int i = 0; i < COLORS.length; i++) {
            int col = i % SWATCHES_PER_ROW;
            int row = i / SWATCHES_PER_ROW;
            int x = paletteX + col * (SWATCH_SIZE + SWATCH_GAP);
            int y = palY + row * (SWATCH_SIZE + SWATCH_GAP);

            if (mouseX >= x && mouseX < x + SWATCH_SIZE && mouseY >= y && mouseY < y + SWATCH_SIZE) {
                selectedColorIndex = i;
                // Jouer un son ici serait bien, mais on va rester simple
                return;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            this.mc.displayGuiScreen(parent);
            return;
        }
        if (keyCode == 15) { // Tab
            if (fieldName.isFocused()) { fieldName.setFocused(false); fieldX.setFocused(true); }
            else if (fieldX.isFocused()) { fieldX.setFocused(false); fieldY.setFocused(true); }
            else if (fieldY.isFocused()) { fieldY.setFocused(false); fieldZ.setFocused(true); }
            else if (fieldZ.isFocused()) { fieldZ.setFocused(false); fieldName.setFocused(true); }
            return;
        }
        if (keyCode == 28) { // Enter
            actionPerformed(this.buttonList.get(3)); // Save (index 3 correspond au bouton save "20")
             // Note: buttonList index depends on add order. Let's call saveWaypoint direct instead if enter is pressed
             saveWaypoint();
             return;
        }

        this.fieldName.textboxKeyTyped(typedChar, keyCode);
        this.fieldX.textboxKeyTyped(typedChar, keyCode);
        this.fieldY.textboxKeyTyped(typedChar, keyCode);
        this.fieldZ.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 10: // Beam
                beamEnabled = !beamEnabled;
                button.displayString = getBeamText();
                break;
            case 11: // Coords
                coordsEnabled = !coordsEnabled;
                button.displayString = getCoordsText();
                break;
            case 12: // Size
                textSize = textSize.next();
                button.displayString = getTextSizeText();
                break;
            case 20: // Save
                saveWaypoint();
                break;
            case 21: // Cancel
                this.mc.displayGuiScreen(parent);
                break;
        }
    }

    private void saveWaypoint() {
        String name = fieldName.getText().trim();
        if (name.isEmpty()) name = "Waypoint";
        
        int x = 0, y = 64, z = 0;
        try {
            x = Integer.parseInt(fieldX.getText().trim());
            y = Integer.parseInt(fieldY.getText().trim());
            z = Integer.parseInt(fieldZ.getText().trim());
        } catch (NumberFormatException e) {
            // On pourrait afficher une erreur rouge, mais pour l'instant on ignore ou on met 0
        }

        int[] c = COLORS[selectedColorIndex];
        
        if (editing != null) {
            // Mise à jour
            editing.setName(name);
            editing.setX(x); editing.setY(y); editing.setZ(z);
            editing.setColor(c[0], c[1], c[2]);
            editing.setBeamVisible(beamEnabled);
            editing.setCoordsVisible(coordsEnabled);
            editing.setTextSize(textSize);
            WaypointManager.INSTANCE.save();
        } else {
            // Création
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
}
