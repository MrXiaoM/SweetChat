package top.mrxiaom.sweet.chat.func;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.AdventureUtil;
import top.mrxiaom.pluginbase.utils.ConfigUtils;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.chat.SweetChat;
import top.mrxiaom.sweet.chat.api.ChatContext;
import top.mrxiaom.sweet.chat.api.IFormatPart;
import top.mrxiaom.sweet.chat.api.IFormatPartProvider;
import top.mrxiaom.sweet.chat.config.ChatFormat;
import top.mrxiaom.sweet.chat.impl.format.PartPlain;
import top.mrxiaom.sweet.chat.impl.format.PartPlayerMessage;

import java.io.File;
import java.util.*;

@AutoRegister
public class ChatListener extends AbstractModule implements Listener {
    private final Map<String, IFormatPartProvider> partRegistry = new HashMap<>();
    private final Map<String, ChatFormat> chatFormatMap = new HashMap<>();
    private final List<ChatFormat> chatFormatWithPriority = new ArrayList<>();
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

    private void reloadChatFormat(File folder) {
        chatFormatMap.clear();
        chatFormatWithPriority.clear();
        Util.reloadFolder(folder, false, (id, file) -> {
            YamlConfiguration config = ConfigUtils.load(file);
            try {
                ChatFormat format = new ChatFormat(this, id, config);
                chatFormatMap.put(id, format);
                chatFormatWithPriority.add(format);
            } catch (Throwable t) {
                warn("[chat] 加载聊天格式配置 " + id + " 时出现错误: " + t.getMessage());
            }
        });
        chatFormatWithPriority.sort(Comparator.comparingInt(ChatFormat::priority));
        info("[chat] 加载了 " + chatFormatMap.size() + " 个聊天格式配置");
    }

    private boolean onChat(Player player, String text) {
        ChatContext ctx = new ChatContext(plugin, player, text);
        // TODO 实现全局模式、本地模式
        ChatFormat format = getChatFormat(chatFormatWithPriority, player);
        if (format == null) {
            return false;
        }

        TextComponent component = format.build(ctx).build();
        AdventureUtil.sendMessage(Bukkit.getConsoleSender(), component);
        for (Player p : Bukkit.getOnlinePlayers()) {
            AdventureUtil.sendMessage(p, component);
        }

        return true;
    }

    @Nullable
    public ChatFormat getChatFormat(List<ChatFormat> list, Player player) {
        for (ChatFormat format : list) {
            if (format.hasPermission(player)) {
                return format;
            }
        }
        return null;
    }

    @NotNull
    public List<IFormatPart> loadFormat(@NotNull List<ConfigurationSection> configList) throws Exception {
        List<IFormatPart> list = new ArrayList<>();
        for (ConfigurationSection section : configList) {
            list.add(loadFormat(section));
        }
        return list;
    }

    /**
     * 从配置读取聊天格式实例
     * @param config 配置 section
     */
    @NotNull
    public IFormatPart loadFormat(@NotNull ConfigurationSection config) throws Exception {
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
