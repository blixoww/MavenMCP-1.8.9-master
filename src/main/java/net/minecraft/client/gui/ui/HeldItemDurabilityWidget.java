package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;

public class HeldItemDurabilityWidget extends BaseWidget {
    public HeldItemDurabilityWidget(String id, int x, int y) {
        super(id, x, y);
        this.width = 80;
        this.height = 18;
        if (getPropOrDefault("showBackground", null) == null) setProp("showBackground", Boolean.FALSE);
    }

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        ItemStack stack = mc.thePlayer.getHeldItem();
        // display only when holding an item (tool/weapon)
        if (stack == null) {
            this.width = 0;
            this.height = 0;
            return;
        }
        FontRenderer fr = mc.fontRendererObj;
        String text = "";
        if (stack.isItemStackDamageable()) {
            int max = stack.getMaxDamage();
            int dmg = stack.getItemDamage();
            if (max > 0) {
                int rem = max - dmg;
                boolean showPercent = Boolean.TRUE.equals(getPropOrDefault("displayPercent", Boolean.TRUE));
                if (showPercent) {
                    int pct = (int) (rem * 100.0F / max);
                    text = pct + "%";
                } else {
                    text = rem + "/" + max;
                }
            }
        } else {
            // show item name when held but not damageable
            text = stack.getDisplayName();
        }
        // autosize
        this.width = fr.getStringWidth(text) + 26;
        this.height = 18;
        // draw item icon
        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
        // draw text
        int col = getColor();
        if ((col & 0x00FFFFFF) == 0) col = 0x00FFFFFF;
        fr.drawStringWithShadow(text, x + 18, y + 4, col & 0x00FFFFFF);
        try {
            if (!UIManager.getInstance().isEditorActive()) updateRelativePosition();
        } catch (Throwable ignored) {
        }
    }
}
