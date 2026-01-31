package top.mrxiaom.sweet.chat.api;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 聊天过滤器提供器，根据配置读取聊天过滤器
 */
@FunctionalInterface
public interface IChatFilterProvider {
    default int providerPriority() {
        return 1000;
    }
    @Nullable
    IChatFilter load(@NotNull ConfigurationSection config) throws RuntimeException;
}
