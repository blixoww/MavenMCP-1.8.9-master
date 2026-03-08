import net.minecraft.client.main.Main;

import java.io.File;
import java.util.Arrays;

/**
 * Welcome to MCP 1.8.9 for Maven
 * This repository has been created to make working with MCP 1.8.9 easier and cleaner.
 * You can view the MCP 1.8.9 repo here: https://github.com/Marcelektro/MCP-919
 * If you have any questions regarding this, feel free to contact me here: https://marcloud.net/discord
 *
 * Have fun with the MC development ^^
 * Marcelektro
 */

public class Start {
    public static void main(String[] args) {
        // Cherche les natives à côté du JAR, sinon fallback test_natives
        File jarSideNatives = new File(
                new File(Start.class.getProtectionDomain()
                        .getCodeSource().getLocation().getPath())
                        .getParentFile(), "natives"
        );
        if (jarSideNatives.exists()) {
            System.setProperty("org.lwjgl.librarypath", jarSideNatives.getAbsolutePath());
        } else {
            System.setProperty("org.lwjgl.librarypath", new File("../test_natives/" +
                    (System.getProperty("os.name").startsWith("Windows") ? "windows" : "linux")
            ).getAbsolutePath());
        }

        // Le launcher fournit : --username, --uuid, --accessToken, --gameDir
        // Minecraft a besoin aussi de : --version, --assetsDir, --assetIndex
        Main.main(concat(new String[]{
                "--version",    "MavenMCP",
                "--userProperties", "{}"
        }, args));
    }

    public static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}