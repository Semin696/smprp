package org.nig.smp.moneys;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.nig.smp.SDSPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MobKillListener implements Listener {

    private final SDSPlugin plugin;
    private final Map<EntityType, MobReward> rewards = new HashMap<>();
    private final Random random = new Random();

    private record MobReward(double min, double max) {}

    public MobKillListener(SDSPlugin plugin) {
        this.plugin = plugin;
        loadRewards();
    }

    private void loadRewards() {
        rewards.clear();
        YamlConfiguration config = SDSPlugin.loadConfigWithDefaults(plugin, "moneys_config.yml");

        ConfigurationSection categories = config.getConfigurationSection("mob-categories");
        if (categories == null) return;

        for (String category : categories.getKeys(false)) {
            ConfigurationSection catSection = categories.getConfigurationSection(category);
            if (catSection == null) continue;

            double min = catSection.getDouble("min");
            double max = catSection.getDouble("max");
            if (min <= 0 || max < min) continue;

            for (String mobName : catSection.getStringList("mobs")) {
                try {
                    EntityType type = EntityType.valueOf(mobName.toUpperCase());
                    rewards.put(type, new MobReward(min, max));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        MobReward reward = rewards.get(event.getEntityType());
        if (reward == null) return;

        double amount = reward.min + (reward.max - reward.min) * random.nextDouble();
        plugin.getMoneyManager().deposit(killer.getUniqueId(), amount);

        killer.sendMessage(Component.text("+ " + plugin.getMoneyManager().format(amount) + " за убийство " + translateName(event.getEntityType())).color(NamedTextColor.GREEN));
    }

    private String translateName(EntityType type) {
        return switch (type) {
            case ZOMBIE -> "зомби";
            case SKELETON -> "скелета";
            case CREEPER -> "крипера";
            case SPIDER -> "паука";
            case ENDERMAN -> "эндермена";
            case BLAZE -> "блейза";
            case WITHER_SKELETON -> "визер-скелета";
            case PIGLIN -> "пиглина";
            case WITCH -> "ведьмы";
            case VINDICATOR -> "виндикатора";
            case EVOKER -> "эвокера";
            case RAVAGER -> "разорителя";
            case PILLAGER -> "разбойника";
            case GUARDIAN -> "стража";
            case ELDER_GUARDIAN -> "древнего стража";
            case SLIME -> "слизня";
            case MAGMA_CUBE -> "магмового куба";
            case GHAST -> "гаста";
            case PHANTOM -> "фантома";
            case DROWNED -> "утопленника";
            case HUSK -> "кадавра";
            case STRAY -> "зимогорца";
            case CAVE_SPIDER -> "пещерного паука";
            case SHULKER -> "шалкера";
            case WARDEN -> "вардена";
            case BREEZE -> "бриза";
            case ENDER_DRAGON -> "дракона Края";
            case WITHER -> "визера";
            case COW -> "корову";
            case PIG -> "свинью";
            case SHEEP -> "овцу";
            case CHICKEN -> "курицу";
            case RABBIT -> "кролика";
            case FOX -> "лису";
            case WOLF -> "волка";
            case CAT -> "кошку";
            case HORSE -> "лошадь";
            case DONKEY -> "осла";
            case MULE -> "мула";
            case LLAMA -> "ламу";
            case PARROT -> "попугая";
            case BEE -> "пчелу";
            case GOAT -> "козу";
            case FROG -> "лягушку";
            case AXOLOTL -> "аксолотля";
            case TURTLE -> "черепаху";
            case DOLPHIN -> "дельфина";
            case SQUID -> "кальмара";
            case GLOW_SQUID -> "светящегося кальмара";
            case SNIFFER -> "нюхача";
            case CAMEL -> "верблюда";
            case ARMADILLO -> "броненосца";
            default -> type.name().toLowerCase();
        };
    }
}
