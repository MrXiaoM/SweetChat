package top.mrxiaom.sweet.chat.api;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

/**
 * 消息替换处理器
 */
public interface IMessageProcessor {
    /**
     * 匹配优先级，数值越小越先匹配
     */
    default int priority() {
        return 1000;
    }

    /**
     * 处理传入的聊天消息，添加替换标签的实现
     * @param inputText 传入的聊天消息，此前可能已经经过处理了
     * @param ctx 聊天上下文
     * @param builder 用于添加标签
     * @return 替换后的聊天消息
     */
    @NotNull String process(@NotNull String inputText, @NotNull ChatContext ctx, @NotNull MiniMessage.Builder builder);
}
