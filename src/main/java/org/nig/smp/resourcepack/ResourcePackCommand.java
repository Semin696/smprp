package org.nig.smp.resourcepack;

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

public class ResourcePackCommand implements CommandExecutor, TabCompleter {

    private final ResourcePack resourcePack;

    public ResourcePackCommand(ResourcePack resourcePack) {
        this.resourcePack = resourcePack;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("resourcepack.admin")) {
            sender.sendMessage(SDSPlugin.color("&cУ вас нет прав"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "build" -> {
                sender.sendMessage(SDSPlugin.color("&eСборка и загрузка ресурспака..."));
                boolean success = resourcePack.buildAndUpload();
                if (success) {
                    sender.sendMessage(SDSPlugin.color("&aРесурспак успешно собран и загружен!"));
                    sender.sendMessage(SDSPlugin.color("&aURL: &f" + resourcePack.getPackUrl()));
                } else {
                    sender.sendMessage(SDSPlugin.color("&cОшибка при сборке/загрузке ресурспака"));
                }
            }
            case "info" -> {
                sender.sendMessage(SDSPlugin.color("&6Информация о ресурспаке:"));
                sender.sendMessage(SDSPlugin.color(" &fURL: &7" + (resourcePack.getPackUrl().isEmpty() ? "не задан" : resourcePack.getPackUrl())));
                sender.sendMessage(SDSPlugin.color(" &fSHA-1: &7" + (resourcePack.getPackHash().isEmpty() ? "не задан" : resourcePack.getPackHash())));
            }
            case "send" -> {
                sender.sendMessage(SDSPlugin.color("&eОтправка ресурспака всем игрокам..."));
                resourcePack.sendToAll();
                sender.sendMessage(SDSPlugin.color("&aРесурспак отправлен всем игрокам"));
            }
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(SDSPlugin.color("&6/resourcepack build &7- собрать, загрузить на GitHub и отправить всем игрокам"));
        sender.sendMessage(SDSPlugin.color("&6/resourcepack info &7- информация о ресурспаке"));
        sender.sendMessage(SDSPlugin.color("&6/resourcepack send &7- отправить текущий ресурспак всем игрокам"));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("resourcepack.admin")) return List.of();

        if (args.length == 1) {
            return Stream.of("build", "info", "send")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}
