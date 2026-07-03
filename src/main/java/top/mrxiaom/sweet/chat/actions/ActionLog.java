package top.mrxiaom.sweet.chat.actions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.api.IActionProvider;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.depend.PAPI;
import top.mrxiaom.sweet.chat.SweetChat;

import java.util.List;

public class ActionLog implements IAction {
    public static final IActionProvider PROVIDER = input -> {
        if (input instanceof ConfigurationSection) {
            ConfigurationSection section = (ConfigurationSection) input;
            if (!section.contains("type") && section.contains("log")) {
                String content = section.getString("log");
                if (content != null) {
                    return new ActionLog(content);
                }
            } else if ("log".equals(section.getString("type"))) {
                String content = section.getString("content");
                if (content != null) {
                    return new ActionLog(content);
                }
            }
        } else {
            String s = String.valueOf(input);
            if (s.startsWith("[log]")) {
                return new ActionLog(s.substring(5));
            }
            if (s.startsWith("log:")) {
                return new ActionLog(s.substring(4));
            }
        }
        return null;
    };
    public final SweetChat plugin = SweetChat.getInstance();
    public final String message;
    public ActionLog(String message) {
        this.message = message;
    }

    @Override
    public void run(@Nullable Player player, @Nullable List<Pair<String, Object>> replacements) {
        String msg;
        if (player != null) {
            msg = Pair.replace(PAPI.setPlaceholders(player, message), replacements);
        } else {
            msg = Pair.replace(message, replacements);
        }
        plugin.info(msg);
    }
}
