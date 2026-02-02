package top.mrxiaom.sweet.chat.func;

import com.google.common.collect.Iterables;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTReflectionUtil;
import de.tr7zw.changeme.nbtapi.utils.nmsmappings.ReflectionMethod;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IRunTask;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.*;
import top.mrxiaom.pluginbase.utils.depend.PAPI;
import top.mrxiaom.sweet.chat.SweetChat;
import top.mrxiaom.sweet.chat.api.ChatContext;
import top.mrxiaom.sweet.chat.api.IMessageProcessor;
import top.mrxiaom.sweet.chat.config.formats.ComponentBuilder;
import top.mrxiaom.sweet.chat.config.replacements.AtConfig;
import top.mrxiaom.sweet.chat.config.replacements.EnumItemSource;
import top.mrxiaom.sweet.chat.config.replacements.EnumPlayerSource;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 管理聊天消息内容替换的模块
 */
@AutoRegister
public class MessageReplacementManager extends AbstractModule {
    private final boolean supportTranslatable = Util.isPresent("org.bukkit.Translatable");
    private final boolean supportLangUtils = Util.isPresent("com.meowj.langutils.lang.LanguageHelper");
    private final List<IMessageProcessor> messagePreProcessorRegistry = new ArrayList<>();
    private final List<IMessageProcessor> messagePostProcessorRegistry = new ArrayList<>();
    private final Map<String, EnumItemSource> itemDisplayInput = new HashMap<>();
    private String itemDisplayFormat;
    private boolean itemDisplayOnlyReplaceOnce;
    private final Map<String, ComponentBuilder> placeholdersInput = new HashMap<>();
    private final Map<Pattern, ComponentBuilder> placeholdersRegex = new HashMap<>();
    private boolean placeholdersOnlyReplaceOnce;
    private AtConfig atConfig;
    private final List<String> bungeeAllPlayers = new ArrayList<>();
    private IRunTask bungeeTask;
    public MessageReplacementManager(SweetChat plugin) {
        super(plugin);
        registerBungee();
    }

    @ApiStatus.Internal
    public void registerMessagePreProcessor(@NotNull IMessageProcessor processor) {
        messagePreProcessorRegistry.add(processor);
        messagePreProcessorRegistry.sort(Comparator.comparingInt(IMessageProcessor::priority));
    }

    @ApiStatus.Internal
    public void unregisterMessagePreProcessor(@NotNull IMessageProcessor processor) {
        messagePreProcessorRegistry.remove(processor);
        messagePreProcessorRegistry.sort(Comparator.comparingInt(IMessageProcessor::priority));
    }

    @ApiStatus.Internal
    public void registerMessagePostProcessor(@NotNull IMessageProcessor processor) {
        messagePostProcessorRegistry.add(processor);
        messagePostProcessorRegistry.sort(Comparator.comparingInt(IMessageProcessor::priority));
    }

    @ApiStatus.Internal
    public void unregisterMessagePostProcessor(@NotNull IMessageProcessor processor) {
        messagePostProcessorRegistry.remove(processor);
        messagePostProcessorRegistry.sort(Comparator.comparingInt(IMessageProcessor::priority));
    }

    @Override
    public void receiveBungee(String subChannel, DataInputStream in) throws IOException {
        if (subChannel.equals("PlayerList")) {
            String playerList = in.readUTF();
            bungeeAllPlayers.clear();
            for (String playerName : playerList.split(",")) {
                bungeeAllPlayers.add(playerName.trim());
            }
        }
    }

    private void getAllPlayers() {
        Player whoever = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (whoever != null) {
            whoever.sendPluginMessage(plugin, "BungeeCord", Bytes.build("PlayerList"));
        }
    }

    @Nullable
    public String getBungeePlayerName(@NotNull String name) {
        for (String playerName : bungeeAllPlayers) {
            if (playerName.equalsIgnoreCase(name)) {
                return playerName;
            }
        }
        return null;
    }

    @Override
    public void reloadConfig(MemoryConfiguration pluginConfig) {
        if (bungeeTask != null) {
            bungeeTask.cancel();
            bungeeTask = null;
        }
        File file = plugin.resolve("./replacements.yml");
        if (!file.exists()) {
            plugin.saveResource("replacements.yml");
        }
        FileConfiguration config = plugin.resolveGotoFlag(ConfigUtils.load(file));
        ConfigurationSection section;
        itemDisplayFormat = config.getString("message-replacements.item-display.format", "[%item%]").replace("%item%", "<item/>");
        itemDisplayOnlyReplaceOnce = config.getBoolean("message-replacements.item-display.one-slot-only-replace-once", true);
        itemDisplayInput.clear();
        section = config.getConfigurationSection("message-replacements.item-display.input");
        if (section != null) for (String key : section.getKeys(false)) {
            String str = section.getString(key);
            EnumItemSource value = Util.valueOr(EnumItemSource.class, str, null);
            if (value != null) {
                itemDisplayInput.put(key, value);
            } else {
                warn("[item-display] input 中的键 " + key + " 对应的值 " + str + " 无效");
            }
        }
        placeholdersOnlyReplaceOnce = config.getBoolean("message-replacements.placeholders.one-key-only-replace-once", true);
        placeholdersInput.clear();
        placeholdersRegex.clear();
        section = config.getConfigurationSection("message-replacements.placeholders.input");
        if (section != null) for (String key : section.getKeys(false)) {
            if (section.isConfigurationSection(key)) {
                ConfigurationSection s = section.getConfigurationSection(key);
                if (s != null) {
                    placeholdersInput.put(key, new ComponentBuilder(s));
                    continue;
                }
            } else {
                String value = section.getString(key);
                if (value != null) {
                    placeholdersInput.put(key, new ComponentBuilder(value));
                    continue;
                }
            }
            warn("[placeholders] input 中的键 " + key + " 对应的值无效");
        }
        for (ConfigurationSection s : ConfigUtils.getSectionList(config, "message-replacements.placeholders.regex")) {
            String regex = s.getString("regex");
            if (regex == null) {
                warn("[placeholders] regex 未设定值");
                continue;
            }
            Pattern pattern;
            try {
                pattern = Pattern.compile(regex);
            } catch (IllegalArgumentException e) {
                warn("[placeholders] regex 的正则表达式 " + regex + " 无效: " + e.getMessage());
                continue;
            }
            placeholdersRegex.put(pattern, new ComponentBuilder(s));
        }
        ConfigurationSection atSection = config.getConfigurationSection("message-replacements.at");
        if (atSection != null) {
            atConfig = new AtConfig(this, atSection);
            if (atConfig.getPlayerSource().equals(EnumPlayerSource.BUNGEE_CORD)) {
                bungeeTask = plugin.getScheduler().runTaskTimer(this::getAllPlayers, 15 * 20L, 15 * 20L);
            }
        } else {
            atConfig = new AtConfig(this, new MemoryConfiguration());
        }
    }

    public AtConfig getAtConfig() {
        return atConfig;
    }

    @NotNull
    public String handle(@NotNull String inputText, @NotNull ChatContext ctx, @NotNull MiniMessage.Builder builder) {
        String text = inputText;
        Player player = ctx.player();
        for (IMessageProcessor processor : messagePreProcessorRegistry) {
            text = processor.process(text, ctx, builder);
        }
        text = handleItemDisplay(player, text, builder);
        text = handlePlaceholders(player, text, builder);
        for (IMessageProcessor processor : messagePostProcessorRegistry) {
            text = processor.process(text, ctx, builder);
        }
        return text;
    }

    @NotNull
    @SuppressWarnings("PatternValidation")
    public String handleItemDisplay(@NotNull Player player, @NotNull String inputText, @NotNull MiniMessage.Builder builder) {
        PlayerInventory inv = player.getInventory();
        Set<EnumItemSource> addedItems = new HashSet<>();
        String text = inputText;
        // 以防玩家类似输入 %i <sweet-chat-item-hand/> 绕过限制
        for (EnumItemSource value : itemDisplayInput.values()) {
            String tagName = value.tagName();
            if (text.contains("<" + tagName + ">")) {
                text = text.replace("<" + tagName + ">", "");
            }
            if (text.contains("<" + tagName + "/")) {
                text = text.replace("<" + tagName + "/>", "");
            }
        }
        for (Map.Entry<String, EnumItemSource> entry : itemDisplayInput.entrySet()) {
            String key = entry.getKey();
            if (text.contains(key)) {
                EnumItemSource value = entry.getValue();
                String tagName = value.tagName();
                // 替换文本
                if (itemDisplayOnlyReplaceOnce) {
                    if (addedItems.contains(value)) continue;
                    text = replaceFirst(text, key, "<" + tagName + "/>");
                } else {
                    text = text.replace(key, "<" + tagName + "/>");
                }

                if (addedItems.contains(value)) continue;
                addedItems.add(value);
                // 添加标签
                Component item = toComponent(value.get(inv), player);
                builder.editTags(tags -> tags.tag(tagName, Tag.selfClosingInserting(item)));
            }
        }
        return text;
    }

    @NotNull
    @SuppressWarnings("PatternValidation")
    public String handlePlaceholders(@NotNull Player player, @NotNull String inputText, @NotNull MiniMessage.Builder builder) {
        // 以防玩家类似输入 <sweet-chat-placeholders-0/> 绕过限制
        String text = inputText.replaceAll("<sweet-chat-placeholders-\\d+/?>", "");
        int i = 0;
        for (Map.Entry<Pattern, ComponentBuilder> entry : placeholdersRegex.entrySet()) {
            Pattern pattern = entry.getKey();
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String tagName = "sweet-chat-placeholders-" + (i++);
                ListPair<String, Object> r = new ListPair<>();
                for (int j = 0; j <= matcher.groupCount(); j++) {
                    r.add("$" + j, matcher.group(j));
                }
                if (placeholdersOnlyReplaceOnce) {
                    text = matcher.replaceFirst("<" + tagName + "/>");
                } else {
                    text = matcher.replaceAll("<" + tagName + "/>");
                }
                Component component = entry.getValue().build(str -> PAPI.setPlaceholders(player, Pair.replace(str, r)));
                builder.editTags(tags -> tags.tag(tagName, Tag.selfClosingInserting(component)));
            }
        }
        for (Map.Entry<String, ComponentBuilder> entry : placeholdersInput.entrySet()) {
            String key = entry.getKey();
            if (text.contains(key)) {
                String tagName = "sweet-chat-placeholders-" + (i++);
                if (placeholdersOnlyReplaceOnce) {
                    text = replaceFirst(text, key, "<" + tagName + "/>");
                } else {
                    text = text.replace(key, "<" + tagName + "/>");
                }
                Component component = entry.getValue().build(str -> PAPI.setPlaceholders(player, str));
                builder.editTags(tags -> tags.tag(tagName, Tag.selfClosingInserting(component)));
            }
        }
        if (atConfig.isEnable()) {
            text = atConfig.handle(player, inputText, builder);
        }
        return text;
    }

    public Component toComponent(@NotNull ItemStack item) {
        return toComponent(item, null);
    }

    public Component toComponent(@NotNull ItemStack item, @Nullable Player player) {
        Component component;
        Component displayName = AdventureItemStack.getItemDisplayName(item);
        if (displayName != null) {
            MiniMessage mm = AdventureUtil.builder()
                    .editTags(tags -> tags.tag("item", Tag.selfClosingInserting(displayName)))
                    .build();
            component = AdventureUtil.miniMessage(mm, itemDisplayFormat);
        } else if (supportTranslatable) {
            // 通过可翻译文本显示物品名
            String key = item.getTranslationKey();
            component = AdventureUtil.miniMessage(itemDisplayFormat.replace("<item/>", "<lang:" + key + ">"));
        } else if (supportLangUtils) {
            // 通过 LangUtils 获取物品名
            if (player == null) {
                component = Component.text(com.meowj.langutils.lang.LanguageHelper.getItemName(item, "fallback"));
            } else {
                component = Component.text(com.meowj.langutils.lang.LanguageHelper.getItemName(item, player));
            }
        } else {
            // 在最糟糕的情况下，通过物品 ID 强行拼接物品英文名
            StringJoiner name = new StringJoiner(" ");
            for (String word : item.getType().name().toLowerCase().split("_")) {
                if (word.length() == 1) {
                    name.add(word.toUpperCase());
                } else {
                    name.add(Character.toUpperCase(word.charAt(0)) + word.substring(1));
                }
            }
            component = AdventureUtil.miniMessage(itemDisplayFormat.replace("<item/>", name.toString()));
        }
        HoverEventSource<?> hover = toHoverEvent(item);
        return component.hoverEvent(hover);
    }

    /**
     * 将物品转换为鼠标悬停显示参数
     * <p>
     * 来自 {@link AdventureItemStack#toHoverEvent(ItemStack)}
     * @param item 物品
     */
    @SuppressWarnings({"deprecation"})
    private static HoverEventSource<?> toHoverEvent(ItemStack item) {
        // Paper 方案 - 直接转换
        if (item instanceof HoverEventSource) {
            return (HoverEventSource<?>) item;
        }
        // Spigot 方案 - 读取物品信息与 NBT
        Object nmsItem = ReflectionMethod.ITEMSTACK_NMSCOPY.run(null, item);
        NBTContainer nbt = NBTReflectionUtil.convertNMSItemtoNBTCompound(nmsItem);
        NBTCompound components = nbt.hasTag("components") ? nbt.getCompound("components") : null;
        NBTCompound tag = nbt.hasTag("tag") ? nbt.getCompound("tag") : null;
        // 1.12.2 及以下的 子ID
        Short damage = nbt.hasTag("Damage") ? nbt.getShort("Damage") : null;

        BinaryTagHolder itemTag;
        if (components != null) { // 1.21.5+
            itemTag = BinaryTagHolder.binaryTagHolder(components.toString());
        } else if (tag != null) { // 1.8-1.21.4
            if (damage != null) {
                tag.setShort("Damage", damage);
            }
            itemTag = BinaryTagHolder.binaryTagHolder(tag.toString());
        } else { // 未知格式
            if (damage != null) {
                itemTag = BinaryTagHolder.binaryTagHolder("{Damage:" + damage + "s}");
            } else {
                itemTag = BinaryTagHolder.binaryTagHolder("{}");
            }
        }
        return HoverEvent.showItem(
                Key.key(nbt.getString("id"), ':'),
                nbt.getInteger("count"),
                itemTag);
    }

    @NotNull
    public static String replaceFirst(@NotNull String str, @NotNull String target, @NotNull String replacement) {
        int i = str.indexOf(target);
        if (i == -1) {
            return str;
        }

        int targetLength = target.length();
        String prefix = str.substring(0, i);
        String suffix = str.substring(i + targetLength);
        return prefix + replacement + suffix;
    }

    public static MessageReplacementManager inst() {
        return instanceOf(MessageReplacementManager.class);
    }
}
