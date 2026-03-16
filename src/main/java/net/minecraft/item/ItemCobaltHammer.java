package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * Cobalt Hammer - pioche 3x3 en cobalt.
 */
public class ItemCobaltHammer extends ItemPickaxe {

    public ItemCobaltHammer() {
        super(Item.ToolMaterial.COBALT);
        this.setUnlocalizedName("cobalt_hammer");
        this.setCreativeTab(CreativeTabs.tabTools);
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World worldIn, Block blockIn, BlockPos pos, EntityLivingBase playerIn) {
        if (!worldIn.isRemote && playerIn instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) playerIn;
            EnumFacing facing = EnumFacing.getHorizontal(
                MathHelper.floor_double((double)(player.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3
            );
            boolean lookingUp = player.rotationPitch < -45.0F;
            boolean lookingDown = player.rotationPitch > 45.0F;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    BlockPos targetPos;
                    if (lookingUp || lookingDown) {
                        targetPos = pos.add(dx, 0, dy);
                    } else if (facing == EnumFacing.NORTH || facing == EnumFacing.SOUTH) {
                        targetPos = pos.add(dx, dy, 0);
                    } else {
                        targetPos = pos.add(0, dy, dx);
                    }
                    IBlockState state = worldIn.getBlockState(targetPos);
                    Block block = state.getBlock();
                    if (block != Blocks.air && block != Blocks.bedrock
                        && block.getBlockHardness(worldIn, targetPos) >= 0
                        && (block.getMaterial() == Material.rock
                            || block.getMaterial() == Material.iron
                            || block.getMaterial() == Material.anvil
                            || this.canHarvestBlock(block))) {
                        block.dropBlockAsItem(worldIn, targetPos, state, 0);
                        worldIn.setBlockToAir(targetPos);
                    }
                }
            }
        }
        return super.onBlockDestroyed(stack, worldIn, blockIn, pos, playerIn);
    }
}

