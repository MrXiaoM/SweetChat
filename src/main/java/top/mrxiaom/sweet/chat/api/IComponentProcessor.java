package top.mrxiaom.sweet.chat.api;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * 聊天消息处理器
 */
public interface IComponentProcessor {
    default int priority() {
        return 1000;
    }
    @NotNull Component process(@NotNull Component component, @NotNull ChatContext ctx);
}
