package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.settings.GameSettings;

public class ToggleSneakWidget extends BaseWidget {
    public ToggleSneakWidget(String id, int x, int y) {
        super(id, x, y);
        this.width = 72;
        this.height = 14;
        setColor(0xFF44EE77);
    }

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        GameSettings gs = mc.gameSettings;
        FontRenderer fr = mc.fontRendererObj;

        // toggleSneakEnabled = la fonctionnalité est activée dans les options
        boolean enabled = gs.toggleSneakEnabled;
        // isToggleSneakActive = actuellement en sneak forcé (seulement pertinent si enabled)
        boolean sneaking = enabled && gs.isToggleSneakActive;

        // Fond : vert si fonctionnalité activée, gris sinon
        int bg = enabled ? (sneaking ? 0x9900AA44 : 0x99115511) : 0x99333333;
        Gui.drawRect(this.x, this.y, this.x + this.width, this.y + this.height, bg);
        // Bordure
        int border = enabled ? 0xFF22CC66 : 0xFF555555;
        Gui.drawRect(this.x, this.y, this.x + this.width, this.y + 1, border);
        Gui.drawRect(this.x, this.y, this.x + 1, this.y + this.height, border);

        // Texte : "Sneak ON" si fonctionnalité active (+ indicateur si actuellement en sneak)
        String status = enabled ? (sneaking ? "\u00a7aSNEAK" : "\u00a7aON") : "\u00a7cOFF";
        int color = enabled ? getColor() : 0xFF888888;
        fr.drawStringWithShadow("Sneak " + status, this.x + 3, this.y + 3, color);
    }
}
