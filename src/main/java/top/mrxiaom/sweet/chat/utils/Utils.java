package top.mrxiaom.sweet.chat.utils;

import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.data.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Utils {

    @Nullable
    public static LocalDateTime parseDateTime(String s) {
        String[] split = s.split(" ", 2);
        LocalDate localDate = Duration.parseLocalDateOrNull(split[0]);
        if (localDate == null) {
            LocalTime localTime = Duration.parseLocalTimeOrNull(split[0]);
            if (localTime != null) {
                return LocalDate.now().atTime(localTime);
            }
            return null;
        }
        LocalTime localTime = Duration.parseLocalTimeOrNull(split.length > 1 ? split[1] : null);
        if (localTime != null) {
            return localDate.atTime(localTime);
        } else {
            return localDate.atTime(0, 0, 0);
        }
    }

}
