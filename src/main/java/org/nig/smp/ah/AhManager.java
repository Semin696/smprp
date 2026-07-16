package org.nig.smp.ah;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.nig.smp.SDSPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AhManager {

    private final SDSPlugin plugin;
    private final Map<Integer, AhListing> listings = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private File dataFile;

    public AhManager(SDSPlugin plugin) {
        this.plugin = plugin;
        loadListings();
    }

    public String sellItem(Player player, double price) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            return msg("no-item");
        }
        if (price <= 0) {
            return msg("invalid-price");
        }

        int current = getPlayerListingCount(player);
        int limit = getMaxListings(player);
        if (current >= limit) {
            return msg("limit-reached", "limit", String.valueOf(limit));
        }

        ItemStack toSell = item.clone();
        item.setAmount(0);
        player.getInventory().setItemInMainHand(item);

        int id = nextId.getAndIncrement();
        AhListing listing = new AhListing(id, player.getUniqueId(), player.getName(), toSell, price, System.currentTimeMillis());
        listings.put(id, listing);
        saveListings();

        return msg("listed", "id", String.valueOf(id), "price", String.format("%.2f", price));
    }

    public String buyItem(Player player, int id) {
        AhListing listing = listings.get(id);
        if (listing == null) {
            return msg("not-found");
        }
        if (listing.getSeller().equals(player.getUniqueId())) {
            return msg("own-listing");
        }

        if (!plugin.getMoneyManager().hasEnough(player.getUniqueId(), listing.getPrice())) {
            return msg("no-money");
        }

        if (player.getInventory().firstEmpty() == -1) {
            return msg("full-inventory");
        }

        plugin.getMoneyManager().withdraw(player.getUniqueId(), listing.getPrice());
        plugin.getMoneyManager().deposit(listing.getSeller(), listing.getPrice());

        player.getInventory().addItem(listing.getItem().clone());

        Player seller = Bukkit.getPlayer(listing.getSeller());
        if (seller != null && seller.isOnline()) {
            seller.sendMessage(prefix().append(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(msg("sold", "id", String.valueOf(id), "price", String.format("%.2f", listing.getPrice())))));
        }

        listings.remove(id);
        saveListings();

        return msg("bought", "id", String.valueOf(id), "price", String.format("%.2f", listing.getPrice()));
    }

    public String cancelListing(Player player, int id) {
        AhListing listing = listings.get(id);
        if (listing == null) {
            return msg("not-found");
        }
        if (!listing.getSeller().equals(player.getUniqueId()) && !player.hasPermission("ah.admin")) {
            return msg("not-yours");
        }

        ItemStack item = listing.getItem();
        if (player.getInventory().firstEmpty() == -1) {
            return msg("full-inventory");
        }

        player.getInventory().addItem(item);
        listings.remove(id);
        saveListings();

        return msg("cancelled", "id", String.valueOf(id));
    }

    public List<AhListing> getListings(int page, int perPage) {
        List<AhListing> all = new ArrayList<>(listings.values());
        all.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        int from = page * perPage;
        int to = Math.min(from + perPage, all.size());
        if (from >= all.size()) return List.of();
        return all.subList(from, to);
    }

    public int getTotalPages(int perPage) {
        return Math.max(1, (int) Math.ceil((double) listings.size() / perPage));
    }

    public List<AhListing> getPlayerListings(Player player) {
        List<AhListing> result = new ArrayList<>();
        for (AhListing listing : listings.values()) {
            if (listing.getSeller().equals(player.getUniqueId())) {
                result.add(listing);
            }
        }
        return result;
    }

    public int getListingCount() {
        return listings.size();
    }

    public int getPlayerListingCount(Player player) {
        int count = 0;
        for (AhListing listing : listings.values()) {
            if (listing.getSeller().equals(player.getUniqueId())) {
                count++;
            }
        }
        return count;
    }

    public int getMaxListings(Player player) {
        int configLimit = 5;
        int max = 0;
        for (var info : player.getEffectivePermissions()) {
            String perm = info.getPermission();
            if (perm.startsWith("ah.limit.")) {
                String suffix = perm.substring("ah.limit.".length());
                try {
                    int value = Integer.parseInt(suffix);
                    if (value > max) max = value;
                } catch (NumberFormatException ignored) {}
            }
        }
        return Math.max(configLimit, max);
    }

    private void loadListings() {
        dataFile = new File(plugin.getDataFolder(), "ah_data.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        int maxId = 0;
        for (String key : cfg.getKeys(false)) {
            int id = Integer.parseInt(key);
            listings.put(id, AhListing.loadFromConfig(id, cfg.getConfigurationSection(key)));
            if (id > maxId) maxId = id;
        }
        nextId.set(maxId + 1);
        plugin.getLogger().info("Loaded " + listings.size() + " auction listings");
    }

    private void saveListings() {
        if (dataFile == null) return;
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<Integer, AhListing> entry : listings.entrySet()) {
            entry.getValue().saveToConfig(cfg.createSection(String.valueOf(entry.getKey())));
        }
        try {
            cfg.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save ah_data.yml: " + e.getMessage());
        }
    }

    public Component prefix() {
        YamlConfiguration cfg = SDSPlugin.loadConfigWithDefaults(plugin, "ah_config.yml");
        String raw = cfg.getString("prefix", "&#65FFAE&lm&#57F574&lc&#48EA3A&lr&#3AE000&lu");
        return LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
    }

    public String msg(String key, String... placeholders) {
        YamlConfiguration cfg = SDSPlugin.loadConfigWithDefaults(plugin, "ah_config.yml");
        String text = cfg.getString("messages." + key);
        if (text == null) return "§cMessage not found: " + key;
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            text = text.replace("%" + placeholders[i] + "%", placeholders[i + 1]);
        }
        return SDSPlugin.color(text);
    }
}
