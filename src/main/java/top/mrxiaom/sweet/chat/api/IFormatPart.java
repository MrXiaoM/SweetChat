package top.mrxiaom.sweet.chat.api;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * 聊天格式部分
 */
public interface IFormatPart {
    @NotNull Component get(@NotNull ChatContext ctx);
}
