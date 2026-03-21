package net.minecraft.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

import java.util.List;

/**
 * Pomme de Cobalt — Plus puissante que la pomme en or enchantée.
 * Effets : Regen V (30s), Resistance II (5min), Absorption IV (2min),
 *          Fire Resist I (5min), Strength III (2min), Speed II (2min).
 */
public class ItemCobaltApple extends ItemFood {

    public ItemCobaltApple() {
        super(8, 2.4F, false);
        this.setAlwaysEdible();
        this.setUnlocalizedName("cobalt_apple");
        this.setCreativeTab(CreativeTabs.tabFood);
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true; // Effet de brillance comme la pomme en or enchantée
    }

    @Override
    protected void onFoodEaten(ItemStack stack, World worldIn, EntityPlayer player) {
        super.onFoodEaten(stack, worldIn, player);
        if (!worldIn.isRemote) {
            player.addPotionEffect(new PotionEffect(Potion.regeneration.id, 30 * 20, 4));
            player.addPotionEffect(new PotionEffect(Potion.resistance.id, 5 * 60 * 20, 1));
            player.addPotionEffect(new PotionEffect(Potion.absorption.id, 2 * 60 * 20, 3));
            player.addPotionEffect(new PotionEffect(Potion.fireResistance.id, 5 * 60 * 20, 0));
            player.addPotionEffect(new PotionEffect(Potion.damageBoost.id, 2 * 60 * 20, 2));
            player.addPotionEffect(new PotionEffect(Potion.moveSpeed.id, 2 * 60 * 20, 1));
        }
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer playerIn, List<String> tooltip, boolean advanced) {
        tooltip.add("");
        tooltip.add("\u00a76\u00a7lEffets en mangeant:");
        tooltip.add("\u00a7d \u2726 Régénération V \u00a78(30s)");
        tooltip.add("\u00a79 \u2726 Résistance II \u00a78(5min)");
        tooltip.add("\u00a7e \u2726 Absorption IV \u00a78(2min)");
        tooltip.add("\u00a7c \u2726 Résistance au feu I \u00a78(5min)");
        tooltip.add("\u00a74 \u2726 Force III \u00a78(2min)");
        tooltip.add("\u00a7b \u2726 Vitesse II \u00a78(2min)");
    }
}

