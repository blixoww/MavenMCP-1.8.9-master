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
    /** Forcer l'expiration d'un listing (admin) */
    public static final int HDV_ADMIN_EXPIRE        = 0x17;

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
    /** Notification temps-réel : un item du joueur vient d'être vendu */
    public static final int HDV_SOLD_NOTIFICATION = 0x26;


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
    /** Solde Points Boutique (PB) — int VarInt */
    public static final int PLAYER_PB        = 0x53;

    // ════════════════════════════════════════════════════════════════════════
    //  Données joueur – Client → Serveur
    // ════════════════════════════════════════════════════════════════════════

    public static final int PLAYER_DATA_REQUEST = 0x58;

    /**
     * Demande au serveur d'envoyer le profil complet du joueur local
     * (réponse attendue : {@link #PROFILE_OPEN} sur PLAYER_DATA_S2C).
     */
    public static final int PROFILE_REQUEST_OWN = 0x59;

    // ════════════════════════════════════════════════════════════════════════
    //  Items custom – Client → Serveur
    // ════════════════════════════════════════════════════════════════════════

    /** Drop d'un item custom (id 432-470) du client vers le serveur */
    public static final int CUSTOM_ITEM_DROP = 0x60;

    // ════════════════════════════════════════════════════════════════════════
    //  Système de Ping (style CS:GO)
    // ════════════════════════════════════════════════════════════════════════

    /** Client place un ping sur le serveur (x, y, z en double) */
    public static final int PING_PLACE   = 0x70;
    /** Serveur diffuse un ping reçu aux membres de la faction à portée */
    public static final int PING_RECEIVE = 0x71;

    // ════════════════════════════════════════════════════════════════════════
    //  Données de faction – Serveur → Client
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Infos de faction d'un joueur proche.
     * Format : String playerName | String factionTag | byte relation
     * relation : 0=own, 1=ally, 2=enemy, 3=neutral
     */
    public static final int FACTION_DATA = 0x80;

    /**
     * Zone actuelle du joueur (chunk claim).
     * Format : String factionName (vide = wilderness) | byte relation
     * relation : 0=own, 1=ally, 2=truce, 3=enemy, 4=neutral
     */
    public static final int FACTION_ZONE = 0x81;

    /**
     * État anonyme d'un joueur (par /annonyme).
     * Format : String playerName | boolean isAnonymous
     */
    public static final int ANONYMOUS_STATUS = 0x82;

    // ════════════════════════════════════════════════════════════════════════
    //  Profil joueur – Serveur → Client (canal PLAYER_DATA_S2C)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Ouvre le profil du joueur local (données fraîches depuis la BD).
     * Format : String name | String faction | String rank
     *        | VarInt kills | VarInt deaths | VarInt playTimeMin
     *        | long balance | VarInt streak | long bounty
     */
    public static final int PROFILE_OPEN = 0x90;

    /**
     * Ouvre le profil d'un autre joueur (demandé via /profil <joueur>).
     * Même format que PROFILE_OPEN.
     */
    public static final int PROFILE_DATA = 0x91;

    // ════════════════════════════════════════════════════════════════════════
    //  Trade – Serveur → Client  (canal TRADE_S2C)
    // ════════════════════════════════════════════════════════════════════════
    public static final int TRADE_OPEN   = 0xA0;
    public static final int TRADE_UPDATE = 0xA1;
    public static final int TRADE_CLOSE  = 0xA2;

    // ════════════════════════════════════════════════════════════════════════
    //  Trade – Client → Serveur  (canal TRADE_C2S)
    // ════════════════════════════════════════════════════════════════════════
    public static final int TRADE_OFFER   = 0xA0;
    public static final int TRADE_TAKE    = 0xA1;
    public static final int TRADE_CONFIRM = 0xA2;
    public static final int TRADE_CANCEL  = 0xA3;
    public static final int TRADE_MONEY   = 0xA4;
    /** C2S — montant de Points Boutique offert (long) */
    public static final int TRADE_PB      = 0xA5;

    // ════════════════════════════════════════════════════════════════════════
    //  Boutique PB – Serveur → Client  (canal BOUTIQUE_S2C)
    // ════════════════════════════════════════════════════════════════════════
    /**
     * Snapshot complet de la boutique : catégories (grades/commandes/spawners) +
     * offre spéciale active (peut être absente).
     * Format :
     *   String  titre
     *   long    balance (monnaie du joueur)
     *   VarInt  pb      (PB du joueur)
     *   VarInt  nbGrades       puis pour chaque : id, nom, [lines lore], prix_monnaie(long), prix_pb(int)
     *   VarInt  nbCommandes    idem + duree(int)
     *   VarInt  nbSpawners     idem + mob(String)
     *   bool    hasOffre
     *   (si hasOffre) ItemStack(via writeItemStackToBuffer), nom, [lore], prix_monnaie, prix_pb,
     *                  stock(VarInt), stockInitial(VarInt), expiresAt(long), id(String)
     */
    public static final int BOUTIQUE_DATA   = 0xB0;
    /** Résultat d'un achat : bool success | String message */
    public static final int BOUTIQUE_RESULT = 0xB1;

    // ════════════════════════════════════════════════════════════════════════
    //  Boutique PB – Client → Serveur  (canal BOUTIQUE_C2S)
    // ════════════════════════════════════════════════════════════════════════
    /** Demande de rafraîchissement de la snapshot boutique */
    public static final int BOUTIQUE_REQUEST = 0xB0;
    /**
     * Demande d'achat.
     * Format : byte categorie (0=grade, 1=cmd, 2=spawner, 3=offre)
     *          String id
     *          bool   payerEnPB
     */
    public static final int BOUTIQUE_BUY     = 0xB1;
}

