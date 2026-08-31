package b.bplugins.plugin.maps.bedrock;

import b.bplugins.plugin.maps.model.MenuNode;
import b.bplugins.plugin.maps.model.WarpLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.List;

public final class BedrockMenu {

    private static final int PAGE_SIZE = 40;

    private BedrockMenu() {
    }

    public static void open(Player player, MenuNode root, List<String> path, int page) {
        MenuNode current = resolve(root, path);
        List<MenuNode> visible = visibleChildren(player, current);

        int totalPages = Math.max(1, (int) Math.ceil(visible.size() / (double) PAGE_SIZE));
        int safePage = Math.min(Math.max(page, 0), totalPages - 1);

        int startIndex = safePage * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, visible.size());
        List<MenuNode> pageEntries = visible.subList(startIndex, endIndex);

        SimpleForm.Builder form = SimpleForm.builder().title(current.getName());
        if (current.getDescription() != null && !current.getDescription().isBlank()) {
            form.content(current.getDescription());
        }

        // Buttons in fester Reihenfolge sammeln, damit der Klick-Index später
        // eindeutig auf eine Aktion zurückgeführt werden kann.
        List<Runnable> actions = new ArrayList<>();

        for (MenuNode child : pageEntries) {
            String label = (child.isCategory() ? "📁 " : "📍 ") + child.getName();
            form.button(label);
            actions.add(() -> {
                if (child.isCategory()) {
                    List<String> childPath = new ArrayList<>(path);
                    childPath.add(child.getName());
                    open(player, root, childPath, 0);
                } else {
                    teleport(player, child);
                }
            });
        }

        if (safePage > 0) {
            form.button("« Vorherige Seite");
            actions.add(() -> open(player, root, path, safePage - 1));
        }
        if (safePage < totalPages - 1) {
            form.button("» Nächste Seite");
            actions.add(() -> open(player, root, path, safePage + 1));
        }
        if (!path.isEmpty()) {
            form.button("« Zurück");
            actions.add(() -> {
                List<String> parentPath = new ArrayList<>(path);
                parentPath.remove(parentPath.size() - 1);
                open(player, root, parentPath, 0);
            });
        }

        form.validResultHandler(response -> {
            Integer clicked = response.clickedButtonId();
            if (clicked != null && clicked >= 0 && clicked < actions.size()) {
                actions.get(clicked).run();
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    private static MenuNode resolve(MenuNode root, List<String> path) {
        MenuNode current = root;
        for (String name : path) {
            MenuNode next = null;
            for (MenuNode child : current.getChildren()) {
                if (child.getName().equals(name)) {
                    next = child;
                    break;
                }
            }
            if (next == null) {
                // Pfad existiert nicht mehr (z.B. gerade gelöscht) - zurück zum Root
                return root;
            }
            current = next;
        }
        return current;
    }

    private static List<MenuNode> visibleChildren(Player player, MenuNode node) {
        List<MenuNode> visible = new ArrayList<>();
        for (MenuNode child : node.getChildren()) {
            if (child.isPublic() || player.hasPermission(child.getPermission())) {
                visible.add(child);
            }
        }
        return visible;
    }

    private static void teleport(Player player, MenuNode warpNode) {
        WarpLocation loc = warpNode.getWarpLocation();
        if (loc == null || Bukkit.getWorld(loc.world()) == null) {
            player.sendMessage("§cFehler: Die Zielwelt existiert nicht!");
            return;
        }
        Location bukkitLocation = new Location(Bukkit.getWorld(loc.world()), loc.x(), loc.y(), loc.z(), loc.yaw(), loc.pitch());
        player.teleportAsync(bukkitLocation);
        player.sendMessage("§7Teleportiert zu §b" + warpNode.getName() + "§7.");
    }
}