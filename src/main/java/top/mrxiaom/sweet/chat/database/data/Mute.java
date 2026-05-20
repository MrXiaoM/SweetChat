package top.mrxiaom.sweet.chat.database.data;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.sweet.chat.api.EnumMuteMode;
import top.mrxiaom.sweet.chat.database.MuteDatabase;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Mute {
    private final MuteDatabase database;
    private final UUID player;
    private @NotNull EnumMuteMode mode = EnumMuteMode.NOT_MUTED;
    private @Nullable LocalDateTime endTime;

    public Mute(MuteDatabase database, UUID player) {
        this.database = database;
        this.player = player;
    }

    public boolean isMuted() {
        switch (mode) {
            case MUTED_TIMED:
                if (endTime != null && LocalDateTime.now().isBefore(endTime)) {
                    return true;
                } else {
                    setNotMuted().submit();
                    return false;
                }
            case MUTED_INFINITE:
                return true;
            default:
                break;
        }
        return false;
    }

    @ApiStatus.Internal
    public void update(@NotNull EnumMuteMode mode, @Nullable LocalDateTime endTime) {
        this.mode = mode;
        this.endTime = endTime;
    }

    public MuteDatabase database() {
        return database;
    }

    public UUID player() {
        return player;
    }

    public @NotNull EnumMuteMode mode() {
        return mode;
    }

    public Mute mode(@NotNull EnumMuteMode mode) {
        this.mode = mode;
        return this;
    }

    public @Nullable LocalDateTime endTime() {
        return endTime;
    }

    public Mute endTime(@Nullable LocalDateTime endTime) {
        this.endTime = endTime;
        return this;
    }

    public Mute setNotMuted() {
        this.mode = EnumMuteMode.NOT_MUTED;
        this.endTime = null;
        return this;
    }

    public Mute setTimedMuted(@NotNull LocalDateTime endTime) {
        Objects.requireNonNull(endTime, "endTime");
        this.mode = EnumMuteMode.MUTED_TIMED;
        this.endTime = endTime;
        return this;
    }

    public Mute setInfiniteMuted() {
        this.mode = EnumMuteMode.MUTED_INFINITE;
        this.endTime = null;
        return this;
    }

    public void submit() {
        try (Connection conn = database.plugin.getConnection()) {
            submit(conn);
        } catch (SQLException e) {
            database.warn(e);
        }
    }

    public void submit(Connection conn) throws SQLException {
        database.setMuteImpl(conn, player, mode.value(), endTime);
    }
}
