package top.mrxiaom.sweet.chat.func;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.AdventureItemStack;
import top.mrxiaom.pluginbase.utils.AdventureUtil;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.pluginbase.utils.depend.PAPI;
import top.mrxiaom.sweet.chat.SweetChat;
import top.mrxiaom.sweet.chat.config.EnumItemSource;

import java.util.*;

@AutoRegister
public class MessageReplacementManager extends AbstractModule {
    private final boolean supportTranslatable = Util.isPresent("org.bukkit.Translatable");
    private final Map<String, EnumItemSource> itemDisplayInput = new HashMap<>();
    private String itemDisplayFormat;
    private boolean itemDisplayOnlyReplaceOnce;
    private final Map<String, String> placeholdersInput = new HashMap<>();
    private boolean placeholdersOnlyReplaceOnce;
    public MessageReplacementManager(SweetChat plugin) {
        super(plugin);
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
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
        section = config.getConfigurationSection("message-replacements.placeholders.input");
        if (section != null) for (String key : section.getKeys(false)) {
            String value = section.getString(key);
            if (value != null) {
                placeholdersInput.put(key, value);
            } else {
                warn("[placeholders] input 中的键 " + key + " 对应的值无效");
            }
        }
    }

    @SuppressWarnings("PatternValidation")
    public String handleItemDisplay(Player player, String inputText, MiniMessage.Builder builder) {
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
                text = text.replace("<" + tagName + "/", "");
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
                Component item = toComponent(value.get(inv));
                builder.editTags(tags -> tags.tag(tagName, Tag.selfClosingInserting(item)));
            }
        }
        return text;
    }

    @SuppressWarnings("PatternValidation")
    public String handlePlaceholders(Player player, String inputText, MiniMessage.Builder builder) {
        String text = inputText;
        int count = 0;
        for (String key : placeholdersInput.keySet()) {
            if (text.contains(key)) count++;
        }
        // 以防玩家类似输入 %i <sweet-chat-placeholders-0/> 绕过限制
        for (int i = 0; i < count; i++) {
            String tagName = "sweet-chat-placeholders-" + i;
            if (text.contains("<" + tagName + ">")) {
                text = text.replace("<" + tagName + ">", "");
            }
            if (text.contains("<" + tagName + "/")) {
                text = text.replace("<" + tagName + "/", "");
            }
        }
        int i = 0;
        for (Map.Entry<String, String> entry : placeholdersInput.entrySet()) {
            String key = entry.getKey();
            if (text.contains(key)) {
                String tagName = "sweet-chat-placeholders-" + (i++);
                if (placeholdersOnlyReplaceOnce) {
                    text = replaceFirst(text, key, "<" + tagName + "/>");
                } else {
                    text = text.replace(key, "<" + tagName + "/>");
                }
                String str = PAPI.setPlaceholders(player, entry.getValue());
                Component component = AdventureUtil.miniMessage(str);
                builder.editTags(tags -> tags.tag(tagName, Tag.selfClosingInserting(component)));
            }
        }
        return text;
    }

    public Component toComponent(ItemStack item) {
        Component component;
        Component displayName = AdventureItemStack.getItemDisplayName(item);
        if (displayName != null) {
            MiniMessage mm = AdventureUtil.builder()
                    .editTags(tags -> tags.tag("item", Tag.selfClosingInserting(displayName)))
                    .build();
            component = AdventureUtil.miniMessage(mm, itemDisplayFormat);
        } else if (supportTranslatable) {
            String key = item.getTranslationKey();
            component = AdventureUtil.miniMessage(itemDisplayFormat.replace("<item/>", "<lang:" + key + ">"));
        } else {
            // TODO: 添加 LangUtils 支持，用于获取物品名
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
        HoverEventSource<?> hover = AdventureItemStack.toHoverEvent(item);
        return component.hoverEvent(hover);
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
