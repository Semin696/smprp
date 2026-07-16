package org.nig.smp.subscription;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.nig.smp.SDSPlugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SubCommand implements CommandExecutor, TabCompleter {

    private final SDSPlugin plugin;
    private LuckPerms luckPerms;

    public SubCommand(SDSPlugin plugin) {
        this.plugin = plugin;
        try {
            this.luckPerms = LuckPermsProvider.get();
        } catch (Exception e) {
            this.luckPerms = null;
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("sub.admin")) {
            sender.sendMessage(SDSPlugin.color("&cУ вас нет прав"));
            return true;
        }

        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        if (luckPerms == null) {
            sender.sendMessage(SDSPlugin.color("&cLuckPerms не найден"));
            return true;
        }

        String sub = args[0].toLowerCase();
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[1]);
        if (target == null) {
            sender.sendMessage(SDSPlugin.color("&cИгрок не найден"));
            return true;
        }

        switch (sub) {
            case "give" -> {
                if (args.length < 4) {
                    sendUsage(sender);
                    return true;
                }
                String type = args[2].toLowerCase();
                long seconds = parseDuration(args[3]);
                if (seconds <= 0) {
                    sender.sendMessage(SDSPlugin.color("&cНеверный формат времени. Используйте: 1h, 30m, 1d, 7d"));
                    return true;
                }
                giveSubscription(sender, target, type, seconds);
            }
            case "remove" -> removeSubscription(sender, target);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void giveSubscription(CommandSender sender, OfflinePlayer target, String type, long seconds) {
        sender.sendMessage(SDSPlugin.color("&eВыдача подписки &f" + type + " &eигроку &f" + target.getName() + "..."));

        luckPerms.getUserManager().loadUser(target.getUniqueId()).thenAcceptAsync(user -> {
            List<String> perms = switch (type) {
                case "plus" -> List.of("homesev.limit.4", "ah.limit.10");
                case "pluspro" -> List.of("homesev.limit.8", "ah.limit.20");
                default -> null;
            };

            if (perms == null) {
                sender.sendMessage(SDSPlugin.color("&cНеизвестный тип подписки. Доступно: plus, pluspro"));
                return;
            }

            for (String perm : perms) {
                user.data().add(Node.builder(perm).value(true).expiry(Duration.ofSeconds(seconds)).build());
            }

            luckPerms.getUserManager().saveUser(user);
            sender.sendMessage(SDSPlugin.color("&aПодписка &f" + type + " &aвыдана игроку &f" + target.getName()
                    + " &aна &f" + formatDuration(seconds)));
        });
    }

    private void removeSubscription(CommandSender sender, OfflinePlayer target) {
        sender.sendMessage(SDSPlugin.color("&eУдаление подписок игрока &f" + target.getName() + "..."));

        luckPerms.getUserManager().loadUser(target.getUniqueId()).thenAcceptAsync(user -> {
            user.data().clear(node -> node.getKey().startsWith("homesev.limit."));
            luckPerms.getUserManager().saveUser(user);
            sender.sendMessage(SDSPlugin.color("&aВсе подписки игрока &f" + target.getName() + " &aудалены"));
        });
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(SDSPlugin.color("&6/sub give <игрок> <plus|pluspro> <время>"));
        sender.sendMessage(SDSPlugin.color("&6/sub remove <игрок>"));
        sender.sendMessage(SDSPlugin.color("&7Время: 1h, 30m, 1d, 7d и т.д."));
    }

    private long parseDuration(String input) {
        long total = 0;
        String num = "";
        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                num += c;
            } else {
                if (num.isEmpty()) continue;
                long n = Long.parseLong(num);
                switch (c) {
                    case 'd' -> total += n * 86400;
                    case 'h' -> total += n * 3600;
                    case 'm' -> total += n * 60;
                    case 's' -> total += n;
                }
                num = "";
            }
        }
        return total;
    }

    private String formatDuration(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("д ");
        if (hours > 0) sb.append(hours).append("ч ");
        if (minutes > 0) sb.append(minutes).append("м");
        if (sb.isEmpty()) sb.append(seconds).append("с");
        return sb.toString().trim();
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("sub.admin")) return List.of();

        if (args.length == 1) {
            return Stream.of("give", "remove")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            return null;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return Stream.of("plus", "pluspro")
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
