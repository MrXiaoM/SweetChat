package top.mrxiaom.sweet.chat.func;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.actions.ActionProviders;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.ConfigUtils;
import top.mrxiaom.sweet.chat.SweetChat;
import top.mrxiaom.sweet.chat.api.IChatFilter;
import top.mrxiaom.sweet.chat.api.IChatFilterProvider;
import top.mrxiaom.sweet.chat.config.filters.ChatFilter;

import java.io.File;
import java.util.*;

@AutoRegister
public class FilterManager extends AbstractModule implements IChatFilterProvider {
    private final List<IChatFilter> fixedFilterRegistry = new ArrayList<>();
    private final List<IChatFilterProvider> filterProviderRegistry = new ArrayList<>();
    private final Map<String, List<String>> textPools = new HashMap<>();
    private final Map<String, List<IAction>> actionPools = new HashMap<>();
    private final List<IChatFilter> configFilterList = new ArrayList<>();
    public FilterManager(SweetChat plugin) {
        super(plugin);
        registerFilterProvider(this);
    }

    public void registerFixedFilter(IChatFilter filter) {
        fixedFilterRegistry.add(filter);
    }

    public void unregisterFixedFilter(IChatFilter filter) {
        fixedFilterRegistry.remove(filter);
    }

    public void registerFilterProvider(IChatFilterProvider provider) {
        filterProviderRegistry.add(provider);
        filterProviderRegistry.sort(Comparator.comparingInt(IChatFilterProvider::providerPriority));
    }

    public void unregisterFilterProvider(IChatFilterProvider provider) {
        filterProviderRegistry.remove(provider);
        filterProviderRegistry.sort(Comparator.comparingInt(IChatFilterProvider::providerPriority));
    }

    @Override
    public int providerPriority() {
        return 2000;
    }

    @Override
    public @Nullable IChatFilter load(@NotNull ConfigurationSection config) throws RuntimeException {
        return new ChatFilter(textPools, actionPools, config);
    }

    @Override
    public void reloadConfig(MemoryConfiguration pluginConfig) {
        File file = plugin.resolve("./filter.yml");
        if (!file.exists()) {
            plugin.saveResource("filter.yml", file);
        }
        ConfigurationSection section;
        FileConfiguration config = plugin.resolveGotoFlag(ConfigUtils.load(file));

        textPools.clear();
        section = config.getConfigurationSection("text-pools");
        if (section != null) for (String key : section.getKeys(false)) {
            textPools.put(key, section.getStringList(key));
        }
        actionPools.clear();
        section = config.getConfigurationSection("action-pools");
        if (section != null) for (String key : section.getKeys(false)) {
            actionPools.put(key, ActionProviders.loadActions(section, key));
        }
        configFilterList.clear();
        List<ConfigurationSection> sectionList = ConfigUtils.getSectionList(config, "filters");
        for (int i = 0; i < sectionList.size(); i++) {
            IChatFilter filter = null;
            for (IChatFilterProvider provider : filterProviderRegistry) {
                try {
                    filter = provider.load(sectionList.get(i));
                    if (filter != null) break;
                } catch (Exception e) {
                    warn("[filter] 加载 filters[" + i + "] 时出现异常: " + e.getMessage());
                }
            }
            if (filter == null) {
                warn("[filter] 加载 filters[" + i + "] 时，找不到合适的过滤器提供器");
            } else {
                configFilterList.add(filter);
            }
        }
        info("[filter] 从配置中加载了 " + configFilterList.size() + " 个过滤器");
    }

    @NotNull
    public List<IChatFilter> getFilters() {
        List<IChatFilter> filters = new ArrayList<>();
        filters.addAll(fixedFilterRegistry);
        filters.addAll(configFilterList);
        return filters;
    }

    public static FilterManager inst() {
        return instanceOf(FilterManager.class);
    }
}
