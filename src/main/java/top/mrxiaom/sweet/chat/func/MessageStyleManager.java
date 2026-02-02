package top.mrxiaom.sweet.chat.func;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.ConfigUtils;
import top.mrxiaom.sweet.chat.SweetChat;
import top.mrxiaom.sweet.chat.api.ChatContext;
import top.mrxiaom.sweet.chat.api.IComponentProcessor;
import top.mrxiaom.sweet.chat.config.styles.ChatStyleByPerm;

import java.io.File;
import java.util.*;

/**
 * 管理玩家消息默认格式的模块
 */
@AutoRegister
public class MessageStyleManager extends AbstractModule {
    private final List<IComponentProcessor> stylePreProcessorRegistry = new ArrayList<>();
    private final List<IComponentProcessor> stylePostProcessorRegistry = new ArrayList<>();
    private final Map<String, ChatStyleByPerm> styleMap = new HashMap<>();
    private final List<ChatStyleByPerm> styleWithPriority = new ArrayList<>();
    public MessageStyleManager(SweetChat plugin) {
        super(plugin);
    }

    @ApiStatus.Internal
    public void registerStylePreProcessor(@NotNull IComponentProcessor processor) {
        stylePreProcessorRegistry.add(processor);
        stylePreProcessorRegistry.sort(Comparator.comparingInt(IComponentProcessor::priority));
    }

    @ApiStatus.Internal
    public void unregisterStylePreProcessor(@NotNull IComponentProcessor processor) {
        stylePreProcessorRegistry.remove(processor);
        stylePreProcessorRegistry.sort(Comparator.comparingInt(IComponentProcessor::priority));
    }

    @ApiStatus.Internal
    public void registerStylePostProcessor(@NotNull IComponentProcessor processor) {
        stylePostProcessorRegistry.add(processor);
        stylePostProcessorRegistry.sort(Comparator.comparingInt(IComponentProcessor::priority));
    }

    @ApiStatus.Internal
    public void unregisterStylePostProcessor(@NotNull IComponentProcessor processor) {
        stylePostProcessorRegistry.remove(processor);
        stylePostProcessorRegistry.sort(Comparator.comparingInt(IComponentProcessor::priority));
    }

    @Override
    public void reloadConfig(MemoryConfiguration pluginConfig) {
        File file = plugin.resolve("./styles.yml");
        if (!file.exists()) {
            plugin.saveResource("styles.yml", file);
        }
        FileConfiguration config = plugin.resolveGotoFlag(ConfigUtils.load(file));
        styleMap.clear();
        styleWithPriority.clear();
        ConfigurationSection section = config.getConfigurationSection("style-by-permission");
        if (section != null) for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s != null) {
                ChatStyleByPerm style = new ChatStyleByPerm(this, key, s);
                styleMap.put(style.id(), style);
                styleWithPriority.add(style);
            }
        }
        styleWithPriority.sort(Comparator.comparingInt(ChatStyleByPerm::priority));
    }

    @Nullable
    public ChatStyleByPerm getStyle(String styleId) {
        return styleMap.get(styleId);
    }

    @Nullable
    public ChatStyleByPerm getStyle(Permissible p) {
        for (ChatStyleByPerm style : styleWithPriority) {
            if (style.hasPermission(p)) {
                return style;
            }
        }
        return null;
    }

    @NotNull
    public Component handleStyle(@NotNull Component input, @NotNull ChatContext ctx) {
        Component component = input.asComponent();
        Player player = ctx.player();
        ChatStyleByPerm style = getStyle(player);
        if (style != null) {
            ctx.tag("style-by-perm", style);
        }
        for (IComponentProcessor processor : stylePreProcessorRegistry) {
            component = processor.process(component, ctx);
        }
        if (style != null) {
            component = style.apply(component, player);
        }
        for (IComponentProcessor processor : stylePostProcessorRegistry) {
            component = processor.process(component, ctx);
        }
        return component;
    }

    public static MessageStyleManager inst() {
        return instanceOf(MessageStyleManager.class);
    }
}
