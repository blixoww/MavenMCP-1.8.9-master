package net.minecraft.client.gui.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

public class SimpleTextWidget extends BaseWidget {
    private String text;

    public SimpleTextWidget(String id, int x, int y, String text) {
        super(id, x, y);
        this.text = text;
        this.width = 100;
        this.height = 12;
    }

    public void setText(String t) {
        this.text = t;
    }

    public String getText() {
        return text;
    }

    public int getTextWidth(FontRenderer fr) {
        return fr != null && text != null ? fr.getStringWidth(text) + 6 : this.width;
    }

    @Override
    protected void draw() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        FontRenderer fr = mc.fontRendererObj;
        this.width = fr.getStringWidth(text) + 6;
        this.height = 12;
        int col = getColor();
        if ((col & 0xFF000000) == 0) col |= 0xFF000000;
        if ((col & 0x00FFFFFF) == 0) col = 0xFFFFFFFF;
        fr.drawStringWithShadow(text, x, y, col);
    }
}
