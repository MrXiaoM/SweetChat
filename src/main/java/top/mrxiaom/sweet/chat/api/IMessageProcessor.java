package top.mrxiaom.sweet.chat.api;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

/**
 * 消息替换处理器
 */
public interface IMessageProcessor {
    default int priority() {
        return 1000;
    }
    @NotNull String process(@NotNull String inputText, @NotNull ChatContext ctx, @NotNull MiniMessage.Builder builder);
}
