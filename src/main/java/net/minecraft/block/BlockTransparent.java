package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.world.IBlockAccess;

/**
 * Transparent Block — X-Ray block.
 *
 * Mécanisme :
 *  - isOpaqueCube() = false  → le moteur rend les faces des blocs voisins
 *                              qui touchent ce bloc, permettant de "voir à travers"
 *  - isFullCube()   = true   → le bloc occupe tout son voxel → rendu cube complet
 *                              (6 faces), taille normale en main
 *  - lightOpacity   = 0      → aucune ombre portée, lumière non bloquée
 *  - TRANSLUCENT             → alpha de la texture respecté, on voit à travers
 */
public class BlockTransparent extends Block {

    public BlockTransparent() {
        super(Material.glass);
        this.setHardness(0.3F);
        this.setResistance(2.0F);
        this.setStepSound(soundTypeGlass);
        this.setUnlocalizedName("transparent_block");
        this.setCreativeTab(CreativeTabs.tabBlock);
        this.setLightOpacity(0);
    }


    /** TRANSLUCENT → l'alpha de la texture PNG est respecté pixel par pixel. */
    @Override
    public EnumWorldBlockLayer getBlockLayer() {
        return EnumWorldBlockLayer.TRANSLUCENT;
    }

    /**
     * Toujours rendre toutes les faces, SAUF si le voisin direct est aussi
     * un TransparentBlock (évite les doublons de faces entre deux blocs adjacents).
     *
     * Ne JAMAIS appeler super ici : super.shouldSideBeRendered retourne false
     * quand le voisin est opaque, ce qui empêcherait justement le x-ray.
     */
    @Override
    public boolean shouldSideBeRendered(IBlockAccess world, BlockPos neighborPos, EnumFacing side) {
        return !(world.getBlockState(neighborPos).getBlock() instanceof BlockTransparent);
    }
}
