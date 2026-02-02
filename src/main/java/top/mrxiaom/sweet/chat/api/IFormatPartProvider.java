package top.mrxiaom.sweet.chat.api;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

/**
 * 聊天部分提供器，根据配置读取聊天部分
 */
@FunctionalInterface
public interface IFormatPartProvider {
    /**
     * 根据传入的配置 section，读取聊天格式部分
     * @param config 配置 section
     * @return 聊天格式部分实例
     * @throws Exception 在配置数值存在异常时抛出
     */
    @NotNull
    IFormatPart load(@NotNull ConfigurationSection config) throws Exception;
}
