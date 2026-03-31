package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CPSWidget extends BaseWidget {
    private final List<Long> clicks = new ArrayList<>();
    private boolean prevAttack = false;

    public CPSWidget(String id, int x, int y) {
        super(id, x, y);
        this.width = 60;
        this.height = 14;
        if (getPropOrDefault("initialized", null) == null) setProp("initialized", Boolean.FALSE);
        if (getPropOrDefault("showBackground", null) == null) setProp("showBackground", Boolean.FALSE);
    }

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        long now = System.currentTimeMillis();
        boolean attack = false;
        try {
            attack = mc.gameSettings.keyBindAttack.isKeyDown();
        } catch (Throwable t) {
            attack = false;
        }
        if (attack && !prevAttack) clicks.add(now);
        prevAttack = attack;
        Iterator<Long> it = clicks.iterator();
        while (it.hasNext()) {
            if (it.next() < now - 1000L) it.remove();
        }
        int cps = clicks.size();
        
        // Preview in editor
        if (cps == 0 && UIManager.getInstance().isEditorActive()) {
            cps = 7;
        }
        
        FontRenderer fr = mc.fontRendererObj;
        String s = "CPS: " + cps;
        if (!Boolean.TRUE.equals(getPropOrDefault("customSize", false))) {
            this.width = fr.getStringWidth(s) + 8;
            this.height = 14;
        }
        // compute color from widget props
        int storedColor = getColor();
        int storedAlpha = (storedColor >> 24) & 0xFF;
        int baseRgb = storedColor & 0x00FFFFFF;
        // background
        boolean showBg = Boolean.TRUE.equals(getPropOrDefault("showBackground", Boolean.FALSE));
        if (showBg) {
            int bgAlpha = Math.max(40, storedAlpha / 2);
            int bgCol = (bgAlpha << 24) | baseRgb;
            Gui.drawRect(0, 0, getWidth(), getHeight(), bgCol);
        }
        // text color: support rainbow if rgbMode
        int drawCol = (0xFF << 24) | baseRgb;
        if (isRGBMode()) {
            long t = System.currentTimeMillis();
            float hue = (t % 10000L) / 10000.0f;
            int c = java.awt.Color.HSBtoRGB(hue, 0.8f, 0.9f);
            drawCol = (storedColor & 0xFF000000) | (c & 0x00FFFFFF);
        }
        fr.drawStringWithShadow(s, 4, 3, drawCol);
    }
}
