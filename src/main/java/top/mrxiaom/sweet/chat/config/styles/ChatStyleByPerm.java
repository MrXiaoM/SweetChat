package top.mrxiaom.sweet.chat.config.styles;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import org.bukkit.block.SculkSensor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.AdventureUtil;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.pluginbase.utils.depend.PAPI;
import top.mrxiaom.sweet.chat.func.MessageStyleManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 按权限指定聊天消息默认样式配置，在 {@link MessageStyleManager} 中加载。
 */
public class ChatStyleByPerm {
    private final String id;
    private final int priority;
    private final @Nullable TextColor color;
    private final List<TextDecoration> decoration;
    private final String custom;
    @ApiStatus.Internal
    public ChatStyleByPerm(MessageStyleManager parent, String id, ConfigurationSection config) {
        this.id = id;
        this.priority = config.getInt("priority", 1000);
        this.custom = config.getString("custom", "");
        String colorText = config.getString("color", "");
        if (!colorText.isEmpty()) {
            NamedTextColor color = NamedTextColor.NAMES.value(colorText);
            if (color != null) {
                this.color = color;
            } else {
                this.color = TextColor.fromCSSHexString(colorText);
            }
        } else {
            color = null;
        }
        this.decoration = new ArrayList<>();
        for (String str : config.getStringList("decoration")) {
            TextDecoration decoration = Util.valueOr(TextDecoration.class, str, null);
            if (decoration != null) {
                this.decoration.add(decoration);
            }
        }
    }

    public String id() {
        return id;
    }

    public int priority() {
        return priority;
    }

    public boolean hasPermission(Permissible p) {
        return p.hasPermission("sweetchat.style." + id);
    }

    public Component apply(Component input) {
        return apply(input, null);
    }

    public Component apply(Component input, Player player) {
        if (this.custom.isEmpty()) {
            Component component = input.asComponent();
            if (this.color != null) {
                component = component.color(this.color);
            }
            for (TextDecoration decoration : this.decoration) {
                component = component.decoration(decoration, TextDecoration.State.TRUE);
            }
            return component;
        } else {
            MiniMessage mm = AdventureUtil.builder()
                    .editTags(tags -> tags.tag("message-content", Tag.selfClosingInserting(input)))
                    .build();
            String custom = player == null ? this.custom : PAPI.setPlaceholders(player, this.custom);
            return AdventureUtil.miniMessage(mm, custom + "<message-content/>");
        }
    }
}
