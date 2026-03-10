package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.world.Explosion;

import java.util.Random;

/**
 * BlockObsidianDoor - Porte en obsidienne.
 * Même logique qu'une porte en bois (BlockDoor) mais avec la résistance de l'obsidienne.
 */
public class BlockObsidianDoor extends BlockDoor {

    public BlockObsidianDoor() {
        super(Material.rock);
        this.setHardness(50.0F);
        this.setResistance(2000.0F);
        this.setStepSound(Block.soundTypePiston);
        this.setUnlocalizedName("obsidian_door");
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return state.getValue(HALF) == BlockDoor.EnumDoorHalf.UPPER
                ? null
                : Item.getItemFromBlock(this);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    /**
     * Empêche la destruction par explosion
     */
    @Override
    public boolean canDropFromExplosion(Explosion explosionIn) {
        return false;
    }
}
