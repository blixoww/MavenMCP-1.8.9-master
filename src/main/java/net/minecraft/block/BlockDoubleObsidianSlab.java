// ...existing code...
package net.minecraft.block;

import java.util.Random;
import net.minecraft.item.Item;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

public class BlockDoubleObsidianSlab extends BlockStoneSlab
{
    public BlockDoubleObsidianSlab()
    {
        super();
        IBlockState iblockstate = this.blockState.getBaseState().withProperty(VARIANT, BlockStoneSlab.EnumType.STONE);
        this.setDefaultState(iblockstate);
    }

    public boolean isDouble()
    {
        return true;
    }

    // Ensure double slab drops the obsidian slab item
    public Item getItemDropped(IBlockState state, Random rand, int fortune)
    {
        return Item.getItemFromBlock(Blocks.obsidian_slab);
    }

    public Item getItem(World worldIn, BlockPos pos)
    {
        return Item.getItemFromBlock(Blocks.obsidian_slab);
    }
}