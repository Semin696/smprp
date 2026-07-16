package org.nig.smp.order;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PrivateChatManager {

    private final Map<UUID, UUID> chats = new ConcurrentHashMap<>();

    public void startChat(Player p1, Player p2) {
        chats.put(p1.getUniqueId(), p2.getUniqueId());
        chats.put(p2.getUniqueId(), p1.getUniqueId());
    }

    public void endChat(Player p) {
        UUID partnerId = chats.remove(p.getUniqueId());
        if (partnerId != null) {
            chats.remove(partnerId);
        }
    }

    public boolean isInChat(Player p) {
        return chats.containsKey(p.getUniqueId());
    }

    public Player getPartner(Player p) {
        UUID partnerId = chats.get(p.getUniqueId());
        if (partnerId == null) return null;
        return p.getServer().getPlayer(partnerId);
    }
}
