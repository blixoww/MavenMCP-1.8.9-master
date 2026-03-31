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

        boolean inEditor = false;
        try {
            inEditor = UIManager.getInstance().isEditorActive();
        } catch (Throwable ignored) {}

        ItemStack stack = mc.thePlayer.getHeldItem();
        if (inEditor && stack == null) {
            stack = getFallbackStack();
        }

        if (stack == null) {
            if (inEditor) {
                this.width = 26; // keep a small visible placeholder so it's selectable in editor
                this.height = 18;
            } else {
                this.width = 0;
                this.height = 0;
            }
            return;
        }

        FontRenderer fr = mc.fontRendererObj;
        String text;

        if (stack.isItemStackDamageable()) {
            int max = stack.getMaxDamage();
            int dmg = stack.getItemDamage();
            if (max > 0) {
                int rem = max - dmg;
                text = rem + "/" + max;
            } else {
                text = stack.getDisplayName();
            }
        } else {
            text = stack.getDisplayName();
        }

        this.width = fr.getStringWidth(text) + 26;
        this.height = 18;

        // Draw from 0,0 because BaseWidget handles translation
        int sX = 0, sY = 0;

        try {
            GlStateManager.pushMatrix();
            RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemAndEffectIntoGUI(stack, sX, sY);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
        } catch (Throwable ignored) {}

        int col = getColor();
        fr.drawStringWithShadow(text, sX + 18, sY + 4, col);
    }
}
