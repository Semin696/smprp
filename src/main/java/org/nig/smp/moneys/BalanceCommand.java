package org.nig.smp.moneys;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nig.smp.SDSPlugin;

import java.util.ArrayList;
import java.util.List;

public class BalanceCommand implements CommandExecutor, TabCompleter {

    private final SDSPlugin plugin;

    public BalanceCommand(SDSPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        MoneyManager manager = plugin.getMoneyManager();

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Использование: /balance <игрок>").color(NamedTextColor.RED));
                return true;
            }
            sender.sendMessage(Component.text("Ваш баланс: " + manager.format(manager.getBalance(player.getUniqueId()))).color(NamedTextColor.GOLD));
            return true;
        }

        if (!sender.hasPermission("moneys.balance.others")) {
            sender.sendMessage(Component.text("У вас нет прав смотреть баланс других игроков!").color(NamedTextColor.RED));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(Component.text("Игрок не найден!").color(NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("Баланс " + target.getName() + ": " + manager.format(manager.getBalance(target.getUniqueId()))).color(NamedTextColor.GOLD));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("moneys.balance.others")) {
            List<String> completions = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(p.getName());
                }
            }
            return completions;
        }
        return List.of();
    }
}
