package org.nig.smp.team;

import org.nig.smp.SDSPlugin;

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
