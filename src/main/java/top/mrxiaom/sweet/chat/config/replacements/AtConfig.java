package top.mrxiaom.sweet.chat.config.replacements;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import top.mrxiaom.pluginbase.actions.ActionProviders;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.utils.ListPair;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.chat.SweetChat;
import top.mrxiaom.sweet.chat.config.formats.ComponentBuilder;
import top.mrxiaom.sweet.chat.func.MessageReplacementManager;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AtConfig {
    private final boolean enable;
    private final Pattern regex;
    private final String playerNameMatch;
    private final int playerNameMinLength;
    private final int playerNameMaxLength;
    private final EnumPlayerSource playerSource;
    private final ComponentBuilder format;
    private final List<IAction> targetActions;

    @ApiStatus.Internal
    public AtConfig(MessageReplacementManager parent, ConfigurationSection config) {
        this.enable = config.getBoolean("enable", false);
        String regexStr = config.getString("regex");
        if (regexStr == null) {
            throw new IllegalArgumentException("未输入 regex");
        }
        try {
            this.regex = Pattern.compile(regexStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("输入的 regex 正则表达式无效");
        }
        this.playerNameMatch = config.getString("player-name.match", "$1");
        this.playerNameMinLength = config.getInt("player-name.min-length", 1);
        this.playerNameMaxLength = config.getInt("player-name.max-length", 24);
        EnumPlayerSource playerSource = Util.valueOr(EnumPlayerSource.class, config.getString("player-source"), EnumPlayerSource.SERVER);
        if (playerSource.isSupported()) {
            this.playerSource = playerSource;
        } else {
            this.playerSource = EnumPlayerSource.SERVER;
            parent.warn("[replacements] at 玩家来源 " + playerSource.name() + " 不可用，已使用缺省值 SERVER 代替");
        }
        this.format = new ComponentBuilder(config.getConfigurationSection("format"));
        this.targetActions = ActionProviders.loadActions(config, "target-actions");
    }

    public boolean isEnable() {
        return enable;
    }

    public List<IAction> getTargetActions() {
        return targetActions;
    }

    public EnumPlayerSource getPlayerSource() {
        return playerSource;
    }

    @SuppressWarnings("PatternValidation")
    public String handle(Player player, String inputText, MiniMessage.Builder builder) {
        Matcher matcher = regex.matcher(inputText);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0, atIndex = 0;
        // 遍历所有匹配的项
        while (matcher.find()) {
            sb.append(inputText, lastEnd, matcher.start());
            MatchResult match = matcher.toMatchResult();

            String replacement;
            // 按配置提取玩家名
            ListPair<String, Object> regexReplacement = new ListPair<>();
            for (int i = 0; i <= match.groupCount(); i++) {
                regexReplacement.add("$" + i, match.group(i));
            }
            String playerName = Pair.replace(playerNameMatch, regexReplacement);
            if (playerName.length() < playerNameMinLength || playerName.length() > playerNameMaxLength) {
                // 玩家名长度不匹配，不进行替换
                replacement = match.group();
            } else {
                String name = playerSource.correctOnlinePlayerName(playerName);
                if (name != null) {
                    // 目标玩家在线，打包数据，生成标签
                    String tagName = "sweet-chat-at-" + (atIndex++);
                    replacement = "<" + tagName + "/>";
                    String insertion = "SweetChat:" + player.getName() + "@" + name;
                    Component component = format.build(str -> str.replace("%at_target%", name)).insertion(insertion);
                    builder.editTags(tags -> tags.tag(tagName, Tag.selfClosingInserting(component)));
                } else {
                    // 目标玩家不在线，不进行替换
                    replacement = match.group();
                }
            }

            sb.append(replacement);
            lastEnd = matcher.end();
        }

        sb.append(inputText, lastEnd, inputText.length());
        return sb.toString();
    }
}
