package net.minecraft.block;

import net.minecraft.creativetab.CreativeTabs;

/**
 * Échelle en acier — grimpe 2x plus vite que l'échelle normale.
 * La logique de vitesse est dans EntityLivingBase.moveEntityWithHeading.
 */
public class BlockSteelLadder extends BlockLadder {

    public BlockSteelLadder() {
        this.setHardness(2.0F);
        this.setResistance(10.0F);
        this.setStepSound(Block.soundTypeMetal);
        this.setUnlocalizedName("steelLadder");
        this.setCreativeTab(CreativeTabs.tabDecorations);
    }
}

