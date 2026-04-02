package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.FontRenderer;

public class ArmorGroupWidget extends BaseWidget {
    public ArmorGroupWidget(String id, int x, int y) {
        super(id, x, y);
        this.width = 120;
        this.height = 20;
        if (getPropOrDefault("displayPercent", null) == null) setProp("displayPercent", Boolean.TRUE);
        if (getPropOrDefault("layout", null) == null) setProp("layout", "vertical"); // horizontal or vertical
    }

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;

        FontRenderer fr = mc.fontRendererObj;
        RenderItem ri = mc.getRenderItem();

        ItemStack[] armor = (mc.thePlayer != null) ? mc.thePlayer.inventory.armorInventory : new ItemStack[4];
        boolean any = false;
        
        // BaseWidget handles translate, so we draw from 0,0
        int startX = 0;
        int curX = startX;
        int startY = 0;
        int curY = startY;
        
        boolean vertical = "vertical".equals(String.valueOf(getPropOrDefault("layout", "vertical")));
        int itemGap = 2;
        boolean showPercent = Boolean.TRUE.equals(getPropOrDefault("displayPercent", Boolean.TRUE));

        boolean isEditor = false;
        try {
            isEditor = UIManager.getInstance().isEditorActive();
        } catch (Throwable ignored) {}

        ItemStack[] displayArmor = new ItemStack[4];
        int present = 0;
        for (int i = 0; i < 4; i++) {
            if (armor != null && armor[i] != null) {
                displayArmor[i] = armor[i];
                present++;
            }
        }
        
        if (present == 0 && isEditor) {
            try {
                displayArmor[3] = new ItemStack(net.minecraft.init.Items.diamond_helmet);
                displayArmor[2] = new ItemStack(net.minecraft.init.Items.diamond_chestplate);
                displayArmor[1] = new ItemStack(net.minecraft.init.Items.diamond_leggings);
                displayArmor[0] = new ItemStack(net.minecraft.init.Items.diamond_boots);
                present = 4;
            } catch (Throwable ignored) {}
        }

        for (int i = 3; i >= 0; i--) {
            ItemStack stack = displayArmor[i];
            if (stack == null) continue;
            any = true;
            
            // Rendu de l'icône — renderItemSafe() confine le glint enchantement à la zone 16×16
            if (vertical) {
                renderItemSafe(ri, mc, stack, startX, curY);
            } else {
                renderItemSafe(ri, mc, stack, curX, startY);
            }

            String txt = "";
            try {
                if (stack.isItemStackDamageable()) {
                    int max = stack.getMaxDamage();
                    int dmg = stack.getItemDamage();
                    int rem = max - dmg;
                    if (showPercent) txt = (int) (rem * 100.0F / max) + "%";
                    else txt = String.valueOf(rem);
                } else txt = "";
            } catch (Throwable t) {}

            int txtColor = getColor();
            if (vertical) {
                fr.drawStringWithShadow(txt, startX + 18, curY + 4, txtColor);
                curY += 20 + itemGap;
            } else {
                fr.drawStringWithShadow(txt, curX + 18, startY + 4, txtColor);
                curX += 40 + itemGap;
            }
        }
        
        // Keep previous width/height if nothing to draw and not in editor
        if (!any) {
            if (isEditor) {
                // Use sensible defaults so editor can still interact and position the widget
                this.width = 50;
                this.height = 20;
            } else {
                // keep minimal zero size when genuinely empty
                this.width = 0;
                this.height = 0;
            }
            return;
        }
        
        if (vertical) {
            this.width = Math.max(50, 50); // ensure stable min width
            this.height = curY;
        } else {
            this.width = Math.max(40, curX);
            this.height = 20;
        }
    }
}
