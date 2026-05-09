package top.mrxiaom.sweet.chat.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.ListPair;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.depend.PAPI;
import top.mrxiaom.sweet.chat.SweetChat;
import top.mrxiaom.sweet.chat.func.PlaceholdersManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家聊天上下文
 */
public class ChatContext {
    private final SweetChat plugin;
    private final Player player;
    private final Map<String, Object> tags = new HashMap<>();
    private final ListPair<String, Object> replacements = new ListPair<>();
    private String text;
    @ApiStatus.Internal
    public ChatContext(SweetChat plugin, Player player, String text) {
        this.plugin = plugin;
        this.player = player;
        this.text = text;
        PlaceholdersManager.inst().addReplacements(player, replacements);
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
     * 玩家 UUID
     */
    public UUID uuid() {
        return player.getUniqueId();
    }

    /**
     * 玩家名称
     */
    public String name() {
        return player.getName();
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

    @NotNull
    public String setPlaceholders(@NotNull String str) {
        return PAPI.setPlaceholders(player, Pair.replace(str, replacements));
    }

    @NotNull
    public List<String> setPlaceholders(@NotNull List<String> list) {
        return PAPI.setPlaceholders(player, Pair.replace(list, replacements));
    }

    /**
     * 获取额外变量列表
     */
    public ListPair<String, Object> replacements() {
        return replacements;
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
