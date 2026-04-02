package net.minecraft.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

/**
 * ItemMultiTool - Outil ultime combinant TOUS les matériaux en un seul item.
 * - 5120 points de durabilité
 * - Efficacité maximale sur tous les blocs (getStrVsBlock = 16.0 universel)
 * - canHarvestBlock = true → drops garantis sur tous les blocs
 * - +5 dégâts contre les entités (6.5 cœurs)
 */
public class ItemMultiTool extends Item {

    public static final int   MAX_USES        = 5120;
    public static final float EFFICIENCY      = 20.0F;
    private static final float DAMAGE_VS_ENTITY = 5.0F;

    public ItemMultiTool() {
        this.maxStackSize = 1;
        this.setMaxDamage(MAX_USES);
        this.setCreativeTab(CreativeTabs.tabTools);
        this.setUnlocalizedName("multi_tool");
    }

    /** Vitesse de minage maximale sur ABSOLUMENT tous les blocs. */
    @Override
    public float getStrVsBlock(ItemStack stack, Block block) {
        return EFFICIENCY;
    }

    /**
     * Indispensable pour que les blocs droppent leurs items.
     * Sans ce override, le jeu considère que l'outil n'est pas le bon → pas de drop.
     */
    @Override
    public boolean canHarvestBlock(Block block) {
        return true;
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        stack.damageItem(2, attacker);
        return true;
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World worldIn, Block blockIn, BlockPos pos, EntityLivingBase playerIn) {
        if ((double) blockIn.getBlockHardness(worldIn, pos) != 0.0D) {
            stack.damageItem(1, playerIn);
        }
        return true;
    }

    @Override
    public boolean isFull3D() {
        return true;
    }

    @Override
    public Multimap<String, AttributeModifier> getItemAttributeModifiers() {
        Multimap<String, AttributeModifier> multimap = HashMultimap.create();
        multimap.put(
                SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName(),
                new AttributeModifier(itemModifierUUID, "Weapon modifier", DAMAGE_VS_ENTITY, 0)
        );
        return multimap;
    }

    @Override
    public int getItemEnchantability() {
        return 30;
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return false;
    }
}
