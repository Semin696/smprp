package org.nig.smp.pvp;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.nig.smp.SDSPlugin;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SafeZoneCommand implements CommandExecutor, TabCompleter {

    private final SafeZoneManager manager;

    public SafeZoneCommand(SafeZoneManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(SDSPlugin.color("&cТолько игроки могут использовать эту команду"));
            return true;
        }

        if (!player.hasPermission("safezone.admin")) {
            player.sendMessage(SDSPlugin.color("&cУ вас нет прав"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (args.length < 2) {
                    player.sendMessage(SDSPlugin.color("&cИспользование: /safezone create <название>"));
                    return true;
                }
                manager.createZone(player, args[1]);
            }
            case "remove" -> {
                if (args.length < 2) {
                    player.sendMessage(SDSPlugin.color("&cИспользование: /safezone remove <название>"));
                    return true;
                }
                manager.removeZone(player, args[1]);
            }
            case "list" -> manager.listZones(player);
            default -> sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(SDSPlugin.color("&6/safezone create <название> &7- создать зону из выделения"));
        player.sendMessage(SDSPlugin.color("&6/safezone remove <название> &7- удалить зону"));
        player.sendMessage(SDSPlugin.color("&6/safezone list &7- список зон"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("safezone.admin")) return List.of();

        if (args.length == 1) {
            return Stream.of("create", "remove", "list")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
