package net.minecraft.client.gui.inventory;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.achievement.GuiAchievements;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.GuiProfil;
import net.minecraft.client.gui.GuiWiki;

public class GuiInventory extends InventoryEffectRenderer {
    /**
     * The old x position of the mouse pointer
     */
    private float oldMouseX;

    /**
     * The old y position of the mouse pointer
     */
    private float oldMouseY;

    public GuiInventory(EntityPlayer p_i1094_1_) {
        super(p_i1094_1_.inventoryContainer);
        this.allowUserInput = true;
    }

    /**
     * Called from the main game loop to update the screen.
     */
    public void updateScreen() {
        if (this.mc.playerController.isInCreativeMode()) {
            this.mc.displayGuiScreen(new GuiContainerCreative(this.mc.thePlayer));
        }

        this.updateActivePotionEffects();
    }

    /** Active/désactive l'affichage des 3 boutons dans l'inventaire. */
    public static boolean showInventoryButtons = true;

    /** Référence au bouton livre pour repositionnement dynamique. */
    private GuiButtonBook bookButton = null;
    /** Bouton profil à gauche du bouton livre. */
    private GuiButtonProfil profilButton = null;
    /** Bouton wiki entre profil et livre. */
    private GuiButtonWiki wikiButton = null;

    /**
     * Adds the buttons (and other controls) to the screen in question. Called when the GUI is displayed and when the
     * window resizes, the buttonList is cleared beforehand.
     */
    public void initGui() {
        this.buttonList.clear();
        this.bookButton = null;
        this.profilButton = null;
        this.wikiButton = null;

        if (this.mc.playerController.isInCreativeMode()) {
            this.mc.displayGuiScreen(new GuiContainerCreative(this.mc.thePlayer));
        } else {
            super.initGui();
            if (showInventoryButtons) {
                // Boutons positionnés juste SOUS la table de craft (2x2 grid y=18-54),
                // centrés sur la zone crafting (x=86-172, centre=129).
                // 3 boutons × 18px + 2 gaps × 2px = 58px → start = 129 - 29 = 100
                //   wiki   : x+100  (livre enchante rouge)
                //   profil : x+120  (tête de joueur)
                //   guide  : x+140  (livre bleu)
                // y = 57 : 3px en dessous du bas de la grille de craft (y=54)
                wikiButton   = new GuiButtonWiki  (4, this.guiLeft + 108, this.guiTop + 62);
                profilButton = new GuiButtonProfil(3, this.guiLeft + 128, this.guiTop + 62);
                bookButton   = new GuiButtonBook  (2, this.guiLeft + 148, this.guiTop + 62);
                this.buttonList.add(wikiButton);
                this.buttonList.add(profilButton);
                this.buttonList.add(bookButton);
            }
        }
    }

    /**
     * Mise à jour de guiLeft quand les effets de potion apparaissent/disparaissent.
     * On repositionne aussi le bouton livre pour qu'il reste ancré à l'inventaire.
     */
    @Override
    protected void updateActivePotionEffects() {
        super.updateActivePotionEffects();
        if (bookButton != null) {
            bookButton.xPosition  = this.guiLeft + 148;
            bookButton.yPosition  = this.guiTop  + 62;
        }
        if (wikiButton != null) {
            wikiButton.xPosition  = this.guiLeft + 108;
            wikiButton.yPosition  = this.guiTop  + 62;
        }
        if (profilButton != null) {
            profilButton.xPosition = this.guiLeft + 128;
            profilButton.yPosition = this.guiTop  + 62;
        }
    }

    /**
     * Draw the foreground layer for the GuiContainer (everything in front of the items). Args : mouseX, mouseY
     */
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRendererObj.drawString(I18n.format("container.crafting"), 86, 16, 4210752);
    }

    /**
     * Draws the screen and all the components in it. Args : mouseX, mouseY, renderPartialTicks
     */
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.oldMouseX = (float) mouseX;
        this.oldMouseY = (float) mouseY;
    }

    /**
     * Args : renderPartialTicks, mouseX, mouseY
     */
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(inventoryBackground);
        int i = this.guiLeft;
        int j = this.guiTop;
        this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.ySize);
        drawEntityOnScreen(i + 51, j + 75, 30, (float) (i + 51) - this.oldMouseX, (float) (j + 75 - 50) - this.oldMouseY, this.mc.thePlayer);
    }

    /**
     * Draws the entity to the screen. Args: xPos, yPos, scale, mouseX, mouseY, entityLiving
     */
    public static void drawEntityOnScreen(int posX, int posY, int scale, float mouseX, float mouseY, EntityLivingBase ent) {
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) posX, (float) posY, 50.0F);
        // Scale X négatif + rotation 180° Z = combinaison vanilla exacte pour afficher le joueur face à la caméra
        GlStateManager.scale((float) (-scale), (float) scale, (float) scale);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        float f = ent.renderYawOffset;
        float f1 = ent.rotationYaw;
        float f2 = ent.rotationPitch;
        float f3 = ent.prevRotationYawHead;
        float f4 = ent.rotationYawHead;
        GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-((float) Math.atan(mouseY / 40.0F)) * 20.0F, 1.0F, 0.0F, 0.0F);
        ent.renderYawOffset = (float) Math.atan(mouseX / 40.0F) * 20.0F;
        ent.rotationYaw = (float) Math.atan(mouseX / 40.0F) * 40.0F;
        ent.rotationPitch = -((float) Math.atan(mouseY / 40.0F)) * 20.0F;
        ent.rotationYawHead = ent.rotationYaw;
        ent.prevRotationYawHead = ent.rotationYaw;
        GlStateManager.translate(0.0F, 0.0F, 0.0F);
        RenderManager rendermanager = Minecraft.getMinecraft().getRenderManager();
        rendermanager.setPlayerViewY(180.0F);
        rendermanager.setRenderShadow(false);
        // Désactiver le blending et activer le depth pour éviter la transparence parasite
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        rendermanager.renderEntityWithPosYaw(ent, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
        rendermanager.setRenderShadow(true);
        ent.renderYawOffset = f;
        ent.rotationYaw = f1;
        ent.rotationPitch = f2;
        ent.prevRotationYawHead = f3;
        ent.rotationYawHead = f4;
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    /**
     * Called by the controls from the buttonList when activated. (Mouse pressed for buttons)
     */
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            this.mc.displayGuiScreen(new GuiAchievements(this, this.mc.thePlayer.getStatFileWriter()));
        }

        if (button.id == 1) {
            this.mc.displayGuiScreen(new GuiStats(this, this.mc.thePlayer.getStatFileWriter()));
        }
        super.actionPerformed(button);
        if (button.id == 2) {
            // Ouvrir l'interface du guide de craft
            this.mc.displayGuiScreen(new GuiCraftGuide(this));
        }
        if (button.id == 4) {
            // Ouvrir le Wiki
            this.mc.displayGuiScreen(new GuiWiki(this));
        }
        if (button.id == 3) {
            // Demande au serveur des données fraîches (kills, morts, killstreak,
            // bounty, temps de jeu...). Le GUI ouvert en local lit en live le
            // cache, donc dès que la réponse arrive l'affichage se met à jour.
            net.minecraft.client.custompackets.handler.PlayerDataHandler.requestProfile();
            net.minecraft.client.custompackets.handler.PlayerDataHandler.requestData();
            this.mc.displayGuiScreen(GuiProfil.forSelf());
        }
    }

    // Style commun pour les petits boutons d'outils dans l'inventaire
    private static abstract class GuiIconButtonBase extends GuiButton {
        protected GuiIconButtonBase(int buttonId, int x, int y) {
            super(buttonId, x, y, 18, 18, "");
        }

        protected abstract ItemStack getIconStack();
        protected abstract String getTooltip();
        protected int getAccentColor() { return 0xFF3D8EFF; }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!this.visible) return;

            boolean hovered = mouseX >= xPosition && mouseX < xPosition + width
                    && mouseY >= yPosition && mouseY < yPosition + height;

            int accent = getAccentColor();
            float pulse = hovered ? (float)(Math.sin(System.currentTimeMillis() / 320.0) * 0.5 + 0.5) : 0f;

            // Fond dégradé du haut (plus clair) vers le bas (plus sombre) - en blanc
            int bgTop    = hovered ? 0xEEFFFFFF : 0xCCF0F0F0;
            int bgBottom = hovered ? 0xEEF0F0F0 : 0xCCE0E0E0;
            net.minecraft.client.gui.GuiRenderUtils.drawGradientRect(
                    xPosition, yPosition, xPosition + width, yPosition + height, bgTop, bgBottom);

            // Reflet subtil en haut
            drawRect(xPosition + 1, yPosition + 1, xPosition + width - 1, yPosition + 3,
                    hovered ? 0x28FFFFFF : 0x12FFFFFF);

            // Contour plein : accent animé sur hover, discret sinon
            int borderColor;
            if (hovered) {
                int baseAlpha = (int)(0x88 + 0x44 * pulse);
                borderColor = (baseAlpha << 24) | (accent & 0x00FFFFFF);
            } else {
                borderColor = 0x55253444;
            }
            net.minecraft.client.gui.GuiRenderUtils.drawRectOutline(
                    xPosition, yPosition, width, height, borderColor);

            // Ligne d'accent en bas uniquement au survol (soulignement)
            if (hovered) {
                int accentAlpha = (int)(0x99 + 0x55 * pulse);
                int accentLine  = (accentAlpha << 24) | (accent & 0x00FFFFFF);
                drawRect(xPosition + 1, yPosition + height - 1,
                        xPosition + width - 1, yPosition + height, accentLine);
            }

            // Halo externe léger au survol
            if (hovered) {
                int glowAlpha = (int)(0x18 + 0x18 * pulse);
                net.minecraft.client.gui.GuiRenderUtils.drawRectOutline(
                        xPosition - 1, yPosition - 1, width + 2, height + 2,
                        (glowAlpha << 24) | (accent & 0x00FFFFFF));
            }

            // Rendu de l'icône item
            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.enableDepth();
            mc.getRenderItem().renderItemAndEffectIntoGUI(getIconStack(), xPosition + 1, yPosition + 1);
            GlStateManager.disableDepth();
            RenderHelper.disableStandardItemLighting();

            // Tooltip amélioro
            if (hovered) {
                net.minecraft.client.gui.FontRenderer fr = mc.fontRendererObj;
                String tooltip = getTooltip();
                int tw = fr.getStringWidth(tooltip);
                // Repositionner pour ne pas déborder à droite
                int tx = xPosition + width / 2 - tw / 2;
                int ty = yPosition + height + 4;
                // Fond + outline du tooltip
                drawRect(tx - 4, ty - 2, tx + tw + 4, ty + 10, 0xF0060B16);
                net.minecraft.client.gui.GuiRenderUtils.drawRectOutline(
                        tx - 4, ty - 2, tw + 8, 12, 0x33FFFFFF);
                // Ligne d'accent au-dessus du texte
                int lineAlpha = (int)(0xBB + 0x33 * pulse);
                drawRect(tx - 4, ty - 2, tx + tw + 4, ty - 1,
                        (lineAlpha << 24) | (accent & 0x00FFFFFF));
                fr.drawStringWithShadow("§f" + tooltip, tx, ty, 0xFFFFFFFF);
            }
        }
    }

    // Bouton livre : guide craft
    public static class GuiButtonBook extends GuiIconButtonBase {
        private static final ItemStack BOOK = new ItemStack(Items.writable_book);
        public GuiButtonBook(int buttonId, int x, int y) { super(buttonId, x, y); }
        @Override protected ItemStack getIconStack() { return BOOK; }
        @Override protected String getTooltip() { return "Guide"; }
        @Override protected int getAccentColor() { return 0xFF3D8EFF; }
    }

    // Bouton wiki : livre enchante (rouge) pour le distinguer du Guide Craft
    public static class GuiButtonWiki extends GuiIconButtonBase {
        private static final ItemStack WIKI = new ItemStack(Items.enchanted_book);
        public GuiButtonWiki(int buttonId, int x, int y) { super(buttonId, x, y); }
        @Override protected ItemStack getIconStack() { return WIKI; }
        @Override protected String getTooltip() { return "Wiki du serveur"; }
        @Override protected int getAccentColor() { return 0xFFE02828; }
    }

    // Bouton profil : tête de joueur (skull de type 3)
    public static class GuiButtonProfil extends GuiIconButtonBase {
        private static final ItemStack SKULL = new ItemStack(Items.skull, 1, 3);
        public GuiButtonProfil(int buttonId, int x, int y) { super(buttonId, x, y); }
        @Override protected ItemStack getIconStack() { return SKULL; }
        @Override protected String getTooltip() { return "Profil"; }
        @Override protected int getAccentColor() { return 0xFFE8A030; }
    }
}
