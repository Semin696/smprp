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

public class MoneyCommand implements CommandExecutor, TabCompleter {

    private final SDSPlugin plugin;

    public MoneyCommand(SDSPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        return switch (sub) {
            case "give" -> handleGive(sender, args);
            case "set" -> handleSet(sender, args);
            case "take" -> handleTake(sender, args);
            case "reset" -> handleReset(sender, args);
            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Использование:").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  /money give <игрок> <сумма>").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  /money set <игрок> <сумма>").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  /money take <игрок> <сумма>").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  /money reset * — сбросить баланс всех").color(NamedTextColor.YELLOW));
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Использование: /money give <игрок> <сумма>").color(NamedTextColor.RED));
            return true;
        }
        if (!sender.hasPermission("moneys.money.give")) {
            sender.sendMessage(Component.text("У вас нет прав!").color(NamedTextColor.RED));
            return true;
        }

        OfflinePlayer target = getTarget(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Игрок не найден!").color(NamedTextColor.RED));
            return true;
        }

        double amount = parseAmount(sender, args[2]);
        if (amount <= 0) return true;

        MoneyManager manager = plugin.getMoneyManager();
        manager.deposit(target.getUniqueId(), amount);
        sender.sendMessage(Component.text("Выдано " + manager.format(amount) + " игроку " + target.getName()).color(NamedTextColor.GREEN));

        Player online = target.getPlayer();
        if (online != null && online.isOnline()) {
            online.sendMessage(Component.text("Вы получили " + manager.format(amount)).color(NamedTextColor.GREEN));
        }
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Использование: /money set <игрок> <сумма>").color(NamedTextColor.RED));
            return true;
        }
        if (!sender.hasPermission("moneys.money.give")) {
            sender.sendMessage(Component.text("У вас нет прав!").color(NamedTextColor.RED));
            return true;
        }

        OfflinePlayer target = getTarget(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Игрок не найден!").color(NamedTextColor.RED));
            return true;
        }

        double amount = parseAmount(sender, args[2]);
        if (amount < 0) return true;

        MoneyManager manager = plugin.getMoneyManager();
        manager.setBalance(target.getUniqueId(), amount);
        sender.sendMessage(Component.text("Установлен баланс " + manager.format(amount) + " игроку " + target.getName()).color(NamedTextColor.GREEN));

        Player online = target.getPlayer();
        if (online != null && online.isOnline()) {
            online.sendMessage(Component.text("Ваш баланс установлен на " + manager.format(amount)).color(NamedTextColor.GREEN));
        }
        return true;
    }

    private boolean handleTake(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Использование: /money take <игрок> <сумма>").color(NamedTextColor.RED));
            return true;
        }
        if (!sender.hasPermission("moneys.money.give")) {
            sender.sendMessage(Component.text("У вас нет прав!").color(NamedTextColor.RED));
            return true;
        }

        OfflinePlayer target = getTarget(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Игрок не найден!").color(NamedTextColor.RED));
            return true;
        }

        double amount = parseAmount(sender, args[2]);
        if (amount <= 0) return true;

        MoneyManager manager = plugin.getMoneyManager();
        if (manager.withdraw(target.getUniqueId(), amount)) {
            sender.sendMessage(Component.text("Снято " + manager.format(amount) + " у игрока " + target.getName()).color(NamedTextColor.GREEN));

            Player online = target.getPlayer();
            if (online != null && online.isOnline()) {
                online.sendMessage(Component.text("С вас списали " + manager.format(amount)).color(NamedTextColor.RED));
            }
        } else {
            sender.sendMessage(Component.text("У игрока недостаточно средств!").color(NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("moneys.money.give")) {
            sender.sendMessage(Component.text("У вас нет прав!").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 2 || !args[1].equals("*")) {
            sender.sendMessage(Component.text("Использование: /money reset *").color(NamedTextColor.RED));
            return true;
        }

        MoneyManager manager = plugin.getMoneyManager();
        manager.resetAllBalances();
        sender.sendMessage(Component.text("Баланс всех игроков сброшен!").color(NamedTextColor.GREEN));
        return true;
    }

    private OfflinePlayer getTarget(String name) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        if (!target.hasPlayedBefore() && !target.isOnline()) return null;
        return target;
    }

    private double parseAmount(CommandSender sender, String arg) {
        try {
            double amount = Double.parseDouble(arg);
            if (amount < 0) {
                sender.sendMessage(Component.text("Сумма не может быть отрицательной!").color(NamedTextColor.RED));
                return -1;
            }
            return amount;
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Неверная сумма!").color(NamedTextColor.RED));
            return -1;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("moneys.money.give")) return List.of();

        if (args.length == 1) {
            List<String> subs = List.of("give", "set", "take", "reset");
            List<String> result = new ArrayList<>();
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) result.add(s);
            }
            return result;
        }

        if (args.length == 2 && !args[0].equalsIgnoreCase("reset")) {
            List<String> completions = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(p.getName());
                }
            }
            return completions;
        }

        return List.of();
    }
}
