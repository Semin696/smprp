package org.nig.smp.moneys;

import org.bukkit.Bukkit;
import org.nig.smp.SDSPlugin;

public class Moneys {

    private final SDSPlugin plugin;
    private MoneyManager moneyManager;

    public Moneys(SDSPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        moneyManager = new MoneyManager(plugin);

        var balanceCmd = plugin.getCommand("balance");
        var moneyCmd = plugin.getCommand("money");
        if (balanceCmd != null) {
            var exec = new BalanceCommand(plugin);
            balanceCmd.setExecutor(exec);
            balanceCmd.setTabCompleter(exec);
        }
        if (moneyCmd != null) {
            var exec = new MoneyCommand(plugin);
            moneyCmd.setExecutor(exec);
            moneyCmd.setTabCompleter(exec);
        }
        Bukkit.getPluginManager().registerEvents(new MobKillListener(plugin), plugin);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MoneyPlaceholderExpansion(plugin).register();
        }
    }

    public void shutdown() {
        if (moneyManager != null) {
            moneyManager.saveData();
        }
    }

    public MoneyManager getMoneyManager() {
        return moneyManager;
    }
}
