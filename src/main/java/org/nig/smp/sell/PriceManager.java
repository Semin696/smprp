package org.nig.smp.sell;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Map;

public class PriceManager {

    private final Plugin auctionPlugin;
    private final double enchantmentPercent;
    private final double marketStackPercent;
    private Method getPriceMethod;
    private Method getListingCountMethod;

    public PriceManager(Plugin auctionPlugin, double enchantmentPercent, double marketStackPercent) {
        this.auctionPlugin = auctionPlugin;
        this.enchantmentPercent = enchantmentPercent;
        this.marketStackPercent = marketStackPercent;
        resolveMethods();
    }

    private void resolveMethods() {
        if (auctionPlugin == null) return;
        try {
            Class<?> cls = auctionPlugin.getClass();
            for (Method m : cls.getDeclaredMethods()) {
                String name = m.getName().toLowerCase();
                if (name.contains("getprice") || name.contains("getitemprice") || name.contains("getmarketprice")) {
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length == 1 && (params[0] == Material.class || params[0] == ItemStack.class || params[0] == String.class)) {
                        getPriceMethod = m;
                        getPriceMethod.setAccessible(true);
                        break;
                    }
                }
            }
            for (Method m : cls.getDeclaredMethods()) {
                String name = m.getName().toLowerCase();
                if (name.contains("getlistingcount") || name.contains("getitemcount") || name.contains("getlistings")) {
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length == 1) {
                        getListingCountMethod = m;
                        getListingCountMethod.setAccessible(true);
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    public double getPrice(ItemStack item) {
        if (item == null || item.getType().isAir()) return 0;

        double basePrice = fetchBasePrice(item);
        double total = basePrice * item.getAmount();
        total += calculateEnchantmentBonus(item, basePrice);
        return Math.max(0, total);
    }

    private double fetchBasePrice(ItemStack item) {
        if (auctionPlugin == null || getPriceMethod == null) return 0;

        try {
            Class<?>[] params = getPriceMethod.getParameterTypes();
            Object arg;
            if (params[0] == Material.class) {
                arg = item.getType();
            } else if (params[0] == ItemStack.class) {
                arg = item;
            } else if (params[0] == String.class) {
                arg = item.getType().name();
            } else {
                return 0;
            }
            Object result = getPriceMethod.invoke(auctionPlugin, arg);
            double price = result instanceof Number ? ((Number) result).doubleValue() : 0;

            if (getListingCountMethod != null) {
                Object countArg;
                Class<?>[] countParams = getListingCountMethod.getParameterTypes();
                if (countParams[0] == Material.class) {
                    countArg = item.getType();
                } else if (countParams[0] == ItemStack.class) {
                    countArg = item;
                } else if (countParams[0] == String.class) {
                    countArg = item.getType().name();
                } else {
                    countArg = null;
                }
                if (countArg != null) {
                    Object countResult = getListingCountMethod.invoke(auctionPlugin, countArg);
                    int count = countResult instanceof Number ? ((Number) countResult).intValue() : 0;
                    if (count > 1) {
                        price *= (1 + (count - 1) * marketStackPercent / 100.0);
                    }
                }
            }

            return price;
        } catch (Exception e) {
            return 0;
        }
    }

    private double calculateEnchantmentBonus(ItemStack item, double basePrice) {
        double bonus = 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;

        Map<Enchantment, Integer> enchants;
        if (meta instanceof EnchantmentStorageMeta bookMeta) {
            enchants = bookMeta.getStoredEnchants();
        } else {
            enchants = meta.getEnchants();
        }

        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            int level = entry.getValue();
            bonus += basePrice * (enchantmentPercent / 100.0) * level;
        }

        return bonus;
    }

    public boolean isAvailable() {
        return auctionPlugin != null;
    }
}
