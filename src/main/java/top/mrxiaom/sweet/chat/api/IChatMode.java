package top.mrxiaom.sweet.chat.api;

import org.jetbrains.annotations.NotNull;

/**
 * 聊天模式实现
 */
public interface IChatMode {
    /**
     * 实现发送聊天消息功能
     * @see top.mrxiaom.sweet.chat.impl.mode.GlobalMode
     * @see top.mrxiaom.sweet.chat.impl.mode.LocalMode
     * @param ctx 聊天上下文
     * @return 是否取消聊天事件
     */
    boolean chat(@NotNull ChatContext ctx);
}
