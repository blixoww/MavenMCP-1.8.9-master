package net.minecraft.client.macro;

import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestionnaire de macros — jusqu'à 10 macros, chacune associée à une touche/bouton souris
 * et à une commande texte. Les macros sont sauvegardées dans macros.txt.
 */
public class MacroManager {
    public static final int MAX_MACROS = 10;
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Singleton
     */
    public static final MacroManager INSTANCE = new MacroManager();

    private final List<Macro> macros = new ArrayList<Macro>();
    private File macrosFile;

    // ── Initialisation ──────────────────────────────────────────────────────────

    public void init(File gameDir) {
        this.macrosFile = new File(gameDir, "macros.txt");
        this.load();
    }

    // ── Accesseurs ──────────────────────────────────────────────────────────────

    public List<Macro> getMacros() {
        return macros;
    }

    public int getCount() {
        return macros.size();
    }

    public boolean canAddMacro() {
        return macros.size() < MAX_MACROS;
    }

    public void addMacro(Macro m) {
        if (macros.size() < MAX_MACROS) {
            macros.add(m);
            save();
        }
    }

    public void removeMacro(int index) {
        if (index >= 0 && index < macros.size()) {
            macros.remove(index);
            save();
        }
    }

    public void setMacro(int index, Macro m) {
        if (index >= 0 && index < macros.size()) {
            macros.set(index, m);
            save();
        }
    }

    // ── Exécution ───────────────────────────────────────────────────────────────

    /**
     * Appelé à chaque tick côté client (hors GUI) pour détecter les pressions de touches.
     */
    public void onTick(Minecraft mc) {
        if (mc.thePlayer == null || mc.currentScreen != null) return;

        for (Macro macro : macros) {
            int keyCode = macro.getKeyCode();
            if (keyCode == 0) continue;

            boolean pressed;
            if (keyCode <= -100) {
                // Convention GameSettings : keyCode = mouseButton - 100  (ex: middle=2 -> -98)
                int btn = keyCode + 100; // -98 -> 2
                pressed = Mouse.isButtonDown(btn);
            } else if (keyCode < 0) {
                // Legacy MacroManager convention : keyCode = -(button+1) (ex: button0 -> -1)
                int btn = -(keyCode + 1);
                pressed = Mouse.isButtonDown(btn);
            } else {
                pressed = Keyboard.isKeyDown(keyCode);
            }

            if (pressed && !macro.isDown()) {
                macro.setDown(true);
                executeMacro(mc, macro);
            } else if (!pressed) {
                macro.setDown(false);
            }
        }
    }

    private void executeMacro(Minecraft mc, Macro macro) {
        String cmd = macro.getCommand().trim();
        if (cmd.isEmpty()) return;

        try {
            if (cmd.startsWith("/")) {
                mc.thePlayer.sendChatMessage(cmd);
            } else {
                mc.thePlayer.sendChatMessage(cmd);
            }
        } catch (Exception e) {
            LOGGER.warn("Erreur lors de l'exécution de la macro : " + cmd, e);
        }
    }

    // ── Persistance ─────────────────────────────────────────────────────────────

    public void save() {
        if (macrosFile == null) return;
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(macrosFile));
            for (Macro m : macros) {
                // Format : keyCode|command
                pw.println(m.getKeyCode() + "|" + m.getCommand().replace("\n", "\\n"));
            }
            pw.close();
        } catch (Exception e) {
            LOGGER.error("Impossible de sauvegarder les macros", e);
        }
    }

    public void load() {
        macros.clear();
        if (macrosFile == null || !macrosFile.exists()) return;
        try {
            BufferedReader br = new BufferedReader(new FileReader(macrosFile));
            String line;
            while ((line = br.readLine()) != null && macros.size() < MAX_MACROS) {
                int sep = line.indexOf('|');
                if (sep < 0) continue;
                try {
                    int keyCode = Integer.parseInt(line.substring(0, sep));
                    String cmd = line.substring(sep + 1).replace("\\n", "\n");
                    macros.add(new Macro(keyCode, cmd));
                } catch (NumberFormatException ignore) {
                }
            }
            br.close();
        } catch (Exception e) {
            LOGGER.error("Impossible de charger les macros", e);
        }
    }

    // ── Classe interne Macro ────────────────────────────────────────────────────

    public static class Macro {
        private int keyCode;   // 0 = non assigné, <= -100 = GameSettings mouse convention, <0 = legacy macro convention, >0 = clavier
        private String command;
        private transient boolean down; // état courant (non sauvegardé)

        public Macro(int keyCode, String command) {
            this.keyCode = keyCode;
            this.command = command;
        }

        public int getKeyCode() {
            return keyCode;
        }

        public void setKeyCode(int k) {
            this.keyCode = k;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String c) {
            this.command = c;
        }

        public boolean isDown() {
            return down;
        }

        public void setDown(boolean d) {
            this.down = d;
        }

        /**
         * Affichage lisible de la touche (compatible avec plusieurs encodages souris).
         */
        public String getKeyDisplayString() {
            if (keyCode == 0) return "---";
            if (keyCode <= -100) {
                // GameSettings convention: keyCode = mouseButton - 100
                int btn = keyCode + 100; // -98 -> 2
                switch (btn) {
                    case 0: return "Clic Gauche";
                    case 1: return "Clic Droit";
                    case 2: return "Clic Molette";
                    default: return "Souris " + btn;
                }
            }
            if (keyCode < 0) {
                // Legacy macro convention: keyCode = -(button+1)
                int btn = -(keyCode + 1);
                return "Souris " + (btn + 1);
            }
            try {
                String name = Keyboard.getKeyName(keyCode);
                return (name != null && !name.isEmpty()) ? name : ("Touche " + keyCode);
            } catch (Exception e) {
                return "Touche " + keyCode;
            }
        }
    }
}

