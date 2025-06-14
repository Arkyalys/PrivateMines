package fr.ju.privateMines.utils;

import fr.ju.privateMines.PrivateMines;

/**
 * Utility class for creating colored progress bars based on plugin configuration.
 */
public final class ProgressBarUtil {
    private ProgressBarUtil() {
        // Utility class
    }

    /**
     * Create a progress bar string for the given percentage using the values
     * configured in <code>config.yml</code> under "Gameplay.progress-bars".
     *
     * @param percentage value between 0 and 100
     * @return colored progress bar
     */
    public static String createProgressBar(int percentage) {
        PrivateMines plugin = PrivateMines.getInstance();

        String completedColor = plugin.getConfigManager().getConfig()
                .getString("Gameplay.progress-bars.completed-color", "&a");
        String remainingColor = plugin.getConfigManager().getConfig()
                .getString("Gameplay.progress-bars.remaining-color", "&7");
        String borderColor = plugin.getConfigManager().getConfig()
                .getString("Gameplay.progress-bars.border-color", "&8");
        String character = plugin.getConfigManager().getConfig()
                .getString("Gameplay.progress-bars.character", "■");
        int length = plugin.getConfigManager().getConfig()
                .getInt("Gameplay.progress-bars.length", 20);

        int completed = (int) Math.round((percentage / 100.0) * length);
        StringBuilder progressBar = new StringBuilder(borderColor + "[");
        for (int i = 0; i < length; i++) {
            if (i < completed) {
                progressBar.append(completedColor).append(character);
            } else {
                progressBar.append(remainingColor).append(character);
            }
        }
        progressBar.append(borderColor).append("]");
        return progressBar.toString();
    }
}
