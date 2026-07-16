package org.nig.smp.ah;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.UUID;

public class AhListing {

    private final int id;
    private final UUID seller;
    private final String sellerName;
    private final ItemStack item;
    private final double price;
    private final long createdAt;

    public AhListing(int id, UUID seller, String sellerName, ItemStack item, double price, long createdAt) {
        this.id = id;
        this.seller = seller;
        this.sellerName = sellerName;
        this.item = item;
        this.price = price;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public UUID getSeller() { return seller; }
    public String getSellerName() { return sellerName; }
    public ItemStack getItem() { return item; }
    public double getPrice() { return price; }
    public long getCreatedAt() { return createdAt; }

    public void saveToConfig(ConfigurationSection section) {
        section.set("seller", seller.toString());
        section.set("seller-name", sellerName);
        section.set("price", price);
        section.set("created-at", createdAt);
        section.set("item-base64", itemToBase64(item));
    }

    public static AhListing loadFromConfig(int id, ConfigurationSection section) {
        return new AhListing(
                id,
                UUID.fromString(section.getString("seller")),
                section.getString("seller-name"),
                itemFromBase64(section.getString("item-base64")),
                section.getDouble("price"),
                section.getLong("created-at")
        );
    }

    private static String itemToBase64(ItemStack item) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BukkitObjectOutputStream oos = new BukkitObjectOutputStream(baos);
            oos.writeObject(item);
            oos.close();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    private static ItemStack itemFromBase64(String data) {
        if (data == null) return null;
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream ois = new BukkitObjectInputStream(bais);
            ItemStack item = (ItemStack) ois.readObject();
            ois.close();
            return item;
        } catch (Exception e) {
            return null;
        }
    }
}
