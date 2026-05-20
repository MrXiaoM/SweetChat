package top.mrxiaom.sweet.chat;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.depend.PlaceholdersExpansion;
import top.mrxiaom.sweet.chat.api.IConditionalPlaceholder;
import top.mrxiaom.sweet.chat.database.data.Mute;
import top.mrxiaom.sweet.chat.func.ChatListener;
import top.mrxiaom.sweet.chat.func.PlaceholdersManager;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class Placeholders extends PlaceholdersExpansion<SweetChat> {
    public Placeholders(SweetChat plugin) {
        super(plugin);
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (params.equals("mode")) {
            String mode = plugin.getModeDatabase().getMode(player.getUniqueId());
            if (mode.isEmpty()) {
                return ChatListener.inst().getChatModeDefault().modeName();
            }
            return mode;
        }
        if (params.startsWith("condition_")) {
            String name = params.substring(10);
            IConditionalPlaceholder placeholder = PlaceholdersManager.inst().getConditionalPlaceholder(name);
            if (placeholder != null) {
                return placeholder.get(player);
            } else {
                return name;
            }
        }
        if (params.equals("is_muted")) {
            Mute mute = plugin.getMuteDatabase().getMute(player.getUniqueId());
            return bool(mute.isMuted());
        }
        if (params.equals("mute_end_time")) {
            Mute mute = plugin.getMuteDatabase().getMute(player.getUniqueId());
            LocalDateTime endTime = mute.endTime();
            if (endTime == null) return "";
            // TODO: 返回结束时间
        }
        if (params.equals("mute_remain_duration")) {
            Mute mute = plugin.getMuteDatabase().getMute(player.getUniqueId());
            LocalDateTime endTime = mute.endTime();
            if (endTime == null) return "";
            // TODO: 返回剩余时间
        }
        if (params.equals("mute_status")) {
            Mute mute = plugin.getMuteDatabase().getMute(player.getUniqueId());
            LocalDateTime endTime = mute.endTime();
            switch (mute.mode()) {
                case MUTED_TIMED:
                    LocalDateTime now = LocalDateTime.now();
                    if (endTime != null && now.isBefore(endTime)) {
                        long seconds = endTime.toEpochSecond(ZoneOffset.UTC) - now.toEpochSecond(ZoneOffset.UTC);
                        // TODO: 返回剩余禁言时间
                        return "";
                    } else {
                        mute.setNotMuted().submit();
                    }
                    break;
                case MUTED_INFINITE:
                    // TODO: 返回永久禁言提示
                    return "";
            }
            // TODO: 返回未禁言
        }
        return super.onPlaceholderRequest(player, params);
    }
}
