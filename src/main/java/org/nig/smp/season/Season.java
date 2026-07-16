package org.nig.smp.season;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.nig.smp.SDSPlugin;

import java.io.File;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Season implements CommandExecutor, TabCompleter, Listener {

    private final SDSPlugin plugin;
    private YamlConfiguration config;
    private BossBar bossBar;
    private int taskId = -1;
    private int remainingSeconds;
    private int totalSeconds;
    private boolean active = false;
    private final Map<UUID, Integer> protectionTimes = new HashMap<>();
    private int protectionTaskId = -1;
    private int protectionDuration;
    private int borderSize;
    private int borderTime;

    private int scheduledTaskId = -1;

    public Season(SDSPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        var cmd = plugin.getCommand("startsezon");
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }
        var stopCmd = plugin.getCommand("stopstartsezon");
        if (stopCmd != null) {
            stopCmd.setExecutor(this);
            stopCmd.setTabCompleter(this);
        }
    }

    private void loadConfig() {
        config = SDSPlugin.loadConfigWithDefaults(plugin, "season_config.yml");
        protectionDuration = config.getInt("protection-duration", 3600);
        borderSize = config.getInt("border-size", 20000);
        borderTime = config.getInt("border-time", 10000);
    }

    private Component getMessage(String key, String... placeholders) {
        String text = config.getString("messages." + key);
        if (text == null) return Component.text("Message not found: " + key);
        String prefix = config.getString("prefix", "");
        text = text.replace("{prefix}", prefix);
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            text = text.replace("%" + placeholders[i] + "%", placeholders[i + 1]);
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (bossBar != null) {
            bossBar.addPlayer(player);
        }
        if (!protectionTimes.isEmpty() && !protectionTimes.containsKey(player.getUniqueId()) && protectionDuration > 0) {
            protectionTimes.put(player.getUniqueId(), protectionDuration);
            player.sendMessage(getMessage("protection-activated", "time", formatTime(protectionDuration)));
        } else if (protectionTimes.containsKey(player.getUniqueId())) {
            int remaining = protectionTimes.get(player.getUniqueId());
            if (remaining > 0) {
                player.sendMessage(getMessage("protection-remaining", "time", formatTime(remaining)));
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        protectionTimes.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (protectionTimes.containsKey(player.getUniqueId())) {
            int remaining = protectionTimes.get(player.getUniqueId());
            if (remaining > 0) {
                event.setCancelled(true);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("season.admin")) {
            sender.sendMessage("\u00A7c\u0423 \u0432\u0430\u0441 \u043D\u0435\u0442 \u043F\u0440\u0430\u0432");
            return true;
        }

        if (label.equalsIgnoreCase("stopstartsezon")) {
            boolean stopped = false;
            if (scheduledTaskId != -1) {
                Bukkit.getScheduler().cancelTask(scheduledTaskId);
                scheduledTaskId = -1;
                stopped = true;
            }
            if (taskId != -1) {
                Bukkit.getScheduler().cancelTask(taskId);
                taskId = -1;
                if (bossBar != null) {
                    bossBar.removeAll();
                    bossBar = null;
                }
                active = false;
                stopped = true;
            }
            if (stopped) {
                Bukkit.broadcastMessage("\u00A7c\u2716 \u0417\u0430\u043F\u0443\u0441\u043A \u0441\u0435\u0437\u043E\u043D\u0430 \u043E\u0441\u0442\u0430\u043D\u043E\u0432\u043B\u0435\u043D");
                sender.sendMessage("\u00A7a\u2713 \u0421\u0435\u0437\u043E\u043D \u043E\u0441\u0442\u0430\u043D\u043E\u0432\u043B\u0435\u043D");
            } else {
                sender.sendMessage("\u00A7c\u041D\u0435\u0442 \u0430\u043A\u0442\u0438\u0432\u043D\u043E\u0433\u043E \u0437\u0430\u043F\u0443\u0441\u043A\u0430 \u0438\u043B\u0438 \u0437\u0430\u043F\u043B\u0430\u043D\u0438\u0440\u043E\u0432\u0430\u043D\u043D\u043E\u0433\u043E \u0441\u0435\u0437\u043E\u043D\u0430");
            }
            return true;
        }

        if (active) {
            sender.sendMessage("\u00A7c\u0421\u0435\u0437\u043E\u043D \u0443\u0436\u0435 \u0438\u0434\u0451\u0442");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("\u00A7c\u0418\u0441\u043F\u043E\u043B\u044C\u0437\u043E\u0432\u0430\u043D\u0438\u0435: /startsezon <\u0432\u0440\u0435\u043C\u044F> [\u0432\u0440\u0435\u043C\u044F \u043D\u0430\u0447\u0430\u043B\u0430]");
            sender.sendMessage("\u00A7f  /startsezon 1h \u2014 \u043D\u0430\u0447\u0430\u0442\u044C \u0441\u0435\u0439\u0447\u0430\u0441");
            sender.sendMessage("\u00A7f  /startsezon 1h 16:00 \u2014 \u0437\u0430\u043F\u043B\u0430\u043D\u0438\u0440\u043E\u0432\u0430\u0442\u044C \u043D\u0430 16:00 \u041C\u0421\u041A");
            return true;
        }

        int seconds = parseDuration(args[0]);
        if (seconds <= 0) {
            sender.sendMessage("\u00A7c\u041D\u0435\u0432\u0435\u0440\u043D\u044B\u0439 \u0444\u043E\u0440\u043C\u0430\u0442 \u0432\u0440\u0435\u043C\u0435\u043D\u0438. \u0418\u0441\u043F\u043E\u043B\u044C\u0437\u0443\u0439\u0442\u0435 \u043D\u0430\u043F\u0440. 1h, 30m, 1h30m");
            return true;
        }

        String time = null;

        for (String arg : args) {
            if (arg.contains(":")) {
                time = arg;
                break;
            }
        }

        if (time != null) {
            scheduleSeason(sender, seconds, time);
        } else {
            startSeason(seconds);
        }
        return true;
    }

    private void scheduleSeason(CommandSender sender, int countdownSeconds, String timeStr) {
        if (scheduledTaskId != -1) {
            Bukkit.getScheduler().cancelTask(scheduledTaskId);
            scheduledTaskId = -1;
        }

        String[] parts = timeStr.split(":");
        int targetHour = Integer.parseInt(parts[0]);
        int targetMin = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

        ZoneId msk = ZoneId.of("Europe/Moscow");
        ZonedDateTime now = ZonedDateTime.now(msk);
        ZonedDateTime target = now.withHour(targetHour).withMinute(targetMin).withSecond(0).withNano(0);
        if (target.isBefore(now) || target.isEqual(now)) {
            target = target.plusDays(1);
        }

        long delaySec = ChronoUnit.SECONDS.between(now, target);

        Bukkit.broadcastMessage("\u00A7e\u23F1 \u0421\u0435\u0437\u043E\u043D \u0437\u0430\u043F\u043B\u0430\u043D\u0438\u0440\u043E\u0432\u0430\u043D!");
        Bukkit.broadcastMessage("\u00A7f\u041E\u0431\u0440\u0430\u0442\u043D\u044B\u0439 \u043E\u0442\u0441\u0447\u0451\u0442 \u043D\u0430\u0447\u043D\u0451\u0442\u0441\u044F \u0432 \u00A7e" + timeStr + " \u041C\u0421\u041A");
        Bukkit.broadcastMessage("\u00A7f\u0414\u043B\u0438\u0442\u0435\u043B\u044C\u043D\u043E\u0441\u0442\u044C \u043E\u0442\u0441\u0447\u0451\u0442\u0430: \u00A7e" + formatTime(countdownSeconds));

        scheduledTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            scheduledTaskId = -1;
            startSeason(countdownSeconds);
        }, delaySec * 20);

        sender.sendMessage("\u00A7a\u2713 \u0421\u0435\u0437\u043E\u043D \u0437\u0430\u043F\u043B\u0430\u043D\u0438\u0440\u043E\u0432\u0430\u043D \u043D\u0430 " + timeStr + " \u041C\u0421\u041A");
    }

    private int parseDuration(String input) {
        int total = 0;
        String num = "";
        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                num += c;
            } else if (c == 'h' || c == 'H') {
                if (!num.isEmpty()) total += Integer.parseInt(num) * 3600;
                num = "";
            } else if (c == 'm' || c == 'M') {
                if (!num.isEmpty()) total += Integer.parseInt(num) * 60;
                num = "";
            } else if (c == 's' || c == 'S') {
                if (!num.isEmpty()) total += Integer.parseInt(num);
                num = "";
            }
        }
        return total;
    }

    private void startSeason(int seconds) {
        active = true;
        totalSeconds = seconds;
        remainingSeconds = seconds;

        Bukkit.setWhitelist(false);
        Bukkit.broadcastMessage("\u00A7e\u26A0 \u0421\u0415\u0417\u041E\u041D \u0421\u041A\u041E\u0420\u041E \u041D\u0410\u0427\u041D\u0401\u0422\u0421\u042F!");
        Bukkit.broadcastMessage("\u00A7f\u0412\u0430\u0439\u0442\u043B\u0438\u0441\u0442 \u0432\u044B\u043A\u043B\u044E\u0447\u0435\u043D \u2014 \u0437\u0430\u0445\u043E\u0434\u0438\u0442\u0435 \u0432\u0441\u0435!");
        Bukkit.broadcastMessage("\u00A7e\u0414\u043E \u043D\u0430\u0447\u0430\u043B\u0430: \u00A7f" + formatTime(seconds));

        bossBar = Bukkit.createBossBar(
            "\u00A7e\u26A0 \u0421\u0415\u0417\u041E\u041D \u0421\u041A\u041E\u0420\u041E \u041D\u0410\u0427\u041D\u0401\u0422\u0421\u042F \u00A7f" + formatTime(seconds),
            BarColor.YELLOW, BarStyle.SOLID);
        bossBar.setProgress(1.0);
        Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            remainingSeconds--;
            if (remainingSeconds <= 0) {
                endSeason();
                return;
            }
            double progress = (double) remainingSeconds / totalSeconds;
            bossBar.setProgress(Math.max(0.0, progress));
            bossBar.setTitle("\u00A7e\u26A0 \u0421\u0415\u0417\u041E\u041D \u0421\u041A\u041E\u0420\u041E \u041D\u0410\u0427\u041D\u0401\u0422\u0421\u042F \u00A7f" + formatTime(remainingSeconds));
        }, 20L, 20L);
    }

    private void endSeason() {
        active = false;
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }

        Bukkit.broadcastMessage("\u00A7a\u266B \u0421\u0415\u0417\u041E\u041D \u041D\u0410\u0427\u0410\u041B\u0421\u042F!");
        int borderK = borderSize / 1000;
        Bukkit.broadcastMessage("\u00A7f\u0413\u0440\u0430\u043D\u0438\u0446\u0430 \u0440\u0430\u0441\u0448\u0438\u0440\u044F\u0435\u0442\u0441\u044F \u0434\u043E " + borderK + "\u0442\u044B\u0441 \u0431\u043B\u043E\u043A\u043E\u0432...");

        Location spawn = Bukkit.getWorlds().getFirst().getSpawnLocation();
        for (World world : Bukkit.getWorlds()) {
            world.getWorldBorder().setCenter(spawn);
            world.getWorldBorder().changeSize(borderSize, borderTime);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleportAsync(spawn);
            player.setWhitelisted(true);
        }
        Bukkit.setWhitelist(true);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 1.0f);
        }

        if (protectionDuration > 0) {
            startProtection();
        }
    }

    private void startProtection() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            protectionTimes.put(player.getUniqueId(), protectionDuration);
            player.sendMessage(getMessage("protection-activated", "time", formatTime(protectionDuration)));
        }

        protectionTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            List<UUID> expired = new ArrayList<>();
            for (Map.Entry<UUID, Integer> entry : protectionTimes.entrySet()) {
                UUID uuid = entry.getKey();
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) {
                    expired.add(uuid);
                    continue;
                }

                int remaining = entry.getValue() - 1;
                if (remaining <= 0) {
                    expired.add(uuid);
                    player.sendMessage(getMessage("protection-expired"));
                    continue;
                }
                entry.setValue(remaining);

                if (remaining % 2 == 0) {
                    player.sendActionBar(getMessage("protection-remaining", "time", formatTime(remaining)));
                }
            }
            for (UUID uuid : expired) {
                protectionTimes.remove(uuid);
            }
            if (protectionTimes.isEmpty() && protectionTaskId != -1) {
                Bukkit.getScheduler().cancelTask(protectionTaskId);
                protectionTaskId = -1;
            }
        }, 20L, 20L);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("startsezon") && args.length == 1) {
            String partial = args[0].toLowerCase();
            return Arrays.asList("1h", "2h", "3h", "6h", "12h", "24h", "30m", "1h30m").stream()
                    .filter(s -> s.startsWith(partial))
                    .toList();
        }
        return List.of();
    }

    private String formatTime(int seconds) {
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append("\u0447 ");
        if (m > 0) sb.append(m).append("\u043C ");
        if (s > 0 || sb.isEmpty()) sb.append(s).append("\u0441");
        return sb.toString().trim();
    }
}
