package b.bplugins.plugin.maps.gui;

import b.bplugins.plugin.maps.model.MenuNode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class NavigationMenu implements InventoryHolder {

    private static final int PAGE_SIZE = 45; // Slots 0-44, Reihen 0-4
    private static final int PREV_PAGE_SLOT = 45;
    private static final int BACK_BUTTON_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 53;

    private final Player viewer;
    private final Deque<MenuNode> path;
    private final Inventory inventory;
    private int page = 0;

    private NavigationMenu(Player viewer, Deque<MenuNode> path) {
        this.viewer = viewer;
        this.path = path;
        MenuNode current = path.peekLast();
        this.inventory = Bukkit.createInventory(this, 54, resolveTitle(current));
        render();
    }

    public static NavigationMenu openRoot(Player player, MenuNode root) {
        Deque<MenuNode> path = new ArrayDeque<>();
        path.add(root);
        NavigationMenu menu = new NavigationMenu(player, path);
        player.openInventory(menu.inventory);
        return menu;
    }

    private NavigationMenu openChild(MenuNode child) {
        Deque<MenuNode> newPath = new ArrayDeque<>(this.path);
        newPath.addLast(child);
        return new NavigationMenu(viewer, newPath);
    }

    private static String resolveTitle(MenuNode node) {
        String name = node.getName();
        return name.length() > 32 ? name.substring(0, 32) : name;
    }

    private List<MenuNode> visibleChildren() {
        List<MenuNode> visible = new ArrayList<>();
        for (MenuNode child : path.peekLast().getChildren()) {
            if (child.isPublic() || viewer.hasPermission(child.getPermission())) {
                visible.add(child);
            }
        }
        return visible;
    }

    private int totalPages(int childCount) {
        return Math.max(1, (int) Math.ceil(childCount / (double) PAGE_SIZE));
    }

    private void render() {
        inventory.clear();
        List<MenuNode> children = visibleChildren();

        int totalPages = totalPages(children.size());
        // Falls z.B. nach dem Löschen von Einträgen die aktuelle Seite nicht mehr existiert
        if (page >= totalPages) {
            page = totalPages - 1;
        }
        if (page < 0) {
            page = 0;
        }

        int startIndex = page * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, children.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            inventory.setItem(slot, buildItem(children.get(i)));
            slot++;
        }

        if (page > 0) {
            inventory.setItem(PREV_PAGE_SLOT, buildPageButton(false, totalPages));
        }
        if (page < totalPages - 1) {
            inventory.setItem(NEXT_PAGE_SLOT, buildPageButton(true, totalPages));
        }
        if (path.size() > 1) {
            inventory.setItem(BACK_BUTTON_SLOT, buildBackButton());
        }
    }

    private ItemStack buildItem(MenuNode node) {
        ItemStack item = new ItemStack(resolveMaterial(node.getIcon()));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String prefix = node.isCategory() ? "§b§l" : "§a§l";
            meta.setDisplayName(prefix + node.getName());
            if (node.getDescription() != null && !node.getDescription().isBlank()) {
                meta.setLore(List.of("§7" + node.getDescription()));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material resolveMaterial(String iconKey) {
        if (iconKey == null) {
            return Material.PAPER;
        }
        try {
            return Material.valueOf(iconKey);
        } catch (IllegalArgumentException e) {
            return Material.PAPER;
        }
    }

    private ItemStack buildBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c« Zurück");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildPageButton(boolean next, int totalPages) {
        ItemStack item = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            int currentDisplay = page + 1;
            if (next) {
                meta.setDisplayName("§e» Nächste Seite §7(" + (currentDisplay + 1) + "/" + totalPages + ")");
            } else {
                meta.setDisplayName("§e« Vorherige Seite §7(" + (currentDisplay - 1) + "/" + totalPages + ")");
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public void handleClick(Player player, int slot) {
        List<MenuNode> children = visibleChildren();
        int totalPages = totalPages(children.size());

        if (slot == BACK_BUTTON_SLOT && path.size() > 1) {
            Deque<MenuNode> parentPath = new ArrayDeque<>(this.path);
            parentPath.removeLast();
            NavigationMenu parentMenu = new NavigationMenu(viewer, parentPath);
            player.openInventory(parentMenu.inventory);
            return;
        }

        if (slot == PREV_PAGE_SLOT && page > 0) {
            page--;
            render();
            return;
        }

        if (slot == NEXT_PAGE_SLOT && page < totalPages - 1) {
            page++;
            render();
            return;
        }

        if (slot < 0 || slot >= PAGE_SIZE) {
            return;
        }

        int index = page * PAGE_SIZE + slot;
        if (index >= children.size()) {
            return;
        }

        MenuNode clicked = children.get(index);
        if (clicked.isCategory()) {
            NavigationMenu childMenu = openChild(clicked);
            player.openInventory(childMenu.inventory);
        } else if (clicked.isWarp()) {
            teleport(player, clicked);
        }
    }

    private void teleport(Player player, MenuNode warpNode) {
        var loc = warpNode.getWarpLocation();
        if (loc == null || Bukkit.getWorld(loc.world()) == null) {
            player.sendMessage("§cFehler: Die Zielwelt existiert nicht!");
            return;
        }
        Location bukkitLocation = new Location(Bukkit.getWorld(loc.world()), loc.x(), loc.y(), loc.z(), loc.yaw(), loc.pitch());
        player.closeInventory();
        player.teleportAsync(bukkitLocation);
        player.sendMessage("§7Teleportiert zu §b" + warpNode.getName() + "§7.");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}