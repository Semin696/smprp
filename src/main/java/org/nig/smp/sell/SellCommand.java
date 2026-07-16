package org.nig.smp.sell;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SellCommand implements CommandExecutor, TabCompleter {

    private final Sell module;

    public SellCommand(Sell module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("all")) {
            module.sellAll(player);
            return true;
        }

        module.openSellGui(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && "all".startsWith(args[0].toLowerCase())) {
            return List.of("all");
        }
        return List.of();
    }
}
