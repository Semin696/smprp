package org.nig.smp.team;

import org.bukkit.Bukkit;
import org.nig.smp.SDSPlugin;

import java.util.Objects;

public class Team {

    private final SDSPlugin plugin;
    private TeamManager teamManager;

    public Team(SDSPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        this.teamManager = new TeamManager(plugin);

        TeamCommand executor = new TeamCommand(teamManager);
        var cmd = plugin.getCommand("team");
        if (cmd != null) {
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        Bukkit.getPluginManager().registerEvents(new FriendlyFireListener(teamManager), plugin);
    }

    public void shutdown() {
        if (teamManager != null) {
            teamManager.save();
        }
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }
}
