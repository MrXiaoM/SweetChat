package top.mrxiaom.sweet.chat.actions;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.api.IAction;
import top.mrxiaom.pluginbase.api.IActionProvider;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.depend.PAPI;

import java.util.List;

import static top.mrxiaom.pluginbase.func.AbstractPluginHolder.t;

public class ActionMessageOp implements IAction {
    public static final IActionProvider PROVIDER = input -> {
        if (input instanceof ConfigurationSection) {
            ConfigurationSection section = (ConfigurationSection) input;
            if (!section.contains("type") && section.contains("message_op")) {
                String content = section.getString("message_op");
                if (content != null) {
                    return new ActionMessageOp(content);
                }
            } else if ("message_op".equals(section.getString("type"))) {
                String content = section.getString("content");
                if (content != null) {
                    return new ActionMessageOp(content);
                }
            }
        } else {
            String s = String.valueOf(input);
            if (s.startsWith("[message_op]")) {
                return new ActionMessageOp(s.substring(12));
            }
            if (s.startsWith("message_op:")) {
                return new ActionMessageOp(s.substring(11));
            }
        }
        return null;
    };
    public final String message;
    public ActionMessageOp(String message) {
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
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isOp()) t(p, msg);
        }
    }
}
