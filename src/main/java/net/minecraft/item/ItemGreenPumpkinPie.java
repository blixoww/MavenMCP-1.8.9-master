package net.minecraft.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

import java.util.List;

/**
 * Green Pumpkin Pie — same effects as previous Pumpkie Pie, kept as separate class for proper naming.
 */
public class ItemGreenPumpkinPie extends ItemFood {

    private static final int DURATION_TICKS = 6 * 60 * 20; // 6 minutes

    public ItemGreenPumpkinPie() {
        super(10, 1.2F, false); // 10 nourriture, 1.2 saturation
        this.setAlwaysEdible();
        this.setUnlocalizedName("greenPumpkinPie");
        this.setCreativeTab(CreativeTabs.tabFood);
    }

    @Override
    protected void onFoodEaten(ItemStack stack, World worldIn, EntityPlayer player) {
        super.onFoodEaten(stack, worldIn, player);
        if (!worldIn.isRemote) {
            // Fire resistance I
            player.addPotionEffect(new PotionEffect(Potion.fireResistance.id, DURATION_TICKS, 0));
            // Speed II
            player.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, DURATION_TICKS, 1));
            // Strength II
            player.addPotionEffect(new PotionEffect(Potion.damageBoost.id, DURATION_TICKS, 1));
        }
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer playerIn, List<String> tooltip, boolean advanced) {
        tooltip.add("");
        tooltip.add("\u00a76\u00a7lEffets en mangeant:");
        tooltip.add("\u00a7c \u2726 Résistance au Feu I \u00a78(6m)");
        tooltip.add("\u00a7b \u2726 Vitesse II \u00a78(6m)");
        tooltip.add("\u00a74 \u2726 Force II \u00a78(6m)");
    }
}

