package org.nig.smp.oraxen.pack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.configuration.file.YamlConfiguration;
import org.nig.smp.SDSPlugin;
import org.nig.smp.oraxen.utils.VirtualFile;
import org.nig.smp.oraxen.utils.ZipUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class OraxenPackBuilder {

    private final SDSPlugin plugin;
    private final File oraxenFolder;
    private final File packFolder;
    private final File outputFile;
    private final List<VirtualFile> files = new ArrayList<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public OraxenPackBuilder(SDSPlugin plugin, File oraxenFolder) {
        this.plugin = plugin;
        this.oraxenFolder = oraxenFolder;
        this.packFolder = new File(oraxenFolder, "pack");
        this.outputFile = new File(packFolder, "pack.zip");
    }

    public File build() {
        files.clear();
        plugin.getLogger().info("Building resource pack from " + oraxenFolder.getAbsolutePath());

        if (!packFolder.exists()) {
            plugin.getLogger().warning("Pack folder not found, creating: " + packFolder.getAbsolutePath());
            packFolder.mkdirs();
        }

        try {
            generateItemModels();
            generateFonts();
            generateSounds();
            collectPackFiles();
            writeZip();
            plugin.getLogger().info("Pack built: " + outputFile.getAbsolutePath());
            return outputFile;
        } catch (Exception e) {
            plugin.getLogger().severe("Pack build failed: " + e.getMessage());
            return null;
        }
    }

    private void generateItemModels() {
        File itemsFolder = new File(oraxenFolder, "items");
        if (!itemsFolder.exists()) return;

        File[] itemFiles = itemsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (itemFiles == null) return;

        for (File itemFile : itemFiles) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(itemFile);
            for (String key : config.getKeys(false)) {
                if (!config.isConfigurationSection(key)) continue;
                if (!config.contains(key + ".Pack")) continue;
                if (!config.getBoolean(key + ".Pack.generate_model", true)) continue;

                String modelName = config.getString(key + ".Pack.model", key.toLowerCase());
                String parent = config.getString(key + ".Pack.parent_model", "item/generated");
                List<String> textureList = config.getStringList(key + ".Pack.textures");

                JsonObject model = new JsonObject();
                model.addProperty("parent", parent.startsWith("minecraft:") ? parent : "minecraft:" + parent);

                if (!textureList.isEmpty()) {
                    JsonObject textures = new JsonObject();
                    for (int i = 0; i < textureList.size(); i++) {
                        String tex = textureList.get(i);
                        if (!tex.contains(":")) tex = "oraxen:item/" + tex;
                        else if (!tex.contains(":")) tex = "minecraft:" + tex;
                        String layer = i == 0 ? "layer0" : "layer" + i;
                        textures.addProperty(layer, tex.replace(".png", ""));
                    }
                    model.add("textures", textures);
                }

                addFile("assets/minecraft/models/item", modelName + ".json", gson.toJson(model));
            }
        }
    }

    private void generateFonts() {
        File glyphsFolder = new File(oraxenFolder, "glyphs");
        if (!glyphsFolder.exists()) return;

        JsonArray providers = new JsonArray();

        File[] glyphFiles = glyphsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (glyphFiles != null) {
            for (File gf : glyphFiles) {
                YamlConfiguration glyphConfig = YamlConfiguration.loadConfiguration(gf);
                for (String key : glyphConfig.getKeys(false)) {
                    if (!glyphConfig.isConfigurationSection(key)) continue;
                    String type = glyphConfig.getString(key + ".type", "bitmap");
                    if ("bitmap".equals(type)) {
                        JsonObject provider = new JsonObject();
                        provider.addProperty("type", "bitmap");
                        provider.addProperty("file", glyphConfig.getString(key + ".file", "oraxen:font/" + key + ".png"));
                        provider.addProperty("ascent", glyphConfig.getInt(key + ".ascent", 8));
                        provider.addProperty("height", glyphConfig.getInt(key + ".height", 8));

                        JsonArray chars = new JsonArray();
                        List<String> rawChars = glyphConfig.getStringList(key + ".chars");
                        if (rawChars.isEmpty()) {
                            rawChars = List.of(glyphConfig.getString(key + ".chars", "\\uF001"));
                        }
                        for (String c : rawChars) chars.add(c);
                        provider.add("chars", chars);

                        providers.add(provider);
                    }
                }
            }
        }

        // Shift provider from font.yml
        File fontFile = new File(oraxenFolder, "font.yml");
        if (fontFile.exists()) {
            YamlConfiguration fontConfig = YamlConfiguration.loadConfiguration(fontFile);
            if (fontConfig.isConfigurationSection("shifts")) {
                JsonObject shiftProvider = new JsonObject();
                shiftProvider.addProperty("type", "space");
                JsonArray advances = new JsonArray();
                for (String key : fontConfig.getConfigurationSection("shifts").getKeys(false)) {
                    JsonObject entry = new JsonObject();
                    entry.addProperty("code", key);
                    entry.addProperty("amount", fontConfig.getInt("shifts." + key, 0));
                    advances.add(entry);
                }
                shiftProvider.add("advances", advances);
                providers.add(shiftProvider);
            }
        }

        if (providers.size() > 0) {
            JsonObject fontJson = new JsonObject();
            fontJson.add("providers", providers);
            addFile("assets/minecraft/font", "default.json", gson.toJson(fontJson));
        }
    }

    private void generateSounds() {
        File soundsFile = new File(oraxenFolder, "sounds.yml");
        if (!soundsFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(soundsFile);
        JsonObject soundsJson = new JsonObject();

        for (String key : config.getKeys(false)) {
            if (!config.isConfigurationSection(key)) continue;
            JsonObject soundEntry = new JsonObject();
            soundEntry.addProperty("category", config.getString(key + ".category", "master"));

            JsonArray sounds = new JsonArray();
            List<String> rawSounds = config.getStringList(key + ".sounds");
            if (rawSounds.isEmpty()) {
                String single = config.getString(key + ".sounds", "");
                if (!single.isEmpty()) rawSounds = List.of(single);
            }
            for (String s : rawSounds) {
                JsonObject soundObj = new JsonObject();
                soundObj.addProperty("name", s);
                soundObj.addProperty("stream", config.getBoolean(key + ".stream", false));
                sounds.add(soundObj);
            }
            soundEntry.add("sounds", sounds);
            soundsJson.add(key, soundEntry);
        }

        if (soundsJson.keySet().size() > 0) {
            addFile("assets/minecraft", "sounds.json", gson.toJson(soundsJson));
        }
    }

    private void collectPackFiles() throws IOException {
        File[] entries = packFolder.listFiles();
        if (entries == null) return;

        for (File entry : entries) {
            String name = entry.getName();
            if (name.equals("pack.zip") || name.equals("uploads") || name.equals("__MACOSX")) continue;

            if (entry.isFile()) {
                byte[] bytes = Files.readAllBytes(entry.toPath());
                VirtualFile vf = new VirtualFile("", name, new ByteArrayInputStream(bytes));
                files.add(vf);
            } else {
                String targetPrefix = mapToAssetPath(name);
                collectDirectory(entry, targetPrefix);
            }
        }
    }

    private String mapToAssetPath(String name) {
        return switch (name) {
            case "font" -> "assets/minecraft/font";
            case "lang" -> "assets/minecraft/lang";
            case "models" -> "assets/minecraft/models/item";
            case "textures" -> "assets/oraxen/textures/item";
            case "sounds" -> "assets/minecraft/sounds";
            default -> "assets/minecraft/" + name;
        };
    }

    private void collectDirectory(File dir, String zipPrefix) throws IOException {
        File[] entries = dir.listFiles();
        if (entries == null) return;

        for (File entry : entries) {
            String entryName = entry.getName();
            if (entryName.equals("pack.zip") || entryName.equals("uploads") || entryName.equals("__MACOSX")) continue;

            String entryPath = zipPrefix.isEmpty() ? entryName : zipPrefix + "/" + entryName;

            if (entry.isDirectory()) {
                collectDirectory(entry, entryPath);
            } else {
                byte[] bytes = Files.readAllBytes(entry.toPath());
                String parent = new File(entryPath).getParent();
                String fullPath = (parent != null ? parent.replace("\\", "/") + "/" : "") + entryName;
                boolean dup = false;
                for (VirtualFile f : files) {
                    if (f.getPath().equals(fullPath)) { dup = true; break; }
                }
                if (!dup) {
                    VirtualFile vf = new VirtualFile(
                        parent != null ? parent.replace("\\", "/") : "",
                        entryName,
                        new ByteArrayInputStream(bytes)
                    );
                    files.add(vf);
                }
            }
        }
    }

    private void addFile(String path, String name, String content) {
        String fullPath = path.isEmpty() ? name : path + "/" + name;
        for (VirtualFile f : files) {
            if (f.getPath().equals(fullPath)) return;
        }
        VirtualFile vf = new VirtualFile(path, name, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        files.add(vf);
    }

    private void writeZip() throws IOException {
        if (outputFile.exists()) outputFile.delete();
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(fos, StandardCharsets.UTF_8)) {
            for (VirtualFile file : files) {
                ZipUtils.addToZip(file.getPath(), file.getInputStream(), zos);
            }
        }
    }

    public File getOutputFile() {
        return outputFile;
    }
}
