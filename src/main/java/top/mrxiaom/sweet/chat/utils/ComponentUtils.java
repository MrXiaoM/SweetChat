package top.mrxiaom.sweet.chat.utils;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import top.mrxiaom.sweet.chat.depend.PacketEventsSupport;
import top.mrxiaom.sweet.chat.func.AbstractPluginHolder;

import java.util.List;

public class ComponentUtils {
    private static BukkitAudiences adventure;
    private static boolean supportPacketEvents;
    public static void init(JavaPlugin plugin) {
        adventure = BukkitAudiences.builder(plugin).build();
    }

    public static void afterEnable() {
        supportPacketEvents = AbstractPluginHolder.get(PacketEventsSupport.class).isPresent();
    }

    public static void send(CommandSender sender, Component component) {
        if (sender instanceof Audience) {
            ((Audience) sender).sendMessage(component);
            return;
        }
        if (supportPacketEvents && sender instanceof Player) {
            PacketEventsSupport.inst().send((Player) sender, component);
            return;
        }
        adventure.sender(sender).sendMessage(component);
    }

    /**
     * 将多行 Component 合并为一个 Component
     */
    public static Component join(List<Component> lines) {
        TextComponent.Builder builder = Component.text();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) builder.appendNewline();
            builder.append(lines.get(i));
        }
        return builder.asComponent();
    }
}
