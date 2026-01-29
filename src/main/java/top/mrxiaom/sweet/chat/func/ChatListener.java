package top.mrxiaom.sweet.chat.func;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.chat.SweetChat;
import top.mrxiaom.sweet.chat.api.ChatContext;

@AutoRegister
public class ChatListener extends AbstractModule implements Listener {
    public ChatListener(SweetChat plugin) {
        super(plugin);
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
