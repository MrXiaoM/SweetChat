package top.mrxiaom.sweet.chat.config.placeholders;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.utils.ListPair;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.depend.PAPI;
import top.mrxiaom.sweet.chat.api.IConditionalPlaceholder;

import java.util.List;
import java.util.Map;

public class CPWhenMatch implements IConditionalPlaceholder {
    private final Map<String, String> variables;
    private final String input;
    private final List<Entry> whenMatchList;
    private final String elseOutput;

    public CPWhenMatch(Map<String, String> variables, String input, List<Entry> whenMatchList, String elseOutput) {
        this.variables = variables;
        this.input = input;
        this.whenMatchList = whenMatchList;
        this.elseOutput = elseOutput;
    }

    @Override
    public @NotNull String get(@NotNull Player player) {
        ListPair<String, Object> r = new ListPair<>();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String key = "${var." + entry.getKey() + "}";
            String value = PAPI.setPlaceholders(player, entry.getValue());
            r.add(key, value);
        }
        r.add("${self}", this.input);
        String input = PAPI.setPlaceholders(player, this.input);
        for (Entry when : whenMatchList) {
            String match = PAPI.setPlaceholders(player, Pair.replace(when.match(), r));
            if (input.equals(match)) {
                return PAPI.setPlaceholders(player, Pair.replace(when.output(), r));
            }
        }
        return PAPI.setPlaceholders(player, Pair.replace(elseOutput, r));
    }

    public static class Entry {
        private final String match;
        private final String output;

        public Entry(String match, String output) {
            this.match = match;
            this.output = output;
        }

        public String match() {
            return match;
        }

        public String output() {
            return output;
        }
    }
}
