package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;

/**
 * Trappe en obsidienne — très résistante, ouvrable manuellement (pas par redstone).
 */
public class BlockObsidianTrapdoor extends BlockTrapDoor {

    public BlockObsidianTrapdoor() {
        super(Material.rock);
        this.setHardness(50.0F);
        this.setResistance(2000.0F);
        this.setStepSound(Block.soundTypePiston);
        this.setUnlocalizedName("obsidianTrapdoor");
        this.setCreativeTab(CreativeTabs.tabRedstone);
    }
}

