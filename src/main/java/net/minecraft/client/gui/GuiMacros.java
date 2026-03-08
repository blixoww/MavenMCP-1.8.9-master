package net.minecraft.client.gui;

import net.minecraft.client.macro.MacroManager;
import net.minecraft.client.macro.MacroManager.Macro;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

/**
 * Interface de gestion des macros.
 * <p>
 * Layout :
 * - Titre en haut
 * - Liste des macros existantes (clé + commande + bouton Supprimer)
 * - Formulaire en bas : champ commande, champ touche, bouton Ajouter
 * - Bouton Terminer
 */
public class GuiMacros extends GuiScreen {
    private static final int COLOR_TITLE = 0xFFFFFF;
    private static final int COLOR_HEADER = 0xAAAAAA;
    private static final int COLOR_ROW_ODD = 0x20FFFFFF;
    private static final int COLOR_ROW_EVN = 0x10FFFFFF;
    private static final int COLOR_WARN = 0xFF5555;
    private static final int COLOR_OK = 0x55FF55;

    private final GuiScreen parent;
    private final MacroManager mgr = MacroManager.INSTANCE;

    // Champs de saisie du formulaire "Ajouter"
    private GuiTextField fieldCommand;
    private int pendingKeyCode = 0;          // touche en attente d'assignation
    private boolean listeningKey = false;    // est-on en mode "appuie une touche" ?

    // Editing (null = pas en cours)
    private Integer editingIndex = null;

    // Scroll pour la liste
    private int scrollOffset = 0;
    private static final int ROW_HEIGHT = 24;
    private static final int LIST_Y_START = 50;
    private int listDisplayHeight;

    public GuiMacros(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        listDisplayHeight = this.height - LIST_Y_START - 90;

        // Champ commande (formulaire du bas)
        this.fieldCommand = new GuiTextField(0, this.fontRendererObj,
                this.width / 2 - 155, this.height - 76, 250, 20);
        this.fieldCommand.setMaxStringLength(256);
        this.fieldCommand.setFocused(true);

        // Bouton "Assigner touche"
        this.buttonList.add(new GuiButton(100, this.width / 2 + 100, this.height - 76, 110, 20,
                getKeyButtonLabel()));

        // Bouton "Ajouter / Sauver"
        this.buttonList.add(new GuiButton(101, this.width / 2 - 155, this.height - 52, 130, 20,
                editingIndex == null ? "Ajouter" : "Sauvegarder"));

        // Bouton "Annuler édition"
        this.buttonList.add(new GuiButton(102, this.width / 2 - 20, this.height - 52, 80, 20,
                "Annuler"));

        // Bouton Terminer
        this.buttonList.add(new GuiButton(200, this.width / 2 - 75, this.height - 26, 150, 20,
                I18n.format("gui.done")));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private String getKeyButtonLabel() {
        if (listeningKey) return "> Appuie une touche <";
        if (pendingKeyCode == 0) return "Touche : ---";
        Macro tmp = new Macro(pendingKeyCode, "");
        return "Touche : " + tmp.getKeyDisplayString();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        List<Macro> macros = mgr.getMacros();

        if (button.id == 200) {
            // Terminer
            mc.displayGuiScreen(parent);
        } else if (button.id == 100) {
            // Assigner touche
            listeningKey = !listeningKey;
            updateKeyButton();
        } else if (button.id == 101) {
            // Ajouter ou Sauvegarder
            String cmd = fieldCommand.getText().trim();
            if (cmd.isEmpty() || pendingKeyCode == 0) return;

            Macro m = new Macro(pendingKeyCode, cmd);
            if (editingIndex != null) {
                mgr.setMacro(editingIndex, m);
                editingIndex = null;
            } else {
                if (mgr.canAddMacro())
                    mgr.addMacro(m);
            }
            resetForm();
        } else if (button.id == 102) {
            // Annuler édition
            editingIndex = null;
            resetForm();
        } else if (button.id >= 300 && button.id < 400) {
            // Supprimer macro
            int idx = button.id - 300;
            mgr.removeMacro(idx);
            if (scrollOffset > 0 && scrollOffset >= mgr.getCount()) scrollOffset--;
        } else if (button.id >= 400 && button.id < 500) {
            // Éditer macro
            int idx = button.id - 400;
            if (idx >= 0 && idx < macros.size()) {
                Macro m = macros.get(idx);
                editingIndex = idx;
                fieldCommand.setText(m.getCommand());
                pendingKeyCode = m.getKeyCode();
                listeningKey = false;
                updateKeyButton();
                updateAddButton();
            }
        }
    }

    private void resetForm() {
        fieldCommand.setText("");
        pendingKeyCode = 0;
        listeningKey = false;
        updateKeyButton();
        updateAddButton();
    }

    private void updateKeyButton() {
        for (Object o : buttonList) {
            if (o instanceof GuiButton && ((GuiButton) o).id == 100) {
                ((GuiButton) o).displayString = getKeyButtonLabel();
            }
        }
    }

    private void updateAddButton() {
        for (Object o : buttonList) {
            if (o instanceof GuiButton && ((GuiButton) o).id == 101) {
                ((GuiButton) o).displayString = editingIndex == null ? "Ajouter" : "Sauvegarder";
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (listeningKey) {
            if (keyCode == 1) // ESC = annuler assignation
            {
                listeningKey = false;
                updateKeyButton();
                return;
            }
            if (keyCode != 0) {
                pendingKeyCode = keyCode;
            } else if (typedChar > 0) {
                pendingKeyCode = typedChar + 256;
            }
            listeningKey = false;
            updateKeyButton();
            return;
        }

        if (keyCode == 1) // ESC
        {
            mc.displayGuiScreen(parent);
            return;
        }
        fieldCommand.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (listeningKey) {
            // Bouton souris : stocker comme keyCode négatif : -(btn+1)
            pendingKeyCode = -(mouseButton + 1);
            listeningKey = false;
            updateKeyButton();
            return;
        }

        // Gestion scroll sur la liste (molette)
        super.mouseClicked(mouseX, mouseY, mouseButton);
        fieldCommand.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            int maxScroll = Math.max(0, mgr.getCount() - listDisplayHeight / ROW_HEIGHT);
            if (wheel < 0) scrollOffset = Math.min(scrollOffset + 1, maxScroll);
            else scrollOffset = Math.max(scrollOffset - 1, 0);
        }
    }

    @Override
    public void updateScreen() {
        fieldCommand.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        List<Macro> macros = mgr.getMacros();

        // ── Titre ──────────────────────────────────────────────────────────────
        this.drawCenteredString(fontRendererObj, "§lGestion des Macros", width / 2, 14, COLOR_TITLE);
        String sub = macros.size() + " / " + MacroManager.MAX_MACROS + " macros";
        this.drawCenteredString(fontRendererObj, sub, width / 2, 26, COLOR_HEADER);

        // ── En-têtes colonnes ─────────────────────────────────────────────────
        int col1 = this.width / 2 - 200;
        int col2 = col1 + 80;
        int col3 = col2 + 290;
        this.drawString(fontRendererObj, "§7Touche", col1, LIST_Y_START - 12, COLOR_HEADER);
        this.drawString(fontRendererObj, "§7Commande", col2, LIST_Y_START - 12, COLOR_HEADER);

        // ── Liste des macros ─────────────────────────────────────────────────
        // Reconstruit les boutons de lignes à chaque frame (dynamique)
        // On les retire puis on les rajoute
        buttonList.removeIf(b -> b.id >= 300 && b.id < 500);

        int visibleRows = listDisplayHeight / ROW_HEIGHT;
        for (int i = 0; i < visibleRows && (i + scrollOffset) < macros.size(); i++) {
            int idx = i + scrollOffset;
            Macro m = macros.get(idx);
            int rowY = LIST_Y_START + i * ROW_HEIGHT;

            // Fond alterné
            int bg = (idx % 2 == 0) ? COLOR_ROW_EVN : COLOR_ROW_ODD;
            drawRect(col1 - 4, rowY - 2, col3 + 4, rowY + ROW_HEIGHT - 4, bg);

            // Touche
            String keyStr = m.getKeyDisplayString();
            this.drawString(fontRendererObj, "§e" + keyStr, col1, rowY + 4, 0xFFFFFF);

            // Commande (tronquée si trop longue)
            String cmd = m.getCommand();
            int maxW = col3 - col2 - 70;
            if (fontRendererObj.getStringWidth(cmd) > maxW)
                cmd = fontRendererObj.trimStringToWidth(cmd, maxW) + "...";
            this.drawString(fontRendererObj, cmd, col2, rowY + 4, 0xDDDDDD);

            // Bouton Éditer
            GuiButton btnEdit = new GuiButton(400 + idx, col3 - 64, rowY, 30, 16, "§aEdt");
            this.buttonList.add(btnEdit);
            // Bouton Supprimer
            GuiButton btnDel = new GuiButton(300 + idx, col3 - 30, rowY, 34, 16, "§cSup");
            this.buttonList.add(btnDel);
        }

        // Scrollbar
        if (macros.size() > visibleRows) {
            int sbX = col3 + 8;
            int sbH = listDisplayHeight;
            drawRect(sbX, LIST_Y_START, sbX + 4, LIST_Y_START + sbH, 0x44FFFFFF);
            int thumbH = Math.max(10, sbH * visibleRows / macros.size());
            int maxScroll = macros.size() - visibleRows;
            int thumbY = LIST_Y_START + (sbH - thumbH) * scrollOffset / Math.max(1, maxScroll);
            drawRect(sbX, thumbY, sbX + 4, thumbY + thumbH, 0xCCFFFFFF);
        }

        // ── Séparateur ───────────────────────────────────────────────────────
        int sepY = this.height - 85;
        drawRect(col1 - 4, sepY, col3 + 4, sepY + 1, 0x88AAAAAA);

        // ── Formulaire ajout ─────────────────────────────────────────────────
        int formY = this.height - 80;
        String formTitle = editingIndex == null ? "§7Nouvelle macro :" : "§7Modifier macro #" + (editingIndex + 1) + " :";
        this.drawString(fontRendererObj, formTitle, this.width / 2 - 155, formY, COLOR_HEADER);

        // Avertissement si limite atteinte
        if (!mgr.canAddMacro() && editingIndex == null) {
            this.drawString(fontRendererObj, "§cLimite de " + MacroManager.MAX_MACROS + " macros atteinte.",
                    this.width / 2 - 155, formY + 10, COLOR_WARN);
        }

        // Validation visuelle du formulaire
        boolean formValid = !fieldCommand.getText().trim().isEmpty() && pendingKeyCode != 0;
        for (Object o : buttonList) {
            if (o instanceof GuiButton && ((GuiButton) o).id == 101)
                ((GuiButton) o).enabled = formValid && (mgr.canAddMacro() || editingIndex != null);
        }

        // Champ commande
        fieldCommand.drawTextBox();

        super.drawScreen(mouseX, mouseY, partialTicks);

        // Tooltip info
        this.drawString(fontRendererObj,
                "§8Astuce : la commande peut commencer par / pour une commande, ou être du texte normal.",
                this.width / 2 - 155, this.height - 10, 0x555555);
    }
}


