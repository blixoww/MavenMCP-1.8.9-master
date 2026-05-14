package net.minecraft.client.applesskin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.FoodStats;
import net.minecraft.util.ResourceLocation;

public final class AppleSkinHud extends Gui {

    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
    private static final AppleSkinHud INSTANCE = new AppleSkinHud();

    public static AppleSkinHud get() { return INSTANCE; }

    public void render() {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;
        if (player == null || mc.playerController == null) return;
        if (!mc.playerController.gameIsSurvivalOrAdventure()) return;
        if (player.capabilities.isCreativeMode) return;

        FoodStats fs = player.getFoodStats();
        if (fs == null) return;

        ScaledResolution res = new ScaledResolution(mc);
        int sw = res.getScaledWidth();
        int sh = res.getScaledHeight();

        int right = sw / 2 + 91;
        int top   = sh - 39;

        int foodLevel    = fs.getFoodLevel();

        // Affichage uniquement quand on tient de la nourriture
        ItemStack held = player.inventory.getCurrentItem();
        if (!AppleSkinHelper.isFood(held)) return;

        float[] vals = AppleSkinHelper.getFoodValues(held);
        if (vals == null) return;

        int gainedHunger = (int) vals[0];
        int previewFood  = Math.min(foodLevel + gainedHunger, 20);

        // Pulse sinusoïdal discret (0.35 → 0.75)
        float pulse = 0.35f + 0.40f * (float) Math.abs(Math.sin(System.currentTimeMillis() / 350.0));

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // 1) Icônes fantômes pour la faim gagnée (couleurs normales, juste semi-transparentes)
        if (previewFood > foodLevel) {
            mc.getTextureManager().bindTexture(ICONS);
            drawHungerPreview(right, top, foodLevel, previewFood, pulse);
        }


        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /**
     * Drumstick fantômes semi-transparents pour les positions qui seront remplies.
     * N'écrase PAS les icônes existantes — ne touche qu'aux positions vides.
     */
    private void drawHungerPreview(int right, int top, int currentFood, int previewFood, float alpha) {
        GlStateManager.color(1f, 1f, 1f, alpha);
        for (int k6 = 0; k6 < 10; k6++) {
            if (k6 * 2 + 2 <= currentFood) continue;   // déjà pleinement rempli
            if (k6 * 2 + 1 > previewFood)  continue;   // pas rempli en preview non plus

            int x = right - k6 * 8 - 9;
            this.drawTexturedModalRect(x, top, 16, 27, 9, 9);  // fond vide vanilla

            if (k6 * 2 + 1 < previewFood) {
                this.drawTexturedModalRect(x, top, 52, 27, 9, 9);  // plein
            } else {
                this.drawTexturedModalRect(x, top, 61, 27, 9, 9);  // demi
            }
        }
        GlStateManager.color(1f, 1f, 1f, 1f);
    }
}
