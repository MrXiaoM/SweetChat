package top.mrxiaom.sweet.chat.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.List;

public class ComponentUtils {
    public static Component join(List<Component> lines) {
        TextComponent.Builder builder = Component.text();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) builder.appendNewline();
            builder.append(lines.get(i));
        }
        return builder.asComponent();
    }
}
