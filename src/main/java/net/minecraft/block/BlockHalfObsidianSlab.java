package net.minecraft.block;

import java.util.Random;
import net.minecraft.item.Item;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.block.properties.IProperty;

public class BlockHalfObsidianSlab extends BlockStoneSlab
{
    public BlockHalfObsidianSlab()
    {
        super();
        IBlockState iblockstate = this.blockState.getBaseState().withProperty(HALF, BlockSlab.EnumBlockHalf.BOTTOM).withProperty(VARIANT, BlockStoneSlab.EnumType.STONE);
        this.setDefaultState(iblockstate);
        this.setCreativeTab(CreativeTabs.tabBlock);
    }

    @Override
    public void getSubBlocks(Item itemIn, CreativeTabs tab, java.util.List list)
    {
        // Only provide a single variant of the obsidian slab in the creative inventory (metadata 0)
        list.add(new ItemStack(itemIn, 1, 0));
    }

    public boolean isDouble()
    {
        return false;
    }

    public Item getItemDropped(IBlockState state, Random rand, int fortune)
    {
        return Item.getItemFromBlock(Blocks.obsidian_slab);
    }

    public Item getItem(World worldIn, BlockPos pos)
    {
        return Item.getItemFromBlock(Blocks.obsidian_slab);
    }

    public String getUnlocalizedName(int meta)
    {
        return super.getUnlocalizedName();
    }

    public IProperty<?> getVariantProperty()
    {
        return VARIANT;
    }

    public Object getVariant(ItemStack stack)
    {
        return BlockStoneSlab.EnumType.byMetadata(stack.getMetadata() & 7);
    }
}
