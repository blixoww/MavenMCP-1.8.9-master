package net.minecraft.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

import java.util.List;

/**
 * Pumpkie Pie — Nourriture qui donne Résistance au Feu, Vitesse II et Force II.
 * Craft: Pumpkin Pie entourée de 4 blocs d'émeraude.
 */
public class ItemPumpkiePie extends ItemFood {

    private static final int DURATION_TICKS = 60 * 20; // 60 secondes

    public ItemPumpkiePie() {
        super(10, 1.2F, false); // 10 nourriture, 1.2 saturation
        this.setAlwaysEdible();
        this.setUnlocalizedName("pumpkiePie");
        this.setCreativeTab(CreativeTabs.tabFood);
    }

    @Override
    protected void onFoodEaten(ItemStack stack, World worldIn, EntityPlayer player) {
        super.onFoodEaten(stack, worldIn, player);
        if (!worldIn.isRemote) {
            // Résistance au feu (niveau 0 = tier I)
            player.addPotionEffect(new PotionEffect(Potion.fireResistance.id, DURATION_TICKS, 0));
            // Vitesse II (niveau 1 = tier II)
            player.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, DURATION_TICKS, 1));
            // Force II (niveau 1 = tier II)
            player.addPotionEffect(new PotionEffect(Potion.damageBoost.id, DURATION_TICKS, 1));
        }
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer playerIn, List<String> tooltip, boolean advanced) {
        tooltip.add("");
        tooltip.add("\u00a76\u00a7lEffets en mangeant:");
        tooltip.add("\u00a7c \u2726 Résistance au Feu I \u00a78(60s)");
        tooltip.add("\u00a7b \u2726 Vitesse II \u00a78(60s)");
        tooltip.add("\u00a74 \u2726 Force II \u00a78(60s)");
    }
}

