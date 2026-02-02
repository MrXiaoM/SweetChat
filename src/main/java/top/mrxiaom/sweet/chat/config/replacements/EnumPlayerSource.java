package top.mrxiaom.sweet.chat.config.replacements;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.chat.depend.TABPlayerSource;
import top.mrxiaom.sweet.chat.func.MessageReplacementManager;

import java.util.function.Function;
import java.util.function.Supplier;

public enum EnumPlayerSource {
    SERVER(
            () -> true,
            name -> Util.getOnlinePlayer(name).map(Player::getName).orElse(null)),
    BUNGEE_CORD(
            () -> {
                try {
                    return Bukkit.spigot().getConfig().getBoolean("settings.bungeecord");
                } catch (LinkageError e) {
                    return false;
                }
            },
            name -> MessageReplacementManager.inst().getBungeePlayerName(name)),
    TAB(
            () -> Util.isPresent("me.neznamy.tab.api.TabAPI"),
            TABPlayerSource::getPlayerName),

    ;
    private final Supplier<Boolean> checker;
    private final Function<String, String> impl;
    EnumPlayerSource(Supplier<Boolean> checker, Function<String, String> impl) {
        this.checker = checker;
        this.impl = impl;
    }

    public boolean isSupported() {
        return checker.get();
    }

    @Nullable
    public String correctOnlinePlayerName(@NotNull String playerName) {
        return impl.apply(playerName);
    }
}
