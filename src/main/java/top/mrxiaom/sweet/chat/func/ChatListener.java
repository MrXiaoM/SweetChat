package top.mrxiaom.sweet.chat.func;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.actions.ActionProviders;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.api.IRegistry;
import top.mrxiaom.pluginbase.data.Duration;
import top.mrxiaom.pluginbase.data.SimpleRegistry;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.*;
import top.mrxiaom.sweet.chat.Messages;
import top.mrxiaom.sweet.chat.SweetChat;
import top.mrxiaom.sweet.chat.api.*;
import top.mrxiaom.sweet.chat.config.formats.ChatFormat;
import top.mrxiaom.sweet.chat.config.replacements.AtConfig;
import top.mrxiaom.sweet.chat.database.MuteDatabase;
import top.mrxiaom.sweet.chat.database.data.Mute;
import top.mrxiaom.sweet.chat.impl.format.PartPlain;
import top.mrxiaom.sweet.chat.impl.format.PartPlayerMessage;
import top.mrxiaom.sweet.chat.impl.mode.GlobalMode;
import top.mrxiaom.sweet.chat.impl.mode.LocalMode;
import top.mrxiaom.sweet.chat.utils.ComponentUtils;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

/**
 * 管理聊天事件监听、聊天格式配置的模块
 */
@AutoRegister
public class ChatListener extends AbstractModule implements Listener {
    private final Map<String, IFormatPartProvider> partRegistry = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, IChatMode> chatModeRegistry = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, ChatFormat> chatFormatMap = new HashMap<>();
    private final IRegistry<IPostChatAction> postChatRegistry = new SimpleRegistry<>();
    private final boolean isPaperEvent = Util.isPresent("io.papermc.paper.event.player.AsyncChatEvent");
    private IChatMode chatModeDefault;
    private final Map<String, List<IChatMode>> chatModeSwitchPrefix = new HashMap<>();
    private boolean cancelEventWhenNoFormat;
    private List<IAction> noFormatActions;
    private boolean shouldCheckMuteInChat;
    public ChatListener(SweetChat plugin) {
        super(plugin);
        registerPart("plain", PartPlain::new);
        registerPart("player message", PartPlayerMessage::new);
        registerChatMode("global", new GlobalMode(this));
        registerChatMode("local", new LocalMode(this));
    }

    private interface IChatEventImpl {
        void registerChat(ChatListener parent, EventPriority priority);
        void registerMute(ChatListener parent, EventPriority priority);
    }
    private enum ChatEventImpl {
        @SuppressWarnings("deprecation")
        BUKKIT(1, new IChatEventImpl() {
            @Override
            public void registerChat(ChatListener parent, EventPriority priority) {
                Bukkit.getPluginManager().registerEvent(AsyncPlayerChatEvent.class, parent, priority, (l, event) -> {
                    if (!(event instanceof AsyncPlayerChatEvent)) return;
                    AsyncPlayerChatEvent e = (AsyncPlayerChatEvent) event;
                    if (e.isCancelled()) return;
                    parent.processChatEvent(e.getPlayer(), e.getMessage(), e);
                    if (e.isCancelled()) {
                        e.setFormat("");
                    }
                }, parent.plugin, true);
            }

            @Override
            public void registerMute(ChatListener parent, EventPriority priority) {
                Bukkit.getPluginManager().registerEvent(AsyncPlayerChatEvent.class, parent, priority, (l, event) -> {
                    if (!(event instanceof AsyncPlayerChatEvent)) return;
                    AsyncPlayerChatEvent e = (AsyncPlayerChatEvent) event;
                    if (e.isCancelled()) return;
                    if (parent.checkMute(e.getPlayer())) {
                        e.setCancelled(true);
                        e.setFormat("");
                    }
                }, parent.plugin, true);
            }
        }),
        PAPER(0, new IChatEventImpl() {
            @Override
            public void registerChat(ChatListener parent, EventPriority priority) {
                Bukkit.getPluginManager().registerEvent(AsyncChatEvent.class, parent, priority, (l, event) -> {
                    if (!(event instanceof AsyncChatEvent)) return;
                    AsyncChatEvent e = (AsyncChatEvent) event;
                    if (e.isCancelled()) return;
                    Player player = e.getPlayer();
                    String message = LegacyComponentSerializer.legacySection().serialize(e.message());
                    parent.processChatEvent(player, message, e);
                }, parent.plugin, true);
            }

            @Override
            public void registerMute(ChatListener parent, EventPriority priority) {
                Bukkit.getPluginManager().registerEvent(AsyncChatEvent.class, parent, priority, (l, event) -> {
                    if (!(event instanceof AsyncChatEvent)) return;
                    AsyncChatEvent e = (AsyncChatEvent) event;
                    if (e.isCancelled()) return;
                    if (parent.checkMute(e.getPlayer())) {
                        e.setCancelled(true);
                    }
                }, parent.plugin, true);
            }
        })

        ;
        private final int priority;
        private final IChatEventImpl impl;
        ChatEventImpl(int priority, IChatEventImpl impl) {
            this.priority = priority;
            this.impl = impl;
        }

        public void registerChat(ChatListener parent, EventPriority priority) {
            impl.registerChat(parent, priority);
        }

        public void registerMute(ChatListener parent, EventPriority priority) {
            impl.registerMute(parent, priority);
        }
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

        if (config.getBoolean("listener.enable", true)) {
            ChatEventImpl chatEvent = getChatEvent(config, "listener.event", ChatEventImpl.PAPER);
            EventPriority priorityChat = getPriority(config, "listener.priority", EventPriority.HIGHEST);

            ChatEventImpl muteEvent = getChatEvent(config, "mute.event", ChatEventImpl.BUKKIT);
            EventPriority priorityMute = getPriority(config, "mute.priority", EventPriority.HIGH);

            info("使用聊天事件类型 " + chatEvent.name() + "，使用禁言事件类型 " + muteEvent.name());
            chatEvent.registerChat(this, priorityChat);
            if (muteEvent.priority > chatEvent.priority
            || (muteEvent.priority == chatEvent.priority && priorityMute.getSlot() < priorityChat.getSlot())) {
                shouldCheckMuteInChat = false;
                chatEvent.registerMute(this, priorityMute);
            } else {
                shouldCheckMuteInChat = true;
                warn("聊天格式处理的事件优先级配置比禁言的事件高，将不会注册禁言事件监听器，将禁言内联到聊天格式处理中");
            }
        }
    }

    private void processChatEvent(Player player, String message, Cancellable e) {
        try {
            if (onChat(player, message)) {
                e.setCancelled(true);
            }
        } catch (Throwable t) {
            warn("玩家 " + player.getName() + " 发送聊天消息 '" + message + "' 时出现异常", t);
            Messages.chat_exception.tm(player);
            e.setCancelled(true);
        }
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
            List<IChatMode> modes = new ArrayList<>();
            String modeValue = section.getString(key);
            if (modeValue == null) continue;
            for (String s : CollectionUtils.split(modeValue, '|')) {
                if (s.trim().isEmpty()) continue;
                IChatMode value = chatModeRegistry.get(s.trim());
                if (value == null) {
                    warn("[chat-mode] 前缀 " + key + " 指定的聊天模式 " + s.trim() + " 无效");
                } else {
                    modes.add(value);
                }
            }
            if (!modes.isEmpty()) {
                chatModeSwitchPrefix.put(key, modes);
            }
        }
        cancelEventWhenNoFormat = config.getBoolean("chat-mode.cancel-event-when-no-format-match.enable", true);
        noFormatActions = ActionProviders.loadActions(config, "chat-mode.cancel-event-when-no-format-match.actions");
        for (IChatMode mode : chatModeRegistry.values()) {
            if (mode instanceof IReloadable) {
                ((IReloadable) mode).reloadConfig(config);
            }
        }
    }

    public void broadcast(Collection<? extends Player> players, Component component) {
        ComponentUtils.send(Bukkit.getConsoleSender(), component);
        Map<String, Player> playerMap = new HashMap<>();
        for (Player player : players) {
            playerMap.put(player.getName(), player);
            ComponentUtils.send(player, component);
        }
        AtConfig at = MessageReplacementManager.inst().getAtConfig();
        if (!at.getTargetActions().isEmpty()) {
            List<String> resolvedPlayers = new ArrayList<>();
            resolveInsertion(component, insertion -> {
                if (insertion.startsWith("SweetChat:") && insertion.contains("@")) {
                    String[] split = insertion.substring(10).split("@", 2);
                    if (split.length != 2) return;
                    String sender = split[0];
                    String target = split[1];
                    if (resolvedPlayers.contains(target)) return;
                    resolvedPlayers.add(target);
                    Player player = playerMap.get(target);
                    if (player != null) {
                        ListPair<String, Object> r = new ListPair<>();
                        r.add("%sender%", sender);
                        ActionProviders.run(plugin, player, at.getTargetActions(), r);
                    }
                }
            });
        }
    }

    private void resolveInsertion(Component component, Consumer<String> handleInsertion) {
        String insertion = component.insertion();
        if (insertion != null) {
            handleInsertion.accept(insertion);
        }
        for (Component child : component.children()) {
            resolveInsertion(child, handleInsertion);
        }
    }

    public boolean checkMute(Player player) {
        MuteDatabase database = plugin.getMuteDatabase();
        Mute mute = database.getMute(player.getUniqueId());
        LocalDateTime endTime = mute.endTime();
        switch (mute.mode()) {
            case MUTED_TIMED: {
                LocalDateTime now = LocalDateTime.now();
                if (endTime != null && now.isBefore(endTime)) {
                    Duration duration = Duration.between(now, endTime);
                    String durationStr = database.formatDuration(duration);
                    String endTimeStr = database.formatEndTime(endTime);
                    return Messages.Chat.muted__timed.tm(player,
                            Pair.of("%duration%", durationStr),
                            Pair.of("%end_time%", endTimeStr));
                } else {
                    mute.setNotMuted().submit();
                }
                break;
            }
            case MUTED_INFINITE: {
                return Messages.Chat.muted__infinite.tm(player);
            }
            default:
                break;
        }
        return false;
    }

    public boolean onChat(Player player, String text) {
        if (shouldCheckMuteInChat) {
            if (checkMute(player)) {
                return true;
            }
        }
        ChatContext ctx = new ChatContext(plugin, player, text);
        boolean success = getChatMode(ctx).chat(ctx);
        if (!success && cancelEventWhenNoFormat) {
            plugin.getScheduler().runTask(() -> ActionProviders.run(plugin, player, noFormatActions));
            return true;
        }
        return success;
    }

    public IChatMode getChatModeDefault() {
        return chatModeDefault;
    }

    @NotNull
    public Set<String> getChatModeKeys() {
        return chatModeRegistry.keySet();
    }

    @Nullable
    public IChatMode getChatMode(String str) {
        return chatModeRegistry.get(str);
    }

    private IChatMode getChatMode(ChatContext ctx) {
        IChatMode current;
        String mode = plugin.getModeDatabase().getMode(ctx.uuid());
        if (!mode.isEmpty()) {
            current = chatModeRegistry.getOrDefault(mode, chatModeDefault);
        } else {
            current = chatModeDefault;
        }
        for (Map.Entry<String, List<IChatMode>> entry : chatModeSwitchPrefix.entrySet()) {
            String prefix = entry.getKey();
            if (ctx.text().startsWith(prefix)) {
                List<IChatMode> list = entry.getValue();
                for (IChatMode chatMode : list) {
                    if (chatMode.equals(current)) continue;
                    ctx.text(ctx.text().substring(prefix.length()));
                    return chatMode;
                }
            }
        }
        return current;
    }

    public boolean canReachChatMode(IChatMode mode) {
        for (List<IChatMode> chatModes : chatModeSwitchPrefix.values()) {
            if (chatModes.contains(mode)) {
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

    @ApiStatus.Internal
    public void registerPart(@NotNull String partType, @NotNull IFormatPartProvider provider) {
        partRegistry.put(partType, provider);
    }

    @ApiStatus.Internal
    public void unregisterPart(@NotNull String partType) {
        partRegistry.remove(partType);
    }

    @ApiStatus.Internal
    public void registerChatMode(@NotNull String modeId, @NotNull IChatMode chatMode) {
        chatModeRegistry.put(modeId, chatMode);
    }

    @ApiStatus.Internal
    public void unregisterChatMode(@NotNull String modeId) {
        chatModeRegistry.remove(modeId);
    }

    @NotNull
    public IRegistry<IPostChatAction> postChatRegistry() {
        return postChatRegistry;
    }

    @SuppressWarnings("SameParameterValue")
    private EventPriority getPriority(ConfigurationSection config, String key, EventPriority def) {
        String str = config.getString(key);
        EventPriority priority = Util.valueOr(EventPriority.class, str, def);
        if (priority == EventPriority.MONITOR) {
            return EventPriority.HIGHEST;
        } else {
            return priority;
        }
    }

    @SuppressWarnings("SameParameterValue")
    private ChatEventImpl getChatEvent(ConfigurationSection config, String key, ChatEventImpl def) {
        String str = config.getString(key);
        ChatEventImpl chatEvent = Util.valueOr(ChatEventImpl.class, str, def);
        if (!isPaperEvent && chatEvent.equals(ChatEventImpl.PAPER)) {
            return ChatEventImpl.BUKKIT;
        } else {
            return chatEvent;
        }
    }

    public static ChatListener inst() {
        return instanceOf(ChatListener.class);
    }
}
