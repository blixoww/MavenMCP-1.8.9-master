package net.minecraft.client.custompackets.data;

/**
 * Stocke les informations de la zone (claim) dans laquelle se trouve le joueur local.
 *
 * <ul>
 *   <li>relation -1 = inconnu / pas encore reçu</li>
 *   <li>relation  0 = même faction (own)  → vert</li>
 *   <li>relation  1 = allié (ally)        → violet</li>
 *   <li>relation  2 = trêve (truce)       → violet</li>
 *   <li>relation  3 = ennemi (enemy)      → rouge</li>
 *   <li>relation  4 = neutre (neutral)    → blanc</li>
 * </ul>
 *
 * Mis à jour par {@link net.minecraft.client.custompackets.CustomPacketSystem}
 * via le packet {@code FACTION_ZONE (0x81)} sur le canal {@code CUSTOM:FACTION_S2C}.
 */
public final class FactionZoneTracker {

    /** Nom de la faction owner du chunk courant ; chaîne vide = wilderness (pas de claim). */
    private static volatile String factionName    = "";
    /** Relation du joueur local envers cette faction (-1 = inconnu). */
    private static volatile int    relation       = -1;
    /** Tag de la propre faction du joueur local ; chaîne vide = sans faction. */
    private static volatile String ownFactionName = "";

    private FactionZoneTracker() {}

    /**
     * Appelé par le handler packet quand le serveur envoie une mise à jour.
     *
     * @param name       nom de la faction du claim (chaîne vide si wilderness)
     * @param rel        relation (0-4)
     * @param ownFaction tag de la propre faction du joueur (chaîne vide si sans faction)
     */
    public static void update(String name, int rel, String ownFaction) {
        factionName    = (name == null)      ? "" : name;
        relation       = rel;
        ownFactionName = (ownFaction == null) ? "" : ownFaction;
    }

    /** Réinitialise à l'état "inconnu" — à appeler à la déconnexion. */
    public static void clear() {
        factionName    = "";
        relation       = -1;
        ownFactionName = "";
    }

    /** Nom de la faction propriétaire du chunk courant. */
    public static String getFactionName() { return factionName; }

    /** Relation du joueur vis-à-vis de cette faction. */
    public static int getRelation() { return relation; }

    /** Tag de la propre faction du joueur local (vide = sans faction). */
    public static String getOwnFactionName() { return ownFactionName; }

    /** {@code true} si le chunk courant est réclamé par une faction. */
    public static boolean isClaimed() {
        return factionName != null && !factionName.isEmpty();
    }
}
