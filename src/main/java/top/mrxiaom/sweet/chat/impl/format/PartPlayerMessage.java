package top.mrxiaom.sweet.chat.impl.format;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.utils.AdventureUtil;
import top.mrxiaom.sweet.chat.api.ChatContext;
import top.mrxiaom.sweet.chat.api.IFormatPart;
import top.mrxiaom.sweet.chat.config.ChatStyleByPerm;
import top.mrxiaom.sweet.chat.func.MessageReplacementManager;
import top.mrxiaom.sweet.chat.func.MessageStyleManager;
import top.mrxiaom.sweet.chat.func.MiniMessageTagsManager;

/**
 * 玩家发送的消息部分
 */
public class PartPlayerMessage implements IFormatPart {
    @ApiStatus.Internal
    public PartPlayerMessage(ConfigurationSection config) {
    }

    @Override
    public @NotNull Component get(@NotNull ChatContext ctx) {
        Player player = ctx.player();
        MiniMessage.Builder builder = MiniMessageTagsManager.inst().builder(player);
        String text = ctx.text();

        if (player.hasPermission("sweetchat.format.papi")) {
            text = ctx.setPlaceholders(text);
        }

        // 解析替换特定格式为 MiniMessage 标签，例如物品展示
        MessageReplacementManager replacer = MessageReplacementManager.inst();
        text = replacer.handleItemDisplay(player, text, builder);
        text = replacer.handlePlaceholders(player, text, builder);

        ChatStyleByPerm style = MessageStyleManager.inst().getStyle(player);
        Component component = AdventureUtil.miniMessage(builder.build(), text);
        if (style != null) {
            return style.apply(component);
        } else {
            return component;
        }
    }
}
