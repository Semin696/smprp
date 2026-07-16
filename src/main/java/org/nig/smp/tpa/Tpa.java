package org.nig.smp.tpa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.nig.smp.SDSPlugin;
import org.nig.smp.tpa.command.RespondCommand;
import org.nig.smp.tpa.command.TpaCommand;
import org.nig.smp.tpa.listener.PlayerListener;
import org.nig.smp.tpa.command.TpaTabCompleter;

import java.io.File;
import java.util.List;

public class Tpa {

    private final SDSPlugin plugin;
    private TpaManager manager;
    private YamlConfiguration config;

    public Tpa(SDSPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        loadConfig();
        manager = new TpaManager();

        var tpaCmd = plugin.getCommand("tpa");
        var tpahereCmd = plugin.getCommand("tpahere");
        var tpacceptCmd = plugin.getCommand("tpaccept");
        var tpadenyCmd = plugin.getCommand("tpadeny");

        var tpaTab = new TpaTabCompleter();
        if (tpaCmd != null) {
            tpaCmd.setExecutor(new TpaCommand(manager, this));
            tpaCmd.setTabCompleter(tpaTab);
        }
        if (tpahereCmd != null) {
            tpahereCmd.setExecutor(new TpaCommand(manager, this));
            tpahereCmd.setTabCompleter(tpaTab);
        }
        if (tpacceptCmd != null) {
            tpacceptCmd.setExecutor(new RespondCommand(manager, this));
            tpacceptCmd.setTabCompleter((sender, cmd, alias, args) -> List.of());
        }
        if (tpadenyCmd != null) {
            tpadenyCmd.setExecutor(new RespondCommand(manager, this));
            tpadenyCmd.setTabCompleter((sender, cmd, alias, args) -> List.of());
        }

        plugin.getServer().getPluginManager().registerEvents(new PlayerListener(manager), plugin);
    }

    private void loadConfig() {
        config = SDSPlugin.loadConfigWithDefaults(plugin, "tpa_config.yml");
    }

    public Component getPrefix() {
        String raw = config.getString("prefix", "&#65FFAE&lm&#57F574&lc&#48EA3A&lr&#3AE000&lu");
        return LegacyComponentSerializer.legacyAmpersand().deserialize(raw + " ");
    }

    public int getCooldownSeconds() {
        return config.getInt("cooldown-seconds", 0);
    }

    public Component getMessage(String path, String... placeholders) {
        String text = config.getString("messages." + path);
        if (text == null) {
            return Component.text("Сообщение не найдено: " + path);
        }
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            text = text.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
