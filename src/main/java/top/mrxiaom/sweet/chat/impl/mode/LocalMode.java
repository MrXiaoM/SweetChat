package top.mrxiaom.sweet.chat.impl.mode;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.sweet.chat.api.ChatContext;
import top.mrxiaom.sweet.chat.api.IChatMode;
import top.mrxiaom.sweet.chat.api.IReloadable;
import top.mrxiaom.sweet.chat.config.ChatFormat;
import top.mrxiaom.sweet.chat.func.ChatListener;
import top.mrxiaom.sweet.chat.utils.ComponentUtils;

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
        TextComponent component = format.build(ctx).build();
        ComponentUtils.send(Bukkit.getConsoleSender(), component);

        Location loc = player.getLocation();
        for (Player p : player.getWorld().getPlayers()) {
            if (p.getLocation().distance(loc) > radius) continue;
            ComponentUtils.send(p, component);
        }

        return true;
    }
}
