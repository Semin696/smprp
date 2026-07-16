package org.nig.smp.team;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class FriendlyFireListener implements Listener {
    private final TeamManager manager;

    public FriendlyFireListener(TeamManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player damager = getDamagerPlayer(event);
        if (damager == null) return;

        if (manager.isOnSameTeam(victim, damager)) {
            event.setCancelled(true);
        }
    }

    private Player getDamagerPlayer(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile
            && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }
}
