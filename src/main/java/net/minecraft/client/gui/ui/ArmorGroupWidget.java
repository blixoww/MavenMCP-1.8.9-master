package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.FontRenderer;

public class ArmorGroupWidget extends BaseWidget {

    /** Dimensions calculées lors du dernier draw() — stables entre les frames. */
    private int computedW = 50;
    private int computedH = 88;

    public ArmorGroupWidget(String id, int x, int y) {
        super(id, x, y);
        // Taille initiale stable (4 armures verticales × 20px + gap)
        this.width  = 50;
        this.height = 88;
        this.minWidth  = 40;
        this.minHeight = 20;
        if (getPropOrDefault("displayPercent", null) == null) setProp("displayPercent", Boolean.TRUE);
        if (getPropOrDefault("layout", null) == null) setProp("layout", "vertical");
    }

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;

        FontRenderer fr = mc.fontRendererObj;
        RenderItem ri = mc.getRenderItem();

        ItemStack[] armor = (mc.thePlayer != null) ? mc.thePlayer.inventory.armorInventory : new ItemStack[4];

        boolean vertical = "vertical".equals(String.valueOf(getPropOrDefault("layout", "vertical")));
        int itemGap = 2;
        boolean showPercent = Boolean.TRUE.equals(getPropOrDefault("displayPercent", Boolean.TRUE));

        boolean isEditor = false;
        try { isEditor = UIManager.getInstance().isEditorActive(); } catch (Throwable ignored) {}

        ItemStack[] displayArmor = new ItemStack[4];
        int present = 0;
        for (int i = 0; i < 4; i++) {
            if (armor != null && armor[i] != null) { displayArmor[i] = armor[i]; present++; }
        }

        if (present == 0 && isEditor) {
            try {
                displayArmor[3] = new ItemStack(net.minecraft.init.Items.diamond_helmet);
                displayArmor[2] = new ItemStack(net.minecraft.init.Items.diamond_chestplate);
                displayArmor[1] = new ItemStack(net.minecraft.init.Items.diamond_leggings);
                displayArmor[0] = new ItemStack(net.minecraft.init.Items.diamond_boots);
                present = 4;
            } catch (Throwable ignored) {}
        }

        if (present == 0) {
            // Rien à afficher — on conserve les dimensions stables calculées
            return;
        }

        int curX = 0, curY = 0;
        boolean any = false;

        for (int i = 3; i >= 0; i--) {
            ItemStack stack = displayArmor[i];
            if (stack == null) continue;
            any = true;

            if (vertical) {
                renderItemSafe(ri, mc, stack, 0, curY);
            } else {
                renderItemSafe(ri, mc, stack, curX, 0);
            }

            String txt = "";
            try {
                if (stack.isItemStackDamageable()) {
                    int max = stack.getMaxDamage();
                    int dmg = stack.getItemDamage();
                    int rem = max - dmg;
                    txt = showPercent ? ((int)(rem * 100.0F / max) + "%") : String.valueOf(rem);
                }
            } catch (Throwable ignored) {}

            int txtColor = getColor();
            if (vertical) {
                fr.drawStringWithShadow(txt, 18, curY + 4, txtColor);
                curY += 20 + itemGap;
            } else {
                fr.drawStringWithShadow(txt, curX + 18, 4, txtColor);
                curX += 40 + itemGap;
            }
        }

        if (!any) return;

        // Calculer les nouvelles dimensions SANS modifier this.width/this.height directement
        // pour ne pas fausser relX/relY. On les stocke dans computedW/H et on les applique
        // seulement si la taille a réellement changé (stabilité).
        int newW, newH;
        if (vertical) {
            newW = 50;
            newH = Math.max(20, curY - itemGap);
        } else {
            newW = Math.max(40, curX - itemGap);
            newH = 20;
        }

        // N'appliquer le changement de taille que si nécessaire
        if (newW != computedW || newH != computedH) {
            computedW = newW;
            computedH = newH;
            // Mettre à jour les dimensions internes et recalculer relX/relY
            this.width  = newW;
            this.height = newH;
            // Recalculer les coordonnées relatives pour conserver la position à l'écran
            updateRelativeFromAbsolute();
        }
    }

    /**
     * Recalcule relX/relY depuis la position absolue actuelle et la résolution d'écran.
     * Appelé quand les dimensions changent pour éviter une dérive de position.
     */
    private void updateRelativeFromAbsolute() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        try {
            net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(mc);
            int sw = sr.getScaledWidth();
            int sh = sr.getScaledHeight();
            if (sw > 0 && sh > 0 && this.x >= 0 && this.y >= 0) {
                int maxX = Math.max(1, sw - this.getWidth());
                int maxY = Math.max(1, sh - this.getHeight());
                this.relX = Math.max(0.0, Math.min(1.0, (double) this.x / maxX));
                this.relY = Math.max(0.0, Math.min(1.0, (double) this.y / maxY));
                this.refW = sw;
                this.refH = sh;
            }
        } catch (Throwable ignored) {}
    }
}


