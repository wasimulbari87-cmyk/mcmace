package dev.wasixarthex.macepatch;

import dev.wasixarthex.macepatch.listener.MaceSmashListener;
import dev.wasixarthex.macepatch.tracker.FallTracker;
import org.bukkit.plugin.java.JavaPlugin;

public class MacePatch extends JavaPlugin {

    private static MacePatch instance;
    private FallTracker fallTracker;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (getServer().getPluginManager().getPlugin("floodgate") == null) {
            getLogger().warning("Floodgate not found! MacePatch requires Floodgate to detect Bedrock players.");
            getLogger().warning("Plugin will load but Bedrock detection will be disabled.");
        }

        fallTracker = new FallTracker(this);
        getServer().getPluginManager().registerEvents(new MaceSmashListener(this), this);
        getLogger().info("MacePatch enabled — Mace smash attack fix active for Bedrock players.");
    }

    @Override
    public void onDisable() {
        if (fallTracker != null) {
            fallTracker.cleanup();
        }
        getLogger().info("MacePatch disabled.");
    }

    public static MacePatch getInstance() {
        return instance;
    }

    public FallTracker getFallTracker() {
        return fallTracker;
    }
}
