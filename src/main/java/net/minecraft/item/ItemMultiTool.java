package net.minecraft.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import java.util.Set;

/**
 * ItemMultiTool - Outil ultime combinant TOUS les matériaux en un seul item.
 * - 5120 points de durabilité
 * - Efficacité maximale sur tous les blocs
 * - +5 dégâts contre les entités (6.5 cœurs)
 * Recette : 3 blocs de cobalt en haut + 2 cobalt sur les côtés + 2 bâtons en bas
 */
public class ItemMultiTool extends Item {

    /** Durabilité fixée à 5120 (bien au-dessus de 5000 demandé) */
    public static final int MAX_USES = 5120;

    /** Vitesse de minage sur TOUS les blocs */
    public static final float EFFICIENCY = 16.0F;

    /** Dégâts bonus contre les entités (+5 demi-cœurs) */
    private static final float DAMAGE_VS_ENTITY = 5.0F;

    private final Set<Block> effectiveBlocks;

    public ItemMultiTool() {
        this.maxStackSize = 1;
        this.setMaxDamage(MAX_USES);
        this.setCreativeTab(CreativeTabs.tabTools);
        this.setUnlocalizedName("multi_tool");

        // Tous les blocs pouvant être minés efficacement
        this.effectiveBlocks = Sets.newHashSet(
                Blocks.planks, Blocks.oak_stairs, Blocks.chest, Blocks.crafting_table,
                Blocks.log, Blocks.log2, Blocks.cobblestone, Blocks.double_stone_slab,
                Blocks.stone_slab, Blocks.brick_block, Blocks.stone, Blocks.mossy_cobblestone,
                Blocks.iron_block, Blocks.gold_block, Blocks.diamond_block, Blocks.emerald_block,
                Blocks.obsidian, Blocks.nether_brick, Blocks.prismarine, Blocks.packed_ice,
                Blocks.iron_ore, Blocks.gold_ore, Blocks.coal_ore, Blocks.diamond_ore,
                Blocks.emerald_ore, Blocks.lapis_ore, Blocks.redstone_ore, Blocks.quartz_ore,
                Blocks.dirt, Blocks.sand, Blocks.gravel, Blocks.grass, Blocks.farmland,
                Blocks.soul_sand, Blocks.clay, Blocks.snow, Blocks.snow_layer,
                Blocks.netherrack, Blocks.hardened_clay, Blocks.stained_hardened_clay,
                Blocks.web, Blocks.wool, Blocks.hay_block,
                Blocks.ruby_ore, Blocks.cobalt_ore,
                Blocks.ruby_block, Blocks.cobalt_block, Blocks.steel_block
        );
    }

    @Override
    public float getStrVsBlock(ItemStack stack, Block block) {
        // Efficace sur tous les blocs
        if (block.getMaterial() == Material.rock
                || block.getMaterial() == Material.iron
                || block.getMaterial() == Material.anvil
                || block.getMaterial() == Material.clay
                || block.getMaterial() == Material.sand
                || block.getMaterial() == Material.ground
                || block.getMaterial() == Material.grass
                || block.getMaterial() == Material.snow
                || block.getMaterial() == Material.craftedSnow
                || block.getMaterial() == Material.wood) {
            return EFFICIENCY;
        }
        return this.effectiveBlocks.contains(block) ? EFFICIENCY : 2.0F;
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
        return 30; // Très enchantable
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return false; // Non réparable (trop puissant)
    }
}


