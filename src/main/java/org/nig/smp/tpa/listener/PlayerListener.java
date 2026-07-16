package org.nig.smp.tpa.listener;

import org.nig.smp.tpa.TpaManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final TpaManager manager;

    public PlayerListener(TpaManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.removePlayerRequests(event.getPlayer().getUniqueId());
    }
}
