package top.mrxiaom.sweet.chat.func;

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
import top.mrxiaom.pluginbase.utils.ConfigUtils;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.chat.SweetChat;
import top.mrxiaom.sweet.chat.api.*;
import top.mrxiaom.sweet.chat.config.ChatFormat;
import top.mrxiaom.sweet.chat.impl.format.PartPlain;
import top.mrxiaom.sweet.chat.impl.format.PartPlayerMessage;
import top.mrxiaom.sweet.chat.impl.mode.GlobalMode;
import top.mrxiaom.sweet.chat.impl.mode.LocalMode;

import java.io.File;
import java.util.*;

/**
 * 管理聊天事件监听、聊天格式配置的模块
 */
@AutoRegister
public class ChatListener extends AbstractModule implements Listener {
    private final Map<String, IFormatPartProvider> partRegistry = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, IChatMode> chatModeRegistry = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, ChatFormat> chatFormatMap = new HashMap<>();
    private IChatMode chatModeDefault;
    private final Map<String, IChatMode> chatModeSwitchPrefix = new HashMap<>();
    public ChatListener(SweetChat plugin) {
        super(plugin);
        registerPart("plain", PartPlain::new);
        registerPart("player message", PartPlayerMessage::new);
        registerChatMode("global", new GlobalMode(this));
        registerChatMode("local", new LocalMode(this));
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        HandlerList.unregisterAll(this);
        File folder = plugin.resolve(config.getString("folder.chat", "./chat"));
        if (!folder.exists()) {
            plugin.saveResource("chat/global/default.yml", new File(folder, "global/default.yml"));
            plugin.saveResource("chat/local/default.yml", new File(folder, "local/default.yml"));
            plugin.saveResource("chat/default.yml", new File(folder, "default.yml"));
        }
        reloadChatFormat(folder);
        reloadChatMode(config);
        EventPriority priority = getPriority(config, "listener.priority", EventPriority.HIGHEST);
        Bukkit.getPluginManager().registerEvent(AsyncPlayerChatEvent.class, this, priority, (listener, event) -> {
            if (event instanceof AsyncPlayerChatEvent) {
                AsyncPlayerChatEvent e = (AsyncPlayerChatEvent) event;
                if (e.isCancelled()) return;
                if (onChat(e.getPlayer(), e.getMessage())) {
                    e.setCancelled(true);
                    e.setFormat("");
                }
            }
        }, plugin, false);
    }

    private void reloadChatFormat(File folder) {
        chatFormatMap.clear();
        Util.reloadFolder(folder, false, (id, file) -> {
            YamlConfiguration config = ConfigUtils.load(file);
            try {
                ChatFormat format = new ChatFormat(this, id.replace("\\", "/"), config);
                chatFormatMap.put(format.id(), format);
            } catch (Throwable t) {
                warn("[chat] 加载聊天格式配置 " + id + " 时出现错误: " + t.getMessage());
            }
        });
        info("[chat] 加载了 " + chatFormatMap.size() + " 个聊天格式配置");
    }

    private void reloadChatMode(MemoryConfiguration config) {
        IChatMode chatMode = chatModeRegistry.get(config.getString("chat-mode.default-mode", "GLOBAL"));
        if (chatMode == null) {
            this.chatModeDefault = chatModeRegistry.get("global");
            warn("[chat-mode] 默认聊天模式的值无效");
        } else {
            this.chatModeDefault = chatMode;
        }
        chatModeSwitchPrefix.clear();
        ConfigurationSection section = config.getConfigurationSection("chat-mode.switch-mode-prefix");
        if (section != null) for (String key : section.getKeys(false)) {
            IChatMode value = chatModeRegistry.get(section.getString(key));
            if (value == null) {
                warn("[chat-mode] 前缀 " + key + " 指定的聊天模式无效");
            } else {
                chatModeSwitchPrefix.put(key, value);
            }
        }
        for (IChatMode mode : chatModeRegistry.values()) {
            if (mode instanceof IReloadable) {
                ((IReloadable) mode).reloadConfig(config);
            }
        }
    }

    public boolean onChat(Player player, String text) {
        ChatContext ctx = new ChatContext(plugin, player, text);
        return getChatMode(ctx).chat(ctx);
    }

    private IChatMode getChatMode(ChatContext ctx) {
        for (Map.Entry<String, IChatMode> entry : chatModeSwitchPrefix.entrySet()) {
            String prefix = entry.getKey();
            if (ctx.text().startsWith(prefix)) {
                ctx.text(ctx.text().substring(prefix.length()));
                return entry.getValue();
            }
        }
        return chatModeDefault;
    }

    public boolean canReachChatMode(IChatMode mode) {
        for (IChatMode chatMode : chatModeSwitchPrefix.values()) {
            if (chatMode.equals(mode)) {
                return true;
            }
        }
        return chatModeDefault.equals(mode);
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
    public Map<String, ChatFormat> getChatFormats() {
        return Collections.unmodifiableMap(chatFormatMap);
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

    public void registerChatMode(@NotNull String modeId, @NotNull IChatMode chatMode) {
        chatModeRegistry.put(modeId, chatMode);
    }

    public void unregisterChatMode(@NotNull String modeId) {
        chatModeRegistry.remove(modeId);
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
