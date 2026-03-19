package net.minecraft.client.custompackets;

/**
 * Identifiants numériques des packets échangés sur chaque canal.
 *
 * Côté plugin Spigot, ces mêmes constantes doivent être référencées
 * pour encoder/décoder les payloads de façon identique.
 *
 * Chaque payload commence par un VarInt = PacketId.
 */
public final class PacketId {

    private PacketId() {}

    // ════════════════════════════════════════════════════════════════════════
    //  Canal principal (C2S / S2C)
    // ════════════════════════════════════════════════════════════════════════

    /** Client demande un ping applicatif */
    public static final int PING = 0x00;
    /** Serveur répond au ping */
    public static final int PONG = 0x01;

    // ════════════════════════════════════════════════════════════════════════
    //  HDV – Client → Serveur
    // ════════════════════════════════════════════════════════════════════════

    /** Demander la liste des offres (page, filtres) */
    public static final int HDV_LIST_REQUEST  = 0x10;
    /** Chercher une offre précise (id listing) */
    public static final int HDV_DETAIL_REQUEST = 0x11;
    /** Placer une offre de vente */
    public static final int HDV_POST_OFFER    = 0x12;
    /** Annuler une offre propre */
    public static final int HDV_CANCEL_OFFER  = 0x13;
    /** Acheter un item listé */
    public static final int HDV_BUY_ITEM      = 0x14;
    /** Récupérer les gains en attente */
    public static final int HDV_COLLECT       = 0x15;
    /** Demander ses propres annonces (actives + vendues) */
    public static final int HDV_MY_LISTINGS_REQUEST = 0x16;

    // ════════════════════════════════════════════════════════════════════════
    //  HDV – Serveur → Client
    // ════════════════════════════════════════════════════════════════════════

    /** Réponse liste des offres */
    public static final int HDV_LIST_RESPONSE  = 0x20;
    /** Réponse détail d'une offre */
    public static final int HDV_DETAIL_RESPONSE = 0x21;
    /** Confirmation / erreur d'une action HDV */
    public static final int HDV_ACTION_RESULT  = 0x22;
    /** Mise à jour en temps réel d'un listing (prix, stock) */
    public static final int HDV_LISTING_UPDATE = 0x23;
    /** Réponse mes annonces (actives + vendues) */
    public static final int HDV_MY_LISTINGS_RESPONSE = 0x24;
    /** Demande au client d'ouvrir l'interface HDV */
    public static final int HDV_OPEN = 0x25;


    // ════════════════════════════════════════════════════════════════════════
    //  Shop – Client → Serveur
    // ════════════════════════════════════════════════════════════════════════

    public static final int SHOP_CATEGORIES_REQUEST = 0x30;
    public static final int SHOP_ITEMS_REQUEST      = 0x31;
    public static final int SHOP_BUY                = 0x32;
    public static final int SHOP_SELL               = 0x33;
    /** Vendre tout (qty = -1 dans SHOP_SELL, alias lisible) */
    public static final int SHOP_SELL_ALL           = 0x33;
    public static final int SHOP_ITEM_DETAIL_REQUEST = 0x34;

    // ════════════════════════════════════════════════════════════════════════
    //  Shop – Serveur → Client
    // ════════════════════════════════════════════════════════════════════════

    public static final int SHOP_CATEGORIES_RESPONSE = 0x40;
    public static final int SHOP_ITEMS_RESPONSE      = 0x41;
    public static final int SHOP_TRANSACTION_RESULT  = 0x42;
    public static final int SHOP_MARKET_STATS        = 0x43;
    /** Serveur demande au client d'ouvrir le GUI shop */
    public static final int SHOP_OPEN                = 0x44;

    // ════════════════════════════════════════════════════════════════════════
    //  Données joueur – Serveur → Client
    // ════════════════════════════════════════════════════════════════════════

    /** Solde de la monnaie virtuelle */
    public static final int PLAYER_BALANCE   = 0x50;
    /** Rang du joueur */
    public static final int PLAYER_RANK      = 0x51;
    /** Stats personnalisées (kills, deaths…) */
    public static final int PLAYER_STATS     = 0x52;

    // ════════════════════════════════════════════════════════════════════════
    //  Données joueur – Client → Serveur
    // ════════════════════════════════════════════════════════════════════════

    public static final int PLAYER_DATA_REQUEST = 0x58;

    // ════════════════════════════════════════════════════════════════════════
    //  Items custom – Client → Serveur
    // ════════════════════════════════════════════════════════════════════════

    /** Drop d'un item custom (id 432-470) du client vers le serveur */
    public static final int CUSTOM_ITEM_DROP = 0x60;
}

