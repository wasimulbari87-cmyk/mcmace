package dev.wasixarthex.macepatch.listener;

import dev.wasixarthex.macepatch.MacePatch;
import dev.wasixarthex.macepatch.tracker.FallTracker;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.geysermc.floodgate.api.FloodgateApi;

public class MaceSmashListener implements Listener {

    private static final double MACE_BASE_DAMAGE = 6.0;
    private static final Material MACE = Material.MACE;

    private final MacePatch plugin;
    private final FallTracker fallTracker;

    public MaceSmashListener(MacePatch plugin) {
        this.plugin = plugin;
        this.fallTracker = plugin.getFallTracker();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMaceHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!isBedrockPlayer(attacker)) return;

        ItemStack heldItem = attacker.getInventory().getItemInMainHand();
        if (heldItem.getType() != MACE) return;
        if (!fallTracker.wasAirborne(attacker)) return;

        double fallHeight = fallTracker.getFallHeight(attacker);
        double minFallHeight = plugin.getConfig().getDouble("min-fall-height", 1.5);

        if (fallHeight < minFallHeight) {
            if (plugin.getConfig().getBoolean("debug")) {
                plugin.getLogger().info("[MacePatch] " + attacker.getName()
                        + " mace hit but fall height " + String.format("%.2f", fallHeight)
                        + " < min " + minFallHeight + " — skipping");
            }
            return;
        }

        int densityLevel = getDensityLevel(heldItem);
        double damagePerBlock = plugin.getConfig().getDouble("damage-per-block", 4.0);
        double densityBonusPerLevel = plugin.getConfig().getDouble("density-bonus-per-level", 1.0);
        double maxBonus = plugin.getConfig().getDouble("max-bonus-damage", 150.0);

        double fallBonus = fallHeight * (damagePerBlock + (densityLevel * densityBonusPerLevel));
        fallBonus = Math.min(fallBonus, maxBonus);
        double correctedDamage = MACE_BASE_DAMAGE + fallBonus;

        if (plugin.getConfig().getBoolean("debug")) {
            plugin.getLogger().info("[MacePatch] Bedrock mace smash correction for " + attacker.getName()
                    + " | fallHeight=" + String.format("%.2f", fallHeight)
                    + " | density=" + densityLevel
                    + " | originalDamage=" + String.format("%.2f", event.getDamage())
                    + " | correctedDamage=" + String.format("%.2f", correctedDamage));
        }

        event.setDamage(correctedDamage);
        applyEnchantmentEffects(attacker, event.getEntity(), heldItem, fallHeight);
        fallTracker.clearPlayer(attacker.getUniqueId());
    }

    private void applyEnchantmentEffects(Player attacker, Entity target, ItemStack mace, double fallHeight) {
        int windBurstLevel = getEnchantmentLevel(mace, Enchantment.WIND_BURST);
        if (windBurstLevel > 0) {
            double launchVelocity = 0.7 * windBurstLevel;
            attacker.getServer().getScheduler().runTaskLater(plugin, () -> {
                var vel = attacker.getVelocity();
                vel.setY(launchVelocity);
                attacker.setVelocity(vel);
                fallTracker.clearPlayer(attacker.getUniqueId());
            }, 1L);
        }

        int breachLevel = getEnchantmentLevel(mace, Enchantment.BREACH);
        if (breachLevel > 0 && target instanceof LivingEntity livingTarget) {
            if (plugin.getConfig().getBoolean("debug")) {
                plugin.getLogger().info("[MacePatch] Breach level " + breachLevel
                        + " applied to " + livingTarget.getName());
            }
        }
    }

    private int getDensityLevel(ItemStack item) {
        return getEnchantmentLevel(item, Enchantment.DENSITY);
    }

    private int getEnchantmentLevel(ItemStack item, Enchantment enchantment) {
        if (item == null || !item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        return meta.getEnchantLevel(enchantment);
    }

    private boolean isBedrockPlayer(Player player) {
        try {
            FloodgateApi api = FloodgateApi.getInstance();
            return api != null && api.isFloodgatePlayer(player.getUniqueId());
        } catch (Exception e) {
            return false;
        }
    }
          }
