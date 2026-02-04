package top.mrxiaom.sweet.chat.depend;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.chat.ChatTypes;
import com.github.retrooper.packetevents.protocol.chat.message.ChatMessageLegacy;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.AdventureUtil;
import top.mrxiaom.sweet.chat.SweetChat;
import top.mrxiaom.sweet.chat.func.AbstractModule;

@AutoRegister(requirePlugins = "packetevents")
public class PacketEventsSupport extends AbstractModule {
    public PacketEventsSupport(SweetChat plugin) {
        super(plugin);
    }

    public void send(Player player, Component message) {
        try {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
            if (user.getPacketVersion().isOlderThan(ClientVersion.V_1_13)) {
                ChatMessageLegacy chatMessage = new ChatMessageLegacy(message, ChatTypes.CHAT);
                user.sendPacket(new PacketChatMessage(chatMessage));
            } else {
                user.sendMessage(message);
            }
        } catch (LinkageError e) {
            AdventureUtil.sendMessage(player, message);
        }
    }

    public static PacketEventsSupport inst() {
        return instanceOf(PacketEventsSupport.class);
    }
}
