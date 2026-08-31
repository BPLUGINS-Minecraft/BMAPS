package b.bplugins.plugin.maps.bedrock;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

public final class FloodgateSupport {

    private static Boolean available;

    private FloodgateSupport() {
    }

    public static boolean isAvailable() {
        if (available == null) {
            available = Bukkit.getPluginManager().getPlugin("floodgate") != null;
        }
        return available;
    }

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