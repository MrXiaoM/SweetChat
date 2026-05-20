package top.mrxiaom.sweet.chat.database;

import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.data.Duration;
import top.mrxiaom.pluginbase.data.DurationDisplayFormat;
import top.mrxiaom.pluginbase.database.IDatabase;
import top.mrxiaom.pluginbase.utils.CollectionUtils;
import top.mrxiaom.sweet.chat.SweetChat;
import top.mrxiaom.sweet.chat.api.EnumMuteMode;
import top.mrxiaom.sweet.chat.database.data.Mute;
import top.mrxiaom.sweet.chat.func.AbstractPluginHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MuteDatabase extends AbstractPluginHolder implements IDatabase, Listener {
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private String TABLE_NAME;
    private final Map<UUID, Mute> cacheMap = new HashMap<>();
    private DurationDisplayFormat durationDisplayFormat;
    private DateTimeFormatter endTimeFormat;
    public MuteDatabase(SweetChat plugin) {
        super(plugin, true);
        registerEvents();
    }

    @Override
    public void reload(Connection conn, String tablePrefix) throws SQLException {
        TABLE_NAME = tablePrefix + "player_mute";
        try (PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE if NOT EXISTS `" + TABLE_NAME + "`(" +
                        "`uuid` VARCHAR(64) PRIMARY KEY," +
                        "`mode` INT," +
                        "`end_time` DATETIME" +
                ");"
        )) {
            ps.execute();
        }
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        this.durationDisplayFormat = DurationDisplayFormat.load(config.getConfigurationSection("mute.time-format.duration"));
        try {
            this.endTimeFormat = DateTimeFormatter.ofPattern(config.getString("mute.time-format.end-time", "yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            this.endTimeFormat = DateTimeFormatter.ISO_DATE_TIME;
        }
    }

    public String formatEndTime(LocalDateTime endTime) {
        return endTime.format(endTimeFormat);
    }

    public String formatDuration(Duration duration) {
        return durationDisplayFormat.get(duration);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        try (Connection conn = plugin.getConnection()) {
            getMuteImpl(conn, e.getPlayer().getUniqueId());
        } catch (SQLException ex) {
            warn(ex);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        removeCache(e.getPlayer().getUniqueId());
    }

    public void removeCache(UUID player) {
        cacheMap.remove(player);
    }

    @NotNull
    public Mute getMute(@NotNull UUID player) {
        Mute exists = cacheMap.get(player);
        if (exists != null) {
            return exists;
        }
        return getMuteImpl(player);
    }

    @NotNull
    public Mute getMuteImpl(@NotNull UUID player) {
        try (Connection conn = plugin.getConnection()) {
            Mute mute = getMuteImpl(conn, player);
            if (mute != null) {
                return mute;
            }
        } catch (SQLException e) {
            warn(e);
        }
        return CollectionUtils.getOrPut(cacheMap, player, this::create);
    }

    @Nullable
    public Mute getMuteImpl(@NotNull Connection conn, @NotNull UUID player) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM `" + TABLE_NAME + "` WHERE `uuid`=?;"
            )) {
            ps.setString(1, player.toString());
            try (ResultSet result = ps.executeQuery()) {
                if (result.next()) {
                    EnumMuteMode mode = EnumMuteMode.valueOf(result.getInt("mode"), EnumMuteMode.NOT_MUTED);
                    String endTimeStr = result.getString("end_time");
                    LocalDateTime endTime = endTimeStr != null
                    ?LocalDateTime.parse(endTimeStr, DATETIME_FORMAT)
                            :null;

                    Mute mute = CollectionUtils.getOrPut(cacheMap, player, this::create);
                    mute.update(mode, endTime);
                    return mute;
                }
            }
        }
        return null;
    }

    public void setMuteImpl(@NotNull Connection conn, @NotNull UUID player, int muteValue, @Nullable LocalDateTime endTime) throws SQLException {
        String endTimeValue = endTime != null
                ? ("'" + endTime.format(DATETIME_FORMAT) + "'")
                : "NULL";
        String sentence;
        if (plugin.options.database().isSQLite()) {
            sentence = "INSERT OR REPLACE INTO `" + TABLE_NAME + "`(`uuid`,`mode`,`end_time`) VALUES(?,?," + endTimeValue + ");";
        } else {
            sentence = "INSERT INTO `" + TABLE_NAME + "`(`uuid`,`mode`,`end_time`) VALUES(?,?," + endTimeValue + ")" +
                    "  ON DUPLICATE KEY UPDATE `mode`=VALUES(`mode`), `end_time`=VALUES(`end_time`);";
        }
        try (PreparedStatement ps = conn.prepareStatement(sentence)) {
            ps.setString(1, player.toString());
            ps.setInt(2, muteValue);
            ps.execute();
        }
    }

    private Mute create(UUID player) {
        return new Mute(this, player);
    }
}
