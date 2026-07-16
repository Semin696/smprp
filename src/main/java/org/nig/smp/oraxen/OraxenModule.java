package org.nig.smp.oraxen;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.nig.smp.SDSPlugin;
import org.nig.smp.oraxen.pack.OraxenPackBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class OraxenModule {

    private final SDSPlugin plugin;
    private File oraxenFolder;
    private OraxenPackBuilder packBuilder;

    public OraxenModule(SDSPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        YamlConfiguration config = SDSPlugin.loadConfigWithDefaults(plugin, "oraxen_config.yml");
        String folderPath = config.getString("Oraxen.folder", "omp");

        File target = new File(plugin.getDataFolder().getParentFile(), folderPath);

        if (!target.exists()) {
            plugin.getLogger().info("Creating Oraxen folder at " + target.getAbsolutePath());
            createDefaultStructure(target);
        }

        oraxenFolder = target;
        packBuilder = new OraxenPackBuilder(plugin, oraxenFolder);
        plugin.getLogger().info("OraxenModule initialized \u2014 folder: " + oraxenFolder.getAbsolutePath());
    }

    private void createDefaultStructure(File root) {
        try {
            root.mkdirs();
            new File(root, "items").mkdirs();
            new File(root, "recipes").mkdirs();
            new File(root, "glyphs").mkdirs();

            File packDir = new File(root, "pack");
            packDir.mkdirs();

            File mcmeta = new File(packDir, "pack.mcmeta");
            if (!mcmeta.exists()) {
                try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(mcmeta), StandardCharsets.UTF_8)) {
                    int pf = getPackFormat();
                    w.write("{\n  \"pack\": {\n    \"pack_format\": " + pf + ",\n    \"description\": \"SDS Resource Pack\"\n  }\n}\n");
                }
            }

            File png = new File(packDir, "pack.png");
            if (!png.exists()) {
                byte[] minimalPng = {
                    (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG header
                    0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,       // IHDR chunk
                    0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,       // 1x1 pixel
                    0x08, 0x02, 0x00, 0x00, 0x00, (byte)0x90, (byte)0x77, (byte)0x53,
                    (byte)0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, // IDAT chunk
                    0x54, 0x08, (byte)0xD7, 0x63, (byte)0xF8, (byte)0xCF, (byte)0xC0,
                    0x00, 0x00, 0x00, 0x03, 0x00, 0x01, (byte)0x86, (byte)0xA0,
                    (byte)0x5F, (byte)0xE5, 0x00, 0x00, 0x00, 0x00, 0x49, // IEND chunk
                    0x45, 0x4E, 0x44, (byte)0xAE, 0x42, 0x60, (byte)0x82
                };
                try (FileOutputStream fos = new FileOutputStream(png)) {
                    fos.write(minimalPng);
                }
            }

            plugin.getLogger().info("Default Oraxen structure created at " + root.getAbsolutePath());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create Oraxen structure: " + e.getMessage());
        }
    }

    private int getPackFormat() {
        try {
            String ver = Bukkit.getMinecraftVersion();
            String[] parts = ver.split("\\.");
            int minor = Integer.parseInt(parts[1]);
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            if (minor < 17) return 7;
            if (minor == 17) return patch == 0 ? 7 : 8;
            if (minor == 18) return patch == 0 ? 8 : 9;
            if (minor == 19) return patch <= 2 ? 9 : 12;
            if (minor == 20) return patch <= 3 ? 15 : patch == 4 ? 18 : patch == 5 ? 32 : 34;
            if (minor == 21) return patch == 1 ? 34 : patch <= 4 ? 46 : 75;
            return 75;
        } catch (Exception e) {
            return 75;
        }
    }

    public File buildPack() {
        if (packBuilder == null) return null;
        return packBuilder.build();
    }

    public boolean isAvailable() {
        return packBuilder != null;
    }
}
