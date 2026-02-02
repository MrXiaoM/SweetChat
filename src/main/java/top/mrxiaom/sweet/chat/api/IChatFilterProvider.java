package top.mrxiaom.sweet.chat.api;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 聊天过滤器提供器，根据配置读取聊天过滤器
 */
@FunctionalInterface
public interface IChatFilterProvider {
    /**
     * 匹配优先级，数值越小越先匹配
     */
    default int providerPriority() {
        return 1000;
    }

    /**
     * 通过配置加载聊天过滤器
     * @param config 配置 section
     * @return 返回 <code>null</code> 代表过滤器配置不匹配
     * @throws RuntimeException 配置出现异常值时抛出
     */
    @Nullable
    IChatFilter load(@NotNull ConfigurationSection config) throws RuntimeException;
}
