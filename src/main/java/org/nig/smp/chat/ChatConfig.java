package org.nig.smp.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.nig.smp.SDSPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ChatConfig {

    private final SDSPlugin plugin;
    private YamlConfiguration config;
    private String localFormat;
    private String publicFormat;
    private boolean placeholderApi;
    private final Map<String, String> messages = new HashMap<>();

    public ChatConfig(SDSPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        config = SDSPlugin.loadConfigWithDefaults(plugin, "chat_config.yml");
        localFormat = config.getString("local-format", "{team}{voice}{prefix}{player}{suffix}&7: {message}");
        publicFormat = config.getString("public-format", "{team}{voice}{prefix}{player}{suffix}&f: {message}");
        placeholderApi = config.getBoolean("placeholder-api", true);

        messages.clear();
        if (config.isConfigurationSection("messages")) {
            for (String key : config.getConfigurationSection("messages").getKeys(false)) {
                messages.put(key, config.getString("messages." + key, ""));
            }
        }
    }

    public String getLocalFormat() {
        return localFormat;
    }

    public String getPublicFormat() {
        return publicFormat;
    }

    public boolean usePlaceholderApi() {
        return placeholderApi;
    }

    public Component format(String key, String... placeholders) {
        String msg = messages.getOrDefault(key, "&cMessage not found: " + key);
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            msg = msg.replace(placeholders[i], placeholders[i + 1]);
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(msg);
    }
}
