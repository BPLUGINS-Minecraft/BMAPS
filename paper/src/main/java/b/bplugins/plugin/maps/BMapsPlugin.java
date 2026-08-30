package b.bplugins.plugin.maps;

import b.bplugins.plugin.maps.command.BMapsCommand;
import b.bplugins.plugin.maps.gui.NavigationMenuListener;
import b.bplugins.plugin.maps.model.MenuNode;
import b.bplugins.plugin.maps.service.BMapsService;
import b.bplugins.plugin.maps.storage.Database;
import b.bplugins.plugin.maps.storage.NodeRepository;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class BMapsPlugin extends JavaPlugin {

    private Database database;
    private NodeRepository repository;
    private BMapsService service;
    private volatile MenuNode navigationRoot;

    @Override
    public void onEnable() {
        this.database = new Database(getDataFolder());
        this.repository = new NodeRepository(database);
        this.service = new BMapsService(repository);

        reloadNavigationTree();

        getServer().getPluginManager().registerEvents(new NavigationMenuListener(), this);
        registerCommands();

        if (b.bplugins.plugin.maps.bedrock.FloodgateSupport.isAvailable()) {
            getLogger().info("Floodgate erkannt - Bedrock-Spieler bekommen natives Formular-Menü.");
        } else {
            getLogger().info("Floodgate nicht gefunden - alle Spieler nutzen das Java-Inventory-Menü.");
        }

        getLogger().info("BMAPS wurde aktiviert.");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
        getLogger().info("BMAPS wurde deaktiviert.");
    }

    /**
     * Lädt den kompletten Navigations-Baum neu aus der Datenbank.
     * Wird nach jeder Änderung (addcategory/addwarp/removenode/...) sowie
     * über /bmaps reload aufgerufen.
     */
    public void reloadNavigationTree() {
        try {
            this.navigationRoot = service.loadTree();
        } catch (SQLException e) {
            getLogger().severe("Konnte Navigationsbaum nicht aus der Datenbank laden: " + e.getMessage());
            if (this.navigationRoot == null) {
                // Fallback: leeres Root-Menü, damit das Plugin nicht komplett tot ist
                this.navigationRoot = MenuNode.category("Navigation", "Datenbank-Fehler - siehe Log", "BARRIER");
            }
        }
    }

    /**
     * Registriert /bmaps über Paper's natives Brigadier-Command-System.
     * WICHTIG: paper-plugin.yml ignoriert den alten "commands:"-Block und
     * onCommand() komplett - das läuft nur noch über LifecycleEvents.COMMANDS.
     */
    private void registerCommands() {
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(BMapsCommand.create(this), "Öffnet die BMAPS Navigation"));
    }

    public BMapsService getService() {
        return service;
    }

    public MenuNode getNavigationRoot() {
        return navigationRoot;
    }
}