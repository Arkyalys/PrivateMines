package fr.ju.privateMines.services;

import fr.ju.privateMines.PrivateMines;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;

/**
 * Service gérant l'exposition des métriques Prometheus pour l'intégration
 * future avec Grafana.
 */
public class MetricsService {

    private final PrivateMines plugin;
    private HTTPServer server;
    private final Gauge activeMines;
    private final Gauge openMines;
    private final Counter blocksMined;
    private final Counter minesCreated;
    private final Counter minesDeleted;
    private final Counter mineResets;
    private final Counter mineExpansions;
    private final Counter mineUpgrades;
    private final Counter mineVisits;
    private final Counter mineTeleports;

    public MetricsService(PrivateMines plugin) {
        this.plugin = plugin;
        this.activeMines = Gauge.build()
                .name("privatemines_active_mines")
                .help("Nombre de mines actives")
                .register();

        this.openMines = Gauge.build()
                .name("privatemines_open_mines")
                .help("Nombre de mines ouvertes aux visiteurs")
                .register();

        this.blocksMined = Counter.build()
                .name("privatemines_blocks_mined_total")
                .help("Nombre total de blocs minés")
                .register();

        this.minesCreated = Counter.build()
                .name("privatemines_mines_created_total")
                .help("Nombre total de mines créées")
                .register();

        this.minesDeleted = Counter.build()
                .name("privatemines_mines_deleted_total")
                .help("Nombre total de mines supprimées")
                .register();

        this.mineResets = Counter.build()
                .name("privatemines_mine_resets_total")
                .help("Nombre total de resets de mines")
                .register();

        this.mineExpansions = Counter.build()
                .name("privatemines_mine_expansions_total")
                .help("Nombre total d'agrandissements de mines")
                .register();

        this.mineUpgrades = Counter.build()
                .name("privatemines_mine_upgrades_total")
                .help("Nombre total d'améliorations de mines")
                .register();

        this.mineVisits = Counter.build()
                .name("privatemines_mine_visits_total")
                .help("Nombre total de visites de mines")
                .register();

        this.mineTeleports = Counter.build()
                .name("privatemines_mine_teleports_total")
                .help("Nombre total de téléportations vers une mine")
                .register();
        DefaultExports.initialize();
        boolean enabled = plugin.getConfigManager().getConfig()
                .getBoolean("Metrics.enabled", false);
        int port = plugin.getConfigManager().getConfig()
                .getInt("Metrics.port", 9310);
        if (enabled) {
            try {
                server = new HTTPServer(port);
                plugin.getLogger().info("Metrics server started on port " + port);
            } catch (Exception e) {
                plugin.getLogger().warning("Unable to start metrics server: " + e.getMessage());
            }
        }
    }

    public void updateActiveMines(int count) {
        activeMines.set(count);
    }

    public void updateOpenMines(int count) {
        openMines.set(count);
    }

    public void incrementBlocksMined() {
        blocksMined.inc();
    }

    public void incrementMinesCreated() {
        minesCreated.inc();
    }

    public void incrementMinesDeleted() {
        minesDeleted.inc();
    }

    public void incrementMineResets() {
        mineResets.inc();
    }

    public void incrementMineExpansions() {
        mineExpansions.inc();
    }

    public void incrementMineUpgrades() {
        mineUpgrades.inc();
    }

    public void incrementVisits() {
        mineVisits.inc();
    }

    public void incrementTeleports() {
        mineTeleports.inc();
    }

    public void stop() {
        if (server != null) {
            server.stop();
        }
    }
}
