package net.minecraft.client.applesskin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.List;

public final class AppleSkinTooltip extends Gui {

    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
    static final AppleSkinTooltip INSTANCE = new AppleSkinTooltip();

    private AppleSkinTooltip() {}

    /**
     * Appelé depuis ItemStack.getTooltip() — n'ajoute rien.
     * Les icônes sont rendues séparément via renderAfterTooltip().
     */
    public static void appendFoodInfo(ItemStack stack, List<String> lines) {
        // Volontairement vide : on affiche les icônes drumstick sous le tooltip, pas de texte
    }

    /**
     * Appelé depuis GuiScreen.renderToolTip() après drawHoveringText().
     * Affiche une rangée d'icônes drumstick (comme la barre de faim vanilla)
     * juste sous la boîte du tooltip, alignée à gauche.
     *
     * @param mc       instance Minecraft
     * @param stack    l'ItemStack survolé
     * @param mouseX   position souris X (paramètre reçu par renderToolTip)
     * @param mouseY   position souris Y
     * @param lines    liste des lignes du tooltip (après formatage couleur)
     * @param fr       FontRenderer pour calculer la largeur max du tooltip
     */
    public static void renderAfterTooltip(Minecraft mc, ItemStack stack,
                                          int mouseX, int mouseY,
                                          List<String> lines, FontRenderer fr) {
        if (!AppleSkinHelper.isFood(stack)) return;
        float[] vals = AppleSkinHelper.getFoodValues(stack);
        if (vals == null) return;
        int hunger = (int) vals[0];
        if (hunger <= 0) return;

        ScaledResolution res = new ScaledResolution(mc);
        int sw = res.getScaledWidth();
        int sh = res.getScaledHeight();

        // Réplication de la logique de positionnement de drawHoveringText
        int maxWidth = 0;
        for (String s : lines) {
            int w = fr.getStringWidth(s);
            if (w > maxWidth) maxWidth = w;
        }

        int tooltipX = mouseX + 12;
        int tooltipY = mouseY - 12;
        int boxHeight = 8;
        if (lines.size() > 1) {
            boxHeight += 2 + (lines.size() - 1) * 10;
        }

        if (tooltipX + maxWidth > sw) tooltipX -= 28 + maxWidth;
        if (tooltipY + boxHeight + 6 > sh) tooltipY = sh - boxHeight - 6;

        // Nombre de positions d'icônes drumstick nécessaires = ceil(hunger / 2), max 10
        int iconCount = Math.min((hunger + 1) / 2, 10);
        int iconsX = tooltipX;
        int iconsY = tooltipY + boxHeight + 5;  // juste sous la bordure basse du tooltip

        // Setup GL pour rendu 2D au premier plan (z=300 comme le tooltip)
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1f, 1f, 1f, 1f);
        mc.getTextureManager().bindTexture(ICONS);

        INSTANCE.zLevel = 300f;

        for (int k6 = 0; k6 < iconCount; k6++) {
            int x = iconsX + k6 * 8;
            // Fond (icône vide)
            INSTANCE.drawTexturedModalRect(x, iconsY, 16, 27, 9, 9);
            // Icône remplie
            if (k6 * 2 + 1 < hunger) {
                INSTANCE.drawTexturedModalRect(x, iconsY, 52, 27, 9, 9);   // drumstick plein
            } else if (k6 * 2 + 1 == hunger) {
                INSTANCE.drawTexturedModalRect(x, iconsY, 61, 27, 9, 9);   // demi-drumstick
            }
        }

        INSTANCE.zLevel = 0f;

        // Restauration du state GL
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.enableRescaleNormal();
    }
}
