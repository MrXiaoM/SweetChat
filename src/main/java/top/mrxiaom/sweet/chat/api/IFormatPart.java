package top.mrxiaom.sweet.chat.api;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * 聊天格式部分
 */
public interface IFormatPart {
    /**
     * 根据聊天上下文和配置，生成聊天格式部分
     * @param ctx 聊天上下文
     * @return 待拼接的文本组件
     */
    @NotNull Component get(@NotNull ChatContext ctx);
}
