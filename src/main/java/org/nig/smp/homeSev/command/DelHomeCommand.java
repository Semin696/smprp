package org.nig.smp.homeSev.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nig.smp.homeSev.manager.HomeManager;

import java.util.List;
import java.util.stream.Collectors;

public class DelHomeCommand implements CommandExecutor, TabCompleter {

    private final HomeManager homeManager;

    public DelHomeCommand(HomeManager homeManager) {
        this.homeManager = homeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /delhome <name>", NamedTextColor.RED));
            return true;
        }

        String homeName = args[0].toLowerCase();

        if (homeManager.getHome(player, homeName) == null) {
            player.sendMessage(Component.text("Home '" + homeName + "' not found!", NamedTextColor.RED));
            return true;
        }

        homeManager.removeHome(player, homeName);
        player.sendMessage(Component.text("Home '" + homeName + "' has been deleted!", NamedTextColor.GREEN));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return List.of();
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return homeManager.getHomeNames(player).stream()
                .filter(name -> name.startsWith(partial))
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
