package b.bplugins.plugin.maps.storage;

import b.bplugins.plugin.maps.model.MenuNode;
import b.bplugins.plugin.maps.model.WarpLocation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Liest und schreibt den Navigations-Baum in die SQLite-Datenbank.
 * parent_id = NULL bedeutet: Knoten liegt auf oberster Ebene (direkt unter dem Root-Menü).
 *
 * Plattformunabhängig: Icons werden als reiner String-Schlüssel gespeichert,
 * nicht als Bukkit-Material - core kennt kein Bukkit.
 */
public final class NodeRepository {

    private final Database database;

    public NodeRepository(Database database) {
        this.database = database;
    }

    // ---------------------------------------------------------------
    // Pfad-Auflösung ("mkdir -p"-artiges Verhalten für Kategorien)
    // ---------------------------------------------------------------

    /**
     * Sucht/erstellt Kategorien entlang der übergebenen Pfad-Segmente.
     * Gibt die id der letzten (tiefsten) Kategorie im Pfad zurück.
     * Existierende Kategorien werden wiederverwendet, fehlende neu angelegt.
     */
    public long resolveOrCreateCategoryPath(List<String> segments, String iconForCreated) throws SQLException {
        Long currentParent = null;
        long currentId = -1;

        for (String segment : segments) {
            Optional<NodeRow> existing = findChild(currentParent, segment);
            if (existing.isPresent()) {
                if (existing.get().type().equals("WARP")) {
                    throw new IllegalStateException("'" + segment + "' ist bereits ein Warp, keine Kategorie.");
                }
                currentId = existing.get().id();
            } else {
                currentId = insertCategory(currentParent, segment, iconForCreated);
            }
            currentParent = currentId;
        }

        if (currentId == -1) {
            throw new IllegalArgumentException("Leerer Pfad übergeben.");
        }
        return currentId;
    }

    /**
     * Löst einen kompletten Pfad auf, OHNE etwas anzulegen.
     * Wird für addwarp (Elternkategorie muss existieren) und removenode genutzt.
     */
    public Optional<NodeRow> resolvePath(List<String> segments) throws SQLException {
        Long currentParent = null;
        NodeRow current = null;

        for (String segment : segments) {
            Optional<NodeRow> found = findChild(currentParent, segment);
            if (found.isEmpty()) {
                return Optional.empty();
            }
            current = found.get();
            currentParent = current.id();
        }
        return Optional.ofNullable(current);
    }

    public Optional<NodeRow> findChild(Long parentId, String name) throws SQLException {
        String sql = parentId == null
                ? "SELECT * FROM nodes WHERE parent_id IS NULL AND name = ? COLLATE NOCASE"
                : "SELECT * FROM nodes WHERE parent_id = ? AND name = ? COLLATE NOCASE";

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (parentId == null) {
                statement.setString(1, name);
            } else {
                statement.setLong(1, parentId);
                statement.setString(2, name);
            }
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        }
    }

    // ---------------------------------------------------------------
    // Anlegen
    // ---------------------------------------------------------------

    public long insertCategory(Long parentId, String name, String icon) throws SQLException {
        String sql = "INSERT INTO nodes (parent_id, type, name, icon) VALUES (?, 'CATEGORY', ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            if (parentId == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setLong(1, parentId);
            }
            statement.setString(2, name);
            statement.setString(3, icon != null ? icon : MenuNode.DEFAULT_CATEGORY_ICON);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public long insertWarp(long parentId, String name, String icon, WarpLocation location) throws SQLException {
        String sql = """
                INSERT INTO nodes (parent_id, type, name, icon, world, x, y, z, yaw, pitch)
                VALUES (?, 'WARP', ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, parentId);
            statement.setString(2, name);
            statement.setString(3, icon != null ? icon : MenuNode.DEFAULT_WARP_ICON);
            statement.setString(4, location.world());
            statement.setDouble(5, location.x());
            statement.setDouble(6, location.y());
            statement.setDouble(7, location.z());
            statement.setFloat(8, location.yaw());
            statement.setFloat(9, location.pitch());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    // ---------------------------------------------------------------
    // Ändern
    // ---------------------------------------------------------------

    public void updateIcon(long id, String icon) throws SQLException {
        String sql = "UPDATE nodes SET icon = ? WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, icon);
            statement.setLong(2, id);
            statement.executeUpdate();
        }
    }

    public void updateDescription(long id, String description) throws SQLException {
        String sql = "UPDATE nodes SET description = ? WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (description == null) {
                statement.setNull(1, java.sql.Types.VARCHAR);
            } else {
                statement.setString(1, description);
            }
            statement.setLong(2, id);
            statement.executeUpdate();
        }
    }

    public void updatePermission(long id, String permission) throws SQLException {
        String sql = "UPDATE nodes SET permission = ? WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (permission == null) {
                statement.setNull(1, java.sql.Types.VARCHAR);
            } else {
                statement.setString(1, permission);
            }
            statement.setLong(2, id);
            statement.executeUpdate();
        }
    }

    public void moveNode(long id, Long newParentId, String newName) throws SQLException {
        String sql = "UPDATE nodes SET parent_id = ?, name = ? WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (newParentId == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setLong(1, newParentId);
            }
            statement.setString(2, newName);
            statement.setLong(3, id);
            statement.executeUpdate();
        }
    }

    /**
     * Prüft, ob candidateId der Knoten selbst oder einer seiner Nachfahren ist.
     * Wird beim "move" genutzt, um zu verhindern, dass eine Kategorie in
     * sich selbst oder in eine ihrer eigenen Unterkategorien verschoben wird.
     */
    public boolean isSelfOrDescendant(long nodeId, long candidateId) throws SQLException {
        if (nodeId == candidateId) {
            return true;
        }
        List<Long> toVisit = new ArrayList<>();
        toVisit.add(nodeId);

        try (Connection connection = database.getConnection()) {
            while (!toVisit.isEmpty()) {
                long current = toVisit.remove(toVisit.size() - 1);
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT id FROM nodes WHERE parent_id = ?")) {
                    statement.setLong(1, current);
                    try (ResultSet rs = statement.executeQuery()) {
                        while (rs.next()) {
                            long childId = rs.getLong("id");
                            if (childId == candidateId) {
                                return true;
                            }
                            toVisit.add(childId);
                        }
                    }
                }
            }
        }
        return false;
    }

    // ---------------------------------------------------------------
    // Löschen
    // ---------------------------------------------------------------

    /**
     * Löscht einen Knoten. Dank "ON DELETE CASCADE" (und PRAGMA foreign_keys=ON)
     * werden bei Kategorien automatisch alle Kind-Knoten mitgelöscht.
     */
    public void deleteNode(long id) throws SQLException {
        String sql = "DELETE FROM nodes WHERE id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    /**
     * Zählt rekursiv alle Kind-Knoten (für die Sicherheitsabfrage bei removenode).
     */
    public int countDescendants(long id) throws SQLException {
        List<Long> toVisit = new ArrayList<>();
        toVisit.add(id);
        int count = 0;

        try (Connection connection = database.getConnection()) {
            while (!toVisit.isEmpty()) {
                long current = toVisit.remove(toVisit.size() - 1);
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT id FROM nodes WHERE parent_id = ?")) {
                    statement.setLong(1, current);
                    try (ResultSet rs = statement.executeQuery()) {
                        while (rs.next()) {
                            long childId = rs.getLong("id");
                            count++;
                            toVisit.add(childId);
                        }
                    }
                }
            }
        }
        return count;
    }

    // ---------------------------------------------------------------
    // Baum laden
    // ---------------------------------------------------------------

    public MenuNode loadTree() throws SQLException {
        MenuNode root = MenuNode.category("Navigation", "Wähle eine Kategorie aus", "COMPASS");

        Map<Long, MenuNode> nodesById = new HashMap<>();
        Map<Long, Long> parentOf = new HashMap<>();

        String sql = "SELECT * FROM nodes ORDER BY sort_order, name COLLATE NOCASE";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                NodeRow row = mapRow(rs);
                MenuNode node = toMenuNode(row);
                nodesById.put(row.id(), node);
                if (row.parentId() != null) {
                    parentOf.put(row.id(), row.parentId());
                }
            }
        }

        for (Map.Entry<Long, MenuNode> entry : nodesById.entrySet()) {
            Long parentId = parentOf.get(entry.getKey());
            MenuNode parentNode = parentId == null ? root : nodesById.get(parentId);
            if (parentNode != null) {
                parentNode.addChild(entry.getValue());
            }
        }

        return root;
    }

    private MenuNode toMenuNode(NodeRow row) {
        MenuNode node;
        if ("CATEGORY".equals(row.type())) {
            node = MenuNode.category(row.name(), row.description(), row.icon());
        } else {
            WarpLocation location = new WarpLocation(row.world(), row.x(), row.y(), row.z(), row.yaw(), row.pitch());
            node = MenuNode.warp(row.name(), row.description(), row.icon(), location);
        }
        node.setDatabaseId(row.id());
        node.setPermission(row.permission());
        return node;
    }

    private NodeRow mapRow(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        long parentIdRaw = rs.getLong("parent_id");
        Long parentId = rs.wasNull() ? null : parentIdRaw;

        return new NodeRow(
                id,
                parentId,
                rs.getString("type"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("icon"),
                rs.getString("permission"),
                rs.getString("world"),
                rs.getDouble("x"),
                rs.getDouble("y"),
                rs.getDouble("z"),
                rs.getFloat("yaw"),
                rs.getFloat("pitch")
        );
    }

    public record NodeRow(long id, Long parentId, String type, String name, String description, String icon,
                          String permission, String world, double x, double y, double z, float yaw, float pitch) {
    }
}