package org.nig.smp.ah;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AhCommand implements CommandExecutor, TabCompleter {

    private final AhManager manager;
    private AhGui gui;
    private static final int PER_PAGE = 9;

    public AhCommand(AhManager manager) {
        this.manager = manager;
    }

    public void setGui(AhGui gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cТолько игроки могут использовать эту команду");
            return true;
        }

        if (args.length == 0) {
            if (gui != null) {
                gui.open(player, 0);
            } else {
                browse(player, 0);
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "sell" -> {
                if (args.length < 2) {
                    player.sendMessage(manager.prefix().append(Component.text("§cИспользование: /ah sell <цена>")));
                    return true;
                }
                try {
                    double price = Double.parseDouble(args[1]);
                    player.sendMessage(manager.prefix().append(Component.text(manager.sellItem(player, price))));
                } catch (NumberFormatException e) {
                    player.sendMessage(manager.prefix().append(Component.text("§cНеверная цена")));
                }
            }
            case "buy" -> {
                if (args.length < 2) {
                    player.sendMessage(manager.prefix().append(Component.text("§cИспользование: /ah buy <id>")));
                    return true;
                }
                try {
                    int id = Integer.parseInt(args[1]);
                    player.sendMessage(manager.prefix().append(Component.text(manager.buyItem(player, id))));
                } catch (NumberFormatException e) {
                    player.sendMessage(manager.prefix().append(Component.text("§cНеверный ID")));
                }
            }
            case "cancel" -> {
                if (args.length < 2) {
                    player.sendMessage(manager.prefix().append(Component.text("§cИспользование: /ah cancel <id>")));
                    return true;
                }
                try {
                    int id = Integer.parseInt(args[1]);
                    player.sendMessage(manager.prefix().append(Component.text(manager.cancelListing(player, id))));
                } catch (NumberFormatException e) {
                    player.sendMessage(manager.prefix().append(Component.text("§cНеверный ID")));
                }
            }
            case "list" -> {
                if (gui != null) {
                    gui.openMyListings(player);
                } else {
                    listPlayer(player);
                }
            }
            case "browse" -> {
                int page = 0;
                if (args.length > 1) {
                    try {
                        page = Math.max(0, Integer.parseInt(args[1]) - 1);
                    } catch (NumberFormatException ignored) {}
                }
                if (gui != null) {
                    gui.open(player, page);
                } else {
                    browse(player, page);
                }
            }
            default -> {
                if (gui != null) {
                    gui.open(player, 0);
                } else {
                    int page = 0;
                    try {
                        page = Math.max(0, Integer.parseInt(args[0]) - 1);
                    } catch (NumberFormatException ignored) {}
                    browse(player, page);
                }
            }
        }

        return true;
    }

    private void browse(Player player, int page) {
        int totalPages = manager.getTotalPages(PER_PAGE);
        List<AhListing> listings = manager.getListings(page, PER_PAGE);

        if (listings.isEmpty()) {
            if (manager.getListingCount() == 0) {
                player.sendMessage(manager.prefix().append(Component.text("§eНа аукционе нет лотов")));
            } else {
                player.sendMessage(manager.prefix().append(Component.text("§cСтраница " + (page + 1) + " пуста")));
            }
            return;
        }

        player.sendMessage(Component.text("§8⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤"));
        player.sendMessage(Component.text("§6§lАУКЦИОН §8| §7Страница " + (page + 1) + "/" + totalPages));
        player.sendMessage(Component.text(""));

        for (AhListing listing : listings) {
            ItemStack item = listing.getItem();
            String itemName = getItemName(item);
            int amount = item.getAmount();

            player.sendMessage(Component.text(" §8#" + listing.getId() + " §f" + itemName
                    + (amount > 1 ? " §8x" + amount : "")
                    + " §7- §6" + String.format("%.2f", listing.getPrice()) + " ₽"
                    + " §8(" + listing.getSellerName() + ")"));
        }

        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("§7Купить: §f/ah buy <id>   §7Продать: §f/ah sell <цена>"));
        player.sendMessage(Component.text("§8⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤"));
    }

    private void listPlayer(Player player) {
        List<AhListing> playerListings = manager.getPlayerListings(player);

        if (playerListings.isEmpty()) {
            player.sendMessage(manager.prefix().append(Component.text("§eУ вас нет активных лотов")));
            return;
        }

        player.sendMessage(Component.text("§8⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤"));
        player.sendMessage(Component.text("§6§lМОИ ЛОТЫ"));
        player.sendMessage(Component.text(""));

        for (AhListing listing : playerListings) {
            ItemStack item = listing.getItem();
            String itemName = getItemName(item);
            player.sendMessage(Component.text(" §8#" + listing.getId() + " §f" + itemName
                    + " §7- §6" + String.format("%.2f", listing.getPrice()) + " ₽"));
        }

        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("§7Отменить: §f/ah cancel <id>"));
        player.sendMessage(Component.text("§8⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤⏤"));
    }

    public String getItemName(ItemStack item) {
        if (item == null || item.getType().isAir()) return "§7Пусто";
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return "§f" + meta.getDisplayName();
        }
        String key = item.getType().getKey().getKey();
        String[] parts = key.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return "§f" + sb.toString();
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player)) return List.of();

        if (args.length == 1) {
            return Stream.of("sell", "buy", "cancel", "list", "browse")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("buy") || args[0].equalsIgnoreCase("cancel"))) {
            return List.of("<id>");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("sell")) {
            return List.of("<цена>");
        }

        return List.of();
    }
}
