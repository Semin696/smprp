package org.nig.smp.pvp;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.nig.smp.SDSPlugin;

public class Pvp {

    private final SDSPlugin plugin;
    private CombatManager combatManager;
    private SafeZoneManager safeZoneManager;

    public Pvp(SDSPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        combatManager = new CombatManager(plugin);
        Bukkit.getPluginManager().registerEvents(combatManager, plugin);

        safeZoneManager = new SafeZoneManager(plugin);
        Bukkit.getPluginManager().registerEvents(safeZoneManager, plugin);

        var safezoneCmd = plugin.getCommand("safezone");
        if (safezoneCmd != null) {
            var executor = new SafeZoneCommand(safeZoneManager);
            safezoneCmd.setExecutor(executor);
            safezoneCmd.setTabCompleter(executor);
        }

        new CombatTimerTask().runTaskTimer(plugin, 0L, 20L);
    }

    public void shutdown() {
        if (combatManager != null) {
            combatManager.cleanupAll();
        }
    }

    public boolean isInCombat(Player player) {
        return combatManager != null && combatManager.isInCombat(player);
    }

    public boolean isInSafeZone(Player player) {
        return safeZoneManager != null && safeZoneManager.isInSafeZone(player);
    }

    private class CombatTimerTask extends BukkitRunnable {
        @Override
        public void run() {
            combatManager.tick();
        }
    }
}
