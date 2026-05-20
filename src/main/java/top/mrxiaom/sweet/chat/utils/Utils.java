package top.mrxiaom.sweet.chat.utils;

import io.netty.util.internal.UnstableApi;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.data.Duration;
import top.mrxiaom.pluginbase.utils.Util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    @Nullable
    public static LocalDate parseLocalDateOrNull(@Nullable String str) {
        if (str == null) return null;
        String[] dateSplit = str.split("-", 3);
        if (dateSplit.length != 3) return null;
        Integer year = Util.parseInt(dateSplit[0]).orElse(null);
        Integer month = Util.parseInt(dateSplit[1]).orElse(null);
        Integer date = Util.parseInt(dateSplit[2]).orElse(null);
        if (year == null || month == null || date == null) return null;
        return LocalDate.of(year, month, date);
    }

    @Nullable
    public static LocalTime parseLocalTimeOrNull(@Nullable String str) {
        if (str == null) return null;
        String[] split = str.split(":", 3);
        Integer hour = split.length > 0 ? Util.parseInt(split[0]).orElse(null) : Integer.valueOf(0);
        Integer minute = split.length > 1 ? Util.parseInt(split[1]).orElse(null) : Integer.valueOf(0);
        Integer second = split.length > 2 ? Util.parseInt(split[2]).orElse(null) : Integer.valueOf(0);
        if (hour == null || minute == null || second == null) return null;
        return LocalTime.of(hour, minute, second);
    }

    @Nullable
    public static LocalDateTime parseDateTime(String s) {
        String[] split = s.split(" ", 2);
        LocalDate localDate = parseLocalDateOrNull(split[0]);
        if (localDate == null) {
            LocalTime localTime = parseLocalTimeOrNull(split[0]);
            if (localTime != null) {
                return LocalDate.now().atTime(localTime);
            }
            return null;
        }
        LocalTime localTime = parseLocalTimeOrNull(split.length > 1 ? split[1] : null);
        if (localTime != null) {
            return localDate.atTime(localTime);
        } else {
            return localDate.atTime(0, 0, 0);
        }
    }

}
