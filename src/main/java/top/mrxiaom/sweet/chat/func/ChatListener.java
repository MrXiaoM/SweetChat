package top.mrxiaom.sweet.chat.func;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.chat.SweetChat;
import top.mrxiaom.sweet.chat.api.ChatContext;
import top.mrxiaom.sweet.chat.api.IFormatPart;
import top.mrxiaom.sweet.chat.api.IFormatPartProvider;
import top.mrxiaom.sweet.chat.impl.format.PartPlain;
import top.mrxiaom.sweet.chat.impl.format.PartPlayerMessage;

import java.util.Map;
import java.util.HashMap;

@AutoRegister
public class ChatListener extends AbstractModule implements Listener {
    private final Map<String, IFormatPartProvider> partRegistry = new HashMap<>();
    public ChatListener(SweetChat plugin) {
        super(plugin);
        registerPart("plain", PartPlain::new);
        registerPart("player message", PartPlayerMessage::new);
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        HandlerList.unregisterAll(this);
        EventPriority priority = getPriority(config, "listener.priority", EventPriority.HIGHEST);
        Bukkit.getPluginManager().registerEvent(AsyncPlayerChatEvent.class, this, priority, (listener, event) -> {
            if (event instanceof AsyncPlayerChatEvent) {
                AsyncPlayerChatEvent e = (AsyncPlayerChatEvent) event;
                if (e.isCancelled()) return;
                if (onChat(e.getPlayer(), e.getMessage())) {
                    e.setCancelled(true);
                }
            }
        }, plugin, false);
    }

    private boolean onChat(Player player, String text) {
        ChatContext ctx = new ChatContext(plugin, player, text);

        // TODO: 处理聊天消息
        return false;
    }

    /**
     * 从配置读取聊天格式实例
     * @param config 配置 section
     */
    @NotNull
    public IFormatPart load(@NotNull ConfigurationSection config) throws Exception {
        String type = config.getString("type");
        if (type == null) {
            throw new IllegalArgumentException("未输入参数 type");
        }
        IFormatPartProvider provider = partRegistry.get(type);
        if (provider == null) {
            throw new IllegalArgumentException("找不到类型为 " + type + " 的聊天格式");
        }
        return provider.load(config);
    }

    public void registerPart(@NotNull String partType, @NotNull IFormatPartProvider provider) {
        partRegistry.put(partType, provider);
    }

    public void unregisterPart(@NotNull String partType) {
        partRegistry.remove(partType);
    }

    @SuppressWarnings("SameParameterValue")
    private static EventPriority getPriority(ConfigurationSection config, String key, EventPriority def) {
        String str = config.getString(key);
        EventPriority priority = Util.valueOr(EventPriority.class, str, def);
        if (priority == EventPriority.MONITOR) {
            return EventPriority.HIGHEST;
        } else {
            return priority;
        }
    }

    public static ChatListener inst() {
        return instanceOf(ChatListener.class);
    }
}
