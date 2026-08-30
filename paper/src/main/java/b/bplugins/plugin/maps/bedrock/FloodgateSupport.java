package b.bplugins.plugin.maps.bedrock;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

/**
 * Kapselt den kompletten Zugriff auf die Floodgate-API an einer Stelle.
 * Floodgate ist ein SOFT-DEPEND (siehe paper-plugin.yml: required: false) -
 * das Plugin muss auch ohne installiertes Floodgate sauber starten.
 *
 * Wichtig: Klassen aus org.geysermc.floodgate dürfen NUR innerhalb dieser
 * Klasse referenziert werden. Die JVM lädt/verifiziert eine Klasse erst,
 * wenn sie tatsächlich gebraucht wird - solange BMapsPlugin/BMapsCommand
 * also nur isAvailable()/isBedrockPlayer() aufrufen (statt direkt
 * FloodgateApi zu importieren), crasht nichts, wenn Floodgate fehlt.
 */
public final class FloodgateSupport {

    private static Boolean available;

    private FloodgateSupport() {
    }

    /**
     * true, wenn das Floodgate-Plugin auf diesem Server installiert und aktiv ist.
     * Ergebnis wird gecacht (ändert sich zur Laufzeit nicht).
     */
    public static boolean isAvailable() {
        if (available == null) {
            available = Bukkit.getPluginManager().getPlugin("floodgate") != null;
        }
        return available;
    }

    /**
     * true, wenn der Spieler über Floodgate/Geyser verbunden ist (Bedrock-Client).
     * Gibt IMMER false zurück, falls Floodgate gar nicht installiert ist -
     * safe aufzurufen, ohne vorher isAvailable() zu prüfen.
     */
    public static boolean isBedrockPlayer(Player player) {
        if (!isAvailable()) {
            return false;
        }
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (Throwable t) {
            // Defensive: falls Floodgate installiert, aber inkompatible Version -
            // lieber als Java-Spieler behandeln statt den Server crashen zu lassen.
            return false;
        }
    }
}