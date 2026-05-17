package net.minecraft.client.custompackets;

import net.minecraft.client.custompackets.data.FactionZoneTracker;
import net.minecraft.client.custompackets.handler.FactionDataCache;
import net.minecraft.client.custompackets.handler.HdvPacketHandler;
import net.minecraft.client.custompackets.handler.PlayerDataHandler;
import net.minecraft.client.custompackets.handler.ShopPacketHandler;
import net.minecraft.client.custompackets.handler.BoutiquePacketHandler;
import net.minecraft.client.visuals.ping.PingManager;
import net.minecraft.network.PacketBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Point d'entrée unique du système de packets custom.
 */
public final class CustomPacketSystem {

    private static final Logger LOGGER = LogManager.getLogger("CustomPackets");
    private static boolean initialized = false;

    private CustomPacketSystem() {}

    /**
     * Initialise le système en enregistrant tous les handlers de packets entrants.
     */
    public static void init() {
        if (initialized) return;
        initialized = true;

        LOGGER.info("[CustomPackets] ✓ Initialisation du système de packets custom...");

        HdvPacketHandler.registerHandlers();
        LOGGER.info("[CustomPackets] ✓ HdvPacketHandler enregistré");

        net.minecraft.client.custompackets.handler.TradePacketHandler.registerHandlers();
        LOGGER.info("[CustomPackets] ✓ TradePacketHandler enregistré");

        ShopPacketHandler.registerHandlers();
        PlayerDataHandler.registerHandlers();
        BoutiquePacketHandler.registerHandlers();
        LOGGER.info("[CustomPackets] ✓ BoutiquePacketHandler enregistré");

        // Système de ping : réception d'un ping S2C (faction/allié/ami proche)
        // Format : double x | double y | double z | String sender (max 64)
        PacketDispatcher.register(PacketChannel.PING_S2C, PacketId.PING_RECEIVE, buf -> {
            double x      = buf.readDouble();
            double y      = buf.readDouble();
            double z      = buf.readDouble();
            // nouveau format : un byte typeOrdinal avant le sender
            byte typeOrdinal = 0;
            try {
                typeOrdinal = buf.readByte();
            } catch (Exception ignored) {}
            String sender = buf.readStringFromBuffer(64);
            LOGGER.debug("[CustomPackets] Ping reçu de {} type {} @{},{},{}", sender, typeOrdinal, x, y, z);
            PingManager.INSTANCE.addRemotePing(x, y, z, sender, typeOrdinal);
        });
        // Appliquer les keybinds persistés maintenant que GameSettings est prêt
        PingManager.INSTANCE.applyStoredKeyBinding();
        LOGGER.info("[CustomPackets] ✓ PingSystem handler enregistré");

        // Données de faction – tag et relation des joueurs proches
        PacketDispatcher.register(PacketChannel.FACTION_S2C, PacketId.FACTION_DATA, buf -> {
            String playerName = buf.readStringFromBuffer(32);
            String factionTag = buf.readStringFromBuffer(32);
            int    relation   = buf.readByte() & 0xFF; // unsigned
            LOGGER.debug("[Faction] {} faction={} relation={}", playerName, factionTag, relation);
            FactionDataCache.update(playerName, factionTag, relation);
        });
        LOGGER.info("[CustomPackets] ✓ FactionData handler enregistré");

        // Zone courante – claim faction du chunk où se trouve le joueur
        // Format : String factionName | byte relation | String ownFaction
        PacketDispatcher.register(PacketChannel.FACTION_S2C, PacketId.FACTION_ZONE, buf -> {
            String name      = buf.readStringFromBuffer(64);
            int    relation  = buf.readByte() & 0xFF;
            String ownFaction = "";
            try { ownFaction = buf.readStringFromBuffer(64); } catch (Exception ignored) {}
            LOGGER.debug("[FactionZone] name='{}' relation={} own='{}'", name, relation, ownFaction);
            FactionZoneTracker.update(name, relation, ownFaction);
        });
        LOGGER.info("[CustomPackets] ✓ FactionZone handler enregistré");

        // Anonymat (commande /annonyme) – masquage côté client
        PacketDispatcher.register(PacketChannel.FACTION_S2C, PacketId.ANONYMOUS_STATUS, buf -> {
            final String playerName  = buf.readStringFromBuffer(32);
            boolean anon = false;
            try { anon = buf.readBoolean(); } catch (Exception ignored) {}
            net.minecraft.client.custompackets.handler.AnonymousCache.set(playerName, anon);
        });
        LOGGER.info("[CustomPackets] ✓ AnonymousStatus handler enregistré");

        // Profil joueur – PROFILE_OPEN (0x90) : propre profil depuis la BD
        PacketDispatcher.register(PacketChannel.PLAYER_DATA_S2C, PacketId.PROFILE_OPEN, buf -> {
            final String name    = buf.readStringFromBuffer(32);
            final String faction = buf.readStringFromBuffer(64);
            final String rank    = buf.readStringFromBuffer(32);
            final int    kills   = buf.readVarIntFromBuffer();
            final int    deaths  = buf.readVarIntFromBuffer();
            final int    ptMin   = buf.readVarIntFromBuffer();
            final long   balance = buf.readLong();
            final int    streak  = buf.readVarIntFromBuffer();
            final long   bounty  = buf.readLong();
            int factionRelation = 0; // propre profil → toujours own (vert)
            try { factionRelation = buf.readVarIntFromBuffer(); } catch (Exception ignored) {}
            // Garder l'ancien PB si le packet n'en envoie pas — sinon on écrase
            // la valeur poussée en continu par PLAYER_PB et l'affichage tombe à 0.
            net.minecraft.client.custompackets.data.PlayerData previous =
                net.minecraft.client.custompackets.handler.PlayerDataHandler.getCachedData();
            int pb = previous != null ? previous.getPb() : 0;
            boolean pbReceived = false;
            try { pb = buf.readVarIntFromBuffer(); pbReceived = true; } catch (Exception ignored) {}
            final int finalRel = factionRelation;
            final int finalPB  = pb;
            // Mettre à jour les caches avec les données fraîches du serveur pour
            // que GuiProfil(selfProfile=true) les lise correctement dès l'ouverture.
            net.minecraft.client.custompackets.data.PlayerData fresh =
                new net.minecraft.client.custompackets.data.PlayerData();
            fresh.setRank(rank);
            fresh.setBalance(balance);
            fresh.setKills(kills);
            fresh.setDeaths(deaths);
            fresh.setPlayTimeMinutes(ptMin);
            fresh.setPb(finalPB);
            net.minecraft.client.custompackets.handler.PlayerDataHandler.setCachedData(fresh);
            // Si le packet n'a pas fourni de PB, redemander au serveur pour
            // garantir un affichage à jour à la prochaine ouverture.
            if (!pbReceived) {
                net.minecraft.client.custompackets.handler.PlayerDataHandler.requestData();
            }
            net.minecraft.client.custompackets.data.KillstreakCache.setCount(streak);
            net.minecraft.client.custompackets.data.BountyCache.setSelfBounty(bounty);
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() ->
                net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(
                    new net.minecraft.client.gui.GuiProfil(
                        name, faction, rank, kills, deaths, ptMin, balance, streak, bounty, true, finalRel, finalPB)));
        });
        LOGGER.info("[CustomPackets] ✓ PROFILE_OPEN handler enregistré");

        // Profil joueur – PROFILE_DATA (0x91) : profil d'un autre joueur
        PacketDispatcher.register(PacketChannel.PLAYER_DATA_S2C, PacketId.PROFILE_DATA, buf -> {
            final String name    = buf.readStringFromBuffer(32);
            final String faction = buf.readStringFromBuffer(64);
            final String rank    = buf.readStringFromBuffer(32);
            final int    kills   = buf.readVarIntFromBuffer();
            final int    deaths  = buf.readVarIntFromBuffer();
            final int    ptMin   = buf.readVarIntFromBuffer();
            final long   balance = buf.readLong();
            final int    streak  = buf.readVarIntFromBuffer();
            final long   bounty  = buf.readLong();
            int factionRelation = 4; // neutre par défaut
            try { factionRelation = buf.readVarIntFromBuffer(); } catch (Exception ignored) {}
            int pb = 0;
            try { pb = buf.readVarIntFromBuffer(); } catch (Exception ignored) {}
            final int finalRel = factionRelation;
            final int finalPB  = pb;
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() ->
                net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(
                    new net.minecraft.client.gui.GuiProfil(
                        name, faction, rank, kills, deaths, ptMin, balance, streak, bounty, false, finalRel, finalPB)));
        });
        LOGGER.info("[CustomPackets] ✓ PROFILE_DATA handler enregistré");

        // Initialiser le combat log
        net.minecraft.client.combatlog.CombatLogManager.INSTANCE.init();
        LOGGER.info("[CustomPackets] ✓ CombatLogManager initialisé");

        PacketDispatcher.register(PacketChannel.SERVER_TO_CLIENT, PacketId.PONG, buf -> {
            long sentAt = buf.readLong();
            long latency = System.currentTimeMillis() - sentAt;
            LOGGER.debug("[CustomPackets] Pong reçu – latence applicative : {} ms", latency);
        });

        if (net.minecraft.client.Minecraft.getMinecraft().thePlayer != null) {
            sendRegisterPacket();
        }

        LOGGER.info("[CustomPackets] ✓ Système initialisé avec succès.");
    }

    /**
     * Envoie le packet "REGISTER" contenant la liste des canaux écoutés.
     */
    public static void sendRegisterPacket() {
        try {
            PacketBuffer buf = PacketSender.newBuffer();
            String channels = PacketChannel.HDV_S2C + "\0" + PacketChannel.HDV_C2S + "\0" +
                              PacketChannel.SHOP_S2C + "\0" + PacketChannel.SHOP_C2S + "\0" +
                              PacketChannel.PLAYER_DATA_S2C + "\0" + PacketChannel.PLAYER_DATA_C2S + "\0" +
                              PacketChannel.CLIENT_TO_SERVER + "\0" + PacketChannel.SERVER_TO_CLIENT + "\0" +
                              PacketChannel.PING_C2S + "\0" + PacketChannel.PING_S2C + "\0" +
                              PacketChannel.FACTION_S2C + "\0" +
                              PacketChannel.TRADE_S2C + "\0" + PacketChannel.TRADE_C2S + "\0" +
                              PacketChannel.BOUTIQUE_S2C + "\0" + PacketChannel.BOUTIQUE_C2S + "\0" +
                              PacketChannel.COMBATLOG;
            buf.writeBytes(channels.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            PacketSender.send("REGISTER", buf);
            LOGGER.info("[CustomPackets] ✓ Packet REGISTER envoyé pour : " + channels);
        } catch (Exception e) {
            LOGGER.error("[CustomPackets] Erreur envoi REGISTER", e);
        }
    }

    public static void sendPing() {
        PacketBuffer buf = PacketSender.newBuffer();
        buf.writeVarIntToBuffer(PacketId.PING);
        buf.writeLong(System.currentTimeMillis());
        PacketSender.send(PacketChannel.CLIENT_TO_SERVER, buf);
    }

    public static boolean isInitialized() { return initialized; }
}
