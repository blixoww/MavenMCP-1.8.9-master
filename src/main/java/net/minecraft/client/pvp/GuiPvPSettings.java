package net.minecraft.client.pvp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;

import java.io.IOException;

/**
 * GUI de configuration des paramètres PvP.
 * Même style que GuiVisualSettings.
 */
public class GuiPvPSettings extends GuiScreen {

    private final GuiScreen parent;
    private PvPSettings settings;
    private int selectedCategory = 0;

    private static final String[] CATEGORIES = {"Knockback", "Sprint KB", "Hit Reg.", "Avanc\u00e9"};
    private static final int ACCENT = 0xFFCC2222;
    private static final int BG_DARK = 0xF0101018;
    private static final int BG_HEADER = 0xFF1A1A24;
    private static final int BORDER = 0x30FFFFFF;

    private static final int SW = 100;
    private static final int TW = 50;
    private static final int GAP = 10;

    private int panelX, panelY, panelW, panelH;
    private int catW, settingsX, settingsW;

    private final float[] catHover = new float[CATEGORIES.length];
    private float animation = 0.0f;
    private long lastTime = -1L;

    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int draggingSlider = -1;

    public GuiPvPSettings(GuiScreen parent) {
        this.parent = parent;
        this.settings = PvPSettings.get();
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.lastTime = Minecraft.getSystemTime();
        this.scrollOffset = 0;

        panelW = Math.min(420, this.width - 20);
        panelH = Math.min(300, this.height - 20);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
        catW = Math.max(80, panelW / 5);
        settingsX = panelX + catW + 1;
        settingsW = panelW - catW - 1;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = Minecraft.getSystemTime();
        if (lastTime > 0) {
            float dt = (now - lastTime) / 1000.0f;
            animation = MathHelper.clamp_float(animation + dt * 5.0f, 0.0f, 1.0f);
        }
        lastTime = now;

        float ease = animation * animation * (3.0f - 2.0f * animation);
        this.drawRect(0, 0, this.width, this.height, (int)(ease * 180) << 24);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, (1.0f - ease) * 8, 0);

        GuiRenderUtils.drawShadow(panelX, panelY, panelW, panelH, 10, (int)(ease * 120));
        Gui.drawRect(panelX, panelY, panelX + panelW, panelY + panelH, BG_DARK);
        Gui.drawRect(panelX, panelY, panelX + panelW, panelY + 1, ACCENT);
        Gui.drawRect(panelX, panelY + 1, panelX + panelW, panelY + 18, BG_HEADER);

        String title = "\u00A7c\u00A7lPVP \u00A7f\u00A7lCONFIG";
        fontRendererObj.drawStringWithShadow(title, panelX + panelW / 2 - fontRendererObj.getStringWidth(title) / 2, panelY + 5, 0xFFFFFFFF);

        // Reset button
        String resetText = "\u00A77[Reset]";
        int rtw = fontRendererObj.getStringWidth(resetText);
        int rtx = panelX + panelW - rtw - 4;
        boolean resetHover = mouseX >= rtx && mouseX < rtx + rtw && mouseY >= panelY + 3 && mouseY < panelY + 15;
        fontRendererObj.drawStringWithShadow(resetHover ? "\u00A7c[Reset]" : resetText, rtx, panelY + 5, 0xFFFFFFFF);

        GuiRenderUtils.drawRectOutline(panelX, panelY, panelW, panelH, BORDER);

        drawCategories(mouseX, mouseY);
        Gui.drawRect(panelX + catW, panelY + 18, panelX + catW + 1, panelY + panelH, 0x20FFFFFF);

        int clipTop = panelY + 20;
        int clipBottom = panelY + panelH - 2;

        ScaledResolution sr = new ScaledResolution(mc);
        int factor = sr.getScaleFactor();
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glScissor(settingsX * factor, (this.height - clipBottom) * factor, settingsW * factor, (clipBottom - clipTop) * factor);

        drawSettings(mouseX, mouseY);

        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);

        GlStateManager.popMatrix();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawCategories(int mx, int my) {
        int y = panelY + 20;
        int h = Math.min(22, (panelH - 22) / CATEGORIES.length);
        for (int i = 0; i < CATEGORIES.length; i++) {
            boolean hovered = mx >= panelX && mx < panelX + catW && my >= y && my < y + h;
            boolean selected = (i == selectedCategory);
            float target = (hovered || selected) ? 1.0f : 0.0f;
            catHover[i] += (target - catHover[i]) * 0.2f;
            float t = catHover[i];
            Gui.drawRect(panelX, y, panelX + catW, y + h, GuiRenderUtils.colorLerp(0x00000000, 0x30FFFFFF, t));
            if (selected) Gui.drawRect(panelX, y, panelX + 2, y + h, ACCENT);
            int textColor = selected ? 0xFFFFFFFF : GuiRenderUtils.colorLerp(0xFF888888, 0xFFFFFFFF, t);
            fontRendererObj.drawStringWithShadow(CATEGORIES[i], panelX + 6, y + (h - 8) / 2, textColor);
            y += h;
        }
    }

    private void drawSettings(int mx, int my) {
        int x = settingsX + 6;
        int y = panelY + 22;
        int w = settingsW - 12;
        int availH = panelH - 24;
        int currentY = y - scrollOffset;

        switch (selectedCategory) {
            case 0: currentY = drawKnockbackSettings(x, currentY, w, mx, my, y, y + availH); break;
            case 1: currentY = drawSprintKbSettings(x, currentY, w, mx, my, y, y + availH); break;
            case 2: currentY = drawHitRegSettings(x, currentY, w, mx, my, y, y + availH); break;
            case 3: currentY = drawAdvancedSettings(x, currentY, w, mx, my, y, y + availH); break;
        }
        maxScroll = Math.max(0, (currentY + scrollOffset) - (y + availH));
    }

    // ── Category: Knockback ─────────────────────────────────────────────

    private int drawKnockbackSettings(int x, int y, int w, int mx, int my, int ct, int cb) {
        y = drawSectionTitle(x, y, w, "\u00A7c\u00A7lKnockback de base", ct, cb);
        y = drawSlider(x, y, w, "Friction", (float)settings.kbFriction, 0.3f, 0.9f, mx, my, ct, cb);
        y = drawDesc(x, y, w, "\u00A78Vanilla=0.50 | Plus haut = KB plus consistant", ct, cb);
        y = drawSlider(x, y, w, "Force horizontale", (float)settings.kbHorizontal, 0.2f, 0.8f, mx, my, ct, cb);
        y = drawDesc(x, y, w, "\u00A78Vanilla=0.40 | Force du recul lat\u00e9ral", ct, cb);
        y = drawSlider(x, y, w, "Force verticale", (float)settings.kbVertical, 0.15f, 0.6f, mx, my, ct, cb);
        y = drawDesc(x, y, w, "\u00A78Vanilla=0.40 | Moins = moins floaty", ct, cb);
        y = drawSlider(x, y, w, "Cap vertical", (float)settings.kbVerticalCap, 0.2f, 0.6f, mx, my, ct, cb);
        y = drawDesc(x, y, w, "\u00A78Vanilla=0.40 | Limite haute du KB vertical", ct, cb);
        return y;
    }

    // ── Category: Sprint KB ─────────────────────────────────────────────

    private int drawSprintKbSettings(int x, int y, int w, int mx, int my, int ct, int cb) {
        y = drawSectionTitle(x, y, w, "\u00A7c\u00A7lSprint Knockback (W-tap)", ct, cb);
        y = drawSlider(x, y, w, "Force horizontale", (float)settings.sprintKbHorizontal, 0.3f, 1.0f, mx, my, ct, cb);
        y = drawDesc(x, y, w, "\u00A78Vanilla=0.50 | R\u00e9compense le W-tap", ct, cb);
        y = drawSlider(x, y, w, "Force verticale", (float)settings.sprintKbVertical, 0.0f, 0.3f, mx, my, ct, cb);
        y = drawDesc(x, y, w, "\u00A78Vanilla=0.10 | L\u00e9g\u00e8re pouss\u00e9e vers le haut", ct, cb);
        y = drawSlider(x, y, w, "Ralentissement attaquant", (float)settings.sprintAttackerSlowdown, 0.3f, 1.0f, mx, my, ct, cb);
        y = drawDesc(x, y, w, "\u00A78Vanilla=0.60 | 1.0 = aucun ralentissement", ct, cb);
        return y;
    }

    // ── Category: Hit Registration ──────────────────────────────────────

    private int drawHitRegSettings(int x, int y, int w, int mx, int my, int ct, int cb) {
        y = drawSectionTitle(x, y, w, "\u00A7c\u00A7lHit Registration / I-Frames", ct, cb);
        y = drawSlider(x, y, w, "I-Frames (ticks)", settings.maxHurtResistantTime, 14, 22, mx, my, ct, cb);
        y = drawDesc(x, y, w, "\u00A78Vanilla=20 | Hit effectif tous les " + (settings.maxHurtResistantTime / 2) + " ticks", ct, cb);
        y = drawSlider(x, y, w, "Cooldown miss (ticks)", settings.missCooldownTicks, 0, 10, mx, my, ct, cb);
        y = drawDesc(x, y, w, "\u00A78Vanilla=10 | 0 = pas de p\u00e9nalit\u00e9 quand on miss le vide", ct, cb);
        return y;
    }

    // ── Category: Advanced ──────────────────────────────────────────────

    private int drawAdvancedSettings(int x, int y, int w, int mx, int my, int ct, int cb) {
        y = drawSectionTitle(x, y, w, "\u00A7c\u00A7lParam\u00e8tres avanc\u00e9s", ct, cb);
        y = drawSlider(x, y, w, "Reach entit\u00e9s", (float)settings.entityReachDistance, 2.5f, 4.0f, mx, my, ct, cb);
        y = drawDesc(x, y, w, "\u00A78Vanilla=3.0 | Distance pour toucher un joueur", ct, cb);
        y = drawSlider(x, y, w, "Hitbox expansion", settings.collisionBorderSize, 0.0f, 0.3f, mx, my, ct, cb);
        y = drawDesc(x, y, w, "\u00A78Vanilla=0.10 | Plus bas = aim plus pr\u00e9cis requis", ct, cb);
        y = drawToggle(x, y, w, "Toujours KB vertical", settings.alwaysApplyVerticalKb, mx, my, ct, cb);
        y = drawDesc(x, y, w, "\u00A78Force la composante verticale m\u00eame au sol", ct, cb);
        y = drawSlider(x, y, w, "Multiplicateur crit", settings.critDamageMultiplier, 1.0f, 2.5f, mx, my, ct, cb);
        y = drawDesc(x, y, w, "\u00A78Vanilla=1.50 | Bonus de d\u00e9g\u00e2ts des coups critiques", ct, cb);

        y += 8;
        y = drawSectionTitle(x, y, w, "\u00A76\u00A7lValeurs recommand\u00e9es", ct, cb);
        y = drawDesc(x, y, w, "\u00A77KB Friction: 0.60 | Horizontal: 0.40 | Vertical: 0.36", ct, cb);
        y = drawDesc(x, y, w, "\u00A77Sprint KB H: 0.60 | Sprint KB V: 0.13", ct, cb);
        y = drawDesc(x, y, w, "\u00A77I-Frames: 19 | Miss cooldown: 0", ct, cb);
        y = drawDesc(x, y, w, "\u00A77Reach: 3.0 | Hitbox: 0.10", ct, cb);
        return y;
    }

    // ── Drawing helpers ─────────────────────────────────────────────────

    private int drawSectionTitle(int x, int y, int w, String title, int ct, int cb) {
        if (y + 14 < ct || y > cb) return y + 18;
        Gui.drawRect(x, y + 12, x + w, y + 13, 0x20FFFFFF);
        fontRendererObj.drawStringWithShadow(title, x, y + 2, 0xFFFFFFFF);
        return y + 18;
    }

    private int drawDesc(int x, int y, int w, String text, int ct, int cb) {
        if (y + 10 < ct || y > cb) return y + 11;
        fontRendererObj.drawStringWithShadow(text, x + 4, y + 1, 0xFF888888);
        return y + 11;
    }

    private int drawToggle(int x, int y, int w, String label, boolean value, int mx, int my, int ct, int cb) {
        if (y + 16 < ct || y > cb) return y + 16;
        fontRendererObj.drawStringWithShadow(label, x, y + 3, 0xFFCCCCCC);
        int tw = 22, th = 10;
        int tx = x + w - tw;
        int ty = y + 2;
        int bg = value ? 0xFF1A7A4A : 0xFF2A2A3A;
        Gui.drawRect(tx, ty, tx + tw, ty + th, bg);
        GuiRenderUtils.drawRectOutline(tx, ty, tw, th, 0x1AFFFFFF);
        int knobX = value ? tx + tw - th + 1 : tx + 1;
        Gui.drawRect(knobX, ty + 1, knobX + th - 2, ty + th - 1, 0xFFEEEEEE);
        return y + 16;
    }

    private int drawSlider(int x, int y, int w, String label, float value, float min, float max, int mx, int my, int ct, int cb) {
        if (y + 16 < ct || y > cb) return y + 16;
        fontRendererObj.drawStringWithShadow(label, x, y + 3, 0xFFCCCCCC);

        String valStr = (max >= 10 && min >= 1) ? String.valueOf((int) value) : String.format("%.2f", value);

        int sx = x + w - SW - TW - GAP;
        int sy = y + 7;

        int tx = x + w - TW;
        fontRendererObj.drawStringWithShadow(valStr, tx + (TW - fontRendererObj.getStringWidth(valStr)), y + 3, 0xFFFF8888);

        Gui.drawRect(sx, sy, sx + SW, sy + 3, 0xFF2A2A3A);
        float ratio = MathHelper.clamp_float((value - min) / (max - min), 0, 1);
        int filledW = (int)(SW * ratio);
        Gui.drawRect(sx, sy, sx + filledW, sy + 3, ACCENT);
        Gui.drawRect(sx + filledW - 2, sy - 1, sx + filledW + 2, sy + 4, 0xFFEEEEEE);
        return y + 16;
    }

    // ── Mouse handling ──────────────────────────────────────────────────

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn);
        if (btn != 0) return;

        // Reset
        int rtw = fontRendererObj.getStringWidth("\u00A77[Reset]");
        int rtx = panelX + panelW - rtw - 4;
        if (mx >= rtx && mx < rtx + rtw && my >= panelY + 3 && my < panelY + 15) {
            settings = new PvPSettings();
            settings.save();
            return;
        }

        // Categories
        int catY = panelY + 20;
        int catH = Math.min(22, (panelH - 22) / CATEGORIES.length);
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (mx >= panelX && mx < panelX + catW && my >= catY && my < catY + catH) {
                selectedCategory = i;
                scrollOffset = 0;
                return;
            }
            catY += catH;
        }

        handleSettingsClick(mx, my);
    }

    private void handleSettingsClick(int mx, int my) {
        int x = settingsX + 6;
        int w = settingsW - 12;
        int clipTop = panelY + 22;
        int clipBottom = panelY + panelH;
        if (my < clipTop || my > clipBottom) return;

        int baseY = panelY + 22 - scrollOffset;
        int relY = my - baseY;

        // Calculate the actual row accounting for section titles and descriptions
        // Each category has different row layouts due to section titles and desc lines
        int row = getRowForCategory(selectedCategory, relY);
        if (row < 0) return;

        // Handle toggles
        if (isOverToggle(mx, x, w)) {
            handleToggleClick(selectedCategory, row);
        }
    }

    private int getRowForCategory(int cat, int relY) {
        // Titles take 18px, descs take 11px, sliders/toggles take 16px
        // We need to figure out which interactive element the click landed on
        // For simplicity, we track the row index of interactive elements only
        return relY / 16; // Approximate - works well enough with the layout
    }

    private void handleToggleClick(int cat, int row) {
        if (cat == 3) {
            // Advanced: toggle is at row index after accounting for layout
            // Section title (18px) + reach slider (16px) + desc (11px) + hitbox slider (16px) + desc (11px) = 72px
            // Toggle starts at 72px offset from top
            int baseY = panelY + 22 - scrollOffset;
            int toggleY = baseY + 18 + 16 + 11 + 16 + 11; // After reach, hitbox, their descs
            // The toggle row
            settings.alwaysApplyVerticalKb = !settings.alwaysApplyVerticalKb;
            settings.save();
        }
    }

    private boolean isOverToggle(int mx, int x, int w) {
        int tw = 22;
        int tx = x + w - tw;
        return mx >= tx && mx < tx + tw;
    }

    @Override
    protected void mouseClickMove(int mx, int my, int btn, long timeSinceClick) {
        if (btn != 0) return;
        int x = settingsX + 6, w = settingsW - 12;
        int sx = x + w - SW - TW - GAP;

        if (draggingSlider == -1) {
            if (mx < sx) return;
            // Determine which slider we're dragging based on pixel position
            draggingSlider = getSliderAtPixelY(my);
        }

        if (draggingSlider >= 0) {
            float ratio = MathHelper.clamp_float((float) (mx - sx) / SW, 0.0f, 1.0f);
            applySliderValue(selectedCategory, draggingSlider, ratio);
        }
    }

    private int getSliderAtPixelY(int my) {
        int baseY = panelY + 22 - scrollOffset;
        int[] offsets = getSliderOffsets(selectedCategory);
        for (int i = 0; i < offsets.length; i++) {
            int sliderY = baseY + offsets[i];
            if (my >= sliderY && my < sliderY + 16) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the pixel Y offsets (from base) of each slider in the given category.
     * Must match the draw order in drawXxxSettings methods.
     */
    private int[] getSliderOffsets(int cat) {
        switch (cat) {
            case 0: // Knockback: title(18) + slider(16) + desc(11) repeats
                return new int[]{18, 18 + 16 + 11, 18 + (16 + 11) * 2, 18 + (16 + 11) * 3};
            case 1: // Sprint KB
                return new int[]{18, 18 + 16 + 11, 18 + (16 + 11) * 2};
            case 2: // Hit Reg
                return new int[]{18, 18 + 16 + 11};
            case 3: // Advanced: reach, hitbox, [toggle], crit
                return new int[]{18, 18 + 16 + 11, 18 + (16 + 11) * 2 + 16 + 11};
            default:
                return new int[0];
        }
    }

    private void applySliderValue(int cat, int sliderIndex, float ratio) {
        switch (cat) {
            case 0: // Knockback
                switch (sliderIndex) {
                    case 0: settings.kbFriction = 0.3 + ratio * 0.6; break;
                    case 1: settings.kbHorizontal = 0.2 + ratio * 0.6; break;
                    case 2: settings.kbVertical = 0.15 + ratio * 0.45; break;
                    case 3: settings.kbVerticalCap = 0.2 + ratio * 0.4; break;
                }
                break;
            case 1: // Sprint KB
                switch (sliderIndex) {
                    case 0: settings.sprintKbHorizontal = 0.3 + ratio * 0.7; break;
                    case 1: settings.sprintKbVertical = ratio * 0.3; break;
                    case 2: settings.sprintAttackerSlowdown = 0.3 + ratio * 0.7; break;
                }
                break;
            case 2: // Hit Reg
                switch (sliderIndex) {
                    case 0: settings.maxHurtResistantTime = 14 + (int)(ratio * 8); break;
                    case 1: settings.missCooldownTicks = (int)(ratio * 10); break;
                }
                break;
            case 3: // Advanced
                switch (sliderIndex) {
                    case 0: settings.entityReachDistance = 2.5 + ratio * 1.5; break;
                    case 1: settings.collisionBorderSize = ratio * 0.3f; break;
                    case 2: settings.critDamageMultiplier = 1.0f + ratio * 1.5f; break;
                }
                break;
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int btn) {
        super.mouseReleased(mx, my, btn);
        if (draggingSlider != -1) {
            settings.save();
            draggingSlider = -1;
        }
    }

    @Override
    protected void keyTyped(char c, int keyCode) throws IOException {
        if (keyCode == 1) {
            settings.save();
            this.mc.displayGuiScreen(parent);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) {
            scrollOffset = MathHelper.clamp_int(scrollOffset + (scroll > 0 ? -16 : 16), 0, maxScroll);
        }
    }

    @Override
    public void onGuiClosed() {
        settings.save();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
