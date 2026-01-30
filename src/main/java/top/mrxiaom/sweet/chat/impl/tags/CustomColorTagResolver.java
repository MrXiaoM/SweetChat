package top.mrxiaom.sweet.chat.impl.tags;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.internal.serializer.SerializableResolver;
import net.kyori.adventure.text.minimessage.internal.serializer.StyleClaim;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.util.Index;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"UnstableApiUsage"})
public class CustomColorTagResolver implements TagResolver, SerializableResolver.Single {
    private static final String COLOR_3 = "c";
    private static final String COLOR_2 = "colour";
    private static final String COLOR = "color";

    private final Map<String, TextColor> COLOR_ALIASES = new HashMap<>();
    private final Index<String, NamedTextColor> NAMES;
    private final StyleClaim<TextColor> STYLE;
    public CustomColorTagResolver(List<NamedTextColor> colors) {
        NAMES = Index.create(NamedTextColor::toString, colors);
        if (colors.contains(NamedTextColor.DARK_GRAY)) COLOR_ALIASES.put("dark_grey", NamedTextColor.DARK_GRAY);
        if (colors.contains(NamedTextColor.GRAY)) COLOR_ALIASES.put("grey", NamedTextColor.GRAY);
        STYLE = StyleClaim.claim(COLOR, Style::color, (color, emitter) -> {
            if (color instanceof NamedTextColor) {
                String name = NAMES.key((NamedTextColor) color);
                if (name != null) emitter.tag(name);
            }
        });
    }

    private static boolean isColorOrAbbreviation(final String name) {
        return name.equals(COLOR) || name.equals(COLOR_2) || name.equals(COLOR_3);
    }

    @Override
    public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue args, @NotNull Context ctx) throws ParsingException {
        if (!this.has(name)) {
            return null;
        }

        String colorName;
        if (isColorOrAbbreviation(name)) {
            colorName = args.popOr("Expected to find a color parameter: <name>").lowerValue();
        } else {
            colorName = name;
        }

        TextColor color = resolveColor(colorName, ctx);
        return Tag.styling(color);
    }

    private @Nullable TextColor resolveColorOrNull(String colorName) {
        TextColor color;
        if (COLOR_ALIASES.containsKey(colorName)) {
            color = COLOR_ALIASES.get(colorName);
        } else {
            color = NAMES.value(colorName);
        }

        return color;
    }

    private @NotNull TextColor resolveColor(@NotNull String colorName, @NotNull Context ctx) throws ParsingException {
        TextColor color = resolveColorOrNull(colorName);
        if (color == null) {
            throw ctx.newException(String.format("Unable to parse a color from '%s'. Please use named colors.", colorName));
        }
        return color;
    }

    @Override
    public boolean has(@NotNull String name) {
        return isColorOrAbbreviation(name)
                || NAMES.value(name) != null
                || COLOR_ALIASES.containsKey(name);
    }

    @Override
    public @Nullable StyleClaim<?> claimStyle() {
        return STYLE;
    }
}
