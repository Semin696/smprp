package org.nig.smp.homeSev;

import org.nig.smp.SDSPlugin;
import org.nig.smp.homeSev.command.DelHomeCommand;
import org.nig.smp.homeSev.command.HomeCommand;
import org.nig.smp.homeSev.command.SetHomeCommand;
import org.nig.smp.homeSev.manager.HomeManager;

public class HomeSev {

    private final SDSPlugin plugin;
    private HomeManager homeManager;

    public HomeSev(SDSPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        this.homeManager = new HomeManager(plugin);

        var homeCmd = plugin.getCommand("home");
        var setHomeCmd = plugin.getCommand("sethome");
        var delHomeCmd = plugin.getCommand("delhome");

        if (homeCmd != null) {
            var executor = new HomeCommand(plugin, homeManager);
            homeCmd.setExecutor(executor);
            homeCmd.setTabCompleter(executor);
        }
        if (setHomeCmd != null) {
            setHomeCmd.setExecutor(new SetHomeCommand(homeManager));
        }
        if (delHomeCmd != null) {
            var executor = new DelHomeCommand(homeManager);
            delHomeCmd.setExecutor(executor);
            delHomeCmd.setTabCompleter(executor);
        }
    }

    public void shutdown() {
        if (homeManager != null) {
            homeManager.saveHomes();
        }
    }

    public HomeManager getHomeManager() {
        return homeManager;
    }
}
