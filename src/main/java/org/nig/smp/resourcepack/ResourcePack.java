package org.nig.smp.resourcepack;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.nig.smp.SDSPlugin;
import org.nig.smp.oraxen.OraxenModule;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

public class ResourcePack implements Listener {

    private final SDSPlugin plugin;
    private final OraxenModule oraxenModule;
    private YamlConfiguration config;
    private File configFile;

    private String packUrl;
    private String packHash;
    private String uploadType;
    private String gitRepo;
    private String gitToken;
    private String gitBranch;
    private String gitPath;
    private String localPackPath;
    private boolean sendOnJoin;
    private int joinDelay;
    private boolean mandatory;

    public ResourcePack(SDSPlugin plugin, OraxenModule oraxenModule) {
        this.plugin = plugin;
        this.oraxenModule = oraxenModule;
    }

    public void init() {
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);

        var cmd = plugin.getCommand("resourcepack");
        if (cmd != null) {
            var executor = new ResourcePackCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        if (config.getBoolean("Pack.auto-build", true)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getLogger().info("Auto-building resource pack...");
                buildAndUpload();
            }, 100L);
        }

        plugin.getLogger().info("ResourcePack module loaded" + (oraxenModule.isAvailable() ? " (OraxenModule active)" : ""));
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "resourcepack.yml");
        if (!configFile.exists()) {
            plugin.saveResource("resourcepack.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        packUrl = config.getString("Pack.pack-url", "");
        packHash = config.getString("Pack.pack-hash", "");
        uploadType = config.getString("Pack.upload.type", "external");
        gitRepo = config.getString("Pack.upload.github.repo", "");
        gitToken = config.getString("Pack.upload.github.token", "");
        gitBranch = config.getString("Pack.upload.github.branch", "main");
        gitPath = config.getString("Pack.upload.github.path", "resourcepack.zip");
        localPackPath = config.getString("Pack.local-path", "");
        sendOnJoin = config.getBoolean("Pack.dispatch.send_on_join", true);
        joinDelay = config.getInt("Pack.dispatch.delay", 40);
        mandatory = config.getBoolean("Pack.dispatch.mandatory", true);
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save resourcepack.yml: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!sendOnJoin) return;

        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !packUrl.isEmpty()) {
                if (!packHash.isEmpty()) {
                    player.setResourcePack(packUrl, HexFormat.of().parseHex(packHash), mandatory);
                } else {
                    player.setResourcePack(packUrl, (byte[]) null, mandatory);
                }
            }
        }, joinDelay);
    }

    public boolean buildAndUpload() {
        File packFile = null;

        if (oraxenModule.isAvailable()) {
            plugin.getLogger().info("Building pack via OraxenModule...");
            packFile = oraxenModule.buildPack();
            if (packFile != null) {
                plugin.getLogger().info("Build returned: " + packFile.getAbsolutePath() + " (exists: " + packFile.exists() + ")");
            }
        } else {
            plugin.getLogger().warning("OraxenModule not available — pack will not be built");
        }

        if (packFile == null || !packFile.exists()) {
            packFile = findPackFile();
        }

        if (packFile == null || !packFile.exists()) {
            File[] tried = {
                new File(plugin.getDataFolder().getParentFile().getParentFile(), "Oraxen/pack/pack.zip"),
                new File(plugin.getDataFolder().getParentFile(), "Oraxen/pack/pack.zip"),
                new File("Oraxen/pack/pack.zip"),
                new File(plugin.getDataFolder(), "resourcepack.zip"),
                new File(plugin.getDataFolder(), "pack.zip"),
            };
            plugin.getLogger().severe("resourcepack.zip not found. Checked paths:");
            for (File f : tried) {
                plugin.getLogger().severe("  " + (f != null ? f.getAbsolutePath() : "null") + " (exists: " + (f != null && f.exists()) + ")");
            }
            return false;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(java.nio.file.Files.readAllBytes(packFile.toPath()));
            packHash = HexFormat.of().formatHex(hash);
            config.set("Pack.pack-hash", packHash);
            saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to compute SHA-1: " + e.getMessage());
            return false;
        }

        boolean uploaded = false;
        if ("github".equalsIgnoreCase(uploadType) && !gitRepo.isEmpty() && !gitToken.isEmpty()) {
            uploaded = uploadToGithub(packFile);
        } else {
            plugin.getLogger().info("Pack hash updated, upload type: " + uploadType);
            uploaded = true;
        }

        if (uploaded && !packUrl.isEmpty()) {
            sendToAll();
        }

        return uploaded;
    }

    public void sendToAll() {
        if (packUrl.isEmpty()) return;
        byte[] hashBytes = null;
        if (!packHash.isEmpty()) {
            hashBytes = HexFormat.of().parseHex(packHash);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setResourcePack(packUrl, hashBytes, mandatory);
        }
        plugin.getLogger().info("Resource pack sent to " + Bukkit.getOnlinePlayers().size() + " players");
    }

    private File findPackFile() {
        if (!localPackPath.isEmpty()) {
            File custom = new File(localPackPath);
            if (custom.exists()) return custom;
            plugin.getLogger().warning("local-pack path not found: " + localPackPath);
        }

        File[] candidates = {
            new File(plugin.getDataFolder().getParentFile(), "omp/pack/pack.zip"),
            new File(plugin.getDataFolder().getParentFile(), "Oraxen/pack/pack.zip"),
            new File("Oraxen/pack/pack.zip"),
            new File(plugin.getDataFolder(), "pack.zip"),
            new File(plugin.getDataFolder(), "resourcepack.zip"),
            new File(plugin.getDataFolder(), "omp/pack/pack.zip"),
        };

        for (File f : candidates) {
            if (f != null && f.exists()) return f;
        }
        return null;
    }

    private boolean uploadToGithub(File packFile) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String apiBase = "https://api.github.com/repos/" + gitRepo + "/contents/" + gitPath;

            byte[] fileBytes = java.nio.file.Files.readAllBytes(packFile.toPath());
            String encoded = Base64.getEncoder().encodeToString(fileBytes);

            String existingSha = getExistingFileSha(client, apiBase);

            String commitMsg = existingSha != null
                    ? "Update resourcepack.zip"
                    : "Add resourcepack.zip";

            String jsonBody = "{\"message\":\"" + commitMsg + "\",\"content\":\"" + encoded
                    + "\",\"branch\":\"" + gitBranch + "\""
                    + (existingSha != null ? ",\"sha\":\"" + existingSha + "\"" : "")
                    + "}";

            HttpRequest uploadRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiBase))
                    .header("Authorization", "Bearer " + gitToken)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(uploadRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201 || response.statusCode() == 200) {
                String downloadUrl = "https://raw.githubusercontent.com/" + gitRepo + "/" + gitBranch + "/" + gitPath;
                packUrl = downloadUrl;
                config.set("Pack.pack-url", packUrl);
                saveConfig();
                plugin.getLogger().info("Resource pack uploaded to GitHub: " + downloadUrl);
                return true;
            } else {
                plugin.getLogger().severe("GitHub upload error: " + response.statusCode() + " " + response.body());
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("GitHub upload failed: " + e.getMessage());
            return false;
        }
    }

    private String getExistingFileSha(HttpClient client, String apiUrl) throws Exception {
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "?ref=" + gitBranch))
                .header("Authorization", "Bearer " + gitToken)
                .header("Accept", "application/vnd.github.v3+json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(getRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return response.body().replaceAll(".*\"sha\":\"([^\"]+)\".*", "$1");
        }
        return null;
    }

    public String getPackUrl() {
        return packUrl;
    }

    public String getPackHash() {
        return packHash;
    }

    public void reload() {
        loadConfig();
        plugin.getLogger().info("ResourcePack config reloaded");
    }

    public SDSPlugin getPlugin() {
        return plugin;
    }
}
