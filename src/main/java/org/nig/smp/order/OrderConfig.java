package org.nig.smp.order;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.nig.smp.SDSPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class OrderConfig {

    private final SDSPlugin plugin;
    private YamlConfiguration config;
    private String prefix;
    private final Map<String, String> messages = new HashMap<>();

    public OrderConfig(SDSPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        config = SDSPlugin.loadConfigWithDefaults(plugin, "order_config.yml");
        prefix = config.getString("prefix", "&#65FFAE&lm&#57F574&lc&#48EA3A&lr&#3AE000&lu");

        messages.clear();
        if (config.isConfigurationSection("messages")) {
            for (String key : config.getConfigurationSection("messages").getKeys(false)) {
                messages.put(key, config.getString("messages." + key, ""));
            }
        }
    }

    public String getPrefix() {
        return prefix;
    }

    public Component format(String key, String... placeholders) {
        String msg = messages.getOrDefault(key, "&cMessage not found: " + key);
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            msg = msg.replace(placeholders[i], placeholders[i + 1]);
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + " " + msg);
    }

    public Component formatRaw(String key, String... placeholders) {
        String msg = messages.getOrDefault(key, "&cMessage not found: " + key);
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            msg = msg.replace(placeholders[i], placeholders[i + 1]);
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(msg);
    }
}
