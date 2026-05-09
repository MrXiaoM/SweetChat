package top.mrxiaom.sweet.chat.api;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface IConditionalPlaceholder {
    @NotNull String get(@NotNull Player player);
}
