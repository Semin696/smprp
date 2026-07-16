package org.nig.smp.market;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.nig.smp.SDSPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Market {

    private final SDSPlugin plugin;
    private YamlConfiguration config;
    private MarketGui gui;

    private double commonMinPrice;
    private double commonMaxPrice;
    private double rareMinPrice;
    private double rareMaxPrice;
    private double epicMinPrice;
    private double epicMaxPrice;
    private double legendaryMinPrice;
    private double legendaryMaxPrice;

    public enum Tier {
        COMMON, RARE, EPIC, LEGENDARY
    }

    private static final Set<Material> ALWAYS_DISABLED = Set.of(
        Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK,
        Material.COMMAND_BLOCK_MINECART, Material.JIGSAW, Material.STRUCTURE_BLOCK,
        Material.STRUCTURE_VOID, Material.BARRIER, Material.LIGHT,
        Material.DEBUG_STICK, Material.KNOWLEDGE_BOOK, Material.BEDROCK,
        Material.END_PORTAL_FRAME, Material.END_GATEWAY, Material.END_PORTAL,
        Material.SPAWNER, Material.TRIAL_SPAWNER, Material.VAULT,
        Material.TEST_BLOCK, Material.TEST_INSTANCE_BLOCK
    );

    private Set<Material> disabledItems = new HashSet<>();
    private final Set<Material> legendaryItems = new HashSet<>();
    private final Set<Material> epicItems = new HashSet<>();
    private final Set<Material> rareItems = new HashSet<>();
    private final Set<Material> commonItems = new HashSet<>();
    private final List<Material> allItems = new ArrayList<>();
    private final Map<Material, Double> itemPrices = new HashMap<>();
    private final Map<Material, Tier> itemTiers = new HashMap<>();
    private final Random random = new Random();

    public Market(SDSPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        loadConfig();
        buildItemList();
        generatePrices();
        gui = new MarketGui(this);

        var cmd = plugin.getCommand("market");
        if (cmd != null) {
            cmd.setExecutor(gui);
            cmd.setTabCompleter(gui);
        }

        Bukkit.getPluginManager().registerEvents(gui, plugin);

        new BukkitRunnable() {
            @Override
            public void run() {
                generatePrices();
            }
        }.runTaskTimer(plugin, 12000L, 12000L);

        plugin.getLogger().info("Market module loaded — " + allItems.size() + " items available");
    }

    private void loadConfig() {
        config = SDSPlugin.loadConfigWithDefaults(plugin, "market_config.yml");
        commonMinPrice = config.getDouble("common-min-price", 50.0);
        commonMaxPrice = config.getDouble("common-max-price", 300.0);
        rareMinPrice = config.getDouble("rare-min-price", 300.0);
        rareMaxPrice = config.getDouble("rare-max-price", 2000.0);
        epicMinPrice = config.getDouble("epic-min-price", 2000.0);
        epicMaxPrice = config.getDouble("epic-max-price", 8000.0);
        legendaryMinPrice = config.getDouble("legendary-min-price", 8000.0);
        legendaryMaxPrice = config.getDouble("legendary-max-price", 30000.0);

        disabledItems.clear();
        disabledItems.addAll(ALWAYS_DISABLED);
        for (String key : config.getStringList("disabled-items")) {
            try {
                disabledItems.add(Material.valueOf(key.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void classifyItems() {
        legendaryItems.clear();
        epicItems.clear();
        rareItems.clear();
        commonItems.clear();
        itemTiers.clear();

        for (Material mat : Material.values()) {
            if (!mat.isItem() || disabledItems.contains(mat)) continue;
            if (mat.name().startsWith("LEGACY_")) continue;

            String name = mat.name();

            if (name.contains("NETHERITE") || name.equals("HEAVY_CORE")
                    || name.equals("ENCHANTED_GOLDEN_APPLE")) {
                legendaryItems.add(mat);
                itemTiers.put(mat, Tier.LEGENDARY);
            } else if (name.equals("ELYTRA") || name.equals("TRIDENT")
                    || name.equals("MACE") || name.equals("NETHER_STAR")
                    || name.equals("BEACON") || name.equals("CONDUIT")
                    || name.equals("TOTEM_OF_UNDYING") || name.equals("HEART_OF_THE_SEA")
                    || name.equals("END_CRYSTAL") || name.equals("DRAGON_EGG")
                    || name.equals("DRAGON_HEAD") || name.equals("DRAGON_BREATH")
                    || name.equals("SHULKER_SHELL") || name.contains("SHULKER_BOX")
                    || name.equals("BUDDING_AMETHYST") || name.equals("REINFORCED_DEEPSLATE")
                    || name.equals("MUSIC_DISC")) {
                epicItems.add(mat);
                itemTiers.put(mat, Tier.EPIC);
            } else if (name.contains("DIAMOND_") || name.contains("GOLDEN_")
                    || name.equals("DIAMOND") || name.equals("EMERALD")
                    || name.equals("ENDER_PEARL") || name.equals("ENDER_EYE")
                    || name.equals("BLAZE_ROD") || name.equals("BLAZE_POWDER")
                    || name.equals("GHAST_TEAR") || name.equals("MAGMA_CREAM")
                    || name.contains("SPONGE")
                    || name.equals("SADDLE") || name.equals("NAME_TAG")
                    || name.equals("GOLDEN_APPLE") || name.equals("EXPERIENCE_BOTTLE")
                    || name.equals("WITHER_SKELETON_SKULL") || name.contains("SKULL")
                    || name.equals("NAUTILUS_SHELL") || name.equals("ECHO_SHARD")
                    || name.equals("GOAT_HORN")
                    || name.contains("ORE") && !name.contains("DEEPSLATE")
                    || name.equals("DEEPSLATE_DIAMOND_ORE") || name.equals("DEEPSLATE_EMERALD_ORE")
                    || name.equals("ANCIENT_DEBRIS") || name.equals("ENCHANTED_BOOK")
                    || name.equals("TURTLE_HELMET")
                    || name.equals("SCULK_SENSOR") || name.equals("SCULK_CATALYST")
                    || name.equals("SCULK_SHRIEKER")) {
                rareItems.add(mat);
                itemTiers.put(mat, Tier.RARE);
            } else {
                commonItems.add(mat);
                itemTiers.put(mat, Tier.COMMON);
            }
        }
    }

    private void buildItemList() {
        allItems.clear();
        classifyItems();
        for (Material mat : commonItems) allItems.add(mat);
        for (Material mat : rareItems) allItems.add(mat);
        for (Material mat : epicItems) allItems.add(mat);
        for (Material mat : legendaryItems) allItems.add(mat);
    }

    public void generatePrices() {
        itemPrices.clear();
        for (Material mat : allItems) {
            double price;
            Tier tier = getItemTier(mat);
            switch (tier) {
                case LEGENDARY:
                    price = legendaryMinPrice + random.nextDouble() * (legendaryMaxPrice - legendaryMinPrice);
                    break;
                case EPIC:
                    price = epicMinPrice + random.nextDouble() * (epicMaxPrice - epicMinPrice);
                    break;
                case RARE:
                    price = rareMinPrice + random.nextDouble() * (rareMaxPrice - rareMinPrice);
                    break;
                default:
                    price = commonMinPrice + random.nextDouble() * (commonMaxPrice - commonMinPrice);
                    break;
            }
            itemPrices.put(mat, price);
        }
        plugin.getLogger().info("Market prices regenerated — " + itemPrices.size() + " items");
    }

    public Tier getItemTier(Material mat) {
        return itemTiers.getOrDefault(mat, Tier.COMMON);
    }

    public double getPrice(Material mat) {
        return itemPrices.getOrDefault(mat, commonMinPrice);
    }

    public double getCommonMinPrice() { return commonMinPrice; }
    public double getCommonMaxPrice() { return commonMaxPrice; }
    public double getRareMinPrice() { return rareMinPrice; }
    public double getRareMaxPrice() { return rareMaxPrice; }
    public double getEpicMinPrice() { return epicMinPrice; }
    public double getEpicMaxPrice() { return epicMaxPrice; }
    public double getLegendaryMinPrice() { return legendaryMinPrice; }
    public double getLegendaryMaxPrice() { return legendaryMaxPrice; }

    public List<Material> getItems() {
        return allItems;
    }

    public String getPrefix() {
        return SDSPlugin.color(config.getString("prefix", "&#65FFAE&lm&#57F574&lc&#48EA3A&lr&#3AE000&lu"));
    }

    public String getMessage(String key, String... replacements) {
        String text = config.getString("messages." + key);
        if (text == null) return "§cMessage not found: " + key;
        for (int i = 0; i < replacements.length - 1; i += 2) {
            text = text.replace("%" + replacements[i] + "%", replacements[i + 1]);
        }
        return SDSPlugin.color(text);
    }

    public SDSPlugin getPlugin() {
        return plugin;
    }
}
