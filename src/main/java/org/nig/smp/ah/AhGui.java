package org.nig.smp.ah;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AhGui implements Listener {

    private static final int PER_PAGE = 28;
    private static final int[] ITEM_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    private static final int PREV_SLOT = 18;
    private static final int NEXT_SLOT = 26;
    private static final int INFO_SLOT = 4;
    private static final int SELL_SLOT = 48;
    private static final int MY_LISTINGS_SLOT = 50;

    private final AhManager manager;
    private final AhCommand command;
    private final Map<UUID, Integer> openPages = new HashMap<>();

    public AhGui(AhManager manager, AhCommand command) {
        this.manager = manager;
        this.command = command;
    }

    public void open(Player player, int page) {
        int totalPages = manager.getTotalPages(PER_PAGE);
        List<AhListing> listings = manager.getListings(page, PER_PAGE);

        Inventory inv = Bukkit.createInventory(null, 54, "§6§lАукцион §8| §7Стр. " + (page + 1) + "/" + totalPages);

        fillBorder(inv);

        for (int i = 0; i < ITEM_SLOTS.length && i < listings.size(); i++) {
            AhListing listing = listings.get(i);
            ItemStack display = listing.getItem().clone();
            ItemMeta meta = display.getItemMeta();
            List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
            if (lore == null) lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("§6Цена: §f" + String.format("%.2f", listing.getPrice()) + " ₽"));
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("§7Продавец: §f" + listing.getSellerName()));
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("§aНажмите, чтобы купить"));
            meta.lore(lore);
            meta.displayName(display.getItemMeta().displayName());
            display.setItemMeta(meta);
            inv.setItem(ITEM_SLOTS[i], display);
        }

        if (page > 0) {
            ItemStack prev = createItem(Material.ARROW, "§e← Предыдущая страница");
            inv.setItem(PREV_SLOT, prev);
        }
        if (page + 1 < totalPages) {
            ItemStack next = createItem(Material.ARROW, "§eСледующая страница →");
            inv.setItem(NEXT_SLOT, next);
        }

        ItemStack info = createItem(Material.GOLD_INGOT,
                "§6§lАУКЦИОН",
                "§7Всего лотов: §f" + manager.getListingCount(),
                "",
                "§7Кликните на предмет чтобы купить",
                "§7Листайте страницы стрелками");
        inv.setItem(INFO_SLOT, info);

        ItemStack sell = createItem(Material.EMERALD, "§a§lПРОДАТЬ ПРЕДМЕТ",
                "§7Держите предмет в руке",
                "§7и нажмите чтобы выставить",
                "§7с ценой из руки");
        inv.setItem(SELL_SLOT, sell);

        ItemStack my = createItem(Material.BOOK, "§e§lМОИ ЛОТЫ",
                "§7Нажмите чтобы посмотреть",
                "§7свои активные лоты");
        inv.setItem(MY_LISTINGS_SLOT, my);

        player.openInventory(inv);
        openPages.put(player.getUniqueId(), page);
    }

    public void openMyListings(Player player) {
        List<AhListing> playerListings = manager.getPlayerListings(player);
        Inventory inv = Bukkit.createInventory(null, 54, "§6§lМои лоты");

        fillBorder(inv);

        int slot = 10;
        for (AhListing listing : playerListings) {
            if (slot % 9 == 8) slot += 2;
            if (slot >= 44) break;

            ItemStack display = listing.getItem().clone();
            ItemMeta meta = display.getItemMeta();
            List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
            if (lore == null) lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("§6Цена: §f" + String.format("%.2f", listing.getPrice()) + " ₽"));
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("§cНажмите чтобы снять с продажи"));
            meta.lore(lore);
            display.setItemMeta(meta);
            inv.setItem(slot, display);
            slot++;
        }

        ItemStack back = createItem(Material.BARRIER, "§c← Назад к аукциону");
        inv.setItem(49, back);

        player.openInventory(inv);
        openPages.put(player.getUniqueId(), -1);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.contains("Аукцион") && !title.contains("Мои лоты")) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        if (title.contains("Мои лоты")) {
            if (slot == 49) {
                open(player, 0);
                return;
            }
            List<AhListing> playerListings = manager.getPlayerListings(player);
            int idx = -1;
            int s = 10;
            for (int i = 0; i < playerListings.size(); i++) {
                if (s % 9 == 8) s += 2;
                if (s == slot) { idx = i; break; }
                s++;
            }
            if (idx >= 0 && idx < playerListings.size()) {
                AhListing listing = playerListings.get(idx);
                String result = manager.cancelListing(player, listing.getId());
                player.sendMessage(manager.prefix().append(LegacyComponentSerializer.legacyAmpersand().deserialize(result)));
                openMyListings(player);
            }
            return;
        }

        if (slot == SELL_SLOT) {
            player.closeInventory();
            player.sendMessage(manager.prefix().append(
                LegacyComponentSerializer.legacyAmpersand().deserialize("§aВозьмите предмет в руку и используйте §e/ah sell <цена>")));
            return;
        }

        if (slot == MY_LISTINGS_SLOT) {
            openMyListings(player);
            return;
        }

        if (slot == PREV_SLOT) {
            int page = openPages.getOrDefault(player.getUniqueId(), 0);
            if (page > 0) open(player, page - 1);
            return;
        }

        if (slot == NEXT_SLOT) {
            int page = openPages.getOrDefault(player.getUniqueId(), 0);
            int totalPages = manager.getTotalPages(PER_PAGE);
            if (page + 1 < totalPages) open(player, page + 1);
            return;
        }

        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            if (ITEM_SLOTS[i] == slot) {
                int page = openPages.getOrDefault(player.getUniqueId(), 0);
                List<AhListing> listings = manager.getListings(page, PER_PAGE);
                if (i < listings.size()) {
                    AhListing listing = listings.get(i);
                    String result = manager.buyItem(player, listing.getId());
                    player.sendMessage(manager.prefix().append(LegacyComponentSerializer.legacyAmpersand().deserialize(result)));
                    open(player, page);
                }
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        String title = event.getView().getTitle();
        if (title.contains("Аукцион") || title.contains("Мои лоты")) {
            openPages.remove(event.getPlayer().getUniqueId());
        }
    }

    private void fillBorder(Inventory inv) {
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (i == PREV_SLOT || i == NEXT_SLOT || i == INFO_SLOT || i == SELL_SLOT || i == MY_LISTINGS_SLOT) continue;
            boolean isItemSlot = false;
            for (int s : ITEM_SLOTS) {
                if (s == i) { isItemSlot = true; break; }
            }
            if (isItemSlot) continue;
            inv.setItem(i, border);
        }
    }

    private ItemStack createItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name));
        if (loreLines.length > 0) {
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
            }
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }
}
