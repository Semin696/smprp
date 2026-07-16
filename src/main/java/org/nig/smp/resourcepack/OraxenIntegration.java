package org.nig.smp.resourcepack;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.nig.smp.SDSPlugin;

import java.io.File;
import java.lang.reflect.Method;

public class OraxenIntegration {

    private final SDSPlugin plugin;
    private Plugin oraxenPlugin;
    private boolean available;

    public OraxenIntegration(SDSPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean init() {
        oraxenPlugin = Bukkit.getPluginManager().getPlugin("Oraxen");
        if (oraxenPlugin == null || !oraxenPlugin.isEnabled()) {
            plugin.getLogger().info("Oraxen not detected, using standalone pack mode");
            available = false;
            return false;
        }

        try {
            Class<?> oraxenPluginClass = oraxenPlugin.getClass();
            Method getMethod = oraxenPluginClass.getMethod("getInstance");
            Object instance = getMethod.invoke(null);

            Method getResourcePack = oraxenPluginClass.getMethod("getResourcePack");
            Object resourcePack = getResourcePack.invoke(instance);

            Method getFile = resourcePack.getClass().getMethod("getFile");
            File packFile = (File) getFile.invoke(resourcePack);

            if (packFile != null && packFile.exists()) {
                plugin.getLogger().info("Oraxen integration active, pack found at: " + packFile.getAbsolutePath());
                available = true;
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Oraxen integration failed: " + e.getMessage());
        }

        available = false;
        return false;
    }

    public boolean generatePack() {
        if (!available) return false;

        try {
            Method reloadPack = Class.forName("io.th0rgal.oraxen.api.OraxenPack")
                    .getMethod("reloadPack");
            reloadPack.invoke(null);
            plugin.getLogger().info("Oraxen pack regeneration triggered");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to trigger Oraxen pack generation: " + e.getMessage());
            return false;
        }
    }

    public File getGeneratedPack() {
        if (!available) return null;

        try {
            Method getPack = Class.forName("io.th0rgal.oraxen.api.OraxenPack")
                    .getMethod("getPack");
            return (File) getPack.invoke(null);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get Oraxen pack file: " + e.getMessage());
            return null;
        }
    }

    public boolean isAvailable() {
        return available;
    }
}
