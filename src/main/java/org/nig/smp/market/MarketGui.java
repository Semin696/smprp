package org.nig.smp.market;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.nig.smp.moneys.MoneyManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class MarketGui implements CommandExecutor, TabCompleter, Listener {

    private static final int CATEGORY_START = 0;
    private static final int CATEGORY_END = 8;
    private static final int[] ITEM_SLOTS = {9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35};
    private static final int PREV_SLOT = 45;
    private static final int SEARCH_SLOT = 47;
    private static final int NEXT_SLOT = 53;
    private static final int BALANCE_SLOT = 49;
    private static final int PER_PAGE = 27;

    private final Market market;
    private final Map<UUID, Integer> openPages = new HashMap<>();
    private final Map<UUID, Integer> openCategories = new HashMap<>();
    private final Map<UUID, String> searchQueries = new HashMap<>();

    private static final List<java.util.function.Predicate<Material>> CATEGORY_FILTERS = List.of(
            m -> m.isBlock() && m.isSolid() && !m.name().contains("ORE")
                    && !m.name().contains("LOG") && !m.name().contains("PLANKS")
                    && !m.name().contains("WOOD") && !m.name().contains("LEAVES")
                    && !m.name().contains("SAPLING") && !m.name().contains("SIGN")
                    && !m.name().contains("BANNER") && !m.name().contains("BED"),
            m -> !m.isSolid() && m.isBlock() && !m.name().contains("POTTED")
                    && !m.name().contains("WALL_") && !m.name().contains("CANDLE")
                    && !m.name().contains("AIR") && !m.name().contains("WATER")
                    && !m.name().contains("LAVA"),
            m -> m.name().contains("REDSTONE") || m.name().contains("REPEATER")
                    || m.name().contains("COMPARATOR") || m.name().contains("PISTON")
                    || m.name().contains("RAIL") || m.name().contains("DISPENSER")
                    || m.name().contains("DROPPER") || m.name().contains("HOPPER")
                    || m.name().contains("OBSERVER") || m.name().contains("TARGET")
                    || m.name().contains("DAYLIGHT") || m == Material.LEVER
                    || m.name().contains("BUTTON") || m.name().contains("PRESSURE")
                    || m.name().contains("TRIPWIRE") || m.name().contains("LECTERN")
                    || m.name().contains("BELL") || m.name().contains("LAMP"),
            m -> m.name().contains("PICKAXE") || m.name().contains("SHOVEL")
                    || m.name().contains("AXE") || m.name().contains("HOE")
                    || m.name().contains("FISHING") || m.name().contains("SHEARS")
                    || m.name().contains("FLINT") || m.name().contains("COMPASS")
                    || m.name().contains("CLOCK") || m.name().contains("SPYGLASS")
                    || m.name().contains("BRUSH") || m.name().contains("BUCKET")
                    || m.name().contains("BONE_MEAL"),
            m -> m.name().contains("SWORD") || m.name().contains("BOW")
                    || m.name().contains("CROSSBOW") || m.name().contains("TRIDENT")
                    || m.name().contains("SHIELD") || m.name().contains("ARROW")
                    || m.name().contains("MACE") || m.name().contains("WIND_CHARGE"),
            m -> m.name().contains("HELMET") || m.name().contains("CHESTPLATE")
                    || m.name().contains("LEGGINGS") || m.name().contains("BOOTS")
                    || m.name().contains("TURTLE_HELMET") || m.name().contains("ELYTRA"),
            m -> m.isEdible() || m.name().contains("COOKED") || m.name().contains("RAW")
                    || m.name().contains("BREAD") || m.name().contains("APPLE")
                    || m.name().contains("MUSHROOM_STEW") || m.name().contains("BEETROOT")
                    || m.name().contains("CARROT") || m.name().contains("POTATO")
                    || m.name().contains("PUMPKIN_PIE") || m.name().contains("COOKIE")
                    || m.name().contains("HONEY_BOTTLE") || m.name().contains("MELON")
                    || m == Material.CHORUS_FRUIT,
            m -> m.name().contains("INGOT") || m.name().contains("GEM")
                    || m.name().contains("CRYSTAL") || m.name().contains("DUST")
                    || m.name().contains("NUGGET") || m.name().contains("RAW_")
                    || m == Material.DIAMOND || m == Material.EMERALD
                    || m == Material.QUARTZ || m == Material.AMETHYST_SHARD
                    || m == Material.NETHERITE_SCRAP || m.name().contains("SHARD")
                    || m == Material.ECHO_SHARD || m == Material.HEART_OF_THE_SEA
                    || m == Material.NAUTILUS_SHELL || m.name().contains("SCUTE")
                    || m == Material.LEATHER || m.name().contains("HIDE")
                    || m == Material.STRING || m == Material.FEATHER
                    || m.name().contains("BONE") || m == Material.GUNPOWDER
                    || m.name().contains("FLOWER") || m.name().contains("DYE")
                    || m.name().contains("PAPER") || m == Material.BOOK
                    || m.name().contains("SLIME") || m.name().contains("CLAY")
                    || m.name().contains("BRICK") || m.name().contains("NETHER_STAR")
                    || m.name().contains("SHULKER") || m.name().contains("SUGAR")
                    || m.name().contains("BLAZE") || m.name().contains("MAGMA")
                    || m.name().contains("GLOWSTONE") || m.name().contains("REDSTONE")
                    || m.name().equals("GHAST_TEAR") || m.name().contains("PHANTOM")
                    || m.name().contains("SPIDER_EYE") || m.name().contains("ROTTEN")
                    || m.name().contains("ENDER_PEARL") || m.name().contains("GOLDEN_APPLE")
                    || m.name().contains("ENCHANTED_GOLDEN_APPLE")
    );

    private static final java.util.function.Predicate<Material> OTHER_FILTER = m -> {
        for (var f : CATEGORY_FILTERS) {
            if (f.test(m)) return false;
        }
        return true;
    };

    private static final List<Category> CATEGORIES = List.of(
            new Category("Строительство", Material.BRICK, CATEGORY_FILTERS.get(0)),
            new Category("Декор", Material.PEONY, CATEGORY_FILTERS.get(1)),
            new Category("Красная пыль", Material.REDSTONE, CATEGORY_FILTERS.get(2)),
            new Category("Инструменты", Material.DIAMOND_PICKAXE, CATEGORY_FILTERS.get(3)),
            new Category("Оружие", Material.DIAMOND_SWORD, CATEGORY_FILTERS.get(4)),
            new Category("Броня", Material.DIAMOND_CHESTPLATE, CATEGORY_FILTERS.get(5)),
            new Category("Еда", Material.COOKED_BEEF, CATEGORY_FILTERS.get(6)),
            new Category("Материалы", Material.DIAMOND, CATEGORY_FILTERS.get(7)),
            new Category("Прочее", Material.CHEST, OTHER_FILTER)
    );

    public MarketGui(Market market) {
        this.market = market;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cТолько игроки могут использовать эту команду");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("search")) {
            String query = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "";
            if (query.isEmpty()) {
                player.sendMessage("§cИспользование: /market search <название>");
                return true;
            }
            searchQueries.put(player.getUniqueId(), query.toLowerCase());
            openSearch(player, query, 0);
            return true;
        }
        searchQueries.remove(player.getUniqueId());
        open(player, 0, 0);
        return true;
    }

    public void open(Player player, int categoryIndex, int page) {
        if (categoryIndex < 0 || categoryIndex >= CATEGORIES.size()) categoryIndex = 0;
        Category category = CATEGORIES.get(categoryIndex);

        List<Material> items = market.getItems().stream()
                .filter(category.filter())
                .collect(Collectors.toList());

        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / PER_PAGE));
        if (page >= totalPages) page = 0;

        int from = page * PER_PAGE;
        int to = Math.min(from + PER_PAGE, items.size());
        List<Material> pageItems = items.subList(from, to);

        Inventory inv = Bukkit.createInventory(null, 54, market.getMessage("gui-title", "category", category.name()));

        for (int i = 0; i < 9; i++) {
            if (i < CATEGORIES.size()) {
                Category cat = CATEGORIES.get(i);
                ItemStack btn = createItem(cat.icon(),
                        (i == categoryIndex ? "§a§l" : "§7") + cat.name(),
                        "§7Товаров: §f" + market.getItems().stream().filter(cat.filter()).count());
                inv.setItem(i, btn);
            } else {
                inv.setItem(i, createItem(Material.GRAY_STAINED_GLASS_PANE, " "));
            }
        }

        fillBorder(inv);
        addSearchButton(inv);
        fillItems(inv, pageItems, page, totalPages, player);
        addBalance(inv, player);

        player.openInventory(inv);
        openPages.put(player.getUniqueId(), page);
        openCategories.put(player.getUniqueId(), categoryIndex);
    }

    public void openSearch(Player player, String query, int page) {
        List<Material> items = market.getItems().stream()
                .filter(m -> m.name().toLowerCase().contains(query))
                .collect(Collectors.toList());

        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / PER_PAGE));
        if (page >= totalPages) page = 0;

        int from = page * PER_PAGE;
        int to = Math.min(from + PER_PAGE, items.size());
        List<Material> pageItems = items.subList(from, to);

        Inventory inv = Bukkit.createInventory(null, 54, "§6§lМАРКЕТ §8| §7Поиск: §f" + query);

        for (int i = 0; i < 9; i++) {
            inv.setItem(i, createItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        fillBorder(inv);
        addSearchButton(inv);
        fillItems(inv, pageItems, page, totalPages, player);
        addBalance(inv, player);

        player.openInventory(inv);
        openPages.put(player.getUniqueId(), page);
    }

    private void addSearchButton(Inventory inv) {
        inv.setItem(SEARCH_SLOT, createItem(Material.COMPASS, "§bПоиск предметов",
                "§7Нажмите или напишите:",
                "§e/market search <название>"));
    }

    private void fillItems(Inventory inv, List<Material> pageItems, int page, int totalPages, Player player) {
        for (int i = 0; i < pageItems.size() && i < ITEM_SLOTS.length; i++) {
            Material mat = pageItems.get(i);
            double price = market.getPrice(mat);
            Market.Tier tier = market.getItemTier(mat);
            ItemStack display = new ItemStack(mat);
            ItemMeta meta = display.getItemMeta();
            if (meta == null) continue;
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            String tierColor;
            String tierName;
            switch (tier) {
                case LEGENDARY:
                    tierColor = "§6";
                    tierName = "Легендарный";
                    break;
                case EPIC:
                    tierColor = "§d";
                    tierName = "Эпический";
                    break;
                case RARE:
                    tierColor = "§e";
                    tierName = "Редкий";
                    break;
                default:
                    tierColor = "§a";
                    tierName = "Обычный";
                    break;
            }
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    tierColor + "✦ " + tierName + " §8| §f" + String.format("%.2f", price) + " ₽"));
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("§aНажмите, чтобы купить"));
            meta.lore(lore);
            display.setItemMeta(meta);
            inv.setItem(ITEM_SLOTS[i], display);
        }

        if (page > 0) {
            inv.setItem(PREV_SLOT, createItem(Material.ARROW, "§e← Предыдущая страница"));
        }
        if (page + 1 < totalPages) {
            inv.setItem(NEXT_SLOT, createItem(Material.ARROW, "§eСледующая страница →"));
        }
    }

    private void addBalance(Inventory inv, Player player) {
        MoneyManager mm = market.getPlugin().getMoneyManager();
        if (mm != null) {
            inv.setItem(BALANCE_SLOT, createItem(Material.GOLD_NUGGET,
                    "§6Баланс: §f" + mm.format(mm.getBalance(player.getUniqueId())) + " ₽"));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.contains("МАРКЕТ")) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        if (slot >= CATEGORY_START && slot <= CATEGORY_END) {
            if (slot < CATEGORIES.size()) {
                open(player, slot, 0);
            }
            return;
        }

        if (slot == PREV_SLOT) {
            int cat = openCategories.getOrDefault(player.getUniqueId(), 0);
            int page = openPages.getOrDefault(player.getUniqueId(), 0);
            if (page > 0) open(player, cat, page - 1);
            return;
        }

        if (slot == NEXT_SLOT) {
            int cat = openCategories.getOrDefault(player.getUniqueId(), 0);
            int page = openPages.getOrDefault(player.getUniqueId(), 0);
            List<Material> items = market.getItems().stream()
                    .filter(CATEGORIES.get(cat).filter())
                    .collect(Collectors.toList());
            int totalPages = (int) Math.ceil((double) items.size() / PER_PAGE);
            if (page + 1 < totalPages) open(player, cat, page + 1);
            return;
        }

        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            if (ITEM_SLOTS[i] == slot) {
                int cat = openCategories.getOrDefault(player.getUniqueId(), 0);
                int page = openPages.getOrDefault(player.getUniqueId(), 0);
                List<Material> items = market.getItems().stream()
                        .filter(CATEGORIES.get(cat).filter())
                        .collect(Collectors.toList());
                int idx = page * PER_PAGE + i;
                if (idx < items.size()) {
                    buyItem(player, items.get(idx));
                }
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        String title = event.getView().getTitle();
        if (title.contains("МАРКЕТ")) {
            openPages.remove(event.getPlayer().getUniqueId());
            openCategories.remove(event.getPlayer().getUniqueId());
        }
    }

    private void buyItem(Player player, Material mat) {
        double price = market.getPrice(mat);
        MoneyManager mm = market.getPlugin().getMoneyManager();
        if (mm == null) {
            player.sendMessage("§cМодуль денег не активен");
            return;
        }

        if (!mm.hasEnough(player.getUniqueId(), price)) {
            player.sendMessage(market.getMessage("no-money", "price", String.format("%.2f", price)));
            return;
        }

        ItemStack item = new ItemStack(mat);
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage("§cИнвентарь полон");
            return;
        }

        mm.withdraw(player.getUniqueId(), price);
        player.getInventory().addItem(item);
        player.sendMessage(market.getMessage("bought", "item", formatMaterial(mat), "price", String.format("%.2f", price)));
    }

    private String formatMaterial(Material mat) {
        String key = mat.getKey().getKey();
        String[] parts = key.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return "§f" + sb;
    }

    private void fillBorder(Inventory inv) {
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (i == PREV_SLOT || i == NEXT_SLOT || i == BALANCE_SLOT) continue;
            if (i >= CATEGORY_START && i <= CATEGORY_END) continue;
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

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return List.of();
    }

    private record Category(String name, Material icon, java.util.function.Predicate<Material> filter) {}
}
