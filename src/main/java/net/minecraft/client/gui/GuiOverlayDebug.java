package net.minecraft.client.gui;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.FrameTimer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

public class GuiOverlayDebug extends Gui
{
    private final Minecraft mc;
    private final FontRenderer fontRenderer;

    // Animation
    private long debugOpenTime = -1L;
    private long lastDebugRenderTime = 0L;
    private static final long ANIMATION_DURATION = 350L;

    /**
     * Bounding boxes (x1, y1, x2, y2) de chaque ligne F3 rendue ce frame.
     * Mis à jour par renderDebugInfo(), lu par UIManager pour masquer les
     * widgets qui se superposent au panneau F3.
     */
    public static final java.util.List<int[]> F3_RECTS = new java.util.ArrayList<>();

    public GuiOverlayDebug(Minecraft mc)
    {
        this.mc = mc;
        this.fontRenderer = mc.fontRendererObj;
    }

    /** Retourne l'offset actuel de l'animation slide-in (décroît de 250 → 0). */
    private float getAnimationOffset()
    {
        if (debugOpenTime < 0L) return 0.0f;
        float t = Math.min(1.0f, (float)(System.currentTimeMillis() - debugOpenTime) / (float)ANIMATION_DURATION);
        // ease-out quad
        t = 1.0f - (1.0f - t) * (1.0f - t);
        return (1.0f - t) * 250.0f;
    }

    public void renderDebugInfo(ScaledResolution scaledResolutionIn)
    {
        this.mc.mcProfiler.startSection("debug");

        // Détection de l'ouverture du F3 (gap > 500 ms = nouvel affichage)
        long now = System.currentTimeMillis();
        if (now - lastDebugRenderTime > 500L)
        {
            debugOpenTime = now;
        }
        lastDebugRenderTime = now;

        // Réinitialiser les rects à chaque frame de debug
        F3_RECTS.clear();

        GlStateManager.pushMatrix();
        this.renderDebugInfoLeft();
        this.renderDebugInfoRight(scaledResolutionIn);
        GlStateManager.popMatrix();

        if (this.mc.gameSettings.showLagometer)
        {
            this.renderLagometer();
        }

        this.mc.mcProfiler.endSection();
    }

    private boolean isReducedDebug()
    {
        return this.mc.thePlayer.hasReducedDebug() || this.mc.gameSettings.reducedDebugInfo;
    }

    protected void renderDebugInfoLeft()
    {
        List<String> list = this.call();

        float offset = getAnimationOffset();
        GlStateManager.pushMatrix();
        GlStateManager.translate(-offset, 0.0f, 0.0f);

        for (int i = 0; i < list.size(); ++i)
        {
            String s = list.get(i);

            if (!Strings.isNullOrEmpty(s))
            {
                int j = this.fontRenderer.FONT_HEIGHT;
                int k = this.fontRenderer.getStringWidth(s);
                int i1 = 2 + j * i;
                drawRect(1, i1 - 1, 2 + k + 1, i1 + j - 1, -1873784752);
                this.fontRenderer.drawString(s, 2, i1, 14737632);
                // Enregistrer le rect (positions finales, sans offset animation)
                F3_RECTS.add(new int[]{1, i1 - 1, 2 + k + 1, i1 + j - 1});
            }
        }

        GlStateManager.popMatrix();
    }

    protected void renderDebugInfoRight(ScaledResolution scaledRes)
    {
        List<String> list = this.getDebugInfoRight();

        float offset = getAnimationOffset();
        GlStateManager.pushMatrix();
        GlStateManager.translate(offset, 0.0f, 0.0f);

        for (int i = 0; i < list.size(); ++i)
        {
            String s = list.get(i);

            if (!Strings.isNullOrEmpty(s))
            {
                int j = this.fontRenderer.FONT_HEIGHT;
                int k = this.fontRenderer.getStringWidth(s);
                int l = scaledRes.getScaledWidth() - 2 - k;
                int i1 = 2 + j * i;
                drawRect(l - 1, i1 - 1, l + k + 1, i1 + j - 1, -1873784752);
                this.fontRenderer.drawString(s, l, i1, 14737632);
                // Enregistrer le rect (positions finales, sans offset animation)
                F3_RECTS.add(new int[]{l - 1, i1 - 1, l + k + 1, i1 + j - 1});
            }
        }

        GlStateManager.popMatrix();
    }

    @SuppressWarnings("incomplete-switch")
    protected List<String> call()
    {
        BlockPos blockpos = new BlockPos(this.mc.getRenderViewEntity().posX, this.mc.getRenderViewEntity().getEntityBoundingBox().minY, this.mc.getRenderViewEntity().posZ);

        // ── En-tête RedConflict ──────────────────────────────────────────────
        String header = EnumChatFormatting.RED + "" + EnumChatFormatting.BOLD + "Red"
                      + EnumChatFormatting.WHITE + "" + EnumChatFormatting.BOLD + "Conflict";

        if (this.isReducedDebug())
        {
            return Lists.newArrayList(new String[] {
                header,
                "",
                EnumChatFormatting.GOLD + "Minecraft 1.8.9 (" + this.mc.getVersion() + "/" + ClientBrandRetriever.getClientModName() + ")",
                EnumChatFormatting.YELLOW + this.mc.debug,
                EnumChatFormatting.GREEN + this.mc.renderGlobal.getDebugInfoRenders(),
                EnumChatFormatting.GREEN + this.mc.renderGlobal.getDebugInfoEntities(),
                EnumChatFormatting.AQUA + "P: " + this.mc.effectRenderer.getStatistics() + ". T: " + this.mc.theWorld.getDebugLoadedEntities(),
                EnumChatFormatting.WHITE + this.mc.theWorld.getProviderName(),
                "",
                EnumChatFormatting.AQUA + String.format("Chunk-relative: %d %d %d",
                        Integer.valueOf(blockpos.getX() & 15),
                        Integer.valueOf(blockpos.getY() & 15),
                        Integer.valueOf(blockpos.getZ() & 15))
            });
        }
        else
        {
            Entity entity = this.mc.getRenderViewEntity();
            EnumFacing enumfacing = entity.getHorizontalFacing();
            String s = "Invalid";

            switch (enumfacing)
            {
                case NORTH:
                    s = "Towards negative Z";
                    break;

                case SOUTH:
                    s = "Towards positive Z";
                    break;

                case WEST:
                    s = "Towards negative X";
                    break;

                case EAST:
                    s = "Towards positive X";
            }

            List<String> list = Lists.newArrayList(new String[] {
                header,
                "",
                EnumChatFormatting.GOLD + "Minecraft 1.8.9 (" + this.mc.getVersion() + "/" + ClientBrandRetriever.getClientModName() + ")",
                EnumChatFormatting.YELLOW + this.mc.debug,
                EnumChatFormatting.GREEN + this.mc.renderGlobal.getDebugInfoRenders(),
                EnumChatFormatting.GREEN + this.mc.renderGlobal.getDebugInfoEntities(),
                EnumChatFormatting.AQUA + "P: " + this.mc.effectRenderer.getStatistics() + ". T: " + this.mc.theWorld.getDebugLoadedEntities(),
                EnumChatFormatting.WHITE + this.mc.theWorld.getProviderName(),
                "",
                EnumChatFormatting.YELLOW + String.format("XYZ: %.3f / %.5f / %.3f",
                        Double.valueOf(this.mc.getRenderViewEntity().posX),
                        Double.valueOf(this.mc.getRenderViewEntity().getEntityBoundingBox().minY),
                        Double.valueOf(this.mc.getRenderViewEntity().posZ)),
                EnumChatFormatting.AQUA + String.format("Block: %d %d %d",
                        Integer.valueOf(blockpos.getX()),
                        Integer.valueOf(blockpos.getY()),
                        Integer.valueOf(blockpos.getZ())),
                EnumChatFormatting.AQUA + String.format("Chunk: %d %d %d in %d %d %d",
                        Integer.valueOf(blockpos.getX() & 15),
                        Integer.valueOf(blockpos.getY() & 15),
                        Integer.valueOf(blockpos.getZ() & 15),
                        Integer.valueOf(blockpos.getX() >> 4),
                        Integer.valueOf(blockpos.getY() >> 4),
                        Integer.valueOf(blockpos.getZ() >> 4)),
                EnumChatFormatting.GREEN + String.format("Facing: %s (%s) (%.1f / %.1f)",
                        enumfacing, s,
                        Float.valueOf(MathHelper.wrapAngleTo180_float(entity.rotationYaw)),
                        Float.valueOf(MathHelper.wrapAngleTo180_float(entity.rotationPitch)))
            });

            if (this.mc.theWorld != null && this.mc.theWorld.isBlockLoaded(blockpos))
            {
                Chunk chunk = this.mc.theWorld.getChunkFromBlockCoords(blockpos);
                list.add(EnumChatFormatting.GREEN + "Biome: " + chunk.getBiome(blockpos, this.mc.theWorld.getWorldChunkManager()).biomeName);
                list.add(EnumChatFormatting.YELLOW + "Light: " + chunk.getLightSubtracted(blockpos, 0)
                        + " (" + chunk.getLightFor(EnumSkyBlock.SKY, blockpos) + " sky, "
                        + chunk.getLightFor(EnumSkyBlock.BLOCK, blockpos) + " block)");
                DifficultyInstance difficultyinstance = this.mc.theWorld.getDifficultyForLocation(blockpos);

                if (this.mc.isIntegratedServerRunning() && this.mc.getIntegratedServer() != null)
                {
                    EntityPlayerMP entityplayermp = this.mc.getIntegratedServer().getConfigurationManager().getPlayerByUUID(this.mc.thePlayer.getUniqueID());

                    if (entityplayermp != null)
                    {
                        difficultyinstance = entityplayermp.worldObj.getDifficultyForLocation(new BlockPos(entityplayermp));
                    }
                }

                list.add(EnumChatFormatting.RED + String.format("Local Difficulty: %.2f (Day %d)",
                        Float.valueOf(difficultyinstance.getAdditionalDifficulty()),
                        Long.valueOf(this.mc.theWorld.getWorldTime() / 24000L)));
            }

            if (this.mc.entityRenderer != null && this.mc.entityRenderer.isShaderActive())
            {
                list.add(EnumChatFormatting.LIGHT_PURPLE + "Shader: " + this.mc.entityRenderer.getShaderGroup().getShaderGroupName());
            }

            if (this.mc.objectMouseOver != null && this.mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && this.mc.objectMouseOver.getBlockPos() != null)
            {
                BlockPos blockpos1 = this.mc.objectMouseOver.getBlockPos();
                list.add(EnumChatFormatting.WHITE + String.format("Looking at: %d %d %d",
                        Integer.valueOf(blockpos1.getX()),
                        Integer.valueOf(blockpos1.getY()),
                        Integer.valueOf(blockpos1.getZ())));
            }

            // ── Armures portées (matériaux custom + vanilla) ──────────────────────
            if (this.mc.thePlayer != null)
            {
                EntityPlayer player = this.mc.thePlayer;
                String[] slotNames = {"Boots", "Leggings", "Chestplate", "Helmet"};
                boolean hasCustomArmor = false;

                for (int armorSlot = 0; armorSlot < 4; armorSlot++)
                {
                    ItemStack armorStack = player.getCurrentArmor(armorSlot);
                    if (armorStack != null && armorStack.getItem() instanceof ItemArmor)
                    {
                        ItemArmor armor = (ItemArmor) armorStack.getItem();
                        ItemArmor.ArmorMaterial mat = armor.getArmorMaterial();
                        if (mat == ItemArmor.ArmorMaterial.STEEL
                                || mat == ItemArmor.ArmorMaterial.EMERALD
                                || mat == ItemArmor.ArmorMaterial.RUBY
                                || mat == ItemArmor.ArmorMaterial.COBALT)
                        {
                            hasCustomArmor = true;
                        }
                    }
                }

                if (hasCustomArmor)
                {
                    list.add("");
                    list.add(EnumChatFormatting.AQUA + "── Armures ──");
                    for (int armorSlot = 0; armorSlot < 4; armorSlot++)
                    {
                        ItemStack armorStack = player.getCurrentArmor(armorSlot);
                        if (armorStack != null && armorStack.getItem() instanceof ItemArmor)
                        {
                            ItemArmor armor = (ItemArmor) armorStack.getItem();
                            ItemArmor.ArmorMaterial mat = armor.getArmorMaterial();
                            int dmgReduction = mat.getDamageReductionAmount(armorSlot);
                            int durLeft = armorStack.getMaxDamage() - armorStack.getItemDamage();
                            int durMax = armorStack.getMaxDamage();
                            String color = (mat == ItemArmor.ArmorMaterial.STEEL)   ? EnumChatFormatting.GRAY.toString()
                                         : (mat == ItemArmor.ArmorMaterial.EMERALD) ? EnumChatFormatting.GREEN.toString()
                                         : (mat == ItemArmor.ArmorMaterial.RUBY)    ? EnumChatFormatting.RED.toString()
                                         : (mat == ItemArmor.ArmorMaterial.COBALT)  ? EnumChatFormatting.BLUE.toString()
                                         : EnumChatFormatting.WHITE.toString();
                            list.add(String.format("%s%s: %s+%d def | %d/%d dur",
                                    color,
                                    slotNames[armorSlot],
                                    EnumChatFormatting.RESET,
                                    dmgReduction,
                                    durLeft,
                                    durMax));
                        }
                    }
                }
            }

            return list;
        }
    }

    protected List<String> getDebugInfoRight()
    {
        long i = Runtime.getRuntime().maxMemory();
        long j = Runtime.getRuntime().totalMemory();
        long k = Runtime.getRuntime().freeMemory();
        long l = j - k;
        long memPercent = l * 100L / i;
        String memColor = memPercent > 80L ? EnumChatFormatting.RED.toString()
                        : memPercent > 50L ? EnumChatFormatting.YELLOW.toString()
                        : EnumChatFormatting.GREEN.toString();

        List<String> list = Lists.newArrayList(new String[] {
            EnumChatFormatting.WHITE + String.format("Java: %s %dbit",
                    System.getProperty("java.version"),
                    Integer.valueOf(this.mc.isJava64bit() ? 64 : 32)),
            memColor + String.format("Mem: % 2d%% %03d/%03dMB",
                    Long.valueOf(memPercent),
                    Long.valueOf(bytesToMb(l)),
                    Long.valueOf(bytesToMb(i))),
            EnumChatFormatting.AQUA + String.format("Allocated: % 2d%% %03dMB",
                    Long.valueOf(j * 100L / i),
                    Long.valueOf(bytesToMb(j))),
            "",
            EnumChatFormatting.YELLOW + String.format("CPU: %s", OpenGlHelper.getCpu()),
            "",
            EnumChatFormatting.WHITE + String.format("Display: %dx%d (%s)",
                    Integer.valueOf(Display.getWidth()),
                    Integer.valueOf(Display.getHeight()),
                    GL11.glGetString(GL11.GL_VENDOR)),
            EnumChatFormatting.GRAY + GL11.glGetString(GL11.GL_RENDERER),
            EnumChatFormatting.GRAY + GL11.glGetString(GL11.GL_VERSION)
        });

        if (this.isReducedDebug())
        {
            return list;
        }
        else
        {
            if (this.mc.objectMouseOver != null && this.mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && this.mc.objectMouseOver.getBlockPos() != null)
            {
                BlockPos blockpos = this.mc.objectMouseOver.getBlockPos();
                IBlockState iblockstate = this.mc.theWorld.getBlockState(blockpos);

                if (this.mc.theWorld.getWorldType() != WorldType.DEBUG_WORLD)
                {
                    iblockstate = iblockstate.getBlock().getActualState(iblockstate, this.mc.theWorld, blockpos);
                }

                list.add("");
                list.add(EnumChatFormatting.GOLD + String.valueOf(Block.blockRegistry.getNameForObject(iblockstate.getBlock())));

                for (Entry<IProperty, Comparable> entry : iblockstate.getProperties().entrySet())
                {
                    String s = ((Comparable)entry.getValue()).toString();

                    if (entry.getValue() == Boolean.TRUE)
                    {
                        s = EnumChatFormatting.GREEN + s;
                    }
                    else if (entry.getValue() == Boolean.FALSE)
                    {
                        s = EnumChatFormatting.RED + s;
                    }

                    list.add(EnumChatFormatting.WHITE + ((IProperty)entry.getKey()).getName() + ": " + s);
                }
            }

            return list;
        }
    }

    private void renderLagometer()
    {
        GlStateManager.disableDepth();
        FrameTimer frametimer = this.mc.getFrameTimer();
        int i = frametimer.getLastIndex();
        int j = frametimer.getIndex();
        long[] along = frametimer.getFrames();
        ScaledResolution scaledresolution = new ScaledResolution(this.mc);
        int k = i;
        int l = 0;
        drawRect(0, scaledresolution.getScaledHeight() - 60, 240, scaledresolution.getScaledHeight(), -1873784752);

        while (k != j)
        {
            int i1 = frametimer.getLagometerValue(along[k], 30);
            int j1 = this.getFrameColor(MathHelper.clamp_int(i1, 0, 60), 0, 30, 60);
            this.drawVerticalLine(l, scaledresolution.getScaledHeight(), scaledresolution.getScaledHeight() - i1, j1);
            ++l;
            k = frametimer.parseIndex(k + 1);
        }

        drawRect(1, scaledresolution.getScaledHeight() - 30 + 1, 14, scaledresolution.getScaledHeight() - 30 + 10, -1873784752);
        this.fontRenderer.drawString("60", 2, scaledresolution.getScaledHeight() - 30 + 2, 14737632);
        this.drawHorizontalLine(0, 239, scaledresolution.getScaledHeight() - 30, -1);
        drawRect(1, scaledresolution.getScaledHeight() - 60 + 1, 14, scaledresolution.getScaledHeight() - 60 + 10, -1873784752);
        this.fontRenderer.drawString("30", 2, scaledresolution.getScaledHeight() - 60 + 2, 14737632);
        this.drawHorizontalLine(0, 239, scaledresolution.getScaledHeight() - 60, -1);
        this.drawHorizontalLine(0, 239, scaledresolution.getScaledHeight() - 1, -1);
        this.drawVerticalLine(0, scaledresolution.getScaledHeight() - 60, scaledresolution.getScaledHeight(), -1);
        this.drawVerticalLine(239, scaledresolution.getScaledHeight() - 60, scaledresolution.getScaledHeight(), -1);

        if (this.mc.gameSettings.limitFramerate <= 120)
        {
            this.drawHorizontalLine(0, 239, scaledresolution.getScaledHeight() - 60 + this.mc.gameSettings.limitFramerate / 2, -16711681);
        }

        GlStateManager.enableDepth();
    }

    private int getFrameColor(int p_181552_1_, int p_181552_2_, int p_181552_3_, int p_181552_4_)
    {
        return p_181552_1_ < p_181552_3_ ? this.blendColors(-16711936, -256, (float)p_181552_1_ / (float)p_181552_3_) : this.blendColors(-256, -65536, (float)(p_181552_1_ - p_181552_3_) / (float)(p_181552_4_ - p_181552_3_));
    }

    private int blendColors(int p_181553_1_, int p_181553_2_, float p_181553_3_)
    {
        int i = p_181553_1_ >> 24 & 255;
        int j = p_181553_1_ >> 16 & 255;
        int k = p_181553_1_ >> 8 & 255;
        int l = p_181553_1_ & 255;
        int i1 = p_181553_2_ >> 24 & 255;
        int j1 = p_181553_2_ >> 16 & 255;
        int k1 = p_181553_2_ >> 8 & 255;
        int l1 = p_181553_2_ & 255;
        int i2 = MathHelper.clamp_int((int)((float)i + (float)(i1 - i) * p_181553_3_), 0, 255);
        int j2 = MathHelper.clamp_int((int)((float)j + (float)(j1 - j) * p_181553_3_), 0, 255);
        int k2 = MathHelper.clamp_int((int)((float)k + (float)(k1 - k) * p_181553_3_), 0, 255);
        int l2 = MathHelper.clamp_int((int)((float)l + (float)(l1 - l) * p_181553_3_), 0, 255);
        return i2 << 24 | j2 << 16 | k2 << 8 | l2;
    }

    private static long bytesToMb(long bytes)
    {
        return bytes / 1024L / 1024L;
    }
}
