package org.nig.smp.homeSev.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public class Home {

    public enum Type {
        STANDARD,
        COMMAND
    }

    private final String name;
    private final String worldName;
    private final double x, y, z;
    private final float yaw, pitch;
    private final Type type;

    public Home(String name, String worldName, double x, double y, double z, float yaw, float pitch, Type type) {
        this.name = name;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.type = type;
    }

    public static Home fromLocation(String name, Location location, Type type) {
        return new Home(
            name,
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ(),
            location.getYaw(),
            location.getPitch(),
            type
        );
    }

    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z, yaw, pitch);
    }

    public String getName() { return name; }
    public String getWorldName() { return worldName; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public Type getType() { return type; }

    public void saveToConfig(ConfigurationSection section) {
        section.set("world", worldName);
        section.set("x", x);
        section.set("y", y);
        section.set("z", z);
        section.set("yaw", yaw);
        section.set("pitch", pitch);
        section.set("type", type.name());
    }

    public static Home loadFromConfig(String name, ConfigurationSection section) {
        String world = section.getString("world");
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw");
        float pitch = (float) section.getDouble("pitch");
        Type type;
        try {
            type = Type.valueOf(section.getString("type", "STANDARD"));
        } catch (IllegalArgumentException e) {
            type = Type.STANDARD;
        }
        return new Home(name, world, x, y, z, yaw, pitch, type);
    }
}
