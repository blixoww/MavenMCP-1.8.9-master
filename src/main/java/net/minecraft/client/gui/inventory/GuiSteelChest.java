package net.minecraft.client.gui.inventory;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.ContainerSteelChest;
import net.minecraft.tileentity.TileEntitySteelChest;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

/**
 * GUI du Coffre en Acier : utilise la texture generic_54 (6 rangées = double coffre).
 * xSize=176, ySize=222 (même que le double coffre vanilla).
 */
public class GuiSteelChest extends GuiContainer
{
    /** Texture identique au double coffre vanilla (6 lignes × 9 = 54 slots) */
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/gui/container/generic_54.png");

    private static final int CHEST_ROWS  = 6;
    private static final int PLAYER_ROWS = 3;

    private final TileEntitySteelChest steelChest;

    public GuiSteelChest(ContainerSteelChest container, TileEntitySteelChest chest)
    {
        super(container);
        this.steelChest  = chest;
        this.allowUserInput = false;
        // ySize = 6 rangées coffre (6×18=108) + séparateur (7) + 4 rangées joueur (4×18=72) + bordures (17+18)
        this.ySize = 114 + PLAYER_ROWS * 18 + 18 + 14; // = 222
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        // Titre du coffre (en haut)
        String title = this.steelChest.hasCustomName()
                ? this.steelChest.getName()
                : StatCollector.translateToLocal("container.steelChest");
        this.fontRendererObj.drawString(title, 8, 6, 0x404040);

        // Libellé inventaire joueur
        this.fontRendererObj.drawString(
                StatCollector.translateToLocal("container.inventory"),
                8, this.ySize - 96 + 2, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY)
    {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        int x = (this.width  - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        // Partie coffre (6 rangées = 6×18+17 = 125 px de haut)
        this.drawTexturedModalRect(x, y, 0, 0, this.xSize, CHEST_ROWS * 18 + 17);
        // Partie inventaire joueur (96 px)
        this.drawTexturedModalRect(x, y + CHEST_ROWS * 18 + 17, 0, 126, this.xSize, 96);
    }
}

