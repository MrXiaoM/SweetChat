package top.mrxiaom.sweet.chat.depend;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import org.jetbrains.annotations.Nullable;

public class TABPlayerSource {
    @Nullable
    public static String getPlayerName(String name) {
        TabAPI api = TabAPI.getInstance();
        TabPlayer player = api.getPlayer(name);
        if (player != null) {
            return player.getName();
        }
        return null;
    }
}
