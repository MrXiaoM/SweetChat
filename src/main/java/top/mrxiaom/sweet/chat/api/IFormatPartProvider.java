package top.mrxiaom.sweet.chat.api;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

/**
 * 聊天部分提供器，根据配置读取聊天部分
 */
@FunctionalInterface
public interface IFormatPartProvider {
    @NotNull
    IFormatPart load(@NotNull ConfigurationSection config) throws Exception;
}
