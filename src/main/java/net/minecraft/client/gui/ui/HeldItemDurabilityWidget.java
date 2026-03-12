package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public class HeldItemDurabilityWidget extends BaseWidget {

    private static ItemStack FALLBACK_STACK = null;

    public HeldItemDurabilityWidget(String id, int x, int y) {
        super(id, x, y);
        this.width = 80;
        this.height = 18;
        if (getPropOrDefault("showBackground", null) == null) setProp("showBackground", Boolean.FALSE);
        if (getPropOrDefault("editorPreview", null) == null) setProp("editorPreview", Boolean.FALSE);
    }

    private static ItemStack getFallbackStack() {
        if (FALLBACK_STACK == null) {
            try {
                FALLBACK_STACK = new ItemStack(Items.diamond_sword);
            } catch (Throwable t) {
                FALLBACK_STACK = null;
            }
        }
        return FALLBACK_STACK;
    }

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;

        // Double vérification : UIManager ET écran courant
        boolean inEditor = UIManager.getInstance().isEditorActive()
                || (mc.currentScreen instanceof net.minecraft.client.gui.GuiUIEditor);

        ItemStack stack = null;
        try {
            stack = mc.thePlayer.getHeldItem();
        } catch (Throwable ignored) {
        }

        if (inEditor && stack == null) {
            stack = getFallbackStack();
        }

        if (stack == null) {
            // Hors éditeur seulement : on masque le widget
            this.width = 0;
            this.height = 0;
            return;
        }

        FontRenderer fr = mc.fontRendererObj;
        String text;

        if (stack.isItemStackDamageable()) {
            int max = stack.getMaxDamage();
            int dmg = stack.getItemDamage();
            if (max > 0) {
                int rem = max - dmg;
                boolean showPercent = Boolean.TRUE.equals(getPropOrDefault("displayPercent", Boolean.TRUE));
                text = showPercent ? ((int) (rem * 100.0F / max)) + "%" : rem + "/" + max;
            } else {
                text = stack.getDisplayName();
            }
        } else {
            text = stack.getDisplayName();
        }

        // Autosize — toujours calculé si on a un stack
        this.width = fr.getStringWidth(text) + 26;
        this.height = 18;

        // Icône
        try {
            GlStateManager.pushMatrix();
            RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemAndEffectIntoGUI(stack, this.x, this.y);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
        } catch (Throwable ignored) {
        }

        // Texte
        int col = getColor();
        if ((col & 0x00FFFFFF) == 0) col = 0x00FFFFFF;
        fr.drawStringWithShadow(text, this.x + 18, this.y + 4, col & 0x00FFFFFF);

        // Recalcul position uniquement hors éditeur
        try {
            if (!inEditor) updateAbsolutePosition();
        } catch (Throwable ignored) {
        }
    }
}
