package org.nig.smp.playerheaddrop;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.nig.smp.SDSPlugin;

import java.io.File;

public class Playerheaddrop implements Listener {

    private final SDSPlugin plugin;
    private YamlConfiguration config;
    private double dropChance;
    private boolean pvpEnabled;
    private boolean pveEnabled;
    private boolean otherEnabled;

    public Playerheaddrop(SDSPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        loadConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void loadConfig() {
        config = SDSPlugin.loadConfigWithDefaults(plugin, "playerheaddrop_config.yml");
        reloadConfigValues();
    }

    private void reloadConfigValues() {
        dropChance = config.getDouble("drop-chance", 1.0);
        pvpEnabled = config.getBoolean("pvp", true);
        pveEnabled = config.getBoolean("pve", true);
        otherEnabled = config.getBoolean("other", true);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (!canDrop(player)) return;
        if (Math.random() > dropChance) return;

        ItemStack head = createHead(player);
        player.getWorld().dropItemNaturally(player.getLocation(), head);
    }

    private boolean canDrop(Player player) {
        Player killer = player.getKiller();

        if (killer != null) return pvpEnabled;

        EntityDamageEvent damage = player.getLastDamageCause();
        if (damage != null) {
            return switch (damage.getCause()) {
                case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK, PROJECTILE -> pveEnabled;
                default -> otherEnabled;
            };
        }

        return otherEnabled;
    }

    private ItemStack createHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            head.setItemMeta(meta);
        }
        return head;
    }
}
