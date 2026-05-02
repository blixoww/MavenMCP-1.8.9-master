package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateWidget extends BaseWidget {
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd ");
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm");

    public DateWidget(String id, int x, int y) {
        super(id, x, y);
        this.width = 140;
        this.height = 12;
    }

    @Override
    public boolean supportsLabelColor() { return true; }

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        Date now = new Date();
        drawLabelValue(mc.fontRendererObj, DATE_FMT.format(now), TIME_FMT.format(now), 0, 0);
    }
}
