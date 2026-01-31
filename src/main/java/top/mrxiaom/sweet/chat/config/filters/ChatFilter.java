package top.mrxiaom.sweet.chat.config.filters;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.actions.ActionProviders;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.sweet.chat.SweetChat;
import top.mrxiaom.sweet.chat.api.ChatContext;
import top.mrxiaom.sweet.chat.api.IChatFilter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 聊天过滤器配置，在 {@link top.mrxiaom.sweet.chat.func.FilterManager} 中加载。
 */
public class ChatFilter implements IChatFilter {
    private final List<String> containsList;
    private final List<Pattern> regexList;
    private final MatcherProvider punishment;
    @ApiStatus.Internal
    public ChatFilter(
            @NotNull Map<String, List<String>> textPools,
            @NotNull Map<String, List<IAction>> actionPools,
            @NotNull ConfigurationSection config
    ) throws RuntimeException {
        this.containsList = config.getStringList("contains");
        this.regexList = new ArrayList<>();
        for (String regex : config.getStringList("regex")) {
            try {
                Pattern compiled = Pattern.compile(regex);
                this.regexList.add(compiled);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("正则表达式编译失败: " + regex);
            }
        }
        String punishment = config.getString("punishment");
        if (punishment == null) {
            throw new IllegalArgumentException("未配置 punishment");
        }
        this.punishment = parsePunishment(textPools, actionPools, punishment.split(" ", 2));
    }

    @NotNull
    public static MatcherProvider parsePunishment(
            @NotNull Map<String, List<String>> textPools,
            @NotNull Map<String, List<IAction>> actionPools,
            @NotNull String[] split
    ) throws RuntimeException {
        String type = split[0];
        if (split.length == 1) {
            if ("DISALLOW".equalsIgnoreCase(type)) {
                return Disallow::new;
            }
        } else {
            String value = split[1];
            if ("REPLACE".equalsIgnoreCase(type)) {
                List<String> texts = textPools.get(value);
                if (texts == null) {
                    throw new IllegalArgumentException("找不到文本池 " + value);
                }
                return (ctx, matchedText) -> new Replace(ctx, matchedText, texts);
            }
            if ("REPLACE_RAW".equalsIgnoreCase(type)) {
                List<String> texts = Collections.singletonList(value);
                return (ctx, matchedText) -> new Replace(ctx, matchedText, texts);
            }
            if ("REPLACE_CHAR".equalsIgnoreCase(type)) {
                List<String> texts = textPools.get(value);
                if (texts == null) {
                    throw new IllegalArgumentException("找不到文本池 " + value);
                }
                return (ctx, matchedText) -> new ReplaceChar(ctx, matchedText, texts);
            }
            if ("REPLACE_CHAR_RAW".equalsIgnoreCase(type)) {
                List<String> texts = Collections.singletonList(value);
                return (ctx, matchedText) -> new ReplaceChar(ctx, matchedText, texts);
            }
            if ("REPLACE_MESSAGE".equalsIgnoreCase(type)) {
                List<String> texts = textPools.get(value);
                if (texts == null) {
                    throw new IllegalArgumentException("找不到文本池 " + value);
                }
                return (ctx, matchedText) -> new ReplaceMessage(ctx, matchedText, texts);
            }
            if ("REPLACE_MESSAGE_RAW".equalsIgnoreCase(type)) {
                List<String> texts = Collections.singletonList(value);
                return (ctx, matchedText) -> new ReplaceMessage(ctx, matchedText, texts);
            }
            if ("ACTION".equalsIgnoreCase(type)) {
                List<IAction> actions = actionPools.get(value);
                if (actions == null) {
                    throw new IllegalArgumentException("找不到操作池 " + value);
                }
                return (ctx, matchedText) -> new Action(ctx, matchedText, actions);
            }
        }
        throw new IllegalArgumentException("无效的惩罚配置");
    }

    @Override
    public @Nullable Matched match(@NotNull ChatContext ctx) {
        String text = ctx.text();
        for (String s : containsList) {
            if (text.contains(s)) {
                return punishment.create(ctx, s);
            }
        }
        for (Pattern pattern : regexList) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.matches()) {
                String matched = text.substring(matcher.start(), matcher.end());
                return punishment.create(ctx, matched);
            }
        }
        return null;
    }

    public interface MatcherProvider {
        Matched create(ChatContext ctx, String matchedText);
    }

    @SuppressWarnings("SameParameterValue")
    private static <T> T random(List<T> list, T def) {
        if (list.isEmpty()) return def;
        if (list.size() == 1) return list.get(0);
        return list.get(new Random().nextInt(list.size()));
    }

    public static abstract class Matched implements IChatFilter.Matched {
        protected final ChatContext ctx;
        protected final String matchedText;
        private Matched(ChatContext ctx, String matchedText) {
            this.ctx = ctx;
            this.matchedText = matchedText;
        }
    }

    public static class Replace extends Matched {
        private final List<String> replacement;
        private Replace(ChatContext ctx, String matchedText, List<String> replacement) {
            super(ctx, matchedText);
            this.replacement = replacement;
        }

        @Override
        public boolean punish() {
            String text = ctx.text();
            String replacement = random(this.replacement, "");
            ctx.text(text.replace(matchedText, replacement));
            return false;
        }
    }

    public static class ReplaceChar extends Matched {
        private final List<String> replacement;
        private ReplaceChar(ChatContext ctx, String matchedText, List<String> replacement) {
            super(ctx, matchedText);
            this.replacement = replacement;
        }

        @Override
        public boolean punish() {
            String text = ctx.text();
            String replacement = random(this.replacement, "");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < matchedText.length(); i++) {
                sb.append(replacement);
            }
            ctx.text(text.replace(matchedText, sb.toString()));
            return false;
        }
    }

    public static class ReplaceMessage extends Matched {
        private final List<String> replacement;
        private ReplaceMessage(ChatContext ctx, String matchedText, List<String> replacement) {
            super(ctx, matchedText);
            this.replacement = replacement;
        }

        @Override
        public boolean punish() {
            String replacement = random(this.replacement, "");
            ctx.text(replacement);
            return false;
        }
    }

    public static class Action extends Matched {
        private final List<IAction> actions;
        private Action(ChatContext ctx, String matchedText, List<IAction> actions) {
            super(ctx, matchedText);
            this.actions = actions;
        }

        @Override
        public boolean punish() {
            SweetChat plugin = ctx.plugin();
            Player player = ctx.player();
            plugin.getScheduler().runTask(() -> ActionProviders.run(plugin, player, actions));
            return true;
        }
    }

    public static class Disallow extends Matched {
        private Disallow(ChatContext ctx, String matchedText) {
            super(ctx, matchedText);
        }

        @Override
        public boolean punish() {
            return true;
        }
    }
}
