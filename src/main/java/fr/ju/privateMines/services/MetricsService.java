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
    private final Counter blocksMined;

    public MetricsService(PrivateMines plugin) {
        this.plugin = plugin;
        this.activeMines = Gauge.build()
                .name("privatemines_active_mines")
                .help("Nombre de mines actives")
                .register();
        this.blocksMined = Counter.build()
                .name("privatemines_blocks_mined_total")
                .help("Nombre total de blocs minés")
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

    public void incrementBlocksMined() {
        blocksMined.inc();
    }

    public void stop() {
        if (server != null) {
            server.stop();
        }
    }
}
