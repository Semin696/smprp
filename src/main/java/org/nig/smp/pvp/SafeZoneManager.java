package org.nig.smp.pvp;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.nig.smp.SDSPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SafeZoneManager implements Listener {

    private final SDSPlugin plugin;
    private final Map<String, SafeZone> zones = new HashMap<>();
    private final Map<Player, Location> pos1 = new HashMap<>();
    private final Map<Player, Location> pos2 = new HashMap<>();
    private File configFile;

    public SafeZoneManager(SDSPlugin plugin) {
        this.plugin = plugin;
        loadZones();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("safezone.admin")) return;
        if (event.getItem() == null || event.getItem().getType() != Material.WOODEN_HOE) return;

        event.setCancelled(true);

        Block block = event.getClickedBlock();
        if (block == null) return;

        Location loc = block.getLocation();

        switch (event.getAction()) {
            case LEFT_CLICK_BLOCK -> {
                pos1.put(player, loc);
                player.sendMessage(SDSPlugin.color("&aПозиция 1: &f" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()));
            }
            case RIGHT_CLICK_BLOCK -> {
                if (!pos1.containsKey(player)) {
                    player.sendMessage(SDSPlugin.color("&cСначала выберите первую позицию (ЛКМ)"));
                    return;
                }
                if (!pos1.get(player).getWorld().equals(loc.getWorld())) {
                    player.sendMessage(SDSPlugin.color("&cТочки должны быть в одном мире"));
                    return;
                }
                pos2.put(player, loc);
                player.sendMessage(SDSPlugin.color("&aПозиция 2: &f" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()));
                player.sendMessage(SDSPlugin.color("&eИспользуйте &f/safezone create <название> &eдля сохранения"));
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof org.bukkit.entity.Player)) return;

        for (SafeZone zone : zones.values()) {
            if (zone.contains(event.getEntity().getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        for (SafeZone zone : zones.values()) {
            if (zone.contains(event.getBlock().getLocation())) {
                if (!event.getPlayer().hasPermission("safezone.bypass")) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(SDSPlugin.color("&cНельзя ломать блоки в защищённой зоне"));
                }
                return;
            }
        }
    }

    public boolean createZone(Player player, String name) {
        Location p1 = pos1.get(player);
        Location p2 = pos2.get(player);

        if (p1 == null || p2 == null) {
            player.sendMessage(SDSPlugin.color("&cВыделите зону деревянной мотыгой: ЛКМ — позиция 1, ПКМ — позиция 2"));
            return false;
        }

        if (zones.containsKey(name.toLowerCase())) {
            player.sendMessage(SDSPlugin.color("&cЗона \"" + name + "\" уже существует"));
            return false;
        }

        SafeZone zone = new SafeZone(
                name,
                p1.getWorld().getName(),
                p1.getBlockX(), p1.getBlockY(), p1.getBlockZ(),
                p2.getBlockX(), p2.getBlockY(), p2.getBlockZ()
        );

        zones.put(name.toLowerCase(), zone);
        saveZones();

        player.sendMessage(SDSPlugin.color("&aЗона &f\"" + name + "\" &aсоздана"));
        pos1.remove(player);
        pos2.remove(player);
        return true;
    }

    public boolean removeZone(Player player, String name) {
        if (zones.remove(name.toLowerCase()) != null) {
            saveZones();
            player.sendMessage(SDSPlugin.color("&aЗона &f\"" + name + "\" &aудалена"));
            return true;
        }
        player.sendMessage(SDSPlugin.color("&cЗона &f\"" + name + "\" &cне найдена"));
        return false;
    }

    public boolean isInSafeZone(Player player) {
        for (SafeZone zone : zones.values()) {
            if (zone.contains(player.getLocation())) return true;
        }
        return false;
    }

    public void listZones(Player player) {
        if (zones.isEmpty()) {
            player.sendMessage(SDSPlugin.color("&eНет защищённых зон"));
            return;
        }
        player.sendMessage(SDSPlugin.color("&6Защищённые зоны:"));
        for (SafeZone zone : zones.values()) {
            player.sendMessage(SDSPlugin.color(" &f- " + zone.getName() + " &7(" + zone.getWorldName() + ")"));
        }
    }

    private void loadZones() {
        configFile = new File(plugin.getDataFolder(), "safezones.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);
        for (String key : cfg.getKeys(false)) {
            zones.put(key, SafeZone.loadFromConfig(key, cfg.getConfigurationSection(key)));
        }
        plugin.getLogger().info("Loaded " + zones.size() + " safe zones");
    }

    private void saveZones() {
        if (configFile == null) return;
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, SafeZone> entry : zones.entrySet()) {
            entry.getValue().saveToConfig(cfg.createSection(entry.getKey()));
        }
        try {
            cfg.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save safezones.yml: " + e.getMessage());
        }
    }
}
