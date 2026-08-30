package b.bplugins.plugin.maps.command;

import java.util.ArrayList;
import java.util.List;

public final class PathUtil {

    private PathUtil() {
    }

    /**
     * Zerlegt einen Pfad wie "Sachsen-Anhalt/Salzlandkreis/Staßfurt" in seine
     * einzelnen Segmente. Führende/doppelte Slashes und Leerraum um die
     * Segmente werden toleriert.
     */
    public static List<String> split(String path) {
        List<String> segments = new ArrayList<>();
        for (String part : path.split("/")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                segments.add(trimmed);
            }
        }
        return segments;
    }

    public static String join(List<String> segments) {
        return String.join(" / ", segments);
    }
}