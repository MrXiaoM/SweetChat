package top.mrxiaom.sweet.chat;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.utils.depend.PlaceholdersExpansion;
import top.mrxiaom.sweet.chat.func.ChatListener;

public class Placeholders extends PlaceholdersExpansion<SweetChat> {
    public Placeholders(SweetChat plugin) {
        super(plugin);
    }

    @Override
    public boolean persist() {
        return true;
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
        return super.onPlaceholderRequest(player, params);
    }
}
