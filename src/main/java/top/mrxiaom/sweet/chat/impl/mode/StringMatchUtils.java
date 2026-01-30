package top.mrxiaom.sweet.chat.impl.mode;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringMatchUtils {
    @NotNull
    public static Predicate<String> parse(@NotNull String input) {
        if (input.startsWith("*") && input.endsWith("*")) {
            String prefix = input.substring(1, input.length() - 1);
            return str -> str.contains(prefix);
        }
        if (input.startsWith("*")) {
            String suffix = input.substring(1);
            return str -> str.endsWith(suffix);
        }
        if (input.endsWith("*")) {
            String prefix = input.substring(0, input.length() - 1);
            return str -> str.startsWith(prefix);
        }
        if (input.startsWith("$ ")) {
            try {
                Pattern regex = Pattern.compile(input.substring(2));
                return str -> {
                    Matcher matcher = regex.matcher(str);
                    int length = str.length();
                    return matcher.matches() && matcher.start() == 0 && matcher.end() == length;
                };
            } catch (IllegalArgumentException e) {
                return str -> false;
            }
        }
        return str -> str.equals(input);
    }

    public static Predicate<String> parse(@NotNull List<String> input) {
        int size = input.size();
        if (size == 0) return str -> false;
        if (size == 1) return parse(input.get(0));
        List<Predicate<String>> list = new ArrayList<>();
        for (String s : input) {
            list.add(parse(s));
        }
        return str -> {
            for (Predicate<String> p : list) {
                if (p.test(str)) return true;
            }
            return false;
        };
    }
}
