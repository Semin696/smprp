package org.nig.smp.admin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class HealFeedCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("admin." + command.getName())) {
            sender.sendMessage("§cУ вас нет прав на эту команду");
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cИспользование: /" + command.getName() + " <ник|*>");
                return true;
            }
            apply(command.getName(), player);
            player.sendMessage("§a" + (command.getName().equalsIgnoreCase("heal") ? "Здоровье" : "Сытость") + " восстановлена");
            return true;
        }

        if (args[0].equals("*")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                apply(command.getName(), p);
            }
            sender.sendMessage("§a" + (command.getName().equalsIgnoreCase("heal") ? "Здоровье" : "Сытость") + " восстановлена всем игрокам");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("§cИгрок не найден");
            return true;
        }

        apply(command.getName(), target);
        sender.sendMessage("§a" + (command.getName().equalsIgnoreCase("heal") ? "Здоровье" : "Сытость") + " восстановлена игроку §f" + target.getName());
        return true;
    }

    private void apply(String cmd, Player player) {
        if (cmd.equalsIgnoreCase("heal")) {
            player.setHealth(player.getMaxHealth());
            player.setFireTicks(0);
        } else {
            player.setFoodLevel(20);
            player.setSaturation(10f);
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> suggestions = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
            if ("*".startsWith(partial)) suggestions.add(0, "*");
            return suggestions;
        }
        return List.of();
    }
}
