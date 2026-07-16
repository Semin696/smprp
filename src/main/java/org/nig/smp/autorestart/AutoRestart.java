package org.nig.smp.autorestart;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.command.TabCompleter;
import org.nig.smp.SDSPlugin;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class AutoRestart implements Listener {

    private final SDSPlugin plugin;
    private YamlConfiguration config;
    private BossBar bossBar;
    private int taskId = -1;
    private boolean countingDown = false;
    private int countdownSeconds;

    private static final ZoneId MSK = ZoneId.of("Europe/Moscow");

    public AutoRestart(SDSPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        loadConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        var cmd = plugin.getCommand("restart_server");
        if (cmd != null) {
            cmd.setExecutor((sender, command, label, args) -> onCommand(sender, args));
            cmd.setTabCompleter((sender, command, alias, args) -> List.of());
        }
        scheduleDailyRestart();
    }

    private void loadConfig() {
        config = SDSPlugin.loadConfigWithDefaults(plugin, "autorestart_config.yml");
    }

    public void shutdown() {
        cancelCountdown();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (bossBar != null) {
            bossBar.addPlayer(event.getPlayer());
        }
    }

    private String color(String s) {
        return SDSPlugin.color(s);
    }

    private String prefix() {
        return color(config.getString("prefix", "&#65FFAE&lm&#57F574&lc&#48EA3A&lr&#3AE000&lu"));
    }

    private String msg(String key) {
        return color(config.getString("messages." + key, "")
            .replace("{prefix}", config.getString("prefix", "&#65FFAE&lm&#57F574&lc&#48EA3A&lr&#3AE000&lu")));
    }

    private String msg(String key, String placeholder, String replacement) {
        return msg(key).replace(placeholder, replacement);
    }

    private void cancelCountdown() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        countingDown = false;
    }

    private void scheduleDailyRestart() {
        if (!config.getBoolean("enabled", true)) return;

        ZonedDateTime now = ZonedDateTime.now(MSK);
        ZonedDateTime midnight = now.toLocalDate().atStartOfDay(MSK).plusDays(1);

        long secondsUntil = Duration.between(now, midnight).getSeconds();

        if (secondsUntil > 300) {
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                Bukkit.broadcastMessage(msg("five-minutes")), (secondsUntil - 300) * 20);
        } else {
            Bukkit.broadcastMessage(msg("less-five-minutes"));
        }

        if (secondsUntil > 60) {
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                startCountdown(60, msg("bossbar-planned")), (secondsUntil - 60) * 20);
        } else {
            startCountdown((int) secondsUntil, msg("bossbar-planned"));
        }

        Bukkit.getScheduler().runTaskLater(plugin, this::restartServer, secondsUntil * 20);
    }

    private void startCountdown(int totalSeconds, String reason) {
        cancelCountdown();
        countingDown = true;
        countdownSeconds = totalSeconds;

        bossBar = Bukkit.createBossBar(reason, BarColor.RED, BarStyle.SOLID);
        bossBar.setProgress(1.0);
        Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (countdownSeconds <= 0) {
                cancelCountdown();
                return;
            }

            countdownSeconds--;
            double progress = (double) countdownSeconds / totalSeconds;
            bossBar.setProgress(Math.max(0.0, progress));
            bossBar.setTitle(reason + " \u00A7c" + formatTime(countdownSeconds));

            if (countdownSeconds <= 10 && countdownSeconds > 0) {
                String title = msg("title", "{seconds}", String.valueOf(countdownSeconds));
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendTitle("", title, 0, 20, 5);
                }
            }

            if (countdownSeconds == 10) {
                Bukkit.broadcastMessage(msg("ten-seconds"));
            }
        }, 0L, 20L);
    }

    private String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        if (mins > 0) {
            return mins + "м " + secs + "с";
        }
        return secs + "с";
    }

    private void restartServer() {
        Bukkit.broadcastMessage(msg("restarting"));
        Bukkit.getScheduler().runTaskLater(plugin, () ->
            Bukkit.getServer().shutdown(), 20L);
    }

    public boolean onCommand(org.bukkit.command.CommandSender sender, String[] args) {
        if (!sender.hasPermission("autorestart.restart")) {
            sender.sendMessage(msg("no-permission"));
            return true;
        }

        if (countingDown) {
            sender.sendMessage(msg("already-scheduled"));
            return true;
        }

        Bukkit.broadcastMessage(msg("command-1"));
        Bukkit.broadcastMessage(msg("command-2"));
        startCountdown(60, msg("bossbar-unscheduled"));
        Bukkit.getScheduler().runTaskLater(plugin, this::restartServer, 60 * 20);
        return true;
    }
}
