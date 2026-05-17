package net.minecraft.item;

import net.minecraft.block.Block;

public class ItemSteelAnvilBlock extends ItemBlock
{
    public ItemSteelAnvilBlock(Block block)
    {
        super(block);
        this.setMaxDamage(0);
        this.setHasSubtypes(true);
    }

    @Override
    public int getMetadata(int damage)
    {
        return damage;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack)
    {
        switch (stack.getItemDamage())
        {
            case 1:  return "tile.steel_anvil.slightlyDamaged";
            case 2:  return "tile.steel_anvil.veryDamaged";
            default: return "tile.steel_anvil.intact";
        }
    }
}
