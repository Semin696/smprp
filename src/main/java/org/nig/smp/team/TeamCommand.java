package org.nig.smp.team;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class TeamCommand implements CommandExecutor, TabCompleter {
    private final TeamManager manager;

    public TeamCommand(TeamManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только игрок может использовать эту команду.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        String result;

        switch (sub) {
            case "create" -> {
                if (args.length < 2) {
                    player.sendMessage(manager.getMsg("usage-create"));
                    return true;
                }
                result = manager.createTeam(player, args[1]);
            }
            case "disband" -> result = manager.disbandTeam(player);
            case "invite" -> {
                if (args.length < 2) {
                    player.sendMessage(manager.getMsg("usage-invite"));
                    return true;
                }
                result = manager.invitePlayer(player, args[1]);
            }
            case "join" -> {
                if (args.length < 2) {
                    player.sendMessage(manager.getMsg("usage-join"));
                    return true;
                }
                result = manager.joinTeam(player, args[1]);
            }
            case "leave" -> result = manager.leaveTeam(player);
            case "kick" -> {
                if (args.length < 2) {
                    player.sendMessage(manager.getMsg("usage-kick"));
                    return true;
                }
                result = manager.kickPlayer(player, args[1]);
            }
            case "sethome" -> result = manager.setHome(player);
            case "home" -> result = manager.home(player);
            case "info" -> {
                String teamName = args.length >= 2 ? args[1] : null;
                result = manager.info(player, teamName);
            }
            case "list" -> result = manager.list();
            default -> {
                sendHelp(player);
                return true;
            }
        }

        player.sendMessage(result);
        return true;
    }

    private void sendHelp(Player player) {
        String p = manager.getPf();
        player.sendMessage(p + "\u00A76=== /team ===");
        player.sendMessage("\u00A7e/team create <name> \u00A77- создать команду");
        player.sendMessage("\u00A7e/team disband \u00A77- распустить команду");
        player.sendMessage("\u00A7e/team invite <player> \u00A77- пригласить игрока");
        player.sendMessage("\u00A7e/team join <name> \u00A77- присоединиться");
        player.sendMessage("\u00A7e/team leave \u00A77- покинуть команду");
        player.sendMessage("\u00A7e/team kick <player> \u00A77- кикнуть игрока");
        player.sendMessage("\u00A7e/team sethome \u00A77- установить дом");
        player.sendMessage("\u00A7e/team home \u00A77- телепортироваться домой");
        player.sendMessage("\u00A7e/team info [name] \u00A77- информация о команде");
        player.sendMessage("\u00A7e/team list \u00A77- список команд");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player)) return List.of();

        if (args.length == 1) {
            return List.of("create", "disband", "invite", "join", "leave",
                "kick", "sethome", "home", "info", "list").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick"))) {
            String partial = args[1].toLowerCase();
            return ((Player) sender).getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
