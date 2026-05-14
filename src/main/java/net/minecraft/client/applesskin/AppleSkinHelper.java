package net.minecraft.client.applesskin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.FoodStats;

public final class AppleSkinHelper {

    private AppleSkinHelper() {}

    public static boolean isFood(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemFood;
    }

    public static float[] getFoodValues(ItemStack stack) {
        if (!isFood(stack)) return null;
        ItemFood food = (ItemFood) stack.getItem();
        int hunger = food.getHealAmount(stack);
        float sat = food.getSaturationModifier(stack);
        return new float[] { hunger, sat };
    }

    public static float computeGainedSaturation(int hunger, float satMod) {
        return Math.min(hunger * satMod * 2.0F, 20.0F);
    }

    public static EntityPlayerSP getPlayer() {
        return Minecraft.getMinecraft().thePlayer;
    }

    public static FoodStats getFoodStats() {
        EntityPlayerSP p = getPlayer();
        return p == null ? null : p.getFoodStats();
    }
}
