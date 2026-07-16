package org.nig.smp.team;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TeamData {
    private String name;
    private UUID leader;
    private List<UUID> members;
    private double homeX, homeY, homeZ;
    private float homeYaw, homePitch;
    private String homeWorld;
    private boolean hasHome;

    public TeamData(String name, UUID leader) {
        this.name = name;
        this.leader = leader;
        this.members = new ArrayList<>();
        this.hasHome = false;
    }

    public TeamData() {
        this.members = new ArrayList<>();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getLeader() { return leader; }
    public void setLeader(UUID leader) { this.leader = leader; }

    public List<UUID> getMembers() { return members; }
    public void setMembers(List<UUID> members) { this.members = members; }

    public boolean hasHome() { return hasHome; }

    public Location getHome() {
        if (!hasHome) return null;
        return new Location(
            Bukkit.getWorld(homeWorld),
            homeX, homeY, homeZ,
            homeYaw, homePitch
        );
    }

    public void setHome(Location loc) {
        this.homeWorld = loc.getWorld().getName();
        this.homeX = loc.getX();
        this.homeY = loc.getY();
        this.homeZ = loc.getZ();
        this.homeYaw = loc.getYaw();
        this.homePitch = loc.getPitch();
        this.hasHome = true;
    }

    public void removeHome() {
        this.hasHome = false;
    }

    public boolean isMember(UUID uuid) {
        return leader.equals(uuid) || members.contains(uuid);
    }

    public boolean addMember(UUID uuid) {
        if (isMember(uuid)) return false;
        return members.add(uuid);
    }

    public boolean removeMember(UUID uuid) {
        return members.remove(uuid);
    }

    public List<UUID> getAllPlayers() {
        List<UUID> all = new ArrayList<>(members);
        all.add(leader);
        return all;
    }
}
