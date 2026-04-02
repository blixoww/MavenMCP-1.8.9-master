package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;

public class ArmorPieceWidget extends BaseWidget
{
    private final int slot; // 0=head,1=chest,2=legs,3=boots
    public ArmorPieceWidget(String id, int x, int y, int slot)
    {
        super(id, x, y);
        this.slot = slot;
        this.width = 40; this.height = 18;
        // default prop: showPercent = true
        if (getPropOrDefault("displayPercent", null) == null) setProp("displayPercent", Boolean.TRUE);
        if (getPropOrDefault("initialized", null) == null) setProp("initialized", Boolean.FALSE);
    }

    @Override
    protected void draw()
    {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        // lazy init default position: middle-left cluster
        if (!Boolean.TRUE.equals(getPropOrDefault("initialized", Boolean.FALSE))) {
            ScaledResolution sr = new ScaledResolution(mc);
            int sh = sr.getScaledHeight();
            int idxY = 40 + slot * 20;
            this.x = 10; this.y = Math.min(sh - 20, idxY);
            setProp("initialized", Boolean.TRUE);
            try { net.minecraft.client.gui.ui.UIManager.getInstance().saveConfig(); } catch (Throwable t) { System.err.println("Failed to save UI init: " + t.getMessage()); }
        }
        ItemStack stack = mc.thePlayer.inventory.armorInventory[3 - slot];
        if (stack == null) {
            // hide when missing
            this.width = 0; this.height = 0; return;
        }
        FontRenderer fr = mc.fontRendererObj;
        RenderItem ri = mc.getRenderItem();
        String txt = "-";
        int contentW;
        if (stack != null)
        {
            int max = stack.getMaxDamage();
            int dmg = stack.getItemDamage();
            if (max > 0)
            {
                int rem = max - dmg;
                boolean showPercent = Boolean.TRUE.equals(getPropOrDefault("displayPercent", Boolean.TRUE));
                if (showPercent)
                {
                    int pct = (int)(rem * 100.0F / max);
                    txt = pct + "%";
                }
                else
                {
                    txt = rem + "/" + max;
                }
            }
            else {
                txt = stack.getDisplayName();
            }
        }

        // Rendu de l'icône — renderItemSafe() confine le glint enchantement à la zone 16×16
        renderItemSafe(ri, mc, stack, x, y);
        contentW = 18 + 4 + fr.getStringWidth(txt);
        this.width = Math.max(40, contentW + 6);
        this.height = 18;

        fr.drawStringWithShadow(txt, x + 18, y + 4, 0xFFFFFF);
    }
}
