package top.mrxiaom.sweet.chat.api;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface IFormatPartProvider {
    @NotNull
    IFormatPart load(@NotNull ConfigurationSection config) throws Exception;
}
