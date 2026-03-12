package net.minecraft.client.renderer.entity;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.layers.LayerArrow;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.client.renderer.entity.layers.LayerDeadmau5Head;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class RenderPlayer extends RendererLivingEntity<AbstractClientPlayer> {
    /**
     * this field is used to indicate the 3-pixel wide arms
     */
    private boolean smallArms;

    public RenderPlayer(RenderManager renderManager) {
        this(renderManager, false);
    }

    public RenderPlayer(RenderManager renderManager, boolean useSmallArms) {
        super(renderManager, new ModelPlayer(0.0F, useSmallArms), 0.5F);
        this.smallArms = useSmallArms;
        this.addLayer(new LayerBipedArmor(this));
        this.addLayer(new LayerHeldItem(this));
        this.addLayer(new LayerArrow(this));
        this.addLayer(new LayerDeadmau5Head(this));
        this.addLayer(new LayerCape(this));
        this.addLayer(new LayerCustomHead(this.getMainModel().bipedHead));
    }

    public ModelPlayer getMainModel() {
        return (ModelPlayer) super.getMainModel();
    }

    /**
     * Renders the desired {@code T} type Entity.
     */
    public void doRender(AbstractClientPlayer entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (!entity.isUser() || this.renderManager.livingPlayer == entity) {
            double d0 = y;

            if (entity.isSneaking() && !(entity instanceof EntityPlayerSP)) {
                d0 = y - 0.125D;
            }

            this.setModelVisibilities(entity);
            super.doRender(entity, x, d0, z, entityYaw, partialTicks);
        }
    }


    private void setModelVisibilities(AbstractClientPlayer clientPlayer) {
        ModelPlayer modelplayer = this.getMainModel();

        if (clientPlayer.isSpectator()) {
            modelplayer.setInvisible(false);
            modelplayer.bipedHead.showModel = true;
            modelplayer.bipedHeadwear.showModel = true;
        } else {
            ItemStack itemstack = clientPlayer.inventory.getCurrentItem();
            modelplayer.setInvisible(true);
            modelplayer.bipedHeadwear.showModel = clientPlayer.isWearing(EnumPlayerModelParts.HAT);
            modelplayer.bipedBodyWear.showModel = clientPlayer.isWearing(EnumPlayerModelParts.JACKET);
            modelplayer.bipedLeftLegwear.showModel = clientPlayer.isWearing(EnumPlayerModelParts.LEFT_PANTS_LEG);
            modelplayer.bipedRightLegwear.showModel = clientPlayer.isWearing(EnumPlayerModelParts.RIGHT_PANTS_LEG);
            modelplayer.bipedLeftArmwear.showModel = clientPlayer.isWearing(EnumPlayerModelParts.LEFT_SLEEVE);
            modelplayer.bipedRightArmwear.showModel = clientPlayer.isWearing(EnumPlayerModelParts.RIGHT_SLEEVE);
            modelplayer.heldItemLeft = 0;
            modelplayer.aimedBow = false;
            modelplayer.isSneak = clientPlayer.isSneaking();

            if (itemstack == null) {
                modelplayer.heldItemRight = 0;
            } else {
                modelplayer.heldItemRight = 1;

                if (clientPlayer.getItemInUseCount() > 0) {
                    EnumAction enumaction = itemstack.getItemUseAction();

                    if (enumaction == EnumAction.BLOCK) {
                        modelplayer.heldItemRight = 3;
                    } else if (enumaction == EnumAction.BOW) {
                        modelplayer.aimedBow = true;
                    }
                }
            }
        }
    }

    /**
     * Returns the location of an entity's texture. Doesn't seem to be called unless you call Render.bindEntityTexture.
     */
    protected ResourceLocation getEntityTexture(AbstractClientPlayer entity) {
        return entity.getLocationSkin();
    }

    public void transformHeldFull3DItemLayer() {
        GlStateManager.translate(0.0F, 0.1875F, 0.0F);
    }

    /**
     * Allows the render to do any OpenGL state modifications necessary before the model is rendered. Args:
     * entityLiving, partialTickTime
     */
    protected void preRenderCallback(AbstractClientPlayer entitylivingbaseIn, float partialTickTime) {
        float f = 0.9375F;
        GlStateManager.scale(f, f, f);
    }

    protected void renderOffsetLivingLabel(AbstractClientPlayer entityIn, double x, double y, double z, String str, float p_177069_9_, double p_177069_10_) {
        // ── Barre de vie JUSTE AU-DESSUS du pseudo (distance max 32 blocs) ─────────
        if (p_177069_10_ < (32.0D * 32.0D)) {
            this.renderHealthBar(entityIn, x, y, z, p_177069_9_);
        }

        // Pseudo en dessous de la barre
        super.renderOffsetLivingLabel(entityIn, x, y, z, str, p_177069_9_, p_177069_10_);
    }

    private void renderHealthBar(AbstractClientPlayer entity, double x, double y, double z, float scale) {
        float maxHealth = entity.getMaxHealth();
        float currentHealth = entity.getHealth();
        if (maxHealth <= 0) return;
        float ratio = Math.max(0f, Math.min(1f, currentHealth / maxHealth));

        float r, g, b;
        if (ratio > 0.6f) {
            r = 0.15f; g = 0.85f; b = 0.15f;
        } else if (ratio > 0.3f) {
            r = 1.0f;  g = 0.5f;  b = 0.0f;
        } else {
            r = 0.9f;  g = 0.1f;  b = 0.1f;
        }

        final float BAR_W  = 40f;
        final float BAR_H  = 3f;
        final float BORDER = 1f;
        final float half   = BAR_W / 2f;

        GlStateManager.pushMatrix();

        // ── Reproduire exactement le translate du parent (RendererLivingEntity.renderLivingLabel) ──
        // Le pseudo est placé à (x, y + entity.height + 0.5, z) en coords monde.
        // On se place là, puis on fait le billboard comme le parent.
        double nameY = y + entity.height + 0.5D;
        if (entity.isSneaking()) nameY -= 0.25D;

        GlStateManager.translate((float) x, (float) nameY, (float) z);
        GL11.glNormal3f(0f, 1f, 0f);
        GlStateManager.rotate(-this.renderManager.playerViewY, 0f, 1f, 0f);
        GlStateManager.rotate(this.renderManager.playerViewX, 1f, 0f, 0f);
        GlStateManager.scale(-scale, -scale, scale);

        // Dans l'espace billboard après scale(-scale,-scale,scale) :
        //   Y=0       = point de référence du pseudo (centre du label)
        //   Y positif = vers le BAS de l'écran
        //   Y négatif = vers le HAUT de l'écran
        //
        // Le pseudo est centré verticalement à Y ≈ 0.
        // On veut la barre JUSTE AU-DESSUS → Y négatif = -(BAR_H + BORDER*2 + marge).
        float margin = 2f;
        float barOffsetY = -(BAR_H + BORDER * 2f + margin);
        GlStateManager.translate(0f, barOffsetY, 0f);

        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        float top    = 0f;
        float bottom = BAR_H + BORDER * 2f;
        float iL = -half + BORDER;
        float iR =  half - BORDER;
        float iT = top    + BORDER;
        float iB = bottom - BORDER;

        // 1. Contour noir
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(-half, top,    0).color(0f, 0f, 0f, 0.85f).endVertex();
        wr.pos(-half, bottom, 0).color(0f, 0f, 0f, 0.85f).endVertex();
        wr.pos( half, bottom, 0).color(0f, 0f, 0f, 0.85f).endVertex();
        wr.pos( half, top,    0).color(0f, 0f, 0f, 0.85f).endVertex();
        tess.draw();

        // 2. Fond gris sombre (zone vide)
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(iL, iT, 0).color(0.25f, 0.25f, 0.25f, 0.9f).endVertex();
        wr.pos(iL, iB, 0).color(0.25f, 0.25f, 0.25f, 0.9f).endVertex();
        wr.pos(iR, iB, 0).color(0.25f, 0.25f, 0.25f, 0.9f).endVertex();
        wr.pos(iR, iT, 0).color(0.25f, 0.25f, 0.25f, 0.9f).endVertex();
        tess.draw();

        // 3. Barre colorée avec dégradé vertical
        if (ratio > 0f) {
            float fillR = iL + (iR - iL) * ratio;
            float rD = r * 0.55f, gD = g * 0.55f, bD = b * 0.55f;
            wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
            wr.pos(iL,    iT, 0).color(r,  g,  b,  1f).endVertex();
            wr.pos(iL,    iB, 0).color(rD, gD, bD, 1f).endVertex();
            wr.pos(fillR, iB, 0).color(rD, gD, bD, 1f).endVertex();
            wr.pos(fillR, iT, 0).color(r,  g,  b,  1f).endVertex();
            tess.draw();

            // Reflet brillant (tiers supérieur)
            float midY = iT + (iB - iT) * 0.35f;
            wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
            wr.pos(iL,    iT,  0).color(1f, 1f, 1f, 0.18f).endVertex();
            wr.pos(iL,    midY,0).color(1f, 1f, 1f, 0.0f ).endVertex();
            wr.pos(fillR, midY,0).color(1f, 1f, 1f, 0.0f ).endVertex();
            wr.pos(fillR, iT,  0).color(1f, 1f, 1f, 0.18f).endVertex();
            tess.draw();
        }

        GlStateManager.enableTexture2D();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();
    }

    public void renderRightArm(AbstractClientPlayer clientPlayer) {
        float f = 1.0F;
        GlStateManager.color(f, f, f);
        ModelPlayer modelplayer = this.getMainModel();
        this.setModelVisibilities(clientPlayer);
        modelplayer.swingProgress = 0.0F;
        modelplayer.isSneak = false;
        modelplayer.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, clientPlayer);
        modelplayer.renderRightArm();
    }

    public void renderLeftArm(AbstractClientPlayer clientPlayer) {
        float f = 1.0F;
        GlStateManager.color(f, f, f);
        ModelPlayer modelplayer = this.getMainModel();
        this.setModelVisibilities(clientPlayer);
        modelplayer.isSneak = false;
        modelplayer.swingProgress = 0.0F;
        modelplayer.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, clientPlayer);
        modelplayer.renderLeftArm();
    }

    /**
     * Sets a simple glTranslate on a LivingEntity.
     */
    protected void renderLivingAt(AbstractClientPlayer entityLivingBaseIn, double x, double y, double z) {
        if (entityLivingBaseIn.isEntityAlive() && entityLivingBaseIn.isPlayerSleeping()) {
            super.renderLivingAt(entityLivingBaseIn, x + (double) entityLivingBaseIn.renderOffsetX, y + (double) entityLivingBaseIn.renderOffsetY, z + (double) entityLivingBaseIn.renderOffsetZ);
        } else {
            super.renderLivingAt(entityLivingBaseIn, x, y, z);
        }
    }

    protected void rotateCorpse(AbstractClientPlayer bat, float p_77043_2_, float p_77043_3_, float partialTicks) {
        if (bat.isEntityAlive() && bat.isPlayerSleeping()) {
            GlStateManager.rotate(bat.getBedOrientationInDegrees(), 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(this.getDeathMaxRotation(bat), 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(270.0F, 0.0F, 1.0F, 0.0F);
        } else {
            super.rotateCorpse(bat, p_77043_2_, p_77043_3_, partialTicks);
        }
    }
}
