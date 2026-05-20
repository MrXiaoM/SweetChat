package top.mrxiaom.sweet.chat.commands;

import com.google.common.collect.Lists;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.data.Duration;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.func.language.Message;
import top.mrxiaom.pluginbase.utils.AdventureUtil;
import top.mrxiaom.pluginbase.utils.Bytes;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.chat.Messages;
import top.mrxiaom.sweet.chat.SweetChat;
import top.mrxiaom.sweet.chat.api.IChatMode;
import top.mrxiaom.sweet.chat.database.MuteDatabase;
import top.mrxiaom.sweet.chat.database.data.Mute;
import top.mrxiaom.sweet.chat.func.AbstractModule;
import top.mrxiaom.sweet.chat.func.ChatListener;
import top.mrxiaom.sweet.chat.utils.Utils;

import java.io.DataInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@AutoRegister
public class CommandMain extends AbstractModule implements CommandExecutor, TabCompleter, Listener {
    public CommandMain(SweetChat plugin) {
        super(plugin);
        registerCommand("sweetchat", this);
        registerBungee();
    }

    @Override
    public void receiveBungee(String subChannel, DataInputStream in) throws IOException {
        if (subChannel.equals("SweetChatRefreshCache")) {
            if (System.currentTimeMillis() > in.readLong()) return;
            long mostSigBits = in.readLong();
            long leastSigBits = in.readLong();
            UUID playerId = new UUID(mostSigBits, leastSigBits);
            plugin.getMuteDatabase().removeCache(playerId);
            String message = in.readUTF();
            if (message.isEmpty()) return;
            Player player = Util.getOnlinePlayer(playerId).orElse(null);
            if (player != null && player.isOnline()) {
                AdventureUtil.sendMessage(player, message);
            }
        }
    }

    private void requireRefreshCache(OfflinePlayer player, String message) {
        requireRefreshCache(player.getUniqueId(), message);
    }

    private void requireRefreshCache(UUID playerId, String message) {
        Bytes.sendByWhoeverOrNot("BungeeCord", Bytes.build(out -> {
            out.writeLong(System.currentTimeMillis() + 3000L);
            out.writeLong(playerId.getMostSignificantBits());
            out.writeLong(playerId.getLeastSignificantBits());
            out.writeUTF(message);
        }, "Forward", "ALL", "SweetChatRefreshCache"));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (args.length >= 2 && "mode".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.chat.mode")) {
            String modeStr = args[1];
            boolean clear;
            if (modeStr.equalsIgnoreCase("clear")) {
                clear = true;
            } else {
                clear = false;
                IChatMode mode = ChatListener.inst().getChatMode(modeStr);
                if (mode == null || !sender.hasPermission("sweet.chat.mode.set." + modeStr)) {
                    return Messages.Commands.mode__not_found.tm(sender);
                }
            }
            Player player;
            if (args.length > 2) {
                if (!sender.hasPermission("sweet.chat.mode.others")) {
                    return Messages.Commands.no_permission.tm(sender);
                }
                player = Util.getOnlinePlayer(args[2]).orElse(null);
                if (player == null) {
                    return Messages.player__not_online.tm(sender);
                }
            } else if (sender instanceof Player) {
                player = (Player) sender;
            } else {
                return Messages.player__only.tm(sender);
            }
            plugin.getScheduler().runTaskAsync(() -> {
                plugin.getModeDatabase().setMode(player.getUniqueId(), clear ? "" : modeStr);
                if (clear) {
                    Messages.Commands.mode__clear.tm(player);
                } else {
                    Messages.Commands.mode__set.tm(player, Pair.of("%mode%", modeStr));
                }
            });
            return true;
        }
        if (args.length >= 3 && "mute".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.chat.mute")) {
            OfflinePlayer player = Util.getOfflinePlayer(args[1]).orElse(null);
            if (player == null) {
                return Messages.player__not_exists.tm(sender);
            }
            MuteDatabase database = plugin.getMuteDatabase();
            StringJoiner joiner = new StringJoiner(" ");
            for (int i = 2; i < args.length; i++) {
                joiner.add(args[i]);
            }
            String input = joiner.toString();
            LocalDateTime endTime;
            String durationStr;
            String endTimeStr;
            if (input.equals("inf") || input.equals("infinite")) {
                endTime = null;
                durationStr = null;
                endTimeStr = null;
            } else {
                LocalDateTime parsed = Utils.parseDateTime(input);
                if (parsed != null) {
                    endTime = parsed;
                    Duration duration = Utils.between(LocalDateTime.now(), endTime);
                    durationStr = database.formatDuration(duration);
                    endTimeStr = database.formatEndTime(endTime);
                } else {
                    Duration duration = Duration.parse(input).orElse(null);
                    if (duration != null) {
                        endTime = duration.addFrom(LocalDateTime.now());
                        durationStr = database.formatDuration(duration);
                        endTimeStr = database.formatEndTime(endTime);
                    } else {
                        return Messages.Commands.mute__duration_invalid.tm(sender);
                    }
                }
            }
            Mute mute = database.getMute(player.getUniqueId());
            if (endTime == null) {
                mute.setInfiniteMuted().submit();
                Player p = player.getPlayer();
                Message msg = Messages.Commands.mute__success__infinite_notice;
                if (p != null && p.isOnline()) {
                    msg.tm(p);
                } else {
                    requireRefreshCache(player, msg.str());
                }
                return Messages.Commands.mute__success__infinite.tm(sender,
                        Pair.of("%player%", player.getName()));
            } else {
                mute.setTimedMuted(endTime).submit();
                Player p = player.getPlayer();
                Message msg = Messages.Commands.mute__success__timed_notice;
                if (p != null && p.isOnline()) {
                    msg.tm(p, Pair.of("%duration%", durationStr),
                            Pair.of("%end_time%", endTimeStr));
                } else {
                    requireRefreshCache(player, msg.str(
                            Pair.of("%duration%", durationStr),
                            Pair.of("%end_time%", endTimeStr)));
                }
                return Messages.Commands.mute__success__timed.tm(sender,
                        Pair.of("%player%", player.getName()),
                        Pair.of("%duration%", durationStr),
                        Pair.of("%end_time%", endTimeStr));
            }
        }
        if (args.length >= 2 && "unmute".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.chat.unmute")) {
            OfflinePlayer player = Util.getOfflinePlayer(args[1]).orElse(null);
            if (player == null) {
                return Messages.player__not_exists.tm(sender);
            }
            MuteDatabase database = plugin.getMuteDatabase();
            Mute mute = database.getMute(player.getUniqueId());
            if (!mute.isMuted()) {
                return Messages.Commands.unmute__unnecessary.tm(sender);
            }
            mute.setNotMuted().submit();
            Player p = player.getPlayer();
            Message msg = Messages.Commands.unmute__success_notice;
            if (p != null && p.isOnline()) {
                msg.tm(p);
            } else {
                requireRefreshCache(player, msg.str());
            }
            return Messages.Commands.unmute__success.tm(sender,
                    Pair.of("player", player.getName()));
        }
        if (args.length > 1 && "sudo".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.chat.sudo")) {
            Player player = Util.getOnlinePlayer(args[1]).orElse(null);
            if (player == null) {
                return Messages.player__not_online.tm(sender);
            }
            StringJoiner joiner = new StringJoiner(" ");
            for (int i = 2; i < args.length; i++) {
                joiner.add(args[i]);
            }
            String text = joiner.toString();
            plugin.getScheduler().runTaskAsync(() -> ChatListener.inst().onChat(player, text));
            return true;
        }
        if (args.length >= 1 && "reload".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.chat.reload")) {
            if (args.length == 2 && "database".equalsIgnoreCase(args[1])) {
                plugin.options.database().reloadConfig();
                plugin.options.database().reconnect();
                return Messages.Commands.reload__database.tm(sender);
            }
            plugin.reloadConfig();
            return Messages.Commands.reload__config.tm(sender);
        }
        return (sender.isOp() ? Messages.Commands.help__admin : Messages.Commands.help__player).tm(sender);
    }

    private void add(CommandSender sender, List<String> list, String s) {
        if (sender.hasPermission("sweet.chat." + s)) {
            list.add(s);
        }
    }

    private static final List<String> muteDurationSample = Lists.newArrayList(
            "inf", "30m", "60s", "yyyy-MM-dd HH:mm:ss", "HH:mm"
    );
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            add(sender, list, "mode");
            add(sender, list, "mute");
            add(sender, list, "unmute");
            add(sender, list, "sudo");
            add(sender, list, "reload");
            return startsWith(list, args[0]);
        }
        if (args.length == 2) {
            if ("sudo".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.chat.sudo")) {
                return null;
            }
            if ("mute".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.chat.mute")) {
                return null;
            }
            if ("unmute".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.chat.unmute")) {
                return null;
            }
            if ("mode".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.chat.mode")) {
                List<String> list = new ArrayList<>();
                for (String str : ChatListener.inst().getChatModeKeys()) {
                    if (sender.hasPermission("sweet.chat.mode.set." + str)) {
                        list.add(str);
                    }
                }
                return startsWith(list, args[1]);
            }
        }
        if (args.length == 3) {
            if ("mode".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.chat.mode.others")) {
                return null;
            }
        }
        if (args.length >= 3) {
            if ("mute".equalsIgnoreCase(args[0]) && sender.hasPermission("sweet.chat.mute")) {
                return muteDurationSample;
            }
        }
        return Collections.emptyList();
    }

    public List<String> startsWith(Collection<String> list, String s) {
        return startsWith(null, list, s);
    }
    public List<String> startsWith(String[] addition, Collection<String> list, String s) {
        String s1 = s.toLowerCase();
        List<String> stringList = new ArrayList<>(list);
        if (addition != null) stringList.addAll(0, Lists.newArrayList(addition));
        stringList.removeIf(it -> !it.toLowerCase().startsWith(s1));
        return stringList;
    }
}
