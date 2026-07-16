package org.nig.smp.moneys;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nig.smp.SDSPlugin;

public class MoneyPlaceholderExpansion extends PlaceholderExpansion {

    private final SDSPlugin plugin;

    public MoneyPlaceholderExpansion(SDSPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "moneys";
    }

    @Override
    public @NotNull String getAuthor() {
        return "nig";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        MoneyManager manager = plugin.getMoneyManager();

        if (params.equalsIgnoreCase("balance")) {
            if (player == null) return "0";
            return manager.format(manager.getBalance(player.getUniqueId()));
        }

        if (params.toLowerCase().startsWith("balance_")) {
            String targetName = params.substring(8);
            for (Player online : plugin.getServer().getOnlinePlayers()) {
                if (online.getName().equalsIgnoreCase(targetName)) {
                    return manager.format(manager.getBalance(online.getUniqueId()));
                }
            }
            return "0";
        }

        return null;
    }
}
