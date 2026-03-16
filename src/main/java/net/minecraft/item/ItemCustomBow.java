package net.minecraft.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.stats.StatList;
import net.minecraft.world.World;

/**
 * Arc custom avec durabilité et multiplicateur de dégâts configurables.
 */
public class ItemCustomBow extends ItemBow {

    private final float damageMultiplier;

    public ItemCustomBow(int durability, float damageMultiplier) {
        super();
        this.setMaxDamage(durability);
        this.damageMultiplier = damageMultiplier;
        this.setCreativeTab(CreativeTabs.tabCombat);
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World worldIn, EntityPlayer playerIn, int timeLeft) {
        boolean flag = playerIn.capabilities.isCreativeMode
                || EnchantmentHelper.getEnchantmentLevel(Enchantment.infinity.effectId, stack) > 0;

        if (flag || playerIn.inventory.hasItem(Items.arrow)) {
            int i = this.getMaxItemUseDuration(stack) - timeLeft;
            float f = (float) i / 20.0F;
            f = (f * f + f * 2.0F) / 3.0F;

            if ((double) f < 0.1D) {
                return;
            }
            if (f > 1.0F) {
                f = 1.0F;
            }

            EntityArrow entityarrow = new EntityArrow(worldIn, playerIn, f * 2.0F);

            if (f == 1.0F) {
                entityarrow.setIsCritical(true);
            }

            // Appliquer le multiplicateur de dégâts
            entityarrow.setDamage(entityarrow.getDamage() * this.damageMultiplier);

            int j = EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, stack);
            if (j > 0) {
                entityarrow.setDamage(entityarrow.getDamage() + (double) j * 0.5D + 0.5D);
            }

            int k = EnchantmentHelper.getEnchantmentLevel(Enchantment.punch.effectId, stack);
            if (k > 0) {
                entityarrow.setKnockbackStrength(k);
            }

            if (EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, stack) > 0) {
                entityarrow.setFire(100);
            }

            stack.damageItem(1, playerIn);
            worldIn.playSoundAtEntity(playerIn, "random.bow", 1.0F,
                    1.0F / (itemRand.nextFloat() * 0.4F + 1.2F) + f * 0.5F);

            if (flag) {
                entityarrow.canBePickedUp = 2;
            } else {
                playerIn.inventory.consumeInventoryItem(Items.arrow);
            }

            playerIn.triggerAchievement(StatList.objectUseStats[Item.getIdFromItem(this)]);

            if (!worldIn.isRemote) {
                worldIn.spawnEntityInWorld(entityarrow);
            }
        }
    }
}

