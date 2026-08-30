package b.bplugins.plugin.maps.command;

import b.bplugins.plugin.maps.BMapsPlugin;
import b.bplugins.plugin.maps.bedrock.BedrockMenu;
import b.bplugins.plugin.maps.bedrock.FloodgateSupport;
import b.bplugins.plugin.maps.gui.NavigationMenu;
import b.bplugins.plugin.maps.model.MenuNode;
import b.bplugins.plugin.maps.model.WarpLocation;
import b.bplugins.plugin.maps.service.BMapsException;
import b.bplugins.plugin.maps.service.BMapsService;
import b.bplugins.plugin.maps.service.RemoveResult;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;

/**
 * Baut den kompletten Command-Baum für BMAPS über Paper's Brigadier-API.
 *
 * Diese Klasse macht bewusst NUR drei Dinge: Argumente parsen, den
 * plattformunabhängigen BMapsService aufrufen und Ergebnisse/Fehler in
 * Chat-Nachrichten übersetzen. Die eigentliche Validierung (leere Pfade,
 * Zyklen, Kollisionen ...) steckt im core-Modul, damit sie bei fabric/
 * velocity nicht erneut geschrieben werden muss.
 */
public final class BMapsCommand {

    private static final String ADMIN_PERMISSION = "bmaps.admin";

    private BMapsCommand() {
    }

    public static LiteralCommandNode<CommandSourceStack> create(BMapsPlugin plugin) {
        BMapsService service = plugin.getService();

        return Commands.literal("bmaps")
                .executes(ctx -> openMenu(plugin, ctx.getSource()))

                .then(Commands.literal("addcategory")
                        .requires(source -> source.getSender().hasPermission(ADMIN_PERMISSION))
                        .then(Commands.argument("path", StringArgumentType.greedyString())
                                .executes(ctx -> addCategory(plugin, service, ctx.getSource(),
                                        StringArgumentType.getString(ctx, "path")))))

                .then(Commands.literal("addwarp")
                        .requires(source -> source.getSender().hasPermission(ADMIN_PERMISSION))
                        .then(Commands.argument("path", StringArgumentType.greedyString())
                                .executes(ctx -> addWarp(plugin, service, ctx.getSource(),
                                        StringArgumentType.getString(ctx, "path")))))

                .then(Commands.literal("removenode")
                        .requires(source -> source.getSender().hasPermission(ADMIN_PERMISSION))
                        .then(Commands.argument("path", StringArgumentType.greedyString())
                                .executes(ctx -> removeNode(plugin, service, ctx.getSource(),
                                        StringArgumentType.getString(ctx, "path")))))

                .then(Commands.literal("updatecategory")
                        .requires(source -> source.getSender().hasPermission(ADMIN_PERMISSION))
                        .then(Commands.argument("path", StringArgumentType.greedyString())
                                .executes(ctx -> updateIcon(plugin, service, ctx.getSource(),
                                        StringArgumentType.getString(ctx, "path"), "CATEGORY"))))

                .then(Commands.literal("updatewarp")
                        .requires(source -> source.getSender().hasPermission(ADMIN_PERMISSION))
                        .then(Commands.argument("path", StringArgumentType.greedyString())
                                .executes(ctx -> updateIcon(plugin, service, ctx.getSource(),
                                        StringArgumentType.getString(ctx, "path"), "WARP"))))

                .then(Commands.literal("setdescription")
                        .requires(source -> source.getSender().hasPermission(ADMIN_PERMISSION))
                        .then(Commands.argument("path", StringArgumentType.string())
                                .then(Commands.argument("description", StringArgumentType.greedyString())
                                        .executes(ctx -> setDescription(plugin, service, ctx.getSource(),
                                                StringArgumentType.getString(ctx, "path"),
                                                StringArgumentType.getString(ctx, "description"))))))

                .then(Commands.literal("setpermission")
                        .requires(source -> source.getSender().hasPermission(ADMIN_PERMISSION))
                        .then(Commands.argument("path", StringArgumentType.string())
                                .then(Commands.argument("permission", StringArgumentType.greedyString())
                                        .executes(ctx -> setPermission(plugin, service, ctx.getSource(),
                                                StringArgumentType.getString(ctx, "path"),
                                                StringArgumentType.getString(ctx, "permission"))))))

                .then(Commands.literal("move")
                        .requires(source -> source.getSender().hasPermission(ADMIN_PERMISSION))
                        .then(Commands.argument("from", StringArgumentType.string())
                                .then(Commands.argument("to", StringArgumentType.greedyString())
                                        .executes(ctx -> moveNode(plugin, service, ctx.getSource(),
                                                StringArgumentType.getString(ctx, "from"),
                                                StringArgumentType.getString(ctx, "to"))))))

                .then(Commands.literal("reload")
                        .requires(source -> source.getSender().hasPermission(ADMIN_PERMISSION))
                        .executes(ctx -> reload(plugin, ctx.getSource())))

                .build();
    }

    // -----------------------------------------------------------------

    private static int openMenu(BMapsPlugin plugin, CommandSourceStack source) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage("§cDieser Befehl kann nur von Spielern genutzt werden.");
            return Command.SINGLE_SUCCESS;
        }

        if (FloodgateSupport.isBedrockPlayer(player)) {
            BedrockMenu.open(player, plugin.getNavigationRoot(), List.of(), 0);
        } else {
            NavigationMenu.openRoot(player, plugin.getNavigationRoot());
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int addCategory(BMapsPlugin plugin, BMapsService service, CommandSourceStack source, String rawPath) {
        List<String> segments = PathUtil.split(rawPath);
        String icon = resolveHeldIconKey(source, MenuNode.DEFAULT_CATEGORY_ICON);

        runAndReport(plugin, source, () -> {
            String path = service.addCategoryPath(segments, icon);
            return "§aKategorie-Pfad §e" + path + " §aangelegt/aktualisiert.";
        });
        return Command.SINGLE_SUCCESS;
    }

    private static int addWarp(BMapsPlugin plugin, BMapsService service, CommandSourceStack source, String rawPath) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage("§cDieser Befehl kann nur von Spielern genutzt werden.");
            return Command.SINGLE_SUCCESS;
        }

        List<String> segments = PathUtil.split(rawPath);
        String icon = resolveHeldIconKey(source, MenuNode.DEFAULT_WARP_ICON);
        Location loc = player.getLocation();
        WarpLocation warpLocation = new WarpLocation(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());

        runAndReport(plugin, source, () -> {
            String path = service.addWarp(segments, icon, warpLocation);
            return "§aWarp §e" + path + " §aan deiner Position angelegt.";
        });
        return Command.SINGLE_SUCCESS;
    }

    private static int updateIcon(BMapsPlugin plugin, BMapsService service, CommandSourceStack source, String rawPath, String expectedType) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage("§cDieser Befehl kann nur von Spielern genutzt werden (Icon = Item in der Hand).");
            return Command.SINGLE_SUCCESS;
        }

        List<String> segments = PathUtil.split(rawPath);
        Material held = player.getInventory().getItemInMainHand().getType();
        if (held == Material.AIR) {
            player.sendMessage("§cHalte das gewünschte Icon-Item in der Hand und führe den Befehl erneut aus.");
            return Command.SINGLE_SUCCESS;
        }

        runAndReport(plugin, source, () -> {
            String path = service.updateIcon(segments, expectedType, held.name());
            return "§aIcon von §e" + path + " §aauf §e" + held.name() + " §aaktualisiert.";
        });
        return Command.SINGLE_SUCCESS;
    }

    private static int setDescription(BMapsPlugin plugin, BMapsService service, CommandSourceStack source, String rawPath, String description) {
        List<String> segments = PathUtil.split(rawPath);

        runAndReport(plugin, source, () -> {
            String path = service.setDescription(segments, description);
            boolean cleared = description.equals("-") || description.equalsIgnoreCase("clear");
            return cleared
                    ? "§aBeschreibung von §e" + path + " §aentfernt."
                    : "§aBeschreibung von §e" + path + " §aaktualisiert.";
        });
        return Command.SINGLE_SUCCESS;
    }

    private static int setPermission(BMapsPlugin plugin, BMapsService service, CommandSourceStack source, String rawPath, String permission) {
        List<String> segments = PathUtil.split(rawPath);

        runAndReport(plugin, source, () -> {
            String path = service.setPermission(segments, permission);
            boolean cleared = permission.equals("-") || permission.equalsIgnoreCase("clear");
            return cleared
                    ? "§a" + path + " §aist jetzt wieder für alle sichtbar."
                    : "§a" + path + " §aerfordert jetzt die Permission §e" + permission + "§a.";
        });
        return Command.SINGLE_SUCCESS;
    }

    private static int moveNode(BMapsPlugin plugin, BMapsService service, CommandSourceStack source, String rawFrom, String rawTo) {
        List<String> fromSegments = PathUtil.split(rawFrom);
        List<String> toSegments = PathUtil.split(rawTo);

        runAndReport(plugin, source, () -> {
            String fromLabel = PathUtil.join(fromSegments);
            String toPath = service.move(fromSegments, toSegments);
            return "§a" + fromLabel + " §anach §e" + toPath + " §averschoben.";
        });
        return Command.SINGLE_SUCCESS;
    }

    private static int removeNode(BMapsPlugin plugin, BMapsService service, CommandSourceStack source, String rawPath) {
        List<String> segments = PathUtil.split(rawPath);

        runAndReport(plugin, source, () -> {
            RemoveResult result = service.removeNode(segments);
            return result.descendantsRemoved() > 0
                    ? "§a" + result.removedPath() + " §aund §e" + result.descendantsRemoved() + " §auntergeordnete Einträge gelöscht."
                    : "§a" + result.removedPath() + " §agelöscht.";
        });
        return Command.SINGLE_SUCCESS;
    }

    private static int reload(BMapsPlugin plugin, CommandSourceStack source) {
        plugin.reloadNavigationTree();
        source.getSender().sendMessage("§aBMAPS-Navigationsbaum neu aus der Datenbank geladen.");
        return Command.SINGLE_SUCCESS;
    }

    // -----------------------------------------------------------------
    // Hilfsmethoden
    // -----------------------------------------------------------------

    /**
     * Führt eine Service-Operation aus, lädt bei Erfolg den Navigationsbaum
     * neu und schickt die Ergebnis-/Fehlermeldung an den Sender. Bündelt
     * die immer gleiche try/catch/reload-Wiederholung aus den Handlern oben.
     */
    private static void runAndReport(BMapsPlugin plugin, CommandSourceStack source, ServiceCall call) {
        try {
            String message = call.run();
            plugin.reloadNavigationTree();
            source.getSender().sendMessage(message);
        } catch (BMapsException e) {
            source.getSender().sendMessage("§c" + e.getMessage());
        } catch (SQLException e) {
            plugin.getLogger().severe("BMAPS-Datenbankfehler: " + e.getMessage());
            source.getSender().sendMessage("§cDatenbankfehler. Siehe Server-Log.");
        }
    }

    @FunctionalInterface
    private interface ServiceCall {
        String run() throws SQLException, BMapsException;
    }

    private static String resolveHeldIconKey(CommandSourceStack source, String fallback) {
        if (source.getSender() instanceof Player player) {
            Material held = player.getInventory().getItemInMainHand().getType();
            if (held != Material.AIR) {
                return held.name();
            }
        }
        return fallback;
    }
}