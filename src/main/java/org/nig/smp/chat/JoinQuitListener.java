package org.nig.smp.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.nig.smp.SDSPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class JoinQuitListener implements Listener {

    private final SDSPlugin plugin;
    private final Set<UUID> announced = new HashSet<>();

    public JoinQuitListener(SDSPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.joinMessage(null);

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                announced.add(uuid);
                Bukkit.broadcast(
                        LegacyComponentSerializer.legacyAmpersand().deserialize("&a[+] &f" + player.getName())
                );
            }
        }, 10L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (announced.remove(event.getPlayer().getUniqueId())) {
            event.quitMessage(
                    LegacyComponentSerializer.legacyAmpersand().deserialize("&c[-] &f" + event.getPlayer().getName())
            );
        } else {
            event.quitMessage(null);
        }
    }
}
