package org.nig.smp.sell;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SellGui implements Listener {

    private static final int SELL_SLOTS_START = 0;
    private static final int SELL_SLOTS_END = 17;
    private static final int CONFIRM_SLOT = 22;
    private static final int TOTAL_SLOT = 24;

    private final Sell module;
    private final Map<UUID, Inventory> openGuis = new HashMap<>();

    public SellGui(Sell module) {
        this.module = module;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, module.getMessage("gui-sell-title", "total", "0"));

        ItemStack border = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 18; i < 27; i++) {
            inv.setItem(i, border);
        }

        ItemStack confirm = createGuiItem(Material.LIME_WOOL, module.getMessage("confirm-lore"));
        inv.setItem(CONFIRM_SLOT, confirm);

        ItemStack totalDisplay = createGuiItem(Material.PAPER, module.getMessage("total-lore", "total", "0"));
        inv.setItem(TOTAL_SLOT, totalDisplay);

        player.openInventory(inv);
        openGuis.put(player.getUniqueId(), inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory inv = event.getInventory();
        if (!openGuis.containsKey(player.getUniqueId()) || !openGuis.get(player.getUniqueId()).equals(inv)) return;

        int slot = event.getRawSlot();

        if (slot >= 18 && slot < 27) {
            if (slot == CONFIRM_SLOT) {
                event.setCancelled(true);
                sellItems(player, inv);
                return;
            }
            event.setCancelled(true);
            return;
        }

        if (slot >= 0 && slot <= 17) {
            Bukkit.getScheduler().runTask(module.getPlugin(), () -> updateTotal(player, inv));
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory inv = event.getInventory();
        if (!openGuis.containsKey(player.getUniqueId()) || !openGuis.get(player.getUniqueId()).equals(inv)) return;

        for (int slot : event.getRawSlots()) {
            if (slot >= 18) {
                event.setCancelled(true);
                return;
            }
        }

        Bukkit.getScheduler().runTask(module.getPlugin(), () -> updateTotal(player, inv));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Inventory inv = event.getInventory();
        if (!openGuis.containsKey(player.getUniqueId()) || !openGuis.get(player.getUniqueId()).equals(inv)) return;

        openGuis.remove(player.getUniqueId());

        for (int i = SELL_SLOTS_START; i <= SELL_SLOTS_END; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && !item.getType().isAir()) {
                player.getInventory().addItem(item).values().forEach(left ->
                    player.getWorld().dropItemNaturally(player.getLocation(), left));
            }
        }
    }

    private void updateTotal(Player player, Inventory inv) {
        double total = 0;
        for (int i = SELL_SLOTS_START; i <= SELL_SLOTS_END; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && !item.getType().isAir()) {
                total += module.getPriceManager().getPrice(item);
            }
        }

        ItemStack totalDisplay = createGuiItem(Material.PAPER,
            module.getMessage("total-lore", "total", String.format("%.2f", total)));
        inv.setItem(TOTAL_SLOT, totalDisplay);

        ItemMeta meta = inv.getItem(CONFIRM_SLOT).getItemMeta();
        if (total > 0) {
            meta.displayName(LegacyComponentSerializer.legacyAmpersand()
                .deserialize(module.getMessage("confirm-lore")));
        } else {
            meta.displayName(LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&cНет предметов для продажи"));
        }
        inv.getItem(CONFIRM_SLOT).setItemMeta(meta);
    }

    private void sellItems(Player player, Inventory inv) {
        double total = 0;
        List<ItemStack> items = new java.util.ArrayList<>();

        for (int i = SELL_SLOTS_START; i <= SELL_SLOTS_END; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && !item.getType().isAir()) {
                total += module.getPriceManager().getPrice(item);
                items.add(item);
                inv.setItem(i, null);
            }
        }

        if (total <= 0) {
            player.sendMessage(module.getPrefix().append(module.getMessageComponent("nothing-to-sell")));
            return;
        }

        module.depositMoney(player, total);
        player.sendMessage(module.getPrefix().append(module.getMessageComponent("sold", "amount", String.format("%.2f", total))));
        player.closeInventory();
    }

    private ItemStack createGuiItem(Material material, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" "));
        meta.lore(Arrays.asList(
            LegacyComponentSerializer.legacyAmpersand().deserialize(lore)
        ));
        item.setItemMeta(meta);
        return item;
    }
}
