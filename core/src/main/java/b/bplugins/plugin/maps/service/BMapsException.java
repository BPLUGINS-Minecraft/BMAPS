package b.bplugins.plugin.maps.service;

/**
 * Fachliche Fehler aus BMapsService (falscher Pfad, Kollision, Zyklus, etc.).
 * Die Nachricht ist bewusst bereits nutzerfreundlich formuliert (ohne
 * Farbcodes - die hängt jede Plattform selbst dran), damit sie 1:1 an
 * Spieler/Konsole weitergereicht werden kann.
 */
public final class BMapsException extends Exception {

    public BMapsException(String message) {
        super(message);
    }
}