package top.mrxiaom.sweet.chat.impl.mode;

import com.ezylang.evalex.Expression;
import com.google.common.collect.Lists;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.actions.ActionProviders;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.chat.SweetChat;
import top.mrxiaom.sweet.chat.api.*;
import top.mrxiaom.sweet.chat.config.formats.ChatFormat;
import top.mrxiaom.sweet.chat.func.BroadcastManager;
import top.mrxiaom.sweet.chat.func.ChatListener;
import top.mrxiaom.sweet.chat.func.FilterManager;

import java.util.*;
import java.util.function.Predicate;

/**
 * 全局广播模式实现
 */
public class GlobalMode implements IChatMode, IReloadable {
    private final ChatListener parent;
    private final List<ChatFormat> formats = new ArrayList<>();
    private String requirement;
    private List<IAction> successActions;
    private List<IAction> denyActions;
    public GlobalMode(ChatListener parent) {
        this.parent = parent;
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        requirement = config.getString("chat-mode.global.requirement", "").trim();
        successActions = ActionProviders.loadActions(config, "chat-mode.global.success-actions");
        denyActions = ActionProviders.loadActions(config, "chat-mode.global.deny-actions");
        formats.clear();
        Map<String, ChatFormat> formats = parent.getChatFormats();
        Predicate<String> p = StringMatchUtils.parse(config.getStringList("chat-mode.global.formats"));
        for (ChatFormat chatFormat : formats.values()) {
            if (p.test(chatFormat.id())) {
                this.formats.add(chatFormat);
            }
        }
        if (formats.isEmpty() && parent.canReachChatMode(this)) {
            parent.warn("[chat-mode] 未对聊天模式 global 配置任何聊天格式");
        }
        this.formats.sort(Comparator.comparingInt(ChatFormat::priority));
    }

    @Override
    public boolean chat(@NotNull ChatContext ctx) {
        ChatFormat format = parent.getChatFormat(formats, ctx.player());
        if (format == null) {
            return false;
        }
        ctx.tag("__internal__format", format);

        if (!requirement.isEmpty()) {
            try {
                SweetChat plugin = parent.plugin;
                String expression = ctx.setPlaceholders(requirement);
                if (new Expression(expression).evaluate().getBooleanValue() == Boolean.TRUE) {
                    plugin.getScheduler().runTask(() -> ActionProviders.run(plugin, ctx.player(), successActions));
                } else {
                    plugin.getScheduler().runTask(() -> ActionProviders.run(plugin, ctx.player(), denyActions));
                    return true;
                }
            } catch (Throwable t) {
                parent.warn("[chat-mode] 全局广播模式条件计算错误", t);
                return true;
            }
        }

        // 聊天过滤器检查
        for (IChatFilter filter : FilterManager.inst().getFilters()) {
            IChatFilter.Matched match = filter.match(ctx);
            if (match != null && match.punish()) {
                return true;
            }
        }

        List<Pair<IFormatPart, Component>> parts = format.buildList(ctx);
        TextComponent.Builder component = Component.text();
        for (Pair<IFormatPart, Component> part : parts) {
            component.append(part.value());
        }
        TextComponent finalMessage = component.build();
        List<Player> players = Lists.newArrayList(Bukkit.getOnlinePlayers());

        ChatListener.inst().broadcast(players, finalMessage);
        BroadcastManager.inst().broadcast(ctx, format);

        ChatPostContext postContext = new ChatPostContext(ctx, players, parts, finalMessage);
        for (IPostChatAction action : parent.postChatRegistry().all()) {
            action.execute(postContext);
        }

        return true;
    }

    @Override
    public String modeName() {
        return "global";
    }
}
