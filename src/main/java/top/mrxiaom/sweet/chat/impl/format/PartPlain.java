package top.mrxiaom.sweet.chat.impl.format;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.AdventureUtil;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.chat.api.ChatContext;
import top.mrxiaom.sweet.chat.api.IFormatPart;
import top.mrxiaom.sweet.chat.utils.ComponentUtils;

import java.util.List;

public class PartPlain implements IFormatPart {
    private final @NotNull String content;
    private final @NotNull List<String> hoverText;
    private final @Nullable ClickEvent.Action clickAction;
    private final @NotNull String clickValue;
    public PartPlain(ConfigurationSection config) {
        this.content = config.getString("content", "");
        this.hoverText = config.getStringList("hover-text");
        this.clickAction = Util.valueOr(ClickEvent.Action.class, config.getString("click.action"), null);
        this.clickValue = config.getString("click.value", "");
    }

    @Override
    public @NotNull Component get(@NotNull ChatContext ctx) {
        if (content.isEmpty()) {
            return Component.empty();
        }
        Component content = AdventureUtil.miniMessage(ctx.setPlaceholders(this.content));
        if (!hoverText.isEmpty()) {
            List<Component> hoverText = AdventureUtil.miniMessage(ctx.setPlaceholders(this.hoverText));
            content = content.hoverEvent(ComponentUtils.join(hoverText));
        }
        if (clickAction != null) {
            String value = ctx.setPlaceholders(clickValue);
            // noinspection deprecation
            content = content.clickEvent(ClickEvent.clickEvent(clickAction, value));
        }
        return content;
    }
}
