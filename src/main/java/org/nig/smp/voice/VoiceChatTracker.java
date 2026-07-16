package org.nig.smp.voice;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.nig.smp.SDSPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VoiceChatTracker implements Listener {

    private final Set<UUID> voicePlayers = new HashSet<>();
    private final Pattern uuidPattern = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    public VoiceChatTracker(SDSPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        java.util.logging.Logger root = java.util.logging.Logger.getLogger("");
        root.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                String msg = record.getMessage();
                if (msg == null) return;
                if (!msg.contains("[voicechat]")) return;

                UUID uuid = extractUuid(msg);
                if (uuid == null) return;

                voicePlayers.add(uuid);
            }

            @Override
            public void flush() {}

            @Override
            public void close() throws SecurityException {}
        });
    }

    public boolean hasVoiceChat(Player player) {
        return voicePlayers.contains(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        voicePlayers.remove(event.getPlayer().getUniqueId());
    }

    private UUID extractUuid(String text) {
        Matcher m = uuidPattern.matcher(text);
        if (m.find()) {
            try {
                return UUID.fromString(m.group());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }
}
