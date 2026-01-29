package top.mrxiaom.sweet.chat.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.chat.SweetChat;

import java.util.HashMap;
import java.util.Map;

/**
 * 玩家聊天上下文
 */
public class ChatContext {
    private final SweetChat plugin;
    private final Player player;
    private final Map<String, Object> tags = new HashMap<>();
    private String text;
    @ApiStatus.Internal
    public ChatContext(SweetChat plugin, Player player, String text) {
        this.plugin = plugin;
        this.player = player;
        this.text = text;
    }

    /**
     * 插件主类实例
     */
    public SweetChat plugin() {
        return plugin;
    }

    /**
     * 玩家实例
     */
    public Player player() {
        return player;
    }

    /**
     * 获取聊天文本
     */
    public String text() {
        return text;
    }

    /**
     * 设置聊天文本
     */
    public void text(String text) {
        this.text = text;
    }

    /**
     * 获取聊天上下文标签
     * @param tagName 标签名
     * @return 如果找不到该标签，返回 <code>null</code>
     */
    @Nullable
    public Object tag(String tagName) {
        return tags.get(tagName);
    }

    /**
     * 获取聊天上下文标签
     * @param tagName 标签名
     * @param def 找不到便签时返回默认值
     */
    @Contract("_, !null -> !null")
    public Object tagOrDefault(String tagName, Object def) {
        return tags.getOrDefault(tagName, def);
    }

    /**
     * 设置聊天上下文标签
     * @param tagName 标签名
     * @param value 值，传入 <code>null</code> 代表删除标签
     */
    public void tag(String tagName, @Nullable Object value) {
        if (value != null) {
            tags.put(tagName, value);
        } else {
            tags.remove(tagName);
        }
    }

    /**
     * 获取当前聊天上下文是否存在指定标签
     * @param tagName 标签名
     */
    public boolean hasTag(String tagName) {
        return tags.containsKey(tagName);
    }
}
