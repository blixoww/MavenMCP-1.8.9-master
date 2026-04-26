package net.minecraft.client.gui;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.ResourcePackListEntry;
import net.minecraft.client.resources.ResourcePackListEntryDefault;
import net.minecraft.client.resources.ResourcePackListEntryFound;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.Sys;

public class GuiScreenResourcePacks extends GuiScreen
{
    private static final Logger logger = LogManager.getLogger();
    private final GuiScreen parentScreen;
    private List<ResourcePackListEntry> availableResourcePacks;
    private List<ResourcePackListEntry> selectedResourcePacks;

    /** List component that contains the available resource packs */
    private GuiResourcePackAvailable availableResourcePacksList;

    /** List component that contains the selected resource packs */
    private GuiResourcePackSelected selectedResourcePacksList;
    private static final int ACCENT = 0xFFDC1E1E;
    private float animation = 0f;
    private long  animLastTime = -1L;

    private boolean changed = false;

    public GuiScreenResourcePacks(GuiScreen parentScreenIn)
    {
        this.parentScreen = parentScreenIn;
    }

    /**
     * Adds the buttons (and other controls) to the screen in question. Called when the GUI is displayed and when the
     * window resizes, the buttonList is cleared beforehand.
     */
    public void initGui()
    {
        this.animation = 0f; this.animLastTime = -1L;
        // descendre légèrement les boutons pour qu'ils soient alignés visuellement avec le footer
        int btnY = this.height - 40;
        this.buttonList.add(new GuiMenuButton(2, this.width / 2 - 154, btnY, 150, 20, "📁 OUVRIR DOSSIER"));
        this.buttonList.add(new GuiMenuButton(1, this.width / 2 + 4, btnY, 150, 20, "DONE", true));

        if (!this.changed)
        {
            this.availableResourcePacks = Lists.<ResourcePackListEntry>newArrayList();
            this.selectedResourcePacks = Lists.<ResourcePackListEntry>newArrayList();
            ResourcePackRepository resourcepackrepository = this.mc.getResourcePackRepository();
            resourcepackrepository.updateRepositoryEntriesAll();
            List<ResourcePackRepository.Entry> list = Lists.newArrayList(resourcepackrepository.getRepositoryEntriesAll());
            list.removeAll(resourcepackrepository.getRepositoryEntries());

            for (ResourcePackRepository.Entry resourcepackrepository$entry : list)
            {
                this.availableResourcePacks.add(new ResourcePackListEntryFound(this, resourcepackrepository$entry));
            }

            for (ResourcePackRepository.Entry resourcepackrepository$entry1 : Lists.reverse(resourcepackrepository.getRepositoryEntries()))
            {
                this.selectedResourcePacks.add(new ResourcePackListEntryFound(this, resourcepackrepository$entry1));
            }

            this.selectedResourcePacks.add(new ResourcePackListEntryDefault(this));
        }

        this.availableResourcePacksList = new GuiResourcePackAvailable(this.mc, 200, this.height, this.availableResourcePacks);
        this.availableResourcePacksList.setSlotXBoundsFromLeft(this.width / 2 - 4 - 200);
        this.availableResourcePacksList.registerScrollButtons(7, 8);
        this.selectedResourcePacksList = new GuiResourcePackSelected(this.mc, 200, this.height, this.selectedResourcePacks);
        this.selectedResourcePacksList.setSlotXBoundsFromLeft(this.width / 2 + 4);
        this.selectedResourcePacksList.registerScrollButtons(7, 8);
    }

    /**
     * Handles mouse input.
     */
    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        this.selectedResourcePacksList.handleMouseInput();
        this.availableResourcePacksList.handleMouseInput();
    }

    public boolean hasResourcePackEntry(ResourcePackListEntry p_146961_1_)
    {
        return this.selectedResourcePacks.contains(p_146961_1_);
    }

    public List<ResourcePackListEntry> getListContaining(ResourcePackListEntry p_146962_1_)
    {
        return this.hasResourcePackEntry(p_146962_1_) ? this.selectedResourcePacks : this.availableResourcePacks;
    }

    public List<ResourcePackListEntry> getAvailableResourcePacks()
    {
        return this.availableResourcePacks;
    }

    public List<ResourcePackListEntry> getSelectedResourcePacks()
    {
        return this.selectedResourcePacks;
    }

    /**
     * Called by the controls from the buttonList when activated. (Mouse pressed for buttons)
     */
    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (button.enabled)
        {
            if (button.id == 2)
            {
                File file1 = this.mc.getResourcePackRepository().getDirResourcepacks();
                String s = file1.getAbsolutePath();

                if (Util.getOSType() == Util.EnumOS.OSX)
                {
                    try
                    {
                        logger.info(s);
                        Runtime.getRuntime().exec(new String[] {"/usr/bin/open", s});
                        return;
                    }
                    catch (IOException ioexception1)
                    {
                        logger.error((String)"Couldn\'t open file", (Throwable)ioexception1);
                    }
                }
                else if (Util.getOSType() == Util.EnumOS.WINDOWS)
                {
                    String s1 = String.format("cmd.exe /C start \"Open file\" \"%s\"", new Object[] {s});

                    try
                    {
                        Runtime.getRuntime().exec(s1);
                        return;
                    }
                    catch (IOException ioexception)
                    {
                        logger.error((String)"Couldn\'t open file", (Throwable)ioexception);
                    }
                }

                boolean flag = false;

                try
                {
                    Class<?> oclass = Class.forName("java.awt.Desktop");
                    Object object = oclass.getMethod("getDesktop", new Class[0]).invoke((Object)null, new Object[0]);
                    oclass.getMethod("browse", new Class[] {URI.class}).invoke(object, new Object[] {file1.toURI()});
                }
                catch (Throwable throwable)
                {
                    logger.error("Couldn\'t open link", throwable);
                    flag = true;
                }

                if (flag)
                {
                    logger.info("Opening via system class!");
                    Sys.openURL("file://" + s);
                }
            }
            else if (button.id == 1)
            {
                if (this.changed)
                {
                    List<ResourcePackRepository.Entry> list = Lists.<ResourcePackRepository.Entry>newArrayList();

                    for (ResourcePackListEntry resourcepacklistentry : this.selectedResourcePacks)
                    {
                        if (resourcepacklistentry instanceof ResourcePackListEntryFound)
                        {
                            list.add(((ResourcePackListEntryFound)resourcepacklistentry).func_148318_i());
                        }
                    }

                    Collections.reverse(list);
                    this.mc.getResourcePackRepository().setRepositories(list);
                    this.mc.gameSettings.resourcePacks.clear();
                    this.mc.gameSettings.incompatibleResourcePacks.clear();

                    for (ResourcePackRepository.Entry resourcepackrepository$entry : list)
                    {
                        this.mc.gameSettings.resourcePacks.add(resourcepackrepository$entry.getResourcePackName());

                        if (resourcepackrepository$entry.func_183027_f() != 1)
                        {
                            this.mc.gameSettings.incompatibleResourcePacks.add(resourcepackrepository$entry.getResourcePackName());
                        }
                    }

                    this.mc.gameSettings.saveOptions();
                    this.mc.refreshResources();
                }

                this.mc.displayGuiScreen(this.parentScreen);
            }
        }
    }

    /**
     * Called when the mouse is clicked. Args : mouseX, mouseY, clickedButton
     */
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.availableResourcePacksList.mouseClicked(mouseX, mouseY, mouseButton);
        this.selectedResourcePacksList.mouseClicked(mouseX, mouseY, mouseButton);
    }

    /**
     * Called when a mouse button is released.  Args : mouseX, mouseY, releaseButton
     */
    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        super.mouseReleased(mouseX, mouseY, state);
    }

    /**
     * Draws the screen and all the components in it. Args : mouseX, mouseY, renderPartialTicks
     */
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        long now = Minecraft.getSystemTime();
        if (animLastTime != -1L) animation = MathHelper.clamp_float(animation + (now - animLastTime) / 250f, 0f, 1f);
        animLastTime = now;
        float e = animation * animation * (3f - 2f * animation);

        // Fond sombre plein — propre
        Gui.drawRect(0, 0, this.width, this.height, 0xFF080B10);

        // Listes resource packs (renderBackground=false, elles ne redessinett pas le dirt)
        this.availableResourcePacksList.drawScreen(mouseX, mouseY, partialTicks);
        this.selectedResourcePacksList.drawScreen(mouseX, mouseY, partialTicks);

        // ── Header ────────────────────────────────────────────────────────
        // Ligne rouge tout en haut
        Gui.drawRect(0, 0, this.width, 1, 0xFFDC1E1E);
        // Fond header
        Gui.drawRect(0, 1, this.width, 36, (int)(e*230) << 24 | 0x0A0D14);
        // Dégradé de fondu sous le header
        GuiRenderUtils.drawGradientRect(0, 36, this.width, 50, (int)(e*120) << 24 | 0x0A0D14, 0x00000000);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, (1f - e) * 8, 0);

        int ta = (int)(e * 255) << 24;
        String t1 = "§c§lRESOURCE ";
        String t2 = "§f§lPACKS";
        int tw = fontRendererObj.getStringWidth(t1) + fontRendererObj.getStringWidth(t2);
        int tx = this.width / 2 - tw / 2;
        fontRendererObj.drawStringWithShadow(t1, tx, 12, ta | 0xFFFFFF);
        fontRendererObj.drawStringWithShadow(t2, tx + fontRendererObj.getStringWidth(t1), 12, ta | 0xFFFFFF);
        // Diviseur animé
        int dw = (int)((tw + 20) * e);
        Gui.drawRect(this.width/2 - dw/2, 24, this.width/2 + dw/2, 25, (int)(e*50) << 24 | 0xFFFFFF);

        GlStateManager.popMatrix();

        // ── Footer ─────────────────────────────────────────────────────────
        // Dégradé de fondu avant le footer
        GuiRenderUtils.drawGradientRect(0, this.height - 58, this.width, this.height - 44, 0x00000000, (int)(e*120) << 24 | 0x0A0D14);
        // Fond footer
        Gui.drawRect(0, this.height - 44, this.width, this.height, (int)(e*230) << 24 | 0x0A0D14);
        // Ligne séparatrice fine
        Gui.drawRect(0, this.height - 44, this.width, this.height - 43, 0x33FFFFFF);

        String folderInfo = I18n.format("resourcePack.folderInfo");
        // Enlever le contenu entre parenthèses (ex. "(Place resource pack files here)") pour alléger l'affichage
        folderInfo = folderInfo.replaceAll("\\(.*\\)", "").trim();
        int fiW = this.fontRendererObj.getStringWidth(folderInfo);
        fontRendererObj.drawString("§8" + folderInfo, this.width / 2 - fiW / 2, this.height - 18, ta | 0xFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /**
     * Marks the selected resource packs list as changed to trigger a resource reload when the screen is closed
     */
    public void markChanged()
    {
        this.changed = true;
    }
}
