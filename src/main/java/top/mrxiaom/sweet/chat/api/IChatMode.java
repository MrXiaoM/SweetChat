package top.mrxiaom.sweet.chat.api;

import org.jetbrains.annotations.NotNull;

/**
 * 聊天模式实现
 */
public interface IChatMode {
    boolean chat(@NotNull ChatContext ctx);
}
