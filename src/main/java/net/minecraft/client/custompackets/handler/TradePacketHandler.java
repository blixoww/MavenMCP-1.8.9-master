package net.minecraft.client.custompackets.handler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.custompackets.PacketChannel;
import net.minecraft.client.custompackets.PacketDispatcher;
import net.minecraft.client.custompackets.PacketId;
import net.minecraft.client.custompackets.PacketSender;
import net.minecraft.client.gui.GuiTrade;
import net.minecraft.network.PacketBuffer;
import net.minecraft.item.ItemStack;

import io.netty.buffer.Unpooled;

public final class TradePacketHandler {

    // Trade state (authoritative from server)
    public static String  partnerName    = "";
    public static boolean isPlayerA      = true;
    public static ItemStack[] myOffer      = new ItemStack[15];
    public static ItemStack[] partnerOffer = new ItemStack[15];
    public static int     myOfferCount    = 0;
    public static int     partnerOfferCount = 0;
    public static boolean myConfirmed     = false;
    public static boolean partnerConfirmed = false;

    // ── Monnaie dans l'échange ────────────────────────────────────────────────
    /** Montant que j'offre (confirmé par le serveur). */
    public static long playerOfferedMoney  = 0L;
    /** Montant offert par le partenaire (lu depuis le serveur). */
    public static long partnerOfferedMoney = 0L;

    /** PB que j'offre. */
    public static int  playerOfferedPB     = 0;
    /** PB offerts par le partenaire. */
    public static int  partnerOfferedPB    = 0;

    private TradePacketHandler() {}

    public static void registerHandlers() {
        PacketDispatcher.register(PacketChannel.TRADE_S2C, PacketId.TRADE_OPEN, TradePacketHandler::handleOpen);
        PacketDispatcher.register(PacketChannel.TRADE_S2C, PacketId.TRADE_UPDATE, TradePacketHandler::handleUpdate);
        PacketDispatcher.register(PacketChannel.TRADE_S2C, PacketId.TRADE_CLOSE, TradePacketHandler::handleClose);
    }

    private static void handleOpen(PacketBuffer buf) {
        partnerName = buf.readStringFromBuffer(32);
        isPlayerA   = buf.readBoolean();
        resetState();
        Minecraft.getMinecraft().addScheduledTask(() ->
            Minecraft.getMinecraft().displayGuiScreen(new GuiTrade()));
    }

    private static void handleUpdate(PacketBuffer buf) {
        myConfirmed      = buf.readBoolean();
        partnerConfirmed = buf.readBoolean();

        myOfferCount = buf.readVarIntFromBuffer();
        myOffer = new ItemStack[myOfferCount];
        for (int i = 0; i < myOfferCount; i++) {
            myOffer[i] = readItem(buf);
        }

        partnerOfferCount = buf.readVarIntFromBuffer();
        partnerOffer = new ItemStack[partnerOfferCount];
        for (int i = 0; i < partnerOfferCount; i++) {
            partnerOffer[i] = readItem(buf);
        }

        // Monnaie (optionnel — backward-compatible si le serveur ne l'envoie pas encore)
        try {
            playerOfferedMoney  = buf.readLong();
            partnerOfferedMoney = buf.readLong();
        } catch (Exception ignored) {}

        // PB (optionnel — backward-compatible)
        try {
            playerOfferedPB  = buf.readVarIntFromBuffer();
            partnerOfferedPB = buf.readVarIntFromBuffer();
        } catch (Exception ignored) {}
    }

    private static void handleClose(PacketBuffer buf) {
        boolean success = buf.readBoolean();
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.currentScreen instanceof GuiTrade) {
                mc.displayGuiScreen(null);
            }
            if (success) {
                mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                    "§8[§c§lRedConflict§8] §r§aTrade effectué avec succès !"));
            } else {
                mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
                    "§8[§c§lRedConflict§8] §r§cTrade annulé."));
            }
            resetState();
        });
    }

    private static ItemStack readItem(PacketBuffer buf) {
        int len = buf.readVarIntFromBuffer();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        try {
            PacketBuffer itemBuf = new PacketBuffer(Unpooled.wrappedBuffer(bytes));
            return itemBuf.readItemStackFromBuffer();
        } catch (Exception e) {
            return null;
        }
    }

    private static void resetState() {
        myOffer = new ItemStack[15];
        partnerOffer = new ItemStack[15];
        myOfferCount = 0;
        partnerOfferCount = 0;
        myConfirmed = false;
        partnerConfirmed = false;
        playerOfferedMoney  = 0L;
        partnerOfferedMoney = 0L;
        playerOfferedPB     = 0;
        partnerOfferedPB    = 0;
    }

    // ── C2S helpers (called from GuiTrade) ──────────────────────────────────

    public static void sendOffer(int invSlot) {
        PacketBuffer buf = PacketSender.newBuffer();
        buf.writeVarIntToBuffer(PacketId.TRADE_OFFER);
        buf.writeVarIntToBuffer(invSlot);
        PacketSender.send(PacketChannel.TRADE_C2S, buf);
    }

    public static void sendTake(int offerIndex) {
        PacketBuffer buf = PacketSender.newBuffer();
        buf.writeVarIntToBuffer(PacketId.TRADE_TAKE);
        buf.writeVarIntToBuffer(offerIndex);
        PacketSender.send(PacketChannel.TRADE_C2S, buf);
    }

    public static void sendConfirm() {
        PacketSender.sendSimple(PacketChannel.TRADE_C2S, PacketId.TRADE_CONFIRM);
    }

    public static void sendCancel() {
        PacketSender.sendSimple(PacketChannel.TRADE_C2S, PacketId.TRADE_CANCEL);
    }

    /** Envoie le montant de monnaie offert au serveur. */
    public static void sendMoneyOffer(long amount) {
        PacketBuffer buf = PacketSender.newBuffer();
        buf.writeVarIntToBuffer(PacketId.TRADE_MONEY);
        buf.writeLong(Math.max(0L, amount));
        PacketSender.send(PacketChannel.TRADE_C2S, buf);
    }

    /** Envoie le montant de PB offert au serveur. */
    public static void sendPBOffer(int amount) {
        PacketBuffer buf = PacketSender.newBuffer();
        buf.writeVarIntToBuffer(PacketId.TRADE_PB);
        buf.writeVarIntToBuffer(Math.max(0, amount));
        PacketSender.send(PacketChannel.TRADE_C2S, buf);
    }
}
