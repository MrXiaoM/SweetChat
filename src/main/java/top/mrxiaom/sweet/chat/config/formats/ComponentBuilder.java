package top.mrxiaom.sweet.chat.config.formats;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.AdventureUtil;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.chat.utils.ComponentUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ComponentBuilder {
    private final @NotNull String content;
    private final @NotNull List<String> hoverText;
    private final @Nullable ClickEvent.Action clickAction;
    private final @NotNull String clickValue;
    public ComponentBuilder(ConfigurationSection config) {
        if (config == null) {
            throw new IllegalArgumentException("未配置格式");
        }
        String content = config.getString("content");
        if (content == null) {
            throw new IllegalArgumentException("找不到 content");
        }
        this.content = content;
        this.hoverText = config.getStringList("hover-text");
        this.clickAction = Util.valueOr(ClickEvent.Action.class, config.getString("click.action"), null);
        this.clickValue = config.getString("click.value", "");
    }

    public ComponentBuilder(@NotNull String content) {
        this.content = content;
        this.hoverText = new ArrayList<>();
        this.clickAction = null;
        this.clickValue = "";
    }

    @Override
    public String toString() {
        return "ComponentBuilder{" +
                "content='" + content + '\'' +
                ", hoverText=" + (hoverText.isEmpty() ? "[]" : ("['" + String.join("', '", hoverText) + "']")) +
                ", clickAction=" + clickAction +
                ", clickValue='" + clickValue + '\'' +
                '}';
    }

    public Component build(Function<String, String> replacer) {
        if (content.isEmpty()) {
            return Component.empty();
        }
        Component content = AdventureUtil.miniMessage(replacer.apply(this.content));
        if (!hoverText.isEmpty()) {
            List<String> lines = new ArrayList<>();
            for (String line : hoverText) {
                lines.add(replacer.apply(line));
            }
            List<Component> hoverText = AdventureUtil.miniMessage(lines);
            content = content.hoverEvent(ComponentUtils.join(hoverText));
        }
        if (clickAction != null) {
            String value = replacer.apply(clickValue);
            // noinspection deprecation
            content = content.clickEvent(ClickEvent.clickEvent(clickAction, value));
        }
        return content;
    }
}
