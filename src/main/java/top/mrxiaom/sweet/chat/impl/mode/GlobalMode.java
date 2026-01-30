package top.mrxiaom.sweet.chat.impl.mode;

import com.ezylang.evalex.Expression;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.actions.ActionProviders;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.utils.AdventureUtil;
import top.mrxiaom.sweet.chat.api.ChatContext;
import top.mrxiaom.sweet.chat.api.IChatMode;
import top.mrxiaom.sweet.chat.api.IReloadable;
import top.mrxiaom.sweet.chat.config.ChatFormat;
import top.mrxiaom.sweet.chat.func.BroadcastManager;
import top.mrxiaom.sweet.chat.func.ChatListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
        if (!requirement.isEmpty()) {
            try {
                String expression = ctx.setPlaceholders(requirement);
                if (new Expression(expression).evaluate().getBooleanValue() == Boolean.TRUE) {
                    ActionProviders.run(parent.plugin, ctx.player(), successActions);
                } else {
                    ActionProviders.run(parent.plugin, ctx.player(), denyActions);
                    return true;
                }
            } catch (Throwable t) {
                parent.warn("[chat-mode] 全局广播模式条件计算错误", t);
                return true;
            }
        }

        TextComponent component = format.build(ctx).build();
        AdventureUtil.sendMessage(Bukkit.getConsoleSender(), component);
        for (Player p : Bukkit.getOnlinePlayers()) {
            AdventureUtil.sendMessage(p, component);
        }

        BroadcastManager.inst().broadcast(ctx, format);
        return true;
    }
}
