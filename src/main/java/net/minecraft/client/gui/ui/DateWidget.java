package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateWidget extends BaseWidget {
    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public DateWidget(String id, int x, int y) {
        super(id, x, y);
        this.width = 140;
        this.height = 12;
    }

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        String s = df.format(new Date());
        int col = getColor();
        if ((col & 0x00FFFFFF) == 0) col = 0x00FFFFFF;
        mc.fontRendererObj.drawStringWithShadow(s, 0, 0, col & 0x00FFFFFF);
    }
}
