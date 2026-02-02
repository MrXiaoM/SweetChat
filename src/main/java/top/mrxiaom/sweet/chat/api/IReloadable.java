package top.mrxiaom.sweet.chat.api;

import org.bukkit.configuration.MemoryConfiguration;

/**
 * 可重载接口
 */
public interface IReloadable {
    /**
     * 重载配置文件
     * @param config 插件 <code>config.yml</code> 配置实例
     */
    void reloadConfig(MemoryConfiguration config);
}
