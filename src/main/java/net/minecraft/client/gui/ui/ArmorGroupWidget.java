package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;

public class ArmorGroupWidget extends BaseWidget {
    public ArmorGroupWidget(String id, int x, int y) {
        super(id, x, y);
        this.width = 120;
        this.height = 20;
        if (getPropOrDefault("initialized", null) == null) setProp("initialized", Boolean.FALSE);
        if (getPropOrDefault("displayPercent", null) == null) setProp("displayPercent", Boolean.TRUE);
        if (getPropOrDefault("layout", null) == null) setProp("layout", "vertical"); // horizontal or vertical
        if (getPropOrDefault("preview", null) == null) setProp("preview", Boolean.FALSE);
    }

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        if (!Boolean.TRUE.equals(getPropOrDefault("initialized", Boolean.FALSE))) {
            if (mc.thePlayer != null) {
                ScaledResolution sr = new ScaledResolution(mc);
                int sh = sr.getScaledHeight();
                int sw = sr.getScaledWidth();
                if (this.x == 0 && this.y == 0) {
                    this.x = Math.max(10, sw - 70);
                    this.y = Math.max(20, sh - 95);
                }
                setProp("initialized", Boolean.TRUE);
            }
        }

        FontRenderer fr = mc.fontRendererObj;
        RenderItem ri = mc.getRenderItem();

        // iterate armor slots: 3=head,2=chest,1=legs,0=boots but we display left-to-right or top-to-bottom
        ItemStack[] armor = (mc.thePlayer != null) ? mc.thePlayer.inventory.armorInventory : new ItemStack[4];
        boolean any = false;
        int startX = x;
        int curX = startX;
        int startY = y;
        int curY = startY;
        boolean vertical = "vertical".equals(String.valueOf(getPropOrDefault("layout", "horizontal")));
        int maxH = 0;
        int itemGap = 2; // tighter spacing between items
        boolean showPercent = Boolean.TRUE.equals(getPropOrDefault("displayPercent", Boolean.TRUE));

        // If no armor and preview enabled, create sample pieces to preview layout
        boolean preview = Boolean.TRUE.equals(getPropOrDefault("preview", Boolean.FALSE));
        ItemStack[] displayArmor = new ItemStack[4];
        int present = 0;
        for (int i = 0; i < 4; i++) {
            if (armor != null && armor[i] != null) {
                displayArmor[i] = armor[i];
                present++;
            }
        }
        if (present == 0 && preview) {
            // try to create placeholder ItemStacks from known items via reflection fallback to null (will just draw text)
            try {
                net.minecraft.item.Item helmet = net.minecraft.init.Items.diamond_helmet;
                net.minecraft.item.Item chest = net.minecraft.init.Items.diamond_chestplate;
                net.minecraft.item.Item legs = net.minecraft.init.Items.diamond_leggings;
                net.minecraft.item.Item boots = net.minecraft.init.Items.diamond_boots;
                displayArmor[3] = new ItemStack(helmet);
                displayArmor[2] = new ItemStack(chest);
                displayArmor[1] = new ItemStack(legs);
                displayArmor[0] = new ItemStack(boots);
            } catch (Throwable ignored) {
            }
        }

        for (int i = 3; i >= 0; i--) {
            ItemStack stack = displayArmor[i];
            if (stack == null) continue;
            any = true;
            // draw icon
            GlStateManager.pushMatrix();
            RenderHelper.enableGUIStandardItemLighting();
            if (vertical) {
                ri.renderItemAndEffectIntoGUI(stack, startX, curY);
            } else {
                ri.renderItemAndEffectIntoGUI(stack, curX, y);
            }
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
            String txt = null;
            try {
                if (stack.isItemStackDamageable()) {
                    int max = stack.getMaxDamage();
                    int dmg = stack.getItemDamage();
                    int rem = max - dmg;
                    if (showPercent) txt = (int) (rem * 100.0F / max) + "%";
                    else txt = rem + "/" + max;
                } else txt = stack.getDisplayName();
            } catch (Throwable t) {
                txt = "";
            }

            int txtColor = getColor();
            if (vertical) {
                fr.drawStringWithShadow(txt, startX + 18, curY + 4, txtColor);
                curY += 20 + itemGap;
            } else {
                fr.drawStringWithShadow(txt, curX + 18, y + 4, txtColor);
                curX += 46 + itemGap;
            }
            maxH = 18;
        }
        if (!any) {
            this.width = 0;
            this.height = 0;
            return;
        }
        if (vertical) {
            this.width = 50;
            this.height = curY - y;
        } else {
            this.width = Math.max(40, curX - startX);
            this.height = maxH + 4;
        }
        try {
            if (this.refW < 0) {
                try { this.setPosition(this.x, this.y); } catch (Throwable ignored) {}
                try { UIManager.getInstance().saveConfig(); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        try {
            if (!UIManager.getInstance().isEditorActive()) updateAbsolutePosition();
        } catch (Throwable ignored) {}
    }
}
