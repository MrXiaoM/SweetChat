package top.mrxiaom.sweet.chat.config.placeholders;

import com.ezylang.evalex.Expression;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.utils.ListPair;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.depend.PAPI;
import top.mrxiaom.sweet.chat.SweetChat;
import top.mrxiaom.sweet.chat.api.IConditionalPlaceholder;

import java.util.List;
import java.util.Map;

public class CPStatements implements IConditionalPlaceholder {
    private final SweetChat plugin;
    private final Map<String, String> variables;
    private final List<Entry> statementsList;
    private final String defaultOutput;
    public CPStatements(SweetChat plugin, Map<String, String> variables, List<Entry> statementsList, String defaultOutput) {
        this.plugin = plugin;
        this.variables = variables;
        this.statementsList = statementsList;
        this.defaultOutput = defaultOutput;
    }

    @Override
    public @NotNull String get(@NotNull Player player) {
        ListPair<String, Object> r = new ListPair<>();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String key = "${var." + entry.getKey() + "}";
            String value = PAPI.setPlaceholders(player, entry.getValue());
            r.add(key, value);
        }
        for (Entry statement : statementsList) {
            try {
                String expression = PAPI.setPlaceholders(player, Pair.replace(statement.eval(), r));
                if (new Expression(expression).evaluate().getBooleanValue() == Boolean.TRUE) {
                    return PAPI.setPlaceholders(player, Pair.replace(statement.output(), r));
                }
            } catch (Exception e) {
                plugin.warn("表达式 " + statement.eval() + " 解析错误", e);
            }
        }
        return PAPI.setPlaceholders(player, Pair.replace(defaultOutput, r));
    }

    public static class Entry {
        private final String eval;
        private final String output;

        public Entry(String eval, String output) {
            this.eval = eval;
            this.output = output;
        }

        public String eval() {
            return eval;
        }

        public String output() {
            return output;
        }
    }
}
