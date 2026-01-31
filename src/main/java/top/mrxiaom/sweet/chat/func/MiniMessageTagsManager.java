package top.mrxiaom.sweet.chat.func;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.permissions.Permissible;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.AdventureUtil;
import top.mrxiaom.sweet.chat.SweetChat;
import top.mrxiaom.sweet.chat.impl.tags.CustomColorTagResolver;
import top.mrxiaom.sweet.chat.impl.tags.HexColorTagResolver;

import java.util.ArrayList;
import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.standard.StandardTags.*;

/**
 * 管理消息中 MiniMessage 标签解析的模块
 */
@AutoRegister
public class MiniMessageTagsManager extends AbstractModule {
    public MiniMessageTagsManager(SweetChat plugin) {
        super(plugin);
    }

    public MiniMessage.Builder builder(Permissible p) {
        TagResolver.Builder builder = TagResolver.builder();
        builder.resolver(reset());
        if (has(p, "color.all")) {
            builder.resolver(color());
        } else {
            List<NamedTextColor> enabledColors = new ArrayList<>();
            if (has(p, "color.named")) {
                enabledColors.addAll(NamedTextColor.NAMES.values());
            } else {
                for (NamedTextColor color : NamedTextColor.NAMES.values()) {
                    String name = color.toString().replace("_", "-").toLowerCase();
                    if (has(p, "color." + name)) {
                        enabledColors.add(color);
                    }
                }
            }
            if (!enabledColors.isEmpty()) {
                try {
                    builder.resolver(new CustomColorTagResolver(enabledColors));
                } catch (LinkageError ignored) {}
            }
            if (has(p, "color.hex")) {
                try {
                    builder.resolver(HexColorTagResolver.INSTANCE);
                } catch (LinkageError ignored) {}
            }
        }
        if (has(p, "color.shadow")) {
            try {
                builder.resolver(shadowColor());
            } catch (LinkageError ignored) {}
        }
        if (has(p, "decoration.all")) {
            builder.resolver(decorations());
        } else {
            if (has(p, "decoration.bold")) builder.resolver(decorations(TextDecoration.BOLD));
            if (has(p, "decoration.italic")) builder.resolver(decorations(TextDecoration.ITALIC));
            if (has(p, "decoration.magic")) builder.resolver(decorations(TextDecoration.OBFUSCATED));
            if (has(p, "decoration.delete")) builder.resolver(decorations(TextDecoration.STRIKETHROUGH));
            if (has(p, "decoration.underline")) builder.resolver(decorations(TextDecoration.UNDERLINED));
        }
        if (has(p, "hover")) builder.resolver(hoverEvent());
        if (has(p, "click")) builder.resolver(clickEvent());
        if (has(p, "gradient")) builder.resolver(gradient());
        if (has(p, "insertion")) builder.resolver(insertion());
        if (has(p, "translatable")) {
            builder.resolver(translatable());
            try {
                builder.resolver(translatableFallback());
            } catch (LinkageError ignored) {}
        }
        if (has(p, "font")) builder.resolver(font());
        return MiniMessage.builder()
                .tags(builder.build())
                .preProcessor(AdventureUtil::legacyToMiniMessage)
                .postProcessor(it -> it.decoration(TextDecoration.ITALIC, false));
    }

    private static boolean has(Permissible p, String... anyPerms) {
        for (String perm : anyPerms) {
            if (p.hasPermission("sweetchat.format." + perm)) {
                return true;
            }
        }
        return false;
    }

    public static MiniMessageTagsManager inst() {
        return instanceOf(MiniMessageTagsManager.class);
    }
}
