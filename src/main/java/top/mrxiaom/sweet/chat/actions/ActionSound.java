package top.mrxiaom.sweet.chat.actions;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.api.IActionProvider;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;

import java.util.List;

public class ActionSound implements IAction {
    public static final IActionProvider PROVIDER = s -> {
        if (s.startsWith("[sound]")) {
            return parse(s.substring(7));
        }
        if (s.startsWith("sound:")) {
            return parse(s.substring(6));
        }
        return null;
    };
    private static IAction parse(String input) {
        String[] split = input.split(",", 3);
        String soundId = split[0].trim();
        float volume = split.length > 1 ? Util.parseFloat(split[1].trim()).orElse(1.0f) : 1.0f;
        float pitch = split.length > 2 ? Util.parseFloat(split[2].trim()).orElse(1.0f) : 1.0f;
        return new ActionSound(soundId, volume, pitch);
    }

    public final String soundId;
    public final float volume;
    public final float pitch;
    public ActionSound(String soundId, float volume, float pitch) {
        this.soundId = soundId;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public void run(@Nullable Player player, @Nullable List<Pair<String, Object>> replacements) {
        if (player != null) {
            player.playSound(player.getLocation(), soundId, volume, pitch);
        }
    }
}
