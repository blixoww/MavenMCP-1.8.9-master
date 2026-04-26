package net.minecraft.client.gui;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MathHelper;

public class GuiControls extends GuiScreen {
    private static final GameSettings.Options[] optionsArr = new GameSettings.Options[]{GameSettings.Options.INVERT_MOUSE, GameSettings.Options.SENSITIVITY, GameSettings.Options.TOUCHSCREEN};

    // ── Style "Red Conflict" ───────────────────────────────────────────────
    private static final int ACCENT   = 0xFFDC1E1E;
    private static final int C_MUTED  = 0xFF707880;
    private float animation = 0.0f;
    private long  animLastTime = -1L;

    /**
     * A reference to the screen object that created this. Used for navigating between screens.
     */
    private final GuiScreen parentScreen;
    protected String screenTitle = "Controls";

    /**
     * Reference to the GameSettings object.
     */
    private final GameSettings options;

    /**
     * The ID of the button that has been pressed.
     */
    public KeyBinding buttonId = null;
    public long time;
    private GuiKeyBindingList keyBindingList;
    private GuiButton buttonReset;
    private GuiTextField searchField;
    private String lastSearch = "";

    public GuiControls(GuiScreen screen, GameSettings settings) {
        this.parentScreen = screen;
        this.options = settings;
    }

    /**
     * Adds the buttons (and other controls) to the screen in question. Called when the GUI is displayed and when the
     * window resizes, the buttonList is cleared beforehand.
     */
    @Override
    public void initGui() {
        this.animation = 0.0f;
        this.animLastTime = -1L;
        int i = 0;
        for (GameSettings.Options gamesettings$options : optionsArr) {
            int bx = this.width / 2 - 155 + i % 2 * 160;
            int by = 36 + 26 * (i >> 1);
            if (gamesettings$options.getEnumFloat()) {
                this.buttonList.add(new GuiStyledSlider(gamesettings$options.returnEnumOrdinal(), bx, by, 150, 22, gamesettings$options));
            } else {
                this.buttonList.add(new GuiMenuButton(gamesettings$options.returnEnumOrdinal(), bx, by, 150, 20, this.options.getKeyBinding(gamesettings$options)));
            }
            ++i;
        }

        if (this.searchField == null) {
            this.searchField = new GuiTextField(0, this.fontRendererObj, this.width / 2 - 150, 98, 300, 20);
            this.searchField.setMaxStringLength(50);
        } else {
            this.searchField.xPosition = this.width / 2 - 150;
            this.searchField.yPosition = 98;
        }

        if (this.searchField != null) this.searchField.setFocused(false);
        if (this.searchField != null) this.searchField.setText(this.lastSearch);

        // Rafraîchir la liste des touches en fonction de la recherche (elle se placera sous le champ, vers y=105)
        this.keyBindingList = new GuiKeyBindingList(this, this.mc);

        // Boutons du bas — style GuiMenuButton
        this.buttonList.add(new GuiMenuButton(200, this.width / 2 - 155, this.height - 29, 150, 20, I18n.format("gui.done"), true));
        this.buttonList.add(this.buttonReset = new GuiMenuButton(201, this.width / 2 - 155 + 160, this.height - 29, 100, 20, I18n.format("controls.resetAll")));
        this.buttonList.add(new GuiMenuButton(202, this.width / 2 - 155 + 265, this.height - 29, 90, 20, "§cMacros..."));
        this.screenTitle = I18n.format("controls.title");
    }

    /**
     * Handles mouse input.
     */
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        this.keyBindingList.handleMouseInput();
    }

    /**
     * Called by the controls from the buttonList when activated. (Mouse pressed for buttons)
     */
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 200) {
            this.mc.displayGuiScreen(this.parentScreen);
        } else if (button.id == 201) {
            for (KeyBinding keybinding : this.mc.gameSettings.keyBindings) {
                keybinding.setKeyCode(keybinding.getKeyCodeDefault());
            }

            KeyBinding.resetKeyBindingArrayAndHash();
        } else if (button.id == 202) {
            this.mc.displayGuiScreen(new GuiMacros(this));
        } else if (button.id < 100) {
            GameSettings.Options opt = GameSettings.Options.getEnumOptions(button.id);
            if (opt != null && !opt.getEnumFloat()) {
                this.options.setOptionValue(opt, 1);
                button.displayString = this.options.getKeyBinding(opt);
            }
        }
    }

    /**
     * Called when the mouse is clicked. Args : mouseX, mouseY, clickedButton
     */
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        // Gestion du clic sur le champ de recherche (correction)
        if (this.searchField != null && mouseX >= this.searchField.xPosition && mouseX < this.searchField.xPosition + this.searchField.width && mouseY >= this.searchField.yPosition && mouseY < this.searchField.yPosition + 20) {
            this.searchField.setFocused(true);
            this.searchField.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }
        if (this.searchField != null) this.searchField.setFocused(false);
        if (this.buttonId != null) {
            this.options.setOptionKeyBinding(this.buttonId, -100 + mouseButton);
            this.buttonId = null;
            KeyBinding.resetKeyBindingArrayAndHash();
        } else if (mouseButton != 0 || !this.keyBindingList.mouseClicked(mouseX, mouseY, mouseButton)) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    /**
     * Called when a mouse button is released.  Args : mouseX, mouseY, releaseButton
     */
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state != 0 || !this.keyBindingList.mouseReleased(mouseX, mouseY, state)) {
            super.mouseReleased(mouseX, mouseY, state);
        }
    }

    /**
     * Fired when a key is typed (except F11 which toggles full screen). This is the equivalent of
     * KeyListener.keyTyped(KeyEvent e). Args : character (character on the key), keyCode (lwjgl Keyboard key code)
     */
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.searchField != null && this.searchField.isFocused()) {
            this.searchField.textboxKeyTyped(typedChar, keyCode);
            // Rafraîchir la liste si la recherche a changé
            if (!this.lastSearch.equals(this.searchField.getText())) {
                this.lastSearch = this.searchField.getText();
                this.keyBindingList = new GuiKeyBindingList(this, this.mc); // Rafraîchir la liste filtrée
            }
            return;
        }
        if (this.buttonId != null) {
            if (keyCode == 1) {
                this.options.setOptionKeyBinding(this.buttonId, 0);
            } else if (keyCode != 0) {
                this.options.setOptionKeyBinding(this.buttonId, keyCode);
            } else if (typedChar > 0) {
                this.options.setOptionKeyBinding(this.buttonId, typedChar + 256);
            }

            this.buttonId = null;
            this.time = Minecraft.getSystemTime();
            KeyBinding.resetKeyBindingArrayAndHash();
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    /**
     * Draws the screen and all the components in it.
     */
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = Minecraft.getSystemTime();
        if (animLastTime != -1L) {
            float dt = (now - animLastTime) / 1000.0f;
            animation = MathHelper.clamp_float(animation + dt * 5.0f, 0.0f, 1.0f);
        }
        animLastTime = now;
        float eased = animation * animation * (3.0f - 2.0f * animation);

        Gui.drawRect(0, 0, this.width, this.height, 0xFF080B10);
        this.keyBindingList.drawScreen(mouseX, mouseY, partialTicks);

        int textAlpha = (int)(eased * 255) << 24;

        // Header discret — dégradé + ligne rouge fine (style ChatOptions, sans terre)
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, (1.0f - eased) * 8, 0);
        GuiRenderUtils.drawGradientRect(0, 0, this.width, 34, (int)(eased * 160) << 24 | 0x000000, 0x00000000);
        Gui.drawRect(0, 0, this.width, 1, (int)(eased * 255) << 24 | (ACCENT & 0xFFFFFF));
        String t1 = "§cTOUCHES ";
        String t2 = "§7& CONTRÔLES";
        int tw = fontRendererObj.getStringWidth(t1) + fontRendererObj.getStringWidth(t2);
        int titleX = this.width / 2 - tw / 2;
        fontRendererObj.drawStringWithShadow(t1, titleX, 11, textAlpha | 0xFFFFFF);
        fontRendererObj.drawStringWithShadow(t2, titleX + fontRendererObj.getStringWidth(t1), 11, textAlpha | 0xFFFFFF);
        int divW = (int)((tw + 20) * eased);
        Gui.drawRect(this.width / 2 - divW / 2, 23, this.width / 2 + divW / 2, 24, (int)(eased * 45) << 24 | 0xFFFFFF);
        GlStateManager.popMatrix();

        if (this.searchField != null) {
            Gui.drawRect(this.searchField.xPosition - 1, this.searchField.yPosition - 1,
                    this.searchField.xPosition + this.searchField.width + 1,
                    this.searchField.yPosition + 21, this.searchField.isFocused() ? (ACCENT | 0xFF000000) : 0x33FFFFFF);
            Gui.drawRect(this.searchField.xPosition, this.searchField.yPosition,
                    this.searchField.xPosition + this.searchField.width,
                    this.searchField.yPosition + 20, 0xFF050709);
            this.searchField.drawTextBox();
        }
        this.fontRendererObj.drawString("§7Recherche", this.width / 2 - 150, this.searchField.yPosition - 12, textAlpha | (C_MUTED & 0xFFFFFF));

        boolean flag = true;
        for (KeyBinding keybinding : this.options.keyBindings) {
            if (keybinding.getKeyCode() != keybinding.getKeyCodeDefault()) { flag = false; break; }
        }
        this.buttonReset.enabled = !flag;

        // Footer discret — dégradé sombre
        GuiRenderUtils.drawGradientRect(0, this.height - 40, this.width, this.height, 0x00000000, (int)(eased * 140) << 24 | 0x000000);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public GuiTextField getSearchField() {
        return this.searchField;
    }
}
