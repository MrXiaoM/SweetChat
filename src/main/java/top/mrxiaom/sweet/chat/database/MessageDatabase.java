package top.mrxiaom.sweet.chat.database;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.MemoryConfiguration;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IRunTask;
import top.mrxiaom.pluginbase.database.IDatabase;
import top.mrxiaom.sweet.chat.SweetChat;
import top.mrxiaom.sweet.chat.func.AbstractPluginHolder;
import top.mrxiaom.sweet.chat.func.ChatListener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 消息广播轮询数据库
 */
public class MessageDatabase extends AbstractPluginHolder implements IDatabase {
    private static final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final GsonComponentSerializer serializer = GsonComponentSerializer.gson();
    private final Set<String> sentMessages = new HashSet<>();
    private String TABLE_NAME;
    private int lastSequence;
    private IRunTask pollTask;
    private String group = "default";
    public MessageDatabase(SweetChat plugin) {
        super(plugin, true);
    }

    @Override
    public void onDisable() {
        if (pollTask != null) {
            pollTask.cancel();
            pollTask = null;
        }
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        group = config.getString("cross-server.group", "default");
    }

    @Override
    public void reload(Connection conn, String tablePrefix) throws SQLException {
        TABLE_NAME = tablePrefix + "broadcast";
        if (pollTask != null) {
            pollTask.cancel();
            pollTask = null;
        }
        if (plugin.options.database().isMySQL()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "CREATE TABLE if NOT EXISTS `" + TABLE_NAME + "`(" +
                            "`sequence` INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                            "`group` VARCHAR(64)," +
                            "`message_id` VARCHAR(64)," +
                            "`sender_name` VARCHAR(64)," +
                            "`send_time` DATETIME," +
                            "`message` LONGTEXT" +
                            ");"
            )) {
                ps.execute();
            }
            Integer lastSequence = getLastSequence(conn);
            if (lastSequence != null) {
                this.lastSequence = lastSequence;
            }
            pollTask = plugin.getScheduler().runTaskTimerAsync(this::poll, 10L, 10L);
        }
    }

    @Nullable
    private Integer getLastSequence(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT sequence FROM `" + TABLE_NAME + "` ORDER BY sequence DESC LIMIT 1;"
        )) {
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
        return null;
    }

    private void handleMessage(String message) {
        Component component = serializer.deserialize(message);
        ChatListener.inst().broadcast(Bukkit.getOnlinePlayers(), component);
    }

    public void insert(String senderName, Component message) {
        insert(senderName, serializer.serialize(message));
    }

    public void insert(String senderName, String message) {
        String uuid = UUID.randomUUID().toString();
        sentMessages.add(uuid);
        String sendTime = LocalDateTime.now().format(timeFormat);
        try (Connection conn = plugin.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO `" + TABLE_NAME + "` " +
                             "(`group`,`message_id`,`sender_name`,`send_time`,`message`) " +
                             "VALUES (?,?,?,'" + sendTime + "',?);"
             )) {
            ps.setString(1, group);
            ps.setString(2, uuid);
            ps.setString(3, senderName);
            ps.setString(4, message);
            ps.execute();
        } catch (SQLException e) {
            warn(e);
        }
    }

    public void poll() {
        List<String> list = new ArrayList<>();
        try (Connection conn = plugin.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM `" + TABLE_NAME + "` WHERE `group`=? AND `sequence`>?"
             )) {
            ps.setString(1, group);
            ps.setInt(2, this.lastSequence);
            try (ResultSet result = ps.executeQuery()) {
                while (result.next()) {
                    String uuid = result.getString("message_id");
                    if (sentMessages.contains(uuid)) continue;
                    String message = result.getString("message");
                    list.add(message);
                }
            }
            Integer lastSequence = getLastSequence(conn);
            if (lastSequence != null) {
                this.lastSequence = lastSequence;
            }
        } catch (SQLException e) {
            warn(e);
        }
        for (String message : list) {
            handleMessage(message);
        }
    }
}
