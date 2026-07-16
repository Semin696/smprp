package org.nig.smp.order;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    private final PrivateChatManager chatManager;
    private final OrderConfig config;

    public ChatListener(PrivateChatManager chatManager, OrderConfig config) {
        this.chatManager = chatManager;
        this.config = config;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!chatManager.isInChat(player)) return;

        event.setCancelled(true);

        Player partner = chatManager.getPartner(player);
        if (partner == null) {
            chatManager.endChat(player);
            player.sendMessage(config.format("no-chat"));
            return;
        }

        Component msg = event.message();
        Component formatted = Component.text(config.getPrefix() + " <")
                .append(player.displayName())
                .append(Component.text("> "))
                .append(msg);

        player.sendMessage(formatted);
        partner.sendMessage(formatted);
    }
}
