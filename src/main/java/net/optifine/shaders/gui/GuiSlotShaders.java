package net.optifine.shaders.gui;

import java.awt.Color;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiRenderUtils;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.resources.I18n;
import net.minecraft.src.Config;
import net.optifine.Lang;
import net.optifine.shaders.IShaderPack;
import net.optifine.shaders.Shaders;
import net.optifine.util.ResUtils;

class GuiSlotShaders extends GuiSlot
{
    private ArrayList shaderslist;
    private int selectedIndex;
    private long lastClickedCached = 0L;
    final GuiShaders shadersGui;

    private static final int ACCENT_COLOR = new Color(220, 30, 30).getRGB();

    public GuiSlotShaders(GuiShaders par1GuiShaders, int width, int height, int top, int bottom, int slotHeight)
    {
        super(par1GuiShaders.getMc(), width, height, top, bottom, slotHeight);
        this.shadersGui = par1GuiShaders;
        this.updateList();
        this.amountScrolled = 0.0F;
        int i = this.selectedIndex * slotHeight;
        int j = (bottom - top) / 2;

        if (i > j)
        {
            this.scrollBy(i - j);
        }
    }

    public int getListWidth()
    {
        return this.width - 20;
    }

    public void updateList()
    {
        this.shaderslist = Shaders.listOfShaders();
        this.selectedIndex = 0;
        int i = 0;

        for (int j = this.shaderslist.size(); i < j; ++i)
        {
            if (((String)this.shaderslist.get(i)).equals(Shaders.currentShaderName))
            {
                this.selectedIndex = i;
                break;
            }
        }
    }

    protected int getSize()
    {
        return this.shaderslist.size();
    }

    protected void elementClicked(int index, boolean doubleClicked, int mouseX, int mouseY)
    {
        if (index != this.selectedIndex || this.lastClicked != this.lastClickedCached)
        {
            String s = (String)this.shaderslist.get(index);
            IShaderPack ishaderpack = Shaders.getShaderPack(s);

            if (this.checkCompatible(ishaderpack, index))
            {
                this.selectIndex(index);
            }
        }
    }

    private void selectIndex(int index)
    {
        this.selectedIndex = index;
        this.lastClickedCached = this.lastClicked;
        Shaders.setShaderPack((String)this.shaderslist.get(index));
        Shaders.uninit();
        this.shadersGui.updateButtons();
    }

    private boolean checkCompatible(IShaderPack sp, final int index)
    {
        if (sp == null)
        {
            return true;
        }
        else
        {
            InputStream inputstream = sp.getResourceAsStream("/shaders/shaders.properties");
            Properties properties = ResUtils.readProperties(inputstream, "Shaders");

            if (properties == null)
            {
                return true;
            }
            else
            {
                String s = "version.1.8.9";
                String s1 = properties.getProperty(s);

                if (s1 == null)
                {
                    return true;
                }
                else
                {
                    s1 = s1.trim();
                    String s2 = "M6_pre2";
                    int i = Config.compareRelease(s2, s1);

                    if (i >= 0)
                    {
                        return true;
                    }
                    else
                    {
                        String s3 = ("HD_U_" + s1).replace('_', ' ');
                        String s4 = I18n.format("of.message.shaders.nv1", new Object[] {s3});
                        String s5 = I18n.format("of.message.shaders.nv2", new Object[0]);
                        GuiYesNoCallback guiyesnocallback = new GuiYesNoCallback()
                        {
                            public void confirmClicked(boolean result, int id)
                            {
                                if (result)
                                {
                                    GuiSlotShaders.this.selectIndex(index);
                                }

                                GuiSlotShaders.this.mc.displayGuiScreen(GuiSlotShaders.this.shadersGui);
                            }
                        };
                        GuiYesNo guiyesno = new GuiYesNo(guiyesnocallback, s4, s5, 0);
                        this.mc.displayGuiScreen(guiyesno);
                        return false;
                    }
                }
            }
        }
    }

    protected boolean isSelected(int index)
    {
        return index == this.selectedIndex;
    }

    protected int getScrollBarX()
    {
        return this.width - 6;
    }

    protected int getContentHeight()
    {
        return this.getSize() * 18;
    }

    protected void drawBackground()
    {
        // Pas de fond par défaut, le panneau est dessiné par GuiShaders
    }

    protected void drawSlot(int index, int posX, int posY, int contentY, int mouseX, int mouseY)
    {
        String s = (String)this.shaderslist.get(index);
        boolean selected = isSelected(index);
        boolean hovered = mouseX >= posX && mouseX < posX + this.getListWidth()
                && mouseY >= posY && mouseY < posY + contentY;

        // Fond de la ligne
        if (selected)
        {
            // Fond rouge accent avec transparence
            Gui.drawRect(posX + 2, posY, posX + this.getListWidth() - 2, posY + contentY - 1,
                    0xCC000000 | (ACCENT_COLOR & 0xFFFFFF));
            // Barre latérale gauche
            Gui.drawRect(posX + 2, posY, posX + 4, posY + contentY - 1, 0xFFDC1E1E);
            // Contour subtil
            GuiRenderUtils.drawRectOutline(posX + 2, posY, this.getListWidth() - 4, contentY - 1, 0x55FFFFFF);
        }
        else if (hovered)
        {
            Gui.drawRect(posX + 2, posY, posX + this.getListWidth() - 2, posY + contentY - 1, 0x44FFFFFF);
        }

        // Nom du shader
        if (s.equals("OFF"))
            s = Lang.get("of.options.shaders.packNone");
        else if (s.equals("(internal)"))
            s = Lang.get("of.options.shaders.packDefault");

        int textColor = selected ? 0xFFFFFF : (hovered ? 0xDDDDDD : 0xAAAAAA);
        int textY = posY + (contentY - 8) / 2;
        this.shadersGui.drawCenteredString(s, (posX + this.getListWidth()) / 2, textY, textColor);
    }

    public int getSelectedIndex()
    {
        return this.selectedIndex;
    }
}
