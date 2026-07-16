package org.nig.smp.order;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.nig.smp.SDSPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OrderCommand implements CommandExecutor {

    private final OrderConfig config;
    private final PrivateChatManager chatManager;
    private final SDSPlugin plugin;
    private final Map<UUID, UUID> pendingOffers = new HashMap<>();
    private final Map<UUID, String[]> offerDetails = new HashMap<>();

    private static final String ACCEPT_HOVER = "\u00A7aНажмите, чтобы принять";
    private static final String DECLINE_HOVER = "\u00A7cНажмите, чтобы отклонить";
    private static final String OFFER_HOVER = "\u00A7bНажмите, чтобы предложить продажу";

    public OrderCommand(OrderConfig config, PrivateChatManager chatManager, SDSPlugin plugin) {
        this.config = config;
        this.chatManager = chatManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(config.format("usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "stop" -> handleStop(player);
            case "offer" -> handleOffer(player, args);
            case "accept" -> handleAccept(player, args);
            case "decline" -> handleDecline(player, args);
            default -> handleOrder(player, args);
        }

        return true;
    }

    private void handleOrder(Player player, String[] args) {
        if (chatManager.isInChat(player)) {
            player.sendMessage(config.format("in-chat-order"));
            return;
        }

        String item = args[0];
        String amount = "";
        if (args.length > 1) {
            amount = " x" + args[1];
        }

        Component offerClick = Component.text("\u00A77[\u00A7bНажмите, чтобы предложить\u00A77]")
                .clickEvent(ClickEvent.runCommand("/order offer " + player.getName() + " " + item + (args.length > 1 ? " " + args[1] : "")))
                .hoverEvent(HoverEvent.showText(Component.text(OFFER_HOVER)));

        Component broadcast = config.formatRaw("broadcast",
                "%player%", player.getName(),
                "%item%", item,
                "%amount%", amount
        ).append(Component.text(" ")).append(offerClick);

        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.sendMessage(broadcast);
        }
    }

    private void handleOffer(Player player, String[] args) {
        if (chatManager.isInChat(player)) {
            player.sendMessage(config.format("in-chat-offer"));
            return;
        }

        if (args.length < 3) {
            player.sendMessage(config.format("usage"));
            return;
        }

        String buyerName = args[1];
        String item = args[2];
        String amount = args.length > 3 ? " x" + args[3] : "";

        Player buyer = plugin.getServer().getPlayerExact(buyerName);
        if (buyer == null || !buyer.isOnline()) {
            player.sendMessage(config.format("not-found"));
            return;
        }

        if (chatManager.isInChat(buyer)) {
            player.sendMessage(config.format("in-chat-order"));
            return;
        }

        pendingOffers.put(buyer.getUniqueId(), player.getUniqueId());
        offerDetails.put(buyer.getUniqueId(), new String[]{item, amount});

        player.sendMessage(config.format("offer-sent-seller",
                "%buyer%", buyer.getName(),
                "%item%", item,
                "%amount%", amount
        ));

        Component acceptBtn = Component.text("\u00A77[\u00A7a\u2705 Принять\u00A77]")
                .clickEvent(ClickEvent.runCommand("/order accept " + player.getName()))
                .hoverEvent(HoverEvent.showText(Component.text(ACCEPT_HOVER)));

        Component declineBtn = Component.text("\u00A77[\u00A7c\u274C Отклонить\u00A77]")
                .clickEvent(ClickEvent.runCommand("/order decline " + player.getName()))
                .hoverEvent(HoverEvent.showText(Component.text(DECLINE_HOVER)));

        Component offerMsg = config.formatRaw("offer-sent-buyer",
                "%seller%", player.getName(),
                "%item%", item,
                "%amount%", amount
        ).append(Component.text(" ")).append(acceptBtn).append(Component.text(" ")).append(declineBtn);

        buyer.sendMessage(offerMsg);
    }

    private void handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(config.format("usage"));
            return;
        }

        UUID sellerId = pendingOffers.remove(player.getUniqueId());
        if (sellerId == null) {
            player.sendMessage(config.format("no-chat"));
            return;
        }

        offerDetails.remove(player.getUniqueId());

        String sellerName = args[1];
        Player seller = plugin.getServer().getPlayerExact(sellerName);
        if (seller == null || !seller.getUniqueId().equals(sellerId) || !seller.isOnline()) {
            player.sendMessage(config.format("not-found"));
            return;
        }

        chatManager.startChat(player, seller);

        player.sendMessage(config.format("offer-accepted", "%player%", seller.getName()));
        seller.sendMessage(config.format("offer-accepted", "%player%", player.getName()));
    }

    private void handleDecline(Player player, String[] args) {
        UUID sellerId = pendingOffers.remove(player.getUniqueId());
        if (sellerId == null) {
            player.sendMessage(config.format("no-chat"));
            return;
        }

        offerDetails.remove(player.getUniqueId());

        if (args.length >= 2) {
            String sellerName = args[1];
            Player seller = plugin.getServer().getPlayerExact(sellerName);
            if (seller != null && seller.isOnline()) {
                seller.sendMessage(config.format("offer-declined", "%player%", player.getName()));
            }
        }

        player.sendMessage(config.format("no-chat"));
    }

    private void handleStop(Player player) {
        Player partner = chatManager.getPartner(player);
        if (partner == null) {
            player.sendMessage(config.format("no-chat"));
            return;
        }

        chatManager.endChat(player);
        player.sendMessage(config.format("chat-ended", "%player%", partner.getName()));
        partner.sendMessage(config.format("chat-ended", "%player%", player.getName()));
    }
}
