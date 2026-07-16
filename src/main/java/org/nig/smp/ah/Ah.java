package org.nig.smp.ah;

import org.bukkit.Bukkit;
import org.nig.smp.SDSPlugin;

public class Ah {

    private final SDSPlugin plugin;
    private AhManager manager;

    public Ah(SDSPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        manager = new AhManager(plugin);

        var cmd = plugin.getCommand("ah");
        if (cmd != null) {
            var executor = new AhCommand(manager);
            var gui = new AhGui(manager, executor);
            executor.setGui(gui);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
            Bukkit.getPluginManager().registerEvents(gui, plugin);
        }

        plugin.getLogger().info("Auction House module loaded (with GUI)");
    }

    public AhManager getManager() {
        return manager;
    }
}
