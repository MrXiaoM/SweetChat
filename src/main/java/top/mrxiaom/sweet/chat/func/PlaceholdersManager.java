package top.mrxiaom.sweet.chat.func;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.ConfigUtils;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.chat.SweetChat;
import top.mrxiaom.sweet.chat.api.IConditionalPlaceholder;
import top.mrxiaom.sweet.chat.config.placeholders.CPStatements;
import top.mrxiaom.sweet.chat.config.placeholders.CPWhenMatch;

import java.io.File;
import java.util.*;
import java.util.function.Supplier;

@AutoRegister
public class PlaceholdersManager extends AbstractModule {
    private boolean enable;
    private final Map<String, IConditionalPlaceholder> conditionalPlaceholderMap = new HashMap<>();
    public PlaceholdersManager(SweetChat plugin) {
        super(plugin);
    }

    public boolean isEnabled() {
        return enable;
    }

    @Override
    public void reloadConfig(MemoryConfiguration pluginConfig) {
        enable = pluginConfig.getBoolean("modules.placeholders", true);

        if (!enable) return;

        File file = plugin.resolve("./placeholders.yml");
        if (!file.exists()) {
            plugin.saveResource("placeholders.yml", file);
        }
        FileConfiguration config = plugin.resolveGotoFlag(ConfigUtils.load(file));

        reloadConditionalPlaceholders(config);
        info("加载了 " + conditionalPlaceholderMap.size() + " 个条件变量");
    }

    private void reloadConditionalPlaceholders(FileConfiguration config) {
        conditionalPlaceholderMap.clear();
        ConfigurationSection root = config.getConfigurationSection("conditional-placeholders");
        if (root != null) for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) continue;
            if (!section.getBoolean("enable", false)) continue;
            Map<String, String> variables = getMap(section, "variables");
            if (section.contains("when") && section.contains("input")) {
                String input = section.getString("input", "");
                String elseOutput = section.getString("else", "${self}");
                List<CPWhenMatch.Entry> whenMatchList = new ArrayList<>();
                List<ConfigurationSection> whenList = ConfigUtils.getSectionList(section, "when");
                for (ConfigurationSection when : whenList) {
                    String match = when.getString("match");
                    String output = when.getString("output");
                    if (match == null) {
                        warn("[conditional-placeholders] " + key + " 的 when 条件缺少 match");
                        continue;
                    }
                    if (output == null) {
                        warn("[conditional-placeholders] " + key + " 的 when 条件缺少 output");
                        continue;
                    }
                    whenMatchList.add(new CPWhenMatch.Entry(match, output));
                }
                if (whenMatchList.isEmpty()) {
                    warn("[conditional-placeholders] " + key + " 的 when 没有任何有效的条目");
                    continue;
                }
                conditionalPlaceholderMap.put(key, new CPWhenMatch(variables, input, whenMatchList, elseOutput));
                continue;
            }
            if (section.contains("statements")) {
                String defaultOutput = section.getString("default", "");
                List<CPStatements.Entry> statements = new ArrayList<>();
                List<ConfigurationSection> statementsList = ConfigUtils.getSectionList(section, "statements");
                for (ConfigurationSection statement : statementsList) {
                    String eval = statement.getString("eval");
                    String output = statement.getString("output");
                    if (eval == null) {
                        warn("[conditional-placeholders] " + key + " 的 statements 条件缺少 eval");
                        continue;
                    }
                    if (output == null) {
                        warn("[conditional-placeholders] " + key + " 的 statements 条件缺少 output");
                        continue;
                    }
                    statements.add(new CPStatements.Entry(eval, output));
                }
                conditionalPlaceholderMap.put(key, new CPStatements(plugin, variables, statements, defaultOutput));
                continue;
            }
            warn("条件变量 " + key + " 不匹配任何类型的格式");
        }
    }

    @NotNull
    public Map<String, IConditionalPlaceholder> getConditionalPlaceholderMap() {
        return Collections.unmodifiableMap(conditionalPlaceholderMap);
    }

    @Nullable
    public IConditionalPlaceholder getConditionalPlaceholder(@NotNull String name) {
        return conditionalPlaceholderMap.get(name);
    }

    @ApiStatus.Internal
    public void addReplacements(@NotNull Player player, @NotNull List<Pair<String, Object>> r) {
        if (!enable) return;
        for (Map.Entry<String, IConditionalPlaceholder> entry : conditionalPlaceholderMap.entrySet()) {
            String key = "${cond:" + entry.getKey() + "}";
            IConditionalPlaceholder placeholder = entry.getValue();
            Supplier<String> value = () -> placeholder.get(player);
            r.add(Pair.of(key, value));
        }
    }

    @NotNull
    private static Map<String, String> getMap(ConfigurationSection config, String key) {
        Map<String, String> map = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection(key);
        if (section != null) for (String s : section.getKeys(false)) {
            map.put(s, String.valueOf(section.get(s)));
        }
        return map;
    }

    public static PlaceholdersManager inst() {
        return instanceOf(PlaceholdersManager.class);
    }
}
