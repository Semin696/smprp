package org.nig.smp.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.nig.smp.SDSPlugin;

public class ChatListener implements Listener {

    private static final int LOCAL_RADIUS = 20;

    private final ChatConfig config;
    private LuckPerms luckPerms;

    public ChatListener(ChatConfig config) {
        this.config = config;
        try {
            this.luckPerms = LuckPermsProvider.get();
        } catch (Exception e) {
            this.luckPerms = null;
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        event.setCancelled(true);

        Player player = event.getPlayer();
        String plainMessage = LegacyComponentSerializer.legacySection().serialize(event.message());

        boolean isPublic = plainMessage.startsWith("!");
        String messageText = isPublic ? plainMessage.substring(1).trim() : plainMessage;

        String prefix = "";
        String suffix = "";
        String teamRaw = "";
        String voice = "";

        if (luckPerms != null) {
            CachedMetaData meta = luckPerms.getPlayerAdapter(Player.class).getMetaData(player);
            if (meta.getPrefix() != null) prefix = meta.getPrefix();
            if (meta.getSuffix() != null) suffix = meta.getSuffix();
        }

        SDSPlugin sds = (SDSPlugin) Bukkit.getPluginManager().getPlugin("mainplug");
        if (sds != null) {
            if (sds.getVoiceChatTracker() != null && sds.getVoiceChatTracker().hasVoiceChat(player)) {
                voice = "§a§lᴠ ";
            }
        }

        String format = isPublic ? config.getPublicFormat() : config.getLocalFormat();
        String playerName = player.getName();
        Component displayName = player.displayName();

        format = format
                .replace("{prefix}", prefix)
                .replace("{suffix}", suffix)
                .replace("{team}", "")
                .replace("{voice}", voice)
                .replace("{displayname}", LegacyComponentSerializer.legacySection().serialize(displayName));

        if (config.usePlaceholderApi()) {
            format = PlaceholderAPI.setPlaceholders(player, format);
        }

        String[] parts = format.split("\\{player\\}", 2);
        String beforePlayer = parts[0];
        String afterPlayer = parts.length > 1 ? parts[1].replace("{message}", "") : "";

        Component beforeComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(beforePlayer);
        Component afterComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(afterPlayer);
        Component messageComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(messageText);

        Component playerComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(playerName);

        if (sds != null && sds.getTeamModule() != null) {
            String teamName = sds.getTeamModule().getTeamManager().getTeamName(player);
            if (!teamName.isEmpty()) {
                playerComponent = playerComponent.hoverEvent(
                        HoverEvent.showText(
                                LegacyComponentSerializer.legacyAmpersand().deserialize("§7команда §f» §7" + teamName)
                        )
                );
            }
        }

        Component formatted = beforeComponent
                .append(playerComponent)
                .append(afterComponent)
                .append(Component.space())
                .append(messageComponent);

        if (isPublic) {
            for (Audience viewer : event.viewers()) {
                viewer.sendMessage(formatted);
            }
        } else {
            Location playerLoc = player.getLocation();
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer.getWorld().equals(player.getWorld())
                        && viewer.getLocation().distanceSquared(playerLoc) <= LOCAL_RADIUS * LOCAL_RADIUS) {
                    viewer.sendMessage(formatted);
                }
            }
        }
    }
}
