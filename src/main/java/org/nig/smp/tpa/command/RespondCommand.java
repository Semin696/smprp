package org.nig.smp.tpa.command;

import org.nig.smp.tpa.Tpa;
import org.nig.smp.tpa.TpaManager;
import org.nig.smp.tpa.TpaRequest;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RespondCommand implements CommandExecutor {

    private final TpaManager manager;
    private final Tpa plugin;

    public RespondCommand(TpaManager manager, Tpa plugin) {
        this.manager = manager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getPrefix().append(plugin.getMessage("only-players")));
            return true;
        }

        TpaRequest request = manager.getRequest(player.getUniqueId());
        if (request == null) {
            player.sendMessage(plugin.getPrefix().append(plugin.getMessage("no-pending-requests")));
            return true;
        }

        if (command.getName().equalsIgnoreCase("tpaccept")) {
            manager.removeRequest(player.getUniqueId());

            Player senderPlayer = Bukkit.getPlayer(request.sender());
            if (senderPlayer == null || !senderPlayer.isOnline()) {
                player.sendMessage(plugin.getPrefix().append(plugin.getMessage("player-offline")));
                return true;
            }

            if (request.type() == TpaRequest.Type.TPA) {
                senderPlayer.teleportAsync(player.getLocation());
                senderPlayer.sendMessage(plugin.getPrefix().append(plugin.getMessage("teleporting-to", "player", player.getName())));
                player.sendMessage(plugin.getPrefix().append(plugin.getMessage("teleporting-player", "player", senderPlayer.getName())));
            } else {
                player.teleportAsync(senderPlayer.getLocation());
                senderPlayer.sendMessage(plugin.getPrefix().append(plugin.getMessage("teleporting-player", "player", player.getName())));
                player.sendMessage(plugin.getPrefix().append(plugin.getMessage("teleporting-to", "player", senderPlayer.getName())));
            }

        } else if (command.getName().equalsIgnoreCase("tpadeny")) {
            manager.removeRequest(player.getUniqueId());

            Player senderPlayer = Bukkit.getPlayer(request.sender());
            if (senderPlayer != null && senderPlayer.isOnline()) {
                senderPlayer.sendMessage(plugin.getPrefix().append(plugin.getMessage("request-denied-sender", "player", player.getName())));
            }
            player.sendMessage(plugin.getPrefix().append(plugin.getMessage("request-denied")));
        }

        return true;
    }
}
