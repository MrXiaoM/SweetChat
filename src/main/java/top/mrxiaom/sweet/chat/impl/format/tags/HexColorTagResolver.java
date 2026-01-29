package top.mrxiaom.sweet.chat.impl.format.tags;

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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"UnstableApiUsage"})
public class HexColorTagResolver implements TagResolver, SerializableResolver.Single {
    private static final String COLOR_3 = "c";
    private static final String COLOR_2 = "colour";
    private static final String COLOR = "color";

    public static final TagResolver INSTANCE = new HexColorTagResolver();
    private static final StyleClaim<TextColor> STYLE = StyleClaim.claim(COLOR, Style::color, (color, emitter) -> {
        if (!(color instanceof NamedTextColor) && color != null) {
            emitter.tag(color.asHexString());
        }
    });
    private HexColorTagResolver() {}

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
        if (colorName.charAt(0) == TextColor.HEX_CHARACTER) {
            color = TextColor.fromHexString(colorName);
        } else {
            color = null;
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
                || TextColor.fromHexString(name) != null;
    }

    @Override
    public @Nullable StyleClaim<?> claimStyle() {
        return STYLE;
    }
}
