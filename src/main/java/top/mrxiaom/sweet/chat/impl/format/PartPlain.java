package top.mrxiaom.sweet.chat.impl.format;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.sweet.chat.api.ChatContext;
import top.mrxiaom.sweet.chat.api.IFormatPart;
import top.mrxiaom.sweet.chat.config.formats.ComponentBuilder;

/**
 * 文本类型的消息部分
 */
public class PartPlain implements IFormatPart {
    private final ComponentBuilder builder;
    @ApiStatus.Internal
    public PartPlain(ConfigurationSection config) {
        this.builder = new ComponentBuilder(config);
    }

    @Override
    public @NotNull Component get(@NotNull ChatContext ctx) {
        return builder.build(ctx::setPlaceholders);
    }
}
