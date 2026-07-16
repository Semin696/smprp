package org.nig.smp.homeSev.command;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.nig.smp.SDSPlugin;
import org.nig.smp.homeSev.manager.HomeManager;
import org.nig.smp.homeSev.model.Home;

import java.util.List;
import java.util.stream.Collectors;

public class HomeCommand implements CommandExecutor, TabCompleter {

    private final SDSPlugin plugin;
    private final HomeManager homeManager;

    public HomeCommand(SDSPlugin plugin, HomeManager homeManager) {
        this.plugin = plugin;
        this.homeManager = homeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(homeManager.msg("only-players")));
            return true;
        }

        if (args.length < 1) {
            List<String> homeNames = homeManager.getHomeNames(player);
            if (homeNames.isEmpty()) {
                player.sendMessage(Component.text(homeManager.msg("no-homes")));
                return true;
            }
            if (homeNames.size() == 1) {
                String name = homeNames.getFirst();
                teleportToHome(player, name);
                return true;
            }
            player.sendMessage(Component.text(homeManager.msg("your-homes", "homes", String.join(", ", homeNames))));
            return true;
        }

        String homeName = args[0].toLowerCase();
        teleportToHome(player, homeName);

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

    private void teleportToHome(Player player, String homeName) {
        if (plugin.isInCombat(player)) {
            player.sendMessage(Component.text(homeManager.msg("in-combat")));
            return;
        }

        Home home = homeManager.getHome(player, homeName);

        if (home == null) {
            player.sendMessage(Component.text(homeManager.msg("home-not-found", "name", homeName)));
            return;
        }

        var location = home.toLocation();
        if (location == null) {
            player.sendMessage(Component.text(homeManager.msg("world-unavailable")));
            return;
        }

        player.teleportAsync(location);
        player.sendMessage(Component.text(homeManager.msg("teleported", "name", homeName)));
    }
}
