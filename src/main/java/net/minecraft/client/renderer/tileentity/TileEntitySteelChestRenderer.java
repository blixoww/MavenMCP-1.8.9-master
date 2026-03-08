package net.minecraft.client.renderer.tileentity;

import net.minecraft.client.model.ModelChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.tileentity.TileEntitySteelChest;
import net.minecraft.util.ResourceLocation;

/**
 * Renderer dédié au Coffre en Acier.
 * Utilise la texture steel_chest.png — copie exacte de la logique de TileEntityChestRenderer.
 */
public class TileEntitySteelChestRenderer extends TileEntitySpecialRenderer<TileEntitySteelChest>
{
    private static final ResourceLocation TEXTURE_STEEL =
            new ResourceLocation("textures/entity/chest/steel_chest.png");

    private final ModelChest model = new ModelChest();

    @Override
    public void renderTileEntityAt(TileEntitySteelChest te, double x, double y, double z,
                                   float partialTicks, int destroyStage)
    {
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(515);
        GlStateManager.depthMask(true);

        int meta = te.hasWorldObj() ? te.getBlockMetadata() : 0;

        // Texture
        if (destroyStage >= 0)
        {
            this.bindTexture(DESTROY_STAGES[destroyStage]);
            GlStateManager.matrixMode(5890);
            GlStateManager.pushMatrix();
            GlStateManager.scale(4.0F, 4.0F, 1.0F);
            GlStateManager.translate(0.0625F, 0.0625F, 0.0625F);
            GlStateManager.matrixMode(5888);
        }
        else
        {
            this.bindTexture(TEXTURE_STEEL);
        }

        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        // Positionnement exact identique au vanilla (translate + scale pour retourner le modèle)
        GlStateManager.translate((float) x, (float) y + 1.0F, (float) z + 1.0F);
        GlStateManager.scale(1.0F, -1.0F, -1.0F);
        GlStateManager.translate(0.5F, 0.5F, 0.5F);

        // Rotation selon la direction du coffre (métadonnées)
        int angle = 0;
        if      (meta == 2) angle = 180;
        else if (meta == 3) angle = 0;
        else if (meta == 4) angle = 90;
        else if (meta == 5) angle = -90;

        GlStateManager.rotate((float) angle, 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(-0.5F, -0.5F, -0.5F);

        // Angle d'ouverture du couvercle (identique au vanilla)
        float f = te.prevLidAngle + (te.lidAngle - te.prevLidAngle) * partialTicks;
        f = 1.0F - f;
        f = 1.0F - f * f * f;
        this.model.chestLid.rotateAngleX = -(f * (float) Math.PI / 2.0F);

        this.model.renderAll();

        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        if (destroyStage >= 0)
        {
            GlStateManager.matrixMode(5890);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
        }
    }
}
