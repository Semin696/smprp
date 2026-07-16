package org.nig.smp.order;

import org.bukkit.Bukkit;
import org.bukkit.command.TabCompleter;
import org.nig.smp.SDSPlugin;

import java.util.List;

public class Order {

    private final SDSPlugin plugin;
    private OrderConfig config;
    private PrivateChatManager chatManager;

    public Order(SDSPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        config = new OrderConfig(plugin);
        chatManager = new PrivateChatManager();

        var cmd = plugin.getCommand("order");
        if (cmd != null) {
            cmd.setExecutor(new OrderCommand(config, chatManager, plugin));
            cmd.setTabCompleter((sender, command, alias, args) -> {
                if (args.length == 1) {
                    String partial = args[0].toLowerCase();
                    return List.of("stop", "offer", "accept", "decline").stream()
                            .filter(s -> s.startsWith(partial))
                            .toList();
                }
                if (args.length == 2 && (args[0].equalsIgnoreCase("offer") || args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("decline"))) {
                    String partial = args[1].toLowerCase();
                    return Bukkit.getOnlinePlayers().stream()
                            .map(p -> p.getName())
                            .filter(n -> n.toLowerCase().startsWith(partial))
                            .toList();
                }
                return List.of();
            });
        }
        Bukkit.getPluginManager().registerEvents(new ChatListener(chatManager, config), plugin);
    }

    public OrderConfig getConfig() {
        return config;
    }

    public PrivateChatManager getChatManager() {
        return chatManager;
    }
}
