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
        // Le score belowName (slot 2) est remplacé par la barre de vie custom — on l'ignore.

        super.renderOffsetLivingLabel(entityIn, x, y, z, str, p_177069_9_, p_177069_10_);

        // ── Barre de vie (distance max 32 blocs) ──────────────────────────────────
        if (p_177069_10_ < (32.0D * 32.0D)) {
            this.renderHealthBar(entityIn, x, y, z, p_177069_9_);
        }
    }

    /**
     * Affiche une barre de vie stylisée sous le pseudo du joueur.
     * Dégradé vert → orange → rouge selon la vie restante.
     * Bordure fine, fond sombre, texte HP centré.
     */
    private void renderHealthBar(AbstractClientPlayer entity, double x, double y, double z, float scale) {
        float maxHealth = entity.getMaxHealth();
        float currentHealth = entity.getHealth();
        if (maxHealth <= 0) return;
        float ratio = Math.max(0f, Math.min(1f, currentHealth / maxHealth));

        // ── Couleur dégradée selon le ratio ──────────────────────────────────────
        float r, g, b;
        if (ratio > 0.6f) {
            // vert vif → vert-jaune  (1.0 → 0.6)
            float t = (ratio - 0.6f) / 0.4f;          // 1 quand plein, 0 à 60%
            r = 1f - t * 0.6f;   // 0.4 → 1.0
            g = 0.85f;
            b = 0f;
        } else if (ratio > 0.3f) {
            // jaune → orange  (0.6 → 0.3)
            float t = (ratio - 0.3f) / 0.3f;
            r = 1f;
            g = 0.5f * t + 0.15f; // 0.65 → 0.15
            b = 0f;
        } else {
            // orange-rouge → rouge vif  (0.3 → 0.0)
            float t = ratio / 0.3f;
            r = 1f;
            g = 0.15f * t;
            b = 0f;
        }

        // ── Dimensions ───────────────────────────────────────────────────────────
        final float BAR_W     = 50f;   // largeur totale
        final float BAR_H     = 3.5f;  // hauteur barre principale
        final float BORDER    = 0.8f;  // épaisseur de la bordure
        final float CORNER    = 1.2f;  // taille coins (simulation arrondi)
        final float half      = BAR_W / 2f;

        // Décalage vertical en dessous du pseudo
        float fontH    = (float) this.getFontRendererFromRenderManager().FONT_HEIGHT;
        float yOffset  = fontH * 1.15f * scale + (BAR_H + BORDER * 2 + 4f) * scale;

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y + entity.height + 0.5f, (float) z);
        GL11.glNormal3f(0f, 1f, 0f);
        GlStateManager.rotate(-this.renderManager.playerViewY,  0f, 1f, 0f);
        GlStateManager.rotate( this.renderManager.playerViewX, 1f, 0f, 0f);
        GlStateManager.scale(-scale, -scale, scale);
        GlStateManager.translate(0f, -yOffset / scale, 0f);

        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        // ── 1. Ombre portée (décalage +1 pixel en bas) ───────────────────────────
        float sx = BORDER + CORNER, sy = -(BAR_H + BORDER * 2);
        float shadowAlpha = 0.35f;
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(-half + sx + 1,            1f, 0).color(0f,0f,0f, shadowAlpha).endVertex();
        wr.pos(-half + sx + 1,   sy - 1f,    0).color(0f,0f,0f, shadowAlpha).endVertex();
        wr.pos( half - sx + 1,   sy - 1f,    0).color(0f,0f,0f, shadowAlpha).endVertex();
        wr.pos( half - sx + 1,            1f, 0).color(0f,0f,0f, shadowAlpha).endVertex();
        tess.draw();

        // ── 2. Fond global (noir semi-transparent) ────────────────────────────────
        float by = -(BAR_H + BORDER * 2);
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        // centre
        wr.pos(-half + CORNER, BORDER,    0).color(0.05f,0.05f,0.05f,0.85f).endVertex();
        wr.pos(-half + CORNER, by,        0).color(0.05f,0.05f,0.05f,0.85f).endVertex();
        wr.pos( half - CORNER, by,        0).color(0.05f,0.05f,0.05f,0.85f).endVertex();
        wr.pos( half - CORNER, BORDER,    0).color(0.05f,0.05f,0.05f,0.85f).endVertex();
        tess.draw();
        // côté gauche (sans les coins)
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(-half,          BORDER - CORNER, 0).color(0.05f,0.05f,0.05f,0.85f).endVertex();
        wr.pos(-half,          by + CORNER,     0).color(0.05f,0.05f,0.05f,0.85f).endVertex();
        wr.pos(-half + CORNER, by + CORNER,     0).color(0.05f,0.05f,0.05f,0.85f).endVertex();
        wr.pos(-half + CORNER, BORDER - CORNER, 0).color(0.05f,0.05f,0.05f,0.85f).endVertex();
        tess.draw();
        // côté droit
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(half - CORNER, BORDER - CORNER, 0).color(0.05f,0.05f,0.05f,0.85f).endVertex();
        wr.pos(half - CORNER, by + CORNER,     0).color(0.05f,0.05f,0.05f,0.85f).endVertex();
        wr.pos(half,          by + CORNER,     0).color(0.05f,0.05f,0.05f,0.85f).endVertex();
        wr.pos(half,          BORDER - CORNER, 0).color(0.05f,0.05f,0.05f,0.85f).endVertex();
        tess.draw();

        // ── 3. Bordure lumineuse (blanc 20%) ──────────────────────────────────────
        // top
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(-half + CORNER, BORDER, 0).color(1f,1f,1f,0.18f).endVertex();
        wr.pos(-half + CORNER, 0f,     0).color(1f,1f,1f,0.18f).endVertex();
        wr.pos( half - CORNER, 0f,     0).color(1f,1f,1f,0.18f).endVertex();
        wr.pos( half - CORNER, BORDER, 0).color(1f,1f,1f,0.18f).endVertex();
        tess.draw();
        // bottom
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        wr.pos(-half + CORNER, by,          0).color(0f,0f,0f,0.3f).endVertex();
        wr.pos(-half + CORNER, by - BORDER, 0).color(0f,0f,0f,0.3f).endVertex();
        wr.pos( half - CORNER, by - BORDER, 0).color(0f,0f,0f,0.3f).endVertex();
        wr.pos( half - CORNER, by,          0).color(0f,0f,0f,0.3f).endVertex();
        tess.draw();

        // ── 4. Barre colorée remplie (avec dégradé vertical) ─────────────────────
        float fillRight = -half + BORDER + (BAR_W - BORDER * 2) * ratio;
        float barBot    =  -(BAR_H + BORDER);
        float barTop    =  -BORDER;
        if (ratio > 0f) {
            // dégradé vertical : couleur claire en haut, plus sombre en bas
            float rD = r * 0.65f, gD = g * 0.65f, bD = b * 0.65f;
            wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
            wr.pos(-half + BORDER, BORDER, 0).color(r,  g,  b,  0.95f).endVertex();
            wr.pos(-half + BORDER, barBot, 0).color(rD, gD, bD, 0.95f).endVertex();
            wr.pos(fillRight,      barBot, 0).color(rD, gD, bD, 0.95f).endVertex();
            wr.pos(fillRight,      BORDER, 0).color(r,  g,  b,  0.95f).endVertex();
            tess.draw();

            // Reflet brillant sur le tiers supérieur
            float midY = BORDER - (BORDER - barBot) * 0.35f;
            wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
            wr.pos(-half + BORDER, BORDER, 0).color(1f, 1f, 1f, 0.12f).endVertex();
            wr.pos(-half + BORDER, midY,   0).color(1f, 1f, 1f, 0.0f ).endVertex();
            wr.pos(fillRight,      midY,   0).color(1f, 1f, 1f, 0.0f ).endVertex();
            wr.pos(fillRight,      BORDER, 0).color(1f, 1f, 1f, 0.12f).endVertex();
            tess.draw();
        }

        // ── 5. Zone vide (gris anthracite) ────────────────────────────────────────
        if (fillRight < half - BORDER) {
            wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
            wr.pos(fillRight,      barTop, 0).color(0.18f,0.18f,0.18f,0.80f).endVertex();
            wr.pos(fillRight,      barBot, 0).color(0.10f,0.10f,0.10f,0.80f).endVertex();
            wr.pos(half - BORDER,  barBot, 0).color(0.10f,0.10f,0.10f,0.80f).endVertex();
            wr.pos(half - BORDER,  barTop, 0).color(0.18f,0.18f,0.18f,0.80f).endVertex();
            tess.draw();
        }

        GlStateManager.enableTexture2D();

        // ── 6. Texte "HP actuels / max HP" centré sous la barre ──────────────────
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (mc != null && mc.fontRendererObj != null) {
            int hp    = (int) Math.ceil(currentHealth);
            int maxHp = (int) maxHealth;
            String hpText = hp + " / " + maxHp;

            // Couleur texte selon ratio
            int textColor;
            if (ratio > 0.6f)      textColor = 0x55FF55; // vert
            else if (ratio > 0.3f) textColor = 0xFFAA00; // orange
            else                   textColor = 0xFF4444; // rouge

            float textScale = 0.6f;
            GlStateManager.pushMatrix();
            GlStateManager.translate(0f, barBot - 1.5f - fontH * textScale, 0f);
            GlStateManager.scale(textScale, textScale, 1f);
            int textW = mc.fontRendererObj.getStringWidth(hpText);
            mc.fontRendererObj.drawStringWithShadow(hpText, -textW / 2f, 0f, textColor);
            GlStateManager.popMatrix();
        }

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
