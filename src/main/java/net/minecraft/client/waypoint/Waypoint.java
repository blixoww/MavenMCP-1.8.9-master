package net.minecraft.client.waypoint;

/**
 * Représente un waypoint dans le monde.
 */
public class Waypoint {

    /**
     * Taille du texte des coordonnées affiché en jeu.
     * SMALL  = petit (lisible de près, discret de loin)
     * MEDIUM = intermédiaire (valeur par défaut)
     * LARGE  = grand (lisible de loin)
     */
    public enum TextSize {
        SMALL(0.6F, "Petit"),
        MEDIUM(1.0F, "Moyen"),
        LARGE(1.6F, "Grand");

        /** Multiplicateur appliqué au scale de base du label */
        public final float multiplier;
        public final String label;

        TextSize(float multiplier, String label) {
            this.multiplier = multiplier;
            this.label = label;
        }

        public TextSize next() {
            TextSize[] vals = values();
            return vals[(ordinal() + 1) % vals.length];
        }
    }

    private String name;
    private int x, y, z;
    private int colorR, colorG, colorB;
    private boolean beamVisible;
    private boolean coordsVisible;
    private boolean enabled;
    private TextSize textSize = TextSize.MEDIUM;

    public Waypoint(String name, int x, int y, int z, int r, int g, int b) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.colorR = r;
        this.colorG = g;
        this.colorB = b;
        this.beamVisible = true;
        this.coordsVisible = true;
        this.enabled = true;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public int getZ() { return z; }
    public void setZ(int z) { this.z = z; }

    public int getColorR() { return colorR; }
    public int getColorG() { return colorG; }
    public int getColorB() { return colorB; }

    public void setColor(int r, int g, int b) {
        this.colorR = r;
        this.colorG = g;
        this.colorB = b;
    }

    public float getColorRf() { return colorR / 255.0F; }
    public float getColorGf() { return colorG / 255.0F; }
    public float getColorBf() { return colorB / 255.0F; }

    public boolean isBeamVisible() { return beamVisible; }
    public void setBeamVisible(boolean beamVisible) { this.beamVisible = beamVisible; }

    public boolean isCoordsVisible() { return coordsVisible; }
    public void setCoordsVisible(boolean coordsVisible) { this.coordsVisible = coordsVisible; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public TextSize getTextSize() { return textSize; }
    public void setTextSize(TextSize textSize) { this.textSize = textSize; }
}

