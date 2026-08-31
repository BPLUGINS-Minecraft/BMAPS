package b.bplugins.plugin.maps.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    private final HikariDataSource dataSource;

    public Database(File dataFolder) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File dbFile = new File(dataFolder, "bmaps.db");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setMaximumPoolSize(1);
        config.setPoolName("BMAPS-SQLite");
        config.addDataSourceProperty("foreign_keys", "true");

        this.dataSource = new HikariDataSource(config);
        initSchema();
    }

    private void initSchema() {
        String sql = """
                CREATE TABLE IF NOT EXISTS nodes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    parent_id INTEGER NULL REFERENCES nodes(id) ON DELETE CASCADE,
                    type TEXT NOT NULL CHECK(type IN ('CATEGORY', 'WARP')),
                    name TEXT NOT NULL,
                    description TEXT,
                    icon TEXT,
                    world TEXT,
                    x REAL,
                    y REAL,
                    z REAL,
                    yaw REAL,
                    pitch REAL,
                    sort_order INTEGER NOT NULL DEFAULT 0
                );
                """;
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;");
            statement.execute(sql);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_nodes_parent ON nodes(parent_id);");
            migrateAddPermissionColumn(statement);
        } catch (SQLException e) {
            throw new IllegalStateException("Konnte BMAPS-Datenbankschema nicht initialisieren", e);
        }
    }

    private void migrateAddPermissionColumn(Statement statement) {
        try {
            statement.execute("ALTER TABLE nodes ADD COLUMN permission TEXT");
        } catch (SQLException e) {
            // Spalte existiert bereits - kein echter Fehler, einfach ignorieren
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}