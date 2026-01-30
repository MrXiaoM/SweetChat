package top.mrxiaom.sweet.chat.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.ConfigUtils;
import top.mrxiaom.sweet.chat.api.ChatContext;
import top.mrxiaom.sweet.chat.api.IFormatPart;
import top.mrxiaom.sweet.chat.func.ChatListener;

import java.util.List;

/**
 * 聊天格式配置，在 {@link ChatListener} 中加载。
 */
public class ChatFormat {
    private final @NotNull String id;
    private final @Nullable String permission;
    private final int priority;
    private final @NotNull List<IFormatPart> chatFormat;
    private final @NotNull List<IFormatPart> crossServerFormat;
    @ApiStatus.Internal
    public ChatFormat(@NotNull ChatListener parent, @NotNull String id, ConfigurationSection config) throws Exception {
        this.id = id;
        String permission = config.getString("permission", "none");
        if (permission.equals("none")) {
            this.permission = null;
        } else {
            this.permission = permission;
        }
        this.priority = config.getInt("priority", 1000);
        this.chatFormat = parent.loadFormat(ConfigUtils.getSectionList(config, "chat-format"));
        if (config.contains("cross-server-format")) {
            this.crossServerFormat = parent.loadFormat(ConfigUtils.getSectionList(config, "cross-server-format"));
        } else {
            this.crossServerFormat = chatFormat;
        }
    }

    @NotNull
    public String id() {
        return id;
    }

    public boolean hasPermission(@NotNull Permissible p) {
        return permission == null || p.hasPermission(permission);
    }

    public int priority() {
        return priority;
    }

    public TextComponent.Builder build(ChatContext ctx) {
        return build(chatFormat, ctx);
    }

    public TextComponent.Builder buildCrossServer(ChatContext ctx) {
        return build(crossServerFormat, ctx);
    }

    private static TextComponent.Builder build(List<IFormatPart> partList, ChatContext ctx) {
        TextComponent.Builder builder = Component.text();
        for (IFormatPart part : partList) {
            builder.append(part.get(ctx));
        }
        return builder;
    }
}
