package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.Potion;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.gui.ScaledResolution;

public class PotionStatusWidget extends BaseWidget {
    private static final ResourceLocation inventoryTex = new ResourceLocation("textures/gui/container/inventory.png");

    public PotionStatusWidget(String id, int x, int y) {
        super(id, x, y);
        this.width = 160;
        this.height = 12;
        // default props
        if (getPropOrDefault("showDuration", null) == null) setProp("showDuration", Boolean.TRUE);
        if (getPropOrDefault("showIcons", null) == null) setProp("showIcons", Boolean.FALSE);
        // editorPreview allows moving the widget even without active effects when in UI editor
        if (getPropOrDefault("editorPreview", null) == null) setProp("editorPreview", Boolean.FALSE);
        // Clean up legacy 'preview' prop stored in older configs to avoid persistent preview outside editor
        try {
            if (this.props.containsKey("preview")) {
                // remove legacy key so it won't trigger preview outside editor
                this.props.remove("preview");
            }
        } catch (Throwable ignored) {
        }
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

        // Only show the fake preview when the HUD editor is visible (or when the widget explicitly asks for it).
        boolean editorPreview = Boolean.TRUE.equals(getPropOrDefault("editorPreview", Boolean.FALSE));
        try {
            Minecraft game = Minecraft.getMinecraft();
            boolean editorScreen = game != null && game.currentScreen instanceof net.minecraft.client.gui.GuiUIEditor;
            if (!editorPreview && editorScreen) {
                editorPreview = net.minecraft.client.gui.ui.UIManager.getInstance().isEditorActive();
            }
            if (editorPreview && !editorScreen) {
                // safety: never show preview outside the editor
                editorPreview = false;
            }
        } catch (Throwable ignored) {}

        if (effects.isEmpty()) {
            if (editorPreview) {
                // create a sample effect only when editing positions AND player has no active effects
                try {
                    int speedId = Potion.moveSpeed.getId();
                    effects.add(new PotionEffect(speedId, 1200, 0)); // 1 minute sample for preview only
                } catch (Throwable t) {
                    for (int i = 0; i < Potion.potionTypes.length; i++) {
                        if (Potion.potionTypes[i] != null) {
                            effects.add(new PotionEffect(i, 1200, 0));
                            break;
                        }
                    }
                }
                if (effects.isEmpty()) {
                    this.width = 0;
                    this.height = 0;
                    return;
                }
            } else {
                this.width = 0;
                this.height = 0;
                return; // hide when no effects and not in editor preview
            }
        }

        // lazy init default position: bottom-left
        if (!Boolean.TRUE.equals(getPropOrDefault("initialized", Boolean.FALSE))) {
            ScaledResolution sr = new ScaledResolution(mc);
            int sh = sr.getScaledHeight();
            this.x = 10;
            this.y = Math.max(20, sh - 80);
            setProp("initialized", Boolean.TRUE);
            try {
                net.minecraft.client.gui.ui.UIManager.getInstance().saveConfig();
            } catch (Throwable t) {
                System.err.println("Failed to save UI init: " + t.getMessage());
            }
        }

        for (PotionEffect pe : effects) {
            // get localized potion name via Potion class if possible
            String name = null;
            try {
                int pid = -1;
                try {
                    pid = pe.getPotionID();
                } catch (Throwable tx) {
                    pid = -1;
                }
                Potion pot = null;
                if (pid >= 0 && pid < Potion.potionTypes.length) pot = Potion.potionTypes[pid];
                if (pot != null) {
                    String pname = pot.getName();
                    try {
                        name = net.minecraft.util.StatCollector.translateToLocal(pname);
                    } catch (Throwable tt) {
                        name = pname;
                    }
                }
            } catch (Throwable t) { /* ignore */ }

            if (name == null) name = "Potion";

            String text = name;
            if (showDuration) {
                try {
                    String dur = Potion.getDurationString(pe);
                    text = name + " (" + dur + ")";
                } catch (Throwable t) {
                    text = name;
                }
            }

            int drawY = this.y + line * 18;
            int lineW = mc.fontRendererObj.getStringWidth(text) + (showIcons ? 20 : 0);
            maxW = Math.max(maxW, lineW);

            if (showIcons) {
                int pid = -1;
                try {
                    pid = pe.getPotionID();
                } catch (Throwable t) {
                    pid = -1;
                }
                Potion pot = null;
                if (pid >= 0 && pid < Potion.potionTypes.length) pot = Potion.potionTypes[pid];

                int iconIdx = -1;
                if (pot != null) {
                    try {
                        iconIdx = pot.getStatusIconIndex();
                    } catch (Throwable t) {
                        iconIdx = -1;
                    }
                }

                if (iconIdx >= 0) {
                    mc.getTextureManager().bindTexture(inventoryTex);
                    int u = (iconIdx % 8) * 18;
                    int v = 198 + (iconIdx / 8) * 18;
                    GlStateManager.enableBlend();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    // draw icon
                    mc.ingameGUI.drawTexturedModalRect(this.x, drawY, u, v, 18, 18);
                    GlStateManager.disableBlend();
                    // draw text next to icon
                    mc.fontRendererObj.drawStringWithShadow(text, this.x + 20, drawY + 4, 0xFFFFFF);
                } else {
                    // fallback: draw potion name (no icon)
                    int col = getColor();
                    int colRgb = col & 0x00FFFFFF;
                    if (colRgb == 0) colRgb = 0x00FFFFFF;
                    mc.fontRendererObj.drawStringWithShadow(text, this.x, drawY + 4, colRgb);
                }
            } else {
                int col = getColor();
                int colRgb = col & 0x00FFFFFF;
                if (colRgb == 0) colRgb = 0x00FFFFFF;
                mc.fontRendererObj.drawStringWithShadow(text, this.x, drawY + 4, colRgb);
            }
            line++;
        }
        // autosize widget according to contents
        this.width = Math.max(60, maxW + 6);
        this.height = Math.max(12, line * 18);
    }
}
