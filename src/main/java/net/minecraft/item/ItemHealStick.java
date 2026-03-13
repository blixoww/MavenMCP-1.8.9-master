package net.minecraft.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

import java.util.List;

/**
 * Heal Stick — Soigne le joueur de 4 cœurs (8 HP) au clic droit.
 * Cooldown de 5 secondes. Durabilité limitée (50 utilisations).
 */
public class ItemHealStick extends Item {

    public ItemHealStick() {
        this.setMaxStackSize(1);
        this.setMaxDamage(50);
        this.setUnlocalizedName("healStick");
        this.setCreativeTab(CreativeTabs.tabCombat);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack itemStackIn, World worldIn, EntityPlayer playerIn) {
        if (!worldIn.isRemote) {
            // Vérifier le cooldown via NBT
            NBTTagCompound tag = itemStackIn.getTagCompound();
            if (tag == null) {
                tag = new NBTTagCompound();
                itemStackIn.setTagCompound(tag);
            }
            long lastUse = tag.getLong("HealStickLastUse");
            long now = worldIn.getTotalWorldTime();
            // 5 secondes = 100 ticks
            if (now - lastUse < 100) {
                long remaining = (100 - (now - lastUse)) / 20;
                playerIn.addChatMessage(new net.minecraft.util.ChatComponentText(
                        "\u00a7cCooldown: \u00a7f" + (remaining + 1) + "s"));
                return itemStackIn;
            }

            // Soigne 8 HP (4 cœurs)
            float heal = Math.min(8.0F, playerIn.getMaxHealth() - playerIn.getHealth());
            if (heal > 0) {
                playerIn.heal(heal);
                // Effet visuel : particules de régénération
                playerIn.addPotionEffect(new PotionEffect(Potion.regeneration.id, 40, 0)); // 2s de regen
                itemStackIn.damageItem(1, playerIn);
                tag.setLong("HealStickLastUse", now);
            }
        }
        return itemStackIn;
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer playerIn, List<String> tooltip, boolean advanced) {
        tooltip.add("\u00a7a+4 \u2764 Soins instantan\u00e9s");
        tooltip.add("\u00a77R\u00e9g\u00e9n\u00e9ration I (2s)");
        tooltip.add("\u00a78Cooldown: \u00a7f5s");
    }

    @Override
    public boolean isFull3D() {
        return true;
    }
}

