package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySteelChest;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class BlockSteelChest extends BlockContainer {
    public static final PropertyDirection FACING = BlockChest.FACING;

    public BlockSteelChest() {
        super(Material.iron);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
        this.setCreativeTab(CreativeTabs.tabDecorations);
        this.setHardness(5.0F);
        this.setResistance(100.0F); // Résistant aux explosions (TNT = 4, Creeper = 3, Wither = 20)
        this.setStepSound(Block.soundTypeMetal);
        this.setUnlocalizedName("steelChest");
        this.setBlockBounds(0.0625F, 0.0F, 0.0625F, 0.9375F, 0.875F, 0.9375F);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean isFullCube() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 2;
    }

    @Override
    public IBlockState onBlockPlaced(World world, BlockPos pos, EnumFacing facing,
                                     float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        EnumFacing dir = placer.getHorizontalFacing().getOpposite();
        return this.getDefaultState().withProperty(FACING, dir);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
                                EntityLivingBase placer, ItemStack stack) {
        EnumFacing dir = EnumFacing.getHorizontal(
                MathHelper.floor_double((double) (placer.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3).getOpposite();
        world.setBlockState(pos, state.withProperty(FACING, dir), 3);
        if (stack.hasDisplayName()) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileEntitySteelChest)
                ((TileEntitySteelChest) te).setCustomName(stack.getDisplayName());
        }
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntitySteelChest();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumFacing side,
                                    float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;

        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntitySteelChest)) return false;

        // Bloqué si bloc solide au-dessus
        if (world.getBlockState(pos.up()).getBlock().isNormalCube()) return false;

        TileEntitySteelChest chest = (TileEntitySteelChest) te;
        player.displayGUIChest(chest);
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntitySteelChest) {
            InventoryHelper.dropInventoryItems(world, pos, (TileEntitySteelChest) te);
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing facing = EnumFacing.getFront(meta);
        if (facing.getAxis() == EnumFacing.Axis.Y) facing = EnumFacing.NORTH;
        return this.getDefaultState().withProperty(FACING, facing);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return ((EnumFacing) state.getValue(FACING)).getIndex();
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, FACING);
    }
}
