package top.mrxiaom.sweet.chat.depend;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.chat.ChatTypes;
import com.github.retrooper.packetevents.protocol.chat.message.ChatMessageLegacy;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import io.github.retrooper.packetevents.PacketEventsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.AdventureUtil;
import top.mrxiaom.sweet.chat.SweetChat;
import top.mrxiaom.sweet.chat.func.AbstractPluginHolder;

import java.lang.reflect.Method;

@AutoRegister(requirePlugins = "packetevents")
public class PacketEventsSupport extends AbstractPluginHolder {
    private final Method fromJson, sendMessage;
    public PacketEventsSupport(SweetChat plugin) throws Exception {
        super(plugin);
        Plugin packetevents = Bukkit.getPluginManager().getPlugin("packetevents");
        if (packetevents instanceof PacketEventsPlugin) {
            ClassLoader classLoader = packetevents.getClass().getClassLoader();
            Class<?> typeComponent = Class.forName("net.kyori.adventure.text.Component", true, classLoader);
            this.fromJson = AdventureSerializer.class.getDeclaredMethod("fromJson", String.class);
            this.sendMessage = User.class.getDeclaredMethod("sendMessage", typeComponent);
            register();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public void send(Player player, Component message) {
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user.getPacketVersion().isOlderThan(ClientVersion.V_1_13)) {
            ChatMessageLegacy chatMessage = new ChatMessageLegacy(message, ChatTypes.CHAT);
            user.sendPacket(new PacketChatMessage(chatMessage));
            return;
        }
        try {
            user.sendMessage(message);
        } catch (LinkageError error) {
            try {
                String json = GsonComponentSerializer.gson().serialize(message);
                Object converted = fromJson.invoke(AdventureSerializer.serializer(), json);
                sendMessage.invoke(user, converted);
            } catch (ReflectiveOperationException t) {
                warn(t);
                AdventureUtil.sendMessage(player, message);
            }
        }
    }

    public static PacketEventsSupport inst() {
        return instanceOf(PacketEventsSupport.class);
    }
}
