package org.nig.smp.chat;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nig.smp.SDSPlugin;
import java.util.UUID;

public class SdsPlaceholderExpansion extends PlaceholderExpansion {

    private LuckPerms luckPerms;

    public SdsPlaceholderExpansion() {
        try {
            this.luckPerms = LuckPermsProvider.get();
        } catch (Exception e) {
            this.luckPerms = null;
        }
    }

    @Override
    public @NotNull String getIdentifier() {
        return "sds";
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
        if (player == null || !player.isOnline()) return "";
        if (!(player instanceof Player onlinePlayer)) return "";

        if (params.equalsIgnoreCase("voice") || params.equalsIgnoreCase("plasmovoice")) {
            Plugin vc = Bukkit.getPluginManager().getPlugin("voicechat");
            if (vc == null) {
                vc = Bukkit.getPluginManager().getPlugin("SimpleVoiceChat");
            }
            if (vc == null) return "§c●";

            Boolean connected = tryVoicechatApi(onlinePlayer);
            if (connected != null) return connected ? "§a●" : "§c●";

            connected = tryVoicechatLegacy(onlinePlayer);
            if (connected != null) return connected ? "§a●" : "§c●";

            connected = tryVoicechatService(onlinePlayer);
            if (connected != null) return connected ? "§a●" : "§c●";

            return "§c●";
        }

        if (luckPerms == null) return "";

        CachedMetaData meta = luckPerms.getPlayerAdapter(Player.class).getMetaData(onlinePlayer);

        if (params.equalsIgnoreCase("prefix")) {
            String prefix = meta.getPrefix();
            return prefix != null ? prefix : "";
        }

        if (params.equalsIgnoreCase("suffix")) {
            String suffix = meta.getSuffix();
            return suffix != null && !suffix.isEmpty() ? suffix : "Нету";
        }

        if (params.equalsIgnoreCase("team")) {
            SDSPlugin sds = (SDSPlugin) Bukkit.getPluginManager().getPlugin("mainplug");
            if (sds == null || sds.getTeamModule() == null) return "";
            return sds.getTeamModule().getTeamManager().getTeamName(onlinePlayer);
        }

        return null;
    }

    private Boolean tryVoicechatApi(Player player) {
        try {
            Class<?> pluginClass = Class.forName("de.maxhenkel.voicechat.VoicechatPlugin");
            Object instance = pluginClass.getMethod("instance").invoke(null);
            Object api = instance.getClass().getMethod("getApi").invoke(instance);
            return (boolean) api.getClass().getMethod("isConnected", UUID.class).invoke(api, player.getUniqueId());
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean tryVoicechatLegacy(Player player) {
        try {
            Class<?> pluginClass = Class.forName("de.maxhenkel.voicechat.VoicechatPlugin");
            Object instance = pluginClass.getMethod("getInstance").invoke(null);
            Object api = instance.getClass().getMethod("getApi").invoke(instance);
            return (boolean) api.getClass().getMethod("isConnected", UUID.class).invoke(api, player.getUniqueId());
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean tryVoicechatService(Player player) {
        try {
            Class<?> apiClass = Class.forName("de.maxhenkel.voicechat.api.VoicechatApi");
            Object api = Bukkit.getServicesManager().load(apiClass);
            if (api == null) return null;
            return (boolean) api.getClass().getMethod("isConnected", UUID.class).invoke(api, player.getUniqueId());
        } catch (Exception e) {
            return null;
        }
    }
}
