package top.mrxiaom.sweet.chat.impl.mode;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.sweet.chat.api.ChatContext;
import top.mrxiaom.sweet.chat.api.IChatFilter;
import top.mrxiaom.sweet.chat.api.IChatMode;
import top.mrxiaom.sweet.chat.api.IReloadable;
import top.mrxiaom.sweet.chat.config.formats.ChatFormat;
import top.mrxiaom.sweet.chat.func.ChatListener;
import top.mrxiaom.sweet.chat.func.FilterManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 附近聊天模式实现
 */
public class LocalMode implements IChatMode, IReloadable {
    private final ChatListener parent;
    private final List<ChatFormat> formats = new ArrayList<>();
    private double radius;
    public LocalMode(ChatListener parent) {
        this.parent = parent;
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        radius = config.getDouble("chat-mode.local.radius", 64.0);
        formats.clear();
        Map<String, ChatFormat> formats = parent.getChatFormats();
        Predicate<String> p = StringMatchUtils.parse(config.getStringList("chat-mode.local.formats"));
        for (ChatFormat chatFormat : formats.values()) {
            if (p.test(chatFormat.id())) {
                this.formats.add(chatFormat);
            }
        }
        if (formats.isEmpty() && parent.canReachChatMode(this)) {
            parent.warn("[chat-mode] 未对聊天模式 local 配置任何聊天格式");
        }
        this.formats.sort(Comparator.comparingInt(ChatFormat::priority));
    }

    @Override
    public boolean chat(@NotNull ChatContext ctx) {
        Player player = ctx.player();
        ChatFormat format = parent.getChatFormat(formats, player);
        if (format == null) {
            return false;
        }

        // 聊天过滤器检查
        for (IChatFilter filter : FilterManager.inst().getFilters()) {
            IChatFilter.Matched match = filter.match(ctx);
            if (match != null && match.punish()) {
                return true;
            }
        }

        TextComponent component = format.build(ctx).build();

        Location loc = player.getLocation();
        List<Player> players = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isOutOfRange(loc, p.getLocation(), radius)) {
                // 本服有权限的玩家无视附近聊天限制
                if (!p.hasPermission("sweetchat.local.bypass")) {
                    continue;
                }
            }
            players.add(p);
        }
        ChatListener.inst().broadcast(players, component);

        return true;
    }

    private static boolean isOutOfRange(Location loc1, Location loc2, double radius) {
        World world1 = loc1.getWorld();
        World world2 = loc2.getWorld();
        if (world1 == null || world1 != world2) return true;
        return loc1.distance(loc2) > radius;
    }
}
