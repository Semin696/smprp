package org.nig.smp;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.nig.smp.autorestart.AutoRestart;
import org.nig.smp.spawn.Spawn;
import org.nig.smp.pvp.CombatManager;
import org.nig.smp.pvp.Pvp;
import org.nig.smp.order.Order;
import org.nig.smp.moneys.Moneys;
import org.nig.smp.playerheaddrop.Playerheaddrop;
import org.nig.smp.team.Team;
import org.nig.smp.tpa.Tpa;
import org.nig.smp.homeSev.HomeSev;
import org.nig.smp.chat.Chat;
import org.nig.smp.moneys.MoneyManager;
import org.nig.smp.homeSev.manager.HomeManager;
import org.nig.smp.season.Season;
import org.nig.smp.sell.Sell;
import org.nig.smp.ah.Ah;
import org.nig.smp.admin.HealFeedCommand;
import org.nig.smp.voice.VoiceChatTracker;
import org.nig.smp.market.Market;
import org.nig.smp.resourcepack.ResourcePack;
import org.nig.smp.oraxen.OraxenModule;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class SDSPlugin extends JavaPlugin {

    private final List<Runnable> shutdownTasks = new ArrayList<>();
    private Moneys moneysModule;
    private Team teamModule;
    private HomeSev homeSevModule;
    private Pvp pvpModule;
    private VoiceChatTracker voiceChatTracker;
    private OraxenModule oraxenModule;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();

        registerMainPlugCommand();
        initializeAllModules();

        getLogger().info("SDS plugin enabled \u2014 all modules loaded");
    }

    @Override
    public void onDisable() {
        shutdownAllModules();
        getLogger().info("SDS plugin disabled");
    }

    private void registerMainPlugCommand() {
        var cmd = getCommand("mainplug");
        if (cmd != null) {
            cmd.setExecutor((sender, command, label, args) -> {
                if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                    if (!sender.hasPermission("mainplug.reload")) {
                        sender.sendMessage(color("&cYou do not have permission to use this command"));
                        return true;
                    }
                    getLogger().info("Reloading all modules...");
                    sender.sendMessage(color("&aReloading all modules..."));
                    shutdownAllModules();
                    initializeAllModules();
                    sender.sendMessage(color("&aAll modules reloaded successfully"));
                    getLogger().info("All modules reloaded successfully");
                    return true;
                }
                sender.sendMessage(color("&cUsage: /mainplug reload"));
                return true;
            });
        }
    }

    private void registerHealFeed() {
        HealFeedCommand exec = new HealFeedCommand();
        for (String name : new String[]{"heal", "feed"}) {
            var cmd = getCommand(name);
            if (cmd != null) {
                cmd.setExecutor(exec);
                cmd.setTabCompleter(exec);
            }
        }
    }

    private void initializeAllModules() {
        registerHealFeed();
        initModule("VoiceChat", () -> {
            voiceChatTracker = new VoiceChatTracker(this);
        });
        initModule("AutoRestart", () -> {
            AutoRestart module = new AutoRestart(this);
            module.init();
            shutdownTasks.add(module::shutdown);
        });
        initModule("Spawn", () -> {
            Spawn module = new Spawn(this);
            module.init();
        });
        initModule("PvP", () -> {
            pvpModule = new Pvp(this);
            pvpModule.init();
            shutdownTasks.add(pvpModule::shutdown);
        });
        initModule("Order", () -> {
            Order module = new Order(this);
            module.init();
        });
        initModule("Moneys", () -> {
            moneysModule = new Moneys(this);
            moneysModule.init();
            shutdownTasks.add(moneysModule::shutdown);
        });
        initModule("PlayerHeadDrop", () -> {
            Playerheaddrop module = new Playerheaddrop(this);
            module.init();
        });
        initModule("Team", () -> {
            teamModule = new Team(this);
            teamModule.init();
            shutdownTasks.add(teamModule::shutdown);
        });
        initModule("TPA", () -> {
            Tpa module = new Tpa(this);
            module.init();
        });
        initModule("HomeSev", () -> {
            homeSevModule = new HomeSev(this);
            homeSevModule.init();
            shutdownTasks.add(homeSevModule::shutdown);
        });
        initModule("Chat", () -> {
            Chat module = new Chat(this);
            module.init();
        });
        initModule("Season", () -> {
            Season module = new Season(this);
            module.init();
        });
        initModule("Sell", () -> {
            Sell module = new Sell(this);
            module.init();
        });
        initModule("AuctionHouse", () -> {
            Ah module = new Ah(this);
            module.init();
        });
        initModule("Market", () -> {
            Market module = new Market(this);
            module.init();
        });
        initModule("OraxenModule", () -> {
            oraxenModule = new OraxenModule(this);
            oraxenModule.init();
        });
        initModule("ResourcePack", () -> {
            ResourcePack module = new ResourcePack(this, oraxenModule);
            module.init();
        });
    }

    private void shutdownAllModules() {
        for (Runnable task : shutdownTasks) {
            try {
                task.run();
            } catch (Exception e) {
                getLogger().warning("Error during module shutdown: " + e.getMessage());
            }
        }
        shutdownTasks.clear();
    }

    private void initModule(String name, Runnable init) {
        try {
            init.run();
            getLogger().info("Module " + name + " initialized");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize module " + name + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isInCombat(Player player) {
        return pvpModule != null && pvpModule.isInCombat(player);
    }

    public MoneyManager getMoneyManager() {
        return moneysModule != null ? moneysModule.getMoneyManager() : null;
    }

    public HomeManager getHomeManager() {
        return homeSevModule != null ? homeSevModule.getHomeManager() : null;
    }

    public Team getTeamModule() {
        return teamModule;
    }

    public VoiceChatTracker getVoiceChatTracker() {
        return voiceChatTracker;
    }

    public static String color(String text) {
        if (text == null) return null;
        return LegacyComponentSerializer.legacySection().serialize(
            LegacyComponentSerializer.legacyAmpersand().deserialize(text)
        );
    }

    public static YamlConfiguration loadConfigWithDefaults(SDSPlugin plugin, String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
            return YamlConfiguration.loadConfiguration(file);
        }

        YamlConfiguration current = YamlConfiguration.loadConfiguration(file);

        try (InputStream in = plugin.getResource(fileName);
             InputStreamReader reader = in != null ? new InputStreamReader(in, StandardCharsets.UTF_8) : null) {
            if (reader != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
                boolean changed = false;
                for (String key : defaults.getKeys(true)) {
                    if (!current.contains(key)) {
                        current.set(key, defaults.get(key));
                        changed = true;
                    }
                }
                if (changed) {
                    current.save(file);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to merge defaults for " + fileName + ": " + e.getMessage());
        }

        return current;
    }
}
