package top.mrxiaom.sweet.chat.api;

import org.bukkit.configuration.MemoryConfiguration;

/**
 * 可重载接口
 */
public interface IReloadable {
    void reloadConfig(MemoryConfiguration config);
}
