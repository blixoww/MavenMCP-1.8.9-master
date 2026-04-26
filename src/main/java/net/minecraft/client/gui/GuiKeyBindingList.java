package net.minecraft.client.gui;

import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.lang3.ArrayUtils;

public class GuiKeyBindingList extends GuiListExtended
{
    // ── Style "Red Conflict" ──────────────────────────────────────────────
    private static final int ACCENT     = 0xFFDC1E1E;
    private static final int C_TEXT     = 0xFFE0E0E0;
    private static final int C_MUTED    = 0xFF707880;
    private static final int C_CONFLICT = 0xFFEE4444;
    private static final int C_LISTEN   = 0xFFFFCC00;

    private final GuiControls field_148191_k;
    private final Minecraft mc;
    private final GuiListExtended.IGuiListEntry[] listEntries;
    private int maxListLabelWidth;

    public GuiKeyBindingList(GuiControls controls, Minecraft mcIn)
    {
        super(mcIn, controls.width, controls.height, (controls.getSearchField() != null ? controls.getSearchField().yPosition + 20 + 10 : 63), controls.height - 32, 20);
        this.renderBackground = false;
        this.field_148191_k = controls;
        this.mc = mcIn;
        KeyBinding[] akeybinding = ArrayUtils.clone(mcIn.gameSettings.keyBindings);
        String search = null;
        if (controls.getSearchField() != null) {
            String txt = controls.getSearchField().getText();
            if (txt != null && !txt.trim().isEmpty()) {
                search = txt.toLowerCase();
            }
        }
        java.util.List<GuiListExtended.IGuiListEntry> filtered = new java.util.ArrayList<>();
        java.util.Set<String> addedCategories = new java.util.HashSet<>();
        Arrays.sort(akeybinding);
        for (KeyBinding keybinding : akeybinding)
        {
            String keyDesc = I18n.format(keybinding.getKeyDescription()).toLowerCase();
            String keyName = GameSettings.getKeyDisplayString(keybinding.getKeyCode()).toLowerCase();
            boolean show = (search == null) || keyDesc.contains(search) || keyName.contains(search);
            if (show) {
                String s1 = keybinding.getKeyCategory();
                if (!addedCategories.contains(s1)) {
                    filtered.add(new GuiKeyBindingList.CategoryEntry(s1));
                    addedCategories.add(s1);
                }
                filtered.add(new GuiKeyBindingList.KeyEntry(keybinding));
            }
        }
        this.listEntries = filtered.toArray(new GuiListExtended.IGuiListEntry[0]);
        this.maxListLabelWidth = 0;
        for (KeyBinding keybinding : akeybinding)
        {
            int j = mcIn.fontRendererObj.getStringWidth(I18n.format(keybinding.getKeyDescription()));
            if (j > this.maxListLabelWidth)
                this.maxListLabelWidth = j;
        }
    }

    @Override
    public void drawBackground() { /* transparent */ }

    @Override
    protected void overlayBackground(int startY, int endY, int startAlpha, int endAlpha) {
        GuiRenderUtils.drawGradientRect(0, startY, this.mc.currentScreen.width, endY,
                (startAlpha << 24) | 0x080B10, (endAlpha << 24) | 0x080B10);
    }


    protected int getSize()    { return this.listEntries.length; }

    public GuiListExtended.IGuiListEntry getListEntry(int index) { return this.listEntries[index]; }

    protected int getScrollBarX()  { return super.getScrollBarX() + 15; }
    public    int getListWidth()   { return super.getListWidth()  + 32; }

    // ═════════════════════════════════════════════════════════════════════
    //  CATEGORY ENTRY
    // ═════════════════════════════════════════════════════════════════════
    public class CategoryEntry implements GuiListExtended.IGuiListEntry
    {
        private final String labelText;
        private final int    labelWidth;

        public CategoryEntry(String p_i45028_2_)
        {
            this.labelText  = I18n.format(p_i45028_2_).toUpperCase();
            this.labelWidth = GuiKeyBindingList.this.mc.fontRendererObj.getStringWidth(this.labelText);
        }

        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected)
        {
            int cx = GuiKeyBindingList.this.mc.currentScreen.width / 2;
            int ty = y + slotHeight - GuiKeyBindingList.this.mc.fontRendererObj.FONT_HEIGHT - 1;

            // Ligne séparatrice
            Gui.drawRect(cx - listWidth / 2, ty + 8, cx + listWidth / 2, ty + 9, 0x22FFFFFF);
            // Fond badge
            int bx = cx - labelWidth / 2 - 5;
            Gui.drawRect(bx, ty - 1, bx + labelWidth + 10, ty + 10, 0xFF0A0D14);
            // Accent rouge à gauche
            Gui.drawRect(bx, ty - 1, bx + 2, ty + 10, ACCENT);
            // Texte catégorie
            GuiKeyBindingList.this.mc.fontRendererObj.drawString(
                    labelText, bx + 6, ty, ACCENT);
        }

        public boolean mousePressed(int s, int x, int y, int b, int rx, int ry) { return false; }
        public void mouseReleased(int s, int x, int y, int e, int rx, int ry)   {}
        public void setSelected(int a, int b, int c)                             {}
    }

    // ═════════════════════════════════════════════════════════════════════
    //  KEY ENTRY
    // ═════════════════════════════════════════════════════════════════════
    public class KeyEntry implements GuiListExtended.IGuiListEntry
    {
        private final KeyBinding keybinding;
        private final String     keyDesc;
        private final GuiMenuButton btnChangeKeyBinding;
        private final GuiMenuButton btnReset;

        // hover state par ligne
        private float rowHover = 0f;
        private long  rowHoverTime = -1L;

        private KeyEntry(KeyBinding p_i45029_2_)
        {
            this.keybinding          = p_i45029_2_;
            this.keyDesc             = I18n.format(p_i45029_2_.getKeyDescription());
            this.btnChangeKeyBinding = new GuiMenuButton(0, 0, 0, 75, 18, "");
            this.btnReset            = new GuiMenuButton(0, 0, 0, 42, 18, "↺");
        }

        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected)
        {
            boolean listening = GuiKeyBindingList.this.field_148191_k.buttonId == this.keybinding;

            // ── Fond zébré avec hover ──────────────────────────────────
            long now = Minecraft.getSystemTime();
            float dt = (rowHoverTime < 0) ? 0f : (float)(now - rowHoverTime);
            rowHoverTime = now;
            boolean hovRow = mouseX >= x && mouseX < x + listWidth && mouseY >= y && mouseY < y + slotHeight;
            float step = dt / 120f;
            rowHover = net.minecraft.util.MathHelper.clamp_float(rowHover + (hovRow ? step : -step), 0f, 1f);
            int rowBg = GuiRenderUtils.colorLerp(
                    slotIndex % 2 == 0 ? 0x08FFFFFF : 0x04FFFFFF,
                    0x14FFFFFF, rowHover);
            Gui.drawRect(x, y, x + listWidth, y + slotHeight, rowBg);

            // ── Nom de la touche ──────────────────────────────────────
            int labelColor = listening ? C_LISTEN : C_TEXT;
            GuiKeyBindingList.this.mc.fontRendererObj.drawString(
                    this.keyDesc,
                    x + 90 - GuiKeyBindingList.this.maxListLabelWidth,
                    y + slotHeight / 2 - GuiKeyBindingList.this.mc.fontRendererObj.FONT_HEIGHT / 2,
                    labelColor);

            // ── Bouton Reset ──────────────────────────────────────────
            this.btnReset.xPosition = x + 190;
            this.btnReset.yPosition = y + 1;
            this.btnReset.enabled   = this.keybinding.getKeyCode() != this.keybinding.getKeyCodeDefault();
            this.btnReset.drawButton(GuiKeyBindingList.this.mc, mouseX, mouseY);

            // ── Bouton Touche ─────────────────────────────────────────
            this.btnChangeKeyBinding.xPosition = x + 105;
            this.btnChangeKeyBinding.yPosition = y + 1;

            // Détection conflit
            boolean conflict = false;
            if (this.keybinding.getKeyCode() != 0) {
                for (KeyBinding kb : GuiKeyBindingList.this.mc.gameSettings.keyBindings) {
                    if (kb != this.keybinding && kb.getKeyCode() == this.keybinding.getKeyCode()) {
                        conflict = true; break;
                    }
                }
            }

            String keyLabel = GameSettings.getKeyDisplayString(this.keybinding.getKeyCode());
            if (listening) {
                this.btnChangeKeyBinding.displayString = "§e> " + keyLabel + " <";
            } else if (conflict) {
                this.btnChangeKeyBinding.displayString = "§c⚠ " + keyLabel;
            } else {
                this.btnChangeKeyBinding.displayString = keyLabel;
            }

            this.btnChangeKeyBinding.drawButton(GuiKeyBindingList.this.mc, mouseX, mouseY);
        }

        public boolean mousePressed(int slotIndex, int x, int y, int b, int rx, int ry)
        {
            if (this.btnChangeKeyBinding.mousePressed(GuiKeyBindingList.this.mc, x, y)) {
                GuiKeyBindingList.this.field_148191_k.buttonId = this.keybinding;
                return true;
            } else if (this.btnReset.mousePressed(GuiKeyBindingList.this.mc, x, y)) {
                GuiKeyBindingList.this.mc.gameSettings.setOptionKeyBinding(this.keybinding, this.keybinding.getKeyCodeDefault());
                KeyBinding.resetKeyBindingArrayAndHash();
                return true;
            }
            return false;
        }

        public void mouseReleased(int s, int x, int y, int e, int rx, int ry)
        {
            this.btnChangeKeyBinding.mouseReleased(x, y);
            this.btnReset.mouseReleased(x, y);
        }

        public void setSelected(int a, int b, int c) {}
    }
}
