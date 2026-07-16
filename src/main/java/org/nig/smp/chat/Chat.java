package org.nig.smp.chat;

import org.bukkit.Bukkit;
import org.nig.smp.SDSPlugin;

public class Chat {

    private final SDSPlugin plugin;
    private ChatConfig config;

    public Chat(SDSPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        config = new ChatConfig(plugin);
        Bukkit.getPluginManager().registerEvents(new ChatListener(config), plugin);
        Bukkit.getPluginManager().registerEvents(new JoinQuitListener(plugin), plugin);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new SdsPlaceholderExpansion().register();
        }

        plugin.getLogger().info("Chat module loaded (local + public)");
    }

    public ChatConfig getConfig() {
        return config;
    }
}
