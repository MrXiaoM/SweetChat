package top.mrxiaom.sweet.chat.config;

/**
 * 跨服消息广播模式
 */
public enum CrossServerMode {
    /**
     * 不广播消息
     */
    NONE,
    /**
     * 通过 BungeeCord 消息通道广播消息
     */
    BUNGEE_CORD,
    /**
     * 通过数据库轮询广播消息
     */
    DATABASE
}
