package org.nig.smp.spawn;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.command.TabCompleter;
import org.nig.smp.SDSPlugin;

import java.io.File;
import java.io.IOException;

public class Spawn implements Listener {

    private final SDSPlugin plugin;
    private YamlConfiguration config;
    private File configFile;
    private YamlConfiguration spawnData;
    private File spawnDataFile;
    private String prefix;

    public Spawn(SDSPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        loadConfig();
        reloadMessages();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        var setspawn = plugin.getCommand("setspawn");
        var spawn = plugin.getCommand("spawn");
        if (setspawn != null) {
            setspawn.setExecutor(this::onCommand);
            setspawn.setTabCompleter((sender, cmd, alias, args) -> List.of());
        }
        if (spawn != null) {
            spawn.setExecutor(this::onCommand);
            spawn.setTabCompleter((sender, cmd, alias, args) -> List.of());
        }
    }

    private void loadConfig() {
        config = SDSPlugin.loadConfigWithDefaults(plugin, "spawn_config.yml");
        spawnDataFile = new File(plugin.getDataFolder(), "spawn.yml");
        spawnData = YamlConfiguration.loadConfiguration(spawnDataFile);
    }

    private void reloadMessages() {
        prefix = color(config.getString("prefix", "&#65FFAE&lm&#57F574&lc&#48EA3A&lr&#3AE000&lu"));
    }

    private String msg(String key) {
        String raw = config.getString("messages." + key, "&cMessage not found: " + key);
        return color(raw.replace("{prefix}", prefix));
    }

    private String color(String s) {
        return SDSPlugin.color(s);
    }

    private Location getSpawnLocation() {
        if (!spawnData.contains("world")) return null;
        return new Location(
                plugin.getServer().getWorld(spawnData.getString("world")),
                spawnData.getDouble("x"),
                spawnData.getDouble("y"),
                spawnData.getDouble("z"),
                (float) spawnData.getDouble("yaw"),
                (float) spawnData.getDouble("pitch")
        );
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            Location loc = getSpawnLocation();
            if (loc != null) {
                player.teleport(loc);
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Location loc = getSpawnLocation();
        if (loc != null) {
            event.setRespawnLocation(loc);
        }
    }

    private void saveSpawn() {
        try {
            spawnData.save(spawnDataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save spawn.yml: " + e.getMessage());
        }
    }

    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(msg("no-player"));
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "setspawn" -> {
                if (!player.hasPermission("spawn.setspawn")) {
                    player.sendMessage(msg("no-permission"));
                    return true;
                }
                Location loc = player.getLocation();
                spawnData.set("world", loc.getWorld().getName());
                spawnData.set("x", loc.getX());
                spawnData.set("y", loc.getY());
                spawnData.set("z", loc.getZ());
                spawnData.set("yaw", (double) loc.getYaw());
                spawnData.set("pitch", (double) loc.getPitch());
                saveSpawn();
                player.sendMessage(msg("spawn-set"));
                return true;
            }
            case "spawn" -> {
                if (!player.hasPermission("spawn.spawn")) {
                    player.sendMessage(msg("no-permission"));
                    return true;
                }
                if (plugin.isInCombat(player)) {
                    player.sendMessage(msg("in-combat"));
                    return true;
                }
                Location loc = getSpawnLocation();
                if (loc == null) {
                    player.sendMessage(msg("spawn-not-set"));
                    return true;
                }
                player.teleport(loc);
                player.sendMessage(msg("teleported"));
                return true;
            }
        }
        return false;
    }
}
