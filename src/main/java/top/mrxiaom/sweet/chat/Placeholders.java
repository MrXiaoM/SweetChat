package top.mrxiaom.sweet.chat;

import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.data.Duration;
import top.mrxiaom.pluginbase.utils.depend.PlaceholdersExpansion;
import top.mrxiaom.sweet.chat.api.IConditionalPlaceholder;
import top.mrxiaom.sweet.chat.database.MuteDatabase;
import top.mrxiaom.sweet.chat.database.data.Mute;
import top.mrxiaom.sweet.chat.func.AbstractPluginHolder;
import top.mrxiaom.sweet.chat.func.ChatListener;
import top.mrxiaom.sweet.chat.func.PlaceholdersManager;

import java.time.LocalDateTime;

public class Placeholders extends PlaceholdersExpansion<SweetChat> {
    private String muteTimed, muteInfinite, muteNot;
    public Placeholders(SweetChat plugin) {
        super(plugin);
        new ReloadListener(plugin);
    }

    public class ReloadListener extends AbstractPluginHolder {
        private ReloadListener(SweetChat plugin) {
            super(plugin, true);
        }

        @Override
        public void reloadConfig(MemoryConfiguration config) {
            muteTimed = config.getString("mute.placeholders.muted-timed", "禁言中 %duration%");
            muteInfinite = config.getString("mute.placeholders.muted-timed", "永久禁言中");
            muteNot = config.getString("mute.placeholders.not-muted", "未禁言");
        }
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
            MuteDatabase database = plugin.getMuteDatabase();
            Mute mute = database.getMute(player.getUniqueId());
            LocalDateTime endTime = mute.endTime();
            if (endTime == null) return "";
            return database.formatEndTime(endTime);
        }
        if (params.equals("mute_remain_duration")) {
            MuteDatabase database = plugin.getMuteDatabase();
            Mute mute = database.getMute(player.getUniqueId());
            LocalDateTime endTime = mute.endTime();
            LocalDateTime now = LocalDateTime.now();
            if (endTime == null || now.isAfter(endTime)) return "";
            Duration duration = Duration.between(now, endTime);
            return database.formatDuration(duration);
        }
        if (params.equals("mute_status")) {
            MuteDatabase database = plugin.getMuteDatabase();
            Mute mute = database.getMute(player.getUniqueId());
            LocalDateTime endTime = mute.endTime();
            switch (mute.mode()) {
                case MUTED_TIMED:
                    LocalDateTime now = LocalDateTime.now();
                    if (endTime != null && now.isBefore(endTime)) {
                        Duration duration = Duration.between(now, endTime);
                        return muteTimed.replace("%duration%", database.formatDuration(duration));
                    } else {
                        mute.setNotMuted().submit();
                    }
                    break;
                case MUTED_INFINITE:
                    return muteInfinite;
            }
            return muteNot;
        }
        return super.onPlaceholderRequest(player, params);
    }
}
