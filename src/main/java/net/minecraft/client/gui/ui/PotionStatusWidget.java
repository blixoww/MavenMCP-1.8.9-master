package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.Potion;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class PotionStatusWidget extends BaseWidget {
    private static final ResourceLocation inventoryTex = new ResourceLocation("textures/gui/container/inventory.png");

    public PotionStatusWidget(String id, int x, int y) {
        super(id, x, y);
        this.width = 160;
        this.height = 12;
        // default props
        if (getPropOrDefault("showDuration", null) == null) setProp("showDuration", Boolean.TRUE);
        if (getPropOrDefault("showIcons", null) == null) setProp("showIcons", Boolean.FALSE);
    }

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        boolean showDuration = Boolean.TRUE.equals(getPropOrDefault("showDuration", Boolean.TRUE));
        boolean showIcons = Boolean.TRUE.equals(getPropOrDefault("showIcons", Boolean.FALSE));
        int line = 0;
        int maxW = 0;
        java.util.List<PotionEffect> effects = new java.util.ArrayList<>();
        try {
            if (mc.thePlayer.getActivePotionEffects() != null) {
                effects.addAll(mc.thePlayer.getActivePotionEffects());
            }
        } catch (Throwable t) {
            try {
                effects.addAll(java.util.Arrays.asList(mc.thePlayer.getActivePotionEffects().toArray(new PotionEffect[0])));
            } catch (Throwable t2) {
                // give up, leave list empty
            }
        }

        // Only show the fake preview when the HUD editor is visible.
        boolean isEditor = false;
        try {
            isEditor = UIManager.getInstance().isEditorActive();
        } catch (Throwable ignored) {}

        if (effects.isEmpty()) {
            if (isEditor) {
                // create sample effects for preview only
                try {
                    effects.add(new PotionEffect(Potion.moveSpeed.getId(), 1200, 0));
                    effects.add(new PotionEffect(Potion.regeneration.getId(), 600, 1));
                } catch (Throwable t) {
                    // fallback
                }
            } else {
                this.width = 0;
                this.height = 0;
                return;
            }
        }

        // Draw from (0,0) because BaseWidget handles translation
        int sX = 0;

        for (PotionEffect pe : effects) {
            String name = null;
            try {
                int pid = pe.getPotionID();
                Potion pot = (pid >= 0 && pid < Potion.potionTypes.length) ? Potion.potionTypes[pid] : null;
                if (pot != null) {
                    name = net.minecraft.util.StatCollector.translateToLocal(pot.getName());
                }
            } catch (Throwable t) { /* ignore */ }

            if (name == null) name = "Potion";

            int amplifier = pe.getAmplifier();
            if (amplifier > 0) {
                String[] romanNumerals = {"I","II","III","IV","V","VI","VII","VIII","IX","X"};
                String roman = (amplifier < romanNumerals.length) ? romanNumerals[amplifier] : String.valueOf(amplifier + 1);
                name = name + " " + roman;
            }

            String text = name;
            if (showDuration) {
                text = name + " (" + Potion.getDurationString(pe) + ")";
            }

            int drawY = line * 18;
            int lineW = mc.fontRendererObj.getStringWidth(text) + (showIcons ? 20 : 0);
            maxW = Math.max(maxW, lineW);

            if (showIcons) {
                Potion pot = Potion.potionTypes[pe.getPotionID()];
                int iconIdx = (pot != null) ? pot.getStatusIconIndex() : -1;

                if (iconIdx >= 0) {
                    mc.getTextureManager().bindTexture(inventoryTex);
                    int u = (iconIdx % 8) * 18;
                    int v = 198 + (iconIdx / 8) * 18;
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    mc.ingameGUI.drawTexturedModalRect(sX, drawY, u, v, 18, 18);
                    GlStateManager.disableBlend();
                    mc.fontRendererObj.drawStringWithShadow(text, sX + 20, drawY + 4, 0xFFFFFF);
                } else {
                    mc.fontRendererObj.drawStringWithShadow(text, sX, drawY + 4, 0xFFFFFF);
                }
            } else {
                mc.fontRendererObj.drawStringWithShadow(text, sX, drawY + 4, 0xFFFFFF);
            }
            line++;
        }
        
        // Update widget dimensions for selection hitbox
        this.width = Math.max(60, maxW + 6);
        this.height = Math.max(12, line * 18);
    }
}
