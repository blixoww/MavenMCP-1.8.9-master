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
        if (getPropOrDefault("dynamicColor", null) == null) setProp("dynamicColor", Boolean.TRUE);
    }

    @Override public boolean supportsLabelColor() { return true; }

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        long now = System.currentTimeMillis();
        boolean attack = false;
        try { attack = mc.gameSettings.keyBindAttack.isKeyDown(); } catch (Throwable t) {}
        if (attack && !prevAttack) clicks.add(now);
        prevAttack = attack;
        Iterator<Long> it = clicks.iterator();
        while (it.hasNext()) { if (it.next() < now - 1000L) it.remove(); }
        int cps = clicks.size();
        if (cps == 0 && UIManager.getInstance().isEditorActive()) cps = 7;

        FontRenderer fr = mc.fontRendererObj;
        String value = String.valueOf(cps);
        if (!Boolean.TRUE.equals(getPropOrDefault("customSize", false))) {
            this.width = fr.getStringWidth("CPS: " + value) + 8;
            this.height = 14;
        }

        int drawCol;
        if (Boolean.TRUE.equals(getPropOrDefault("dynamicColor", Boolean.FALSE))) {
            if      (cps < 5)  drawCol = 0xFFFFFFFF;
            else if (cps < 7)  drawCol = 0xFFFFFF55;
            else if (cps < 10) drawCol = 0xFFFFAA00;
            else if (cps < 15) drawCol = 0xFFFF5555;
            else               drawCol = 0xFFAA00AA;
        } else {
            drawCol = getColor();
        }
        int baseRgb = drawCol & 0x00FFFFFF;
        int alpha   = (drawCol >> 24) & 0xFF;

        boolean showBg = Boolean.TRUE.equals(getPropOrDefault("showBackground", Boolean.FALSE));
        if (showBg) {
            int bgCol = (Math.max(40, alpha / 2) << 24) | baseRgb;
            Gui.drawRect(0, 0, getWidth(), getHeight(), bgCol);
        }

        drawLabelValue(fr, "CPS: ", value, 4, 3, drawCol);
    }
}
