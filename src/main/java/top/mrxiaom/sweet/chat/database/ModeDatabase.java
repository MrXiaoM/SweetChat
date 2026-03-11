package top.mrxiaom.sweet.chat.database;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import top.mrxiaom.pluginbase.database.IDatabase;
import top.mrxiaom.sweet.chat.SweetChat;
import top.mrxiaom.sweet.chat.func.AbstractPluginHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ModeDatabase extends AbstractPluginHolder implements IDatabase, Listener {
    private String TABLE_NAME;
    private final Map<UUID, String> modeMap = new HashMap<>();
    public ModeDatabase(SweetChat plugin) {
        super(plugin, true);
        registerEvents();
    }

    @Override
    public void reload(Connection conn, String tablePrefix) throws SQLException {
        TABLE_NAME = tablePrefix + "mode";
        try (PreparedStatement ps = conn.prepareStatement(
                "CREATE TABLE if NOT EXISTS `" + TABLE_NAME + "`(" +
                        "`uuid` VARCHAR(64) PRIMARY KEY," +
                        "`mode` INT" +
                ");"
        )) {
            ps.execute();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        getModeImpl(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        modeMap.remove(e.getPlayer().getUniqueId());
    }

    @NotNull
    public String getMode(UUID player) {
        String exists = modeMap.get(player);
        if (exists != null) {
            return exists;
        }
        return getModeImpl(player);
    }

    public void setMode(UUID player, @NotNull String mode) {
        modeMap.put(player, mode);
        setModeImpl(player, mode);
    }

    private String getModeImpl(UUID player) {
        try (Connection conn = plugin.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM `" + TABLE_NAME + "` WHERE `uuid`=?;"
            )) {
            ps.setString(1, player.toString());
            try (ResultSet result = ps.executeQuery()) {
                if (result.next()) {
                    String mode = result.getString("mode");
                    modeMap.put(player, mode);
                    return mode;
                }
            }
        } catch (SQLException e) {
            warn(e);
        }
        return "";
    }

    private void setModeImpl(UUID player, String mode) {
        String sentence;
        if (plugin.options.database().isSQLite()) {
            sentence = "INSERT OR REPLACE INTO `" + TABLE_NAME + "`(`uuid`,`mode`) VALUES(?,?);";
        } else {
            sentence = "INSERT INTO `" + TABLE_NAME + "`(`uuid`,`mode`) VALUES(?,?)" +
                    "  ON DUPLICATE KEY UPDATE `mode`=VALUES(`mode`);";
        }
        try (Connection conn = plugin.getConnection();
            PreparedStatement ps = conn.prepareStatement(sentence)) {
            ps.setString(1, player.toString());
            ps.setString(2, mode);
            ps.execute();
        } catch (SQLException e) {
            warn(e);
        }
    }
}
