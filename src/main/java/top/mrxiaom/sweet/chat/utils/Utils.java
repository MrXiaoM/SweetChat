package top.mrxiaom.sweet.chat.utils;

import io.netty.util.internal.UnstableApi;
import top.mrxiaom.pluginbase.data.Duration;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class Utils {
    @UnstableApi
    public static Duration between(LocalDateTime startTime, LocalDateTime endTime) {
        long totalSeconds = endTime.toEpochSecond(ZoneOffset.UTC) - startTime.toEpochSecond(ZoneOffset.UTC);
        int days = (int)((totalSeconds / 86400));
        int hours = (int)((totalSeconds / 3600) % 24);
        int minutes = (int)((totalSeconds / 60) % 60);
        int seconds = (int)(totalSeconds % 60);
        return new Duration(days, hours, minutes, seconds);
    }
}
