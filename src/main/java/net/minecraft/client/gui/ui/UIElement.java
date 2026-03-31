package net.minecraft.client.gui.ui;

public interface UIElement
{
    String getId();
    int getX();
    int getY();
    void setPosition(int x, int y);
    int getWidth();
    int getHeight();
    void setWidth(int w);
    void setHeight(int h);
    
    float getScale();
    void setScale(float scale);

    void render(int mouseX, int mouseY, float partialTicks);
    boolean containsPoint(int x, int y);
    boolean isEnabled();
    void setEnabled(boolean e);
    int getColor();
    void setColor(int rgba);
    boolean isRGBMode();
    void setRGBMode(boolean v);
}
