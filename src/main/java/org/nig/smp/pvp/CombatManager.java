package org.nig.smp.pvp;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.nig.smp.SDSPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class CombatManager implements Listener {

    private final SDSPlugin plugin;
    private final Map<UUID, CombatData> combatMap = new HashMap<>();

    private int combatDuration;
    private String pvpCommand;
    private String bossBarTitle;
    private BarColor bossBarColor;
    private BarStyle bossBarStyle;
    private String scoreboardTitle;
    private List<String> scoreboardLines;
    private boolean newbieProtectionEnabled;
    private int newbieProtectionHours;

    public CombatManager(SDSPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        YamlConfiguration cfg = SDSPlugin.loadConfigWithDefaults(plugin, "pvp_config.yml");

        combatDuration = cfg.getInt("combat-duration", 60);
        pvpCommand = cfg.getString("pvp-command", "");

        bossBarTitle = color(cfg.getString("bossbar.title", "&c&lПВП РЕЖИМ &f{time}"));
        bossBarColor = BarColor.valueOf(cfg.getString("bossbar.color", "RED"));
        bossBarStyle = BarStyle.valueOf(cfg.getString("bossbar.style", "SOLID"));

        scoreboardTitle = color(cfg.getString("scoreboard.title", "&c&lПВП РЕЖИМ"));
        scoreboardLines = cfg.getStringList("scoreboard.lines").stream()
            .map(CombatManager::color)
            .collect(Collectors.toList());

        newbieProtectionEnabled = cfg.getBoolean("newbie-protection.enabled", true);
        newbieProtectionHours = cfg.getInt("newbie-protection.playtime-hours", 2);
    }

    private boolean hasNewbieProtection(Player player) {
        if (!newbieProtectionEnabled) return false;
        int ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        double hours = ticks / (20.0 * 3600.0);
        return hours < newbieProtectionHours;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getEntity() instanceof Player damaged)) return;
        if (event.getFinalDamage() <= 0) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK && event.getCause() != DamageCause.PROJECTILE && event.getCause() != DamageCause.ENTITY_SWEEP_ATTACK) return;

        if (hasNewbieProtection(damager)) {
            damager.sendMessage(color("&cУ вас защита новичка! Вы не можете атаковать других игроков."));
            event.setCancelled(true);
            return;
        }
        if (hasNewbieProtection(damaged)) {
            damager.sendMessage(color("&cУ этого игрока защита новичка!"));
            event.setCancelled(true);
            return;
        }

        enterCombat(damager);
        enterCombat(damaged);
    }

    public void enterCombat(Player player) {
        CombatData data = combatMap.get(player.getUniqueId());
        if (data == null) {
            data = new CombatData(player);
            combatMap.put(player.getUniqueId(), data);
            if (!pvpCommand.isEmpty()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), pvpCommand);
            }
        }

        data.remaining = combatDuration;

        data.bossBar.setTitle(bossBarTitle.replace("{time}", formatTime(data.remaining)));
        data.bossBar.setProgress(1.0);
        data.bossBar.addPlayer(player);

        updateScoreboard(player, data);
    }

    public void tick() {
        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, CombatData> entry : combatMap.entrySet()) {
            CombatData data = entry.getValue();
            Player player = data.player;

            if (!player.isOnline()) {
                removeBossBar(player);
                restoreScoreboard(player, data);
                toRemove.add(entry.getKey());
                continue;
            }

            data.remaining--;
            if (data.remaining <= 0) {
                removeBossBar(player);
                restoreScoreboard(player, data);
                if (!pvpCommand.isEmpty()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), pvpCommand);
                }
                toRemove.add(entry.getKey());
                continue;
            }

            data.bossBar.setTitle(bossBarTitle.replace("{time}", formatTime(data.remaining)));
            data.bossBar.setProgress((double) data.remaining / combatDuration);

            updateScoreboard(player, data);
        }

        for (UUID uuid : toRemove) {
            combatMap.remove(uuid);
        }
    }

    public void exitCombat(Player player) {
        CombatData data = combatMap.get(player.getUniqueId());
        if (data == null) return;
        removeBossBar(player);
        restoreScoreboard(player, data);
        combatMap.remove(player.getUniqueId());
    }

    private void removeBossBar(Player player) {
        CombatData data = combatMap.get(player.getUniqueId());
        if (data == null) return;
        data.bossBar.removePlayer(player);
        data.bossBar.setVisible(false);
    }

    private void restoreScoreboard(Player player, CombatData data) {
        player.setScoreboard(data.previousScoreboard != null ? data.previousScoreboard : Bukkit.getScoreboardManager().getNewScoreboard());
    }

    public boolean isInCombat(Player player) {
        return combatMap.containsKey(player.getUniqueId());
    }

    public void cleanupAll() {
        for (CombatData data : combatMap.values()) {
            data.bossBar.removeAll();
        }
        combatMap.clear();
    }

    private void updateScoreboard(Player player, CombatData data) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("pvp", "dummy", scoreboardTitle);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int score = scoreboardLines.size();
        for (String line : scoreboardLines) {
            String formatted = line
                .replace("{player}", player.getName())
                .replace("{health}", formatHealth(player))
                .replace("{time}", formatTime(data.remaining));
            objective.getScore(formatted).setScore(score--);
        }

        player.setScoreboard(board);
    }

    private String formatHealth(Player player) {
        double hearts = Math.ceil(player.getHealth()) / 2;
        return String.format("%.1f", hearts);
    }

    private String formatTime(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        return String.format("%d:%02d", min, sec);
    }

    static String color(String text) {
        return SDSPlugin.color(text);
    }

    private class CombatData {
        final Player player;
        final BossBar bossBar;
        final Scoreboard previousScoreboard;
        int remaining;

        CombatData(Player player) {
            this.player = player;
            this.bossBar = Bukkit.createBossBar(
                bossBarTitle.replace("{time}", formatTime(combatDuration)),
                bossBarColor,
                bossBarStyle
            );
            this.bossBar.setVisible(true);
            this.remaining = combatDuration;
            this.previousScoreboard = player.getScoreboard();
        }
    }
}
