package net.minecraft.client.renderer.culling;

import net.minecraft.util.AxisAlignedBB;

public class Frustum implements ICamera
{
    private final ClippingHelper clippingHelper;
    private double xPosition;
    private double yPosition;
    private double zPosition;

    // Cache simple pour le dernier AABB testé. Si la même boîte est testée consécutivement,
    // on retourne le résultat mis en cache sans recalculer les plans du frustum.
    private double lastMinX = Double.NaN;
    private double lastMinY = Double.NaN;
    private double lastMinZ = Double.NaN;
    private double lastMaxX = Double.NaN;
    private double lastMaxY = Double.NaN;
    private double lastMaxZ = Double.NaN;
    private boolean lastResult = false;

    public Frustum()
    {
        this(ClippingHelperImpl.getInstance());
    }

    public Frustum(ClippingHelper p_i46196_1_)
    {
        this.clippingHelper = p_i46196_1_;
    }

    public void setPosition(double p_78547_1_, double p_78547_3_, double p_78547_5_)
    {
        this.xPosition = p_78547_1_;
        this.yPosition = p_78547_3_;
        this.zPosition = p_78547_5_;

        // Invalidate last cached box when the camera moves.
        this.lastMinX = Double.NaN;
    }

    /**
     * Calls the clipping helper. Returns true if the box is inside all 6 clipping planes, otherwise returns false.
     */
    public boolean isBoxInFrustum(double p_78548_1_, double p_78548_3_, double p_78548_5_, double p_78548_7_, double p_78548_9_, double p_78548_11_)
    {
        // Quantités localisées (relative to frustum position)
        double minX = p_78548_1_ - this.xPosition;
        double minY = p_78548_3_ - this.yPosition;
        double minZ = p_78548_5_ - this.zPosition;
        double maxX = p_78548_7_ - this.xPosition;
        double maxY = p_78548_9_ - this.yPosition;
        double maxZ = p_78548_11_ - this.zPosition;

        // Si la même boîte a été testée juste avant, renvoyer le résultat en cache (évite recalculs répétés)
        if (minX == this.lastMinX && minY == this.lastMinY && minZ == this.lastMinZ && maxX == this.lastMaxX && maxY == this.lastMaxY && maxZ == this.lastMaxZ)
        {
            return this.lastResult;
        }

        boolean result = this.clippingHelper.isBoxInFrustum(minX, minY, minZ, maxX, maxY, maxZ);

        // Stocker dans le cache simple
        this.lastMinX = minX;
        this.lastMinY = minY;
        this.lastMinZ = minZ;
        this.lastMaxX = maxX;
        this.lastMaxY = maxY;
        this.lastMaxZ = maxZ;
        this.lastResult = result;

        return result;
    }

    /**
     * Returns true if the bounding box is inside all 6 clipping planes, otherwise returns false.
     */
    public boolean isBoundingBoxInFrustum(AxisAlignedBB p_78546_1_)
    {
        return this.isBoxInFrustum(p_78546_1_.minX, p_78546_1_.minY, p_78546_1_.minZ, p_78546_1_.maxX, p_78546_1_.maxY, p_78546_1_.maxZ);
    }
}
