package org.nig.smp.tpa.command;

import org.nig.smp.tpa.Tpa;
import org.nig.smp.tpa.TpaManager;
import org.nig.smp.tpa.TpaRequest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TpaCommand implements CommandExecutor {

    private final TpaManager manager;
    private final Tpa plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public TpaCommand(TpaManager manager, Tpa plugin) {
        this.manager = manager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getPrefix().append(plugin.getMessage("only-players")));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(plugin.getPrefix().append(plugin.getMessage("usage", "command", command.getName())));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(plugin.getPrefix().append(plugin.getMessage("player-not-found")));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(plugin.getPrefix().append(plugin.getMessage("cannot-teleport-to-self")));
            return true;
        }

        int cooldownSeconds = plugin.getCooldownSeconds();
        if (cooldownSeconds > 0) {
            UUID uuid = player.getUniqueId();
            long now = System.currentTimeMillis();
            if (cooldowns.containsKey(uuid)) {
                long remaining = (cooldowns.get(uuid) - now) / 1000;
                if (remaining > 0) {
                    player.sendMessage(plugin.getPrefix().append(plugin.getMessage("cooldown", "time", String.valueOf(remaining))));
                    return true;
                }
            }
        }

        TpaRequest.Type type;
        if (command.getName().equalsIgnoreCase("tpa")) {
            type = TpaRequest.Type.TPA;
        } else if (command.getName().equalsIgnoreCase("tpahere")) {
            type = TpaRequest.Type.TPAHERE;
        } else {
            return false;
        }

        manager.createRequest(player.getUniqueId(), target.getUniqueId(), type);

        int cd = plugin.getCooldownSeconds();
        if (cd > 0) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + cd * 1000L);
        }

        player.sendMessage(plugin.getPrefix().append(plugin.getMessage("request-sent", "target", target.getName())));

        String promptPath = switch (type) {
            case TPA -> "prompt-tpa";
            case TPAHERE -> "prompt-tpahere";
        };

        Component acceptButton = Component.text()
            .append(plugin.getMessage("button-accept"))
            .hoverEvent(HoverEvent.showText(plugin.getMessage("button-accept-hover")))
            .clickEvent(ClickEvent.runCommand("/tpaccept"))
            .build();

        Component denyButton = Component.text()
            .append(plugin.getMessage("button-deny"))
            .hoverEvent(HoverEvent.showText(plugin.getMessage("button-deny-hover")))
            .clickEvent(ClickEvent.runCommand("/tpadeny"))
            .build();

        target.sendMessage(
            plugin.getPrefix().append(plugin.getMessage(promptPath, "player", player.getName()))
                .append(Component.space())
                .append(acceptButton)
                .append(Component.space())
                .append(denyButton)
        );

        return true;
    }
}
