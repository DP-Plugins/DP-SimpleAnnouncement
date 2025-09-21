package com.darksoldier1404.dpsa;

import com.darksoldier1404.dppc.utils.ColorUtils;
import com.darksoldier1404.dppc.utils.ConfigUtils;
import com.darksoldier1404.dppc.utils.PluginUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SimpleAnnouncement extends JavaPlugin implements CommandExecutor, TabCompleter {
    public static SimpleAnnouncement plugin;
    public static YamlConfiguration config;
    public static String prefix;
    public static BukkitTask task;

    public static SimpleAnnouncement getInstance() {
        return plugin;
    }

    @Override
    public void onLoad() {
        plugin = this;
        PluginUtil.addPlugin(plugin, 27284);
    }

    @Override
    public void onEnable() {
        init();
        getCommand("dpsa").setExecutor(this);
    }

    @Override
    public void onDisable() {
        save();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(prefix + "§cYou do not have permission to use this command.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(prefix + "/dpsa reload - Reload the plugin configuration.");
            sender.sendMessage(prefix + "/dpsa announce <message> - Make an announcement.");
            sender.sendMessage(prefix + "/dpsa add <message> - Add an announcement.");
            sender.sendMessage(prefix + "/dpsa del <index> - Delete an announcement by index.");
            sender.sendMessage(prefix + "/dpsa type <random/sequential> - Set announcement type.");
            sender.sendMessage(prefix + "/dpsa interval <seconds> - Set announcement interval in seconds.");
            sender.sendMessage(prefix + "/dpsa list - List all announcements.");
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            init();
            sender.sendMessage(prefix + "§aConfiguration reloaded.");
            return true;
        }
        if (args[0].equalsIgnoreCase("announce")) {
            if (args.length < 2) {
                sender.sendMessage(prefix + "§cUsage: /dpsa announce <message>");
                return true;
            }
            String message = String.join(" ", args).substring(args[0].length() + 1);
            getServer().broadcastMessage(prefix + ColorUtils.applyColor(message));
            return true;
        }
        if (args[0].equalsIgnoreCase("add")) {
            if (args.length < 2) {
                sender.sendMessage(prefix + "§cUsage: /dpsa add <message>");
                return true;
            }
            String message = String.join(" ", args).substring(args[0].length() + 1);
            List<String> messages = new java.util.ArrayList<>(config.getStringList("Announcements.messages"));
            messages.add(message);
            config.set("Announcements.messages", messages);
            save();
            init();
            sender.sendMessage(prefix + "§aAnnouncement added.");
            return true;
        }
        if (args[0].equalsIgnoreCase("del")) {
            if (args.length != 2) {
                sender.sendMessage(prefix + "§cUsage: /dpsa del <index>");
                return true;
            }
            try {
                int index = Integer.parseInt(args[1]) - 1;
                List<String> messages = new java.util.ArrayList<>(config.getStringList("Announcements.messages"));
                if (index < 0 || index >= messages.size()) {
                    sender.sendMessage(prefix + "§cInvalid index.");
                    return true;
                }
                messages.remove(index);
                config.set("Announcements.messages", messages);
                save();
                init();
                sender.sendMessage(prefix + "§aAnnouncement deleted.");
            } catch (NumberFormatException e) {
                sender.sendMessage(prefix + "§cInvalid number format.");
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("type")) {
            if (args.length != 2 || (!args[1].equalsIgnoreCase("random") && !args[1].equalsIgnoreCase("sequential"))) {
                sender.sendMessage(prefix + "§cUsage: /dpsa type <random/sequential>");
                return true;
            }
            config.set("Announcements.type", args[1].toLowerCase());
            save();
            init();
            sender.sendMessage(prefix + "§aAnnouncement type set to " + args[1].toLowerCase() + ".");
            return true;
        }
        if (args[0].equalsIgnoreCase("interval")) {
            if (args.length != 2) {
                sender.sendMessage(prefix + "§cUsage: /dpsa interval <seconds>");
                return true;
            }
            try {
                int seconds = Integer.parseInt(args[1]);
                if (seconds <= 0) {
                    sender.sendMessage(prefix + "§cInterval must be greater than 0.");
                    return true;
                }
                config.set("Announcements.interval", seconds);
                save();
                init();
                sender.sendMessage(prefix + "§aAnnouncement interval set to " + seconds + " seconds.");
            } catch (NumberFormatException e) {
                sender.sendMessage(prefix + "§cInvalid number format.");
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(prefix + "§aAnnouncements:");
            for (int i = 0; i < config.getStringList("Announcements.messages").size(); i++) {
                sender.sendMessage("§e" + (i + 1) + ". " + ColorUtils.applyColor(config.getStringList("Announcements.messages").get(i)));
            }
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "announce", "add", "del", "type", "interval", "list");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("type")) {
            return Arrays.asList("random", "sequential");
        }
        return Collections.emptyList();
    }

    public static void init() {
        config = ConfigUtils.loadDefaultPluginConfig(plugin);
        prefix = ColorUtils.applyColor(config.getString("Settings.prefix"));
        startAnnouncementTask();
    }

    public static void save() {
        ConfigUtils.savePluginConfig(plugin, config);
    }

    public static void startAnnouncementTask() {
        if (task != null) {
            task.cancel();
        }
        int interval = config.getInt("Announcements.interval", 300);
        String type = config.getString("Announcements.type", "sequential");
        task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (config.getStringList("Announcements.messages").isEmpty()) return;
            String message;
            if (type.equalsIgnoreCase("random")) {
                int index = (int) (Math.random() * config.getStringList("Announcements.messages").size());
                message = config.getStringList("Announcements.messages").get(index);
            } else {
                int index = config.getInt("Announcements.lastIndex", -1) + 1;
                if (index >= config.getStringList("Announcements.messages").size()) {
                    index = 0;
                }
                message = config.getStringList("Announcements.messages").get(index);
                config.set("Announcements.lastIndex", index);
            }
            plugin.getServer().broadcastMessage(prefix + ColorUtils.applyColor(message));
        }, 0L, interval * 20L);
    }

}
