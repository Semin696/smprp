package org.nig.smp.sell;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.nig.smp.SDSPlugin;

public class Sell {

    private final SDSPlugin plugin;
    private YamlConfiguration config;
    private PriceManager priceManager;
    private SellGui gui;

    public Sell(SDSPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        loadConfig();

        Plugin auctionPlugin = Bukkit.getPluginManager().getPlugin("AuctionPlugin");
        if (auctionPlugin == null) {
            plugin.getLogger().warning("AuctionPlugin not found — using fallback prices (all items = 0)");
        } else {
            plugin.getLogger().info("AuctionPlugin found — prices from market");
        }

        double enchantPct = config.getDouble("enchantment-percent", 3.0);
        double marketPct = config.getDouble("market-stack-percent", 1.0);
        priceManager = new PriceManager(auctionPlugin, enchantPct, marketPct);

        gui = new SellGui(this);
        Bukkit.getPluginManager().registerEvents(gui, plugin);

        var cmd = plugin.getCommand("sell");
        if (cmd != null) {
            var exec = new SellCommand(this);
            cmd.setExecutor(exec);
            cmd.setTabCompleter(exec);
        }
    }

    private void loadConfig() {
        config = SDSPlugin.loadConfigWithDefaults(plugin, "sell_config.yml");
    }

    public void openSellGui(Player player) {
        gui.open(player);
    }

    public void sellAll(Player player) {
        PlayerInventory inv = player.getInventory();
        double total = 0;
        int itemsSold = 0;

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) continue;
            total += priceManager.getPrice(item);
            itemsSold += item.getAmount();
            inv.setItem(i, null);
        }

        ItemStack[] armor = inv.getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            ItemStack item = armor[i];
            if (item == null || item.getType().isAir()) continue;
            total += priceManager.getPrice(item);
            itemsSold += item.getAmount();
            armor[i] = null;
        }
        inv.setArmorContents(armor);

        inv.setItemInOffHand(null);

        if (total <= 0) {
            player.sendMessage(getPrefix().append(getMessageComponent("nothing-to-sell")));
            return;
        }

        depositMoney(player, total);
        player.sendMessage(getPrefix().append(getMessageComponent("sold", "amount", String.format("%.2f", total))));
    }

    public void depositMoney(Player player, double amount) {
        if (plugin.getMoneyManager() != null) {
            plugin.getMoneyManager().deposit(player.getUniqueId(), amount);
        }
    }

    public Component getPrefix() {
        String raw = config.getString("prefix", "&#65FFAE&lm&#57F574&lc&#48EA3A&lr&#3AE000&lu");
        return LegacyComponentSerializer.legacyAmpersand().deserialize(raw + " ");
    }

    public Component getMessageComponent(String path, String... placeholders) {
        String text = config.getString("messages." + path);
        if (text == null) return Component.text("Message not found: " + path);
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            text = text.replace("%" + placeholders[i] + "%", placeholders[i + 1]);
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    public String getMessage(String path, String... placeholders) {
        String text = config.getString("messages." + path);
        if (text == null) return "Message not found: " + path;
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            text = text.replace("%" + placeholders[i] + "%", placeholders[i + 1]);
        }
        return text;
    }

    public PriceManager getPriceManager() {
        return priceManager;
    }

    public SDSPlugin getPlugin() {
        return plugin;
    }
}
