package net.minecraft.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.Random;

/**
 * BlockRandomOre - Minerai aléatoire.
 * Aussi rare que le diamant (spawn Y <= 16, veine de 1 à 8 blocs).
 * En minant, donne aléatoirement :
 *  - Ruby (33%)
 *  - Cobalt Ingot (33%)
 *  - Emerald (34%)
 */
public class BlockRandomOre extends BlockOre {

    public BlockRandomOre() {
        super();
        this.setCreativeTab(CreativeTabs.tabBlock);
        this.setHardness(3.0F);          // Même dureté que le minerai de diamant
        this.setResistance(5.0F);
        this.setStepSound(Block.soundTypeStone);
        this.setUnlocalizedName("random_ore");
    }

    /**
     * Choisit aléatoirement l'item à dropper : ruby (0), cobalt_ingot (1) ou emerald (2).
     */
    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        int roll = rand.nextInt(3);
        switch (roll) {
            case 0: return Items.ruby;
            case 1: return Items.cobalt_ingot;
            default: return Items.emerald;
        }
    }

    /**
     * Toujours 1 item dropé (comme le diamant de base, avant fortune).
     */
    @Override
    public int quantityDropped(Random random) {
        return 1;
    }

    /**
     * Avec fortune : peut dropper plus, comme le diamant.
     */
    @Override
    public int quantityDroppedWithBonus(int fortune, Random random) {
        if (fortune > 0) {
            int i = random.nextInt(fortune + 2) - 1;
            if (i < 0) i = 0;
            return this.quantityDropped(random) * (i + 1);
        }
        return this.quantityDropped(random);
    }

    /**
     * XP droppé lors du minage (3 à 7, identique au diamant).
     */
    @Override
    public void dropBlockAsItemWithChance(World worldIn, BlockPos pos, IBlockState state, float chance, int fortune) {
        super.dropBlockAsItemWithChance(worldIn, pos, state, chance, fortune);
        this.dropXpOnBlockBreak(worldIn, pos, MathHelper.getRandomIntegerInRange(worldIn.rand, 3, 7));
    }
}

