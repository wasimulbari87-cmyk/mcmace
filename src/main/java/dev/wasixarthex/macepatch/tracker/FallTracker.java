package dev.wasixarthex.macepatch.tracker;

import dev.wasixarthex.macepatch.MacePatch;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FallTracker implements Listener {

    private final MacePatch plugin;
    private final Map<UUID, Double> airOriginY = new HashMap<>();
    private final Map<UUID, Boolean> isAirborne = new HashMap<>();
    private final Map<UUID, Integer> airTicks = new HashMap<>();
    private BukkitTask tickTask;

    public FallTracker(MacePatch plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startTickTask();
    }

    private void startTickTask() {
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (!isBedrockPlayer(player)) continue;

                UUID uuid = player.getUniqueId();
                boolean onGround = player.isOnGround();

                if (!onGround) {
                    if (!Boolean.TRUE.equals(isAirborne.get(uuid))) {
                        airOriginY.put(uuid, player.getLocation().getY());
                        isAirborne.put(uuid, true);
                        airTicks.put(uuid, 0);

                        if (plugin.getConfig().getBoolean("debug")) {
                            plugin.getLogger().info("[MacePatch] " + player.getName()
                                    + " left ground at Y=" + player.getLocation().getY());
                        }
                    } else {
                        double currentY = player.getLocation().getY();
                        double originY = airOriginY.getOrDefault(uuid, currentY);
                        if (currentY > originY) {
                            airOriginY.put(uuid, currentY);
                        }

                        int ticks = airTicks.getOrDefault(uuid, 0) + 1;
                        int maxTicks = plugin.getConfig().getInt("fall-track-ticks", 60);
                        if (ticks > maxTicks) {
                            clearPlayer(uuid);
                        } else {
                            airTicks.put(uuid, ticks);
                        }
                    }
                } else {
                    isAirborne.put(uuid, false);
                }
            }
        }, 1L, 1L);
    }

    public double getFallHeight(Player player) {
        UUID uuid = player.getUniqueId();
        if (!airOriginY.containsKey(uuid)) return 0.0;
        double originY = airOriginY.get(uuid);
        double currentY = player.getLocation().getY();
        return Math.max(0.0, originY - currentY);
    }

    public boolean wasAirborne(Player player) {
        UUID uuid = player.getUniqueId();
        return airOriginY.containsKey(uuid) && airTicks.containsKey(uuid);
    }

    public void clearPlayer(UUID uuid) {
        airOriginY.remove(uuid);
        isAirborne.remove(uuid);
        airTicks.remove(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {}

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        clearPlayer(event.getPlayer().getUniqueId());
    }

    private boolean isBedrockPlayer(Player player) {
        try {
            FloodgateApi api = FloodgateApi.getInstance();
            return api != null && api.isFloodgatePlayer(player.getUniqueId());
        } catch (Exception e) {
            return false;
        }
    }

    public void cleanup() {
        if (tickTask != null) tickTask.cancel();
        airOriginY.clear();
        isAirborne.clear();
        airTicks.clear();
    }
              }
