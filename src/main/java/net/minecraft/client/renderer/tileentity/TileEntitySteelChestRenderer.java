package net.minecraft.client.renderer.tileentity;

import net.minecraft.client.model.ModelChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.tileentity.TileEntitySteelChest;
import net.minecraft.util.ResourceLocation;

/**
 * Renderer du Coffre en Acier.
 * Utilise la texture dédiée {@code textures/entity/chest/steel_chest.png}
 * au lieu de la texture du coffre normal.
 */
public class TileEntitySteelChestRenderer extends TileEntitySpecialRenderer<TileEntitySteelChest>
{
    /** Texture dédiée au coffre en acier. */
    private static final ResourceLocation TEXTURE_STEEL = new ResourceLocation("textures/entity/chest/steel_chest.png");

    private final ModelChest model = new ModelChest();

    @Override
    public void renderTileEntityAt(TileEntitySteelChest te, double x, double y, double z, float partialTicks, int destroyStage)
    {
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(515);
        GlStateManager.depthMask(true);

        int meta = te.hasWorldObj() ? te.getBlockMetadata() : 3;

        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

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

        GlStateManager.translate((float) x, (float) y + 1.0F, (float) z + 1.0F);
        GlStateManager.scale(1.0F, -1.0F, -1.0F);
        GlStateManager.translate(0.5F, 0.5F, 0.5F);

        int rotation = 0;
        switch (meta)
        {
            case 2: rotation = 180; break;
            case 3: rotation = 0;   break;
            case 4: rotation = 90;  break;
            case 5: rotation = -90; break;
        }

        GlStateManager.rotate((float) rotation, 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(-0.5F, -0.5F, -0.5F);

        // Animation du couvercle
        float lidAngle = te.prevLidAngle + (te.lidAngle - te.prevLidAngle) * partialTicks;
        lidAngle = 1.0F - lidAngle;
        lidAngle = 1.0F - lidAngle * lidAngle * lidAngle;
        model.chestLid.rotateAngleX = -(lidAngle * (float) Math.PI / 2.0F);
        model.renderAll();

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

