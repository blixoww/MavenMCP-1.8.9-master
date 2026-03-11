package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.util.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import java.util.Random;

/**
 * BlockObsidianDoor - Porte en obsidienne.
 * Même logique qu'une porte en bois (BlockDoor) mais avec la résistance de l'obsidienne.
 * Ne réagit PAS à la redstone (sinon le doPhysics referme la porte instantanément).
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
     * Override onNeighborBlockChange pour empêcher la logique redstone de refermer la porte.
     * On garde uniquement la logique structurelle (casser si la moitié partenaire disparaît).
     */
    @Override
    public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock) {
        if (state.getValue(HALF) == BlockDoor.EnumDoorHalf.UPPER) {
            BlockPos blockpos = pos.down();
            IBlockState iblockstate = worldIn.getBlockState(blockpos);

            if (iblockstate.getBlock() != this) {
                worldIn.setBlockToAir(pos);
            }
            // Ne PAS propager vers le bas — ça empêche la cascade redstone
        } else {
            BlockPos blockpos1 = pos.up();
            IBlockState iblockstate1 = worldIn.getBlockState(blockpos1);

            if (iblockstate1.getBlock() != this) {
                worldIn.setBlockToAir(pos);
            }

            if (!World.doesBlockHaveSolidTopSurface(worldIn, pos.down())) {
                worldIn.setBlockToAir(pos);
                if (iblockstate1.getBlock() == this) {
                    worldIn.setBlockToAir(blockpos1);
                }
            }
            // PAS de logique redstone — la porte s'ouvre/ferme uniquement par clic
        }
    }

    /**
     * Empêche la destruction par explosion
     */
    @Override
    public boolean canDropFromExplosion(Explosion explosionIn) {
        return false;
    }
}
