package top.mrxiaom.sweet.chat.func;

import com.google.common.io.ByteArrayDataOutput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.AdventureUtil;
import top.mrxiaom.pluginbase.utils.Bytes;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.chat.SweetChat;
import top.mrxiaom.sweet.chat.config.CrossServerMode;
import top.mrxiaom.sweet.chat.database.MessageDatabase;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

@AutoRegister
public class BroadcastManager extends AbstractModule {
    private CrossServerMode mode;
    private String group = "default";
    private static final GsonComponentSerializer serializer = GsonComponentSerializer.gson();
    public BroadcastManager(SweetChat plugin) {
        super(plugin);
        registerBungee();
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        mode = Util.valueOr(CrossServerMode.class, config.getString("cross-server.mode"), CrossServerMode.NONE);
        group = config.getString("cross-server.group", "default");
    }

    @Override
    public void receiveBungee(String subChannel, DataInputStream in) throws IOException {
        if (subChannel.equals("SweetChat")) {
            long outdate = in.readLong();
            if (System.currentTimeMillis() > outdate) return;
            String packetType = in.readUTF();
            if (packetType.equals("broadcast")) {
                String group = in.readUTF();
                if (this.group.equals(group)) {
                    Component component = serializer.deserialize(in.readUTF());
                    AdventureUtil.sendMessage(Bukkit.getConsoleSender(), component);
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        AdventureUtil.sendMessage(p, component);
                    }
                }
            }
        }
    }

    public void broadcast(Player player, Component message) {
        String senderName = player.getName();
        switch (mode) {
            case BUNGEE_CORD: {
                byte[] packet = build(out -> {
                    out.writeLong(System.currentTimeMillis() + 10000L);
                    out.writeUTF("broadcast");
                    out.writeUTF(group);
                    out.writeUTF(serializer.serialize(message));
                });
                player.sendPluginMessage(plugin, "BungeeCord", buildBungee(packet, "Forward", "ALL", "SweetChat"));
                break;
            }
            case DATABASE: {
                plugin.getScheduler().runTaskAsync(() -> {
                    MessageDatabase db = plugin.getMessageDatabase();
                    db.insert(senderName, message);
                });
                break;
            }
            default:
                break;
        }
    }

    private static byte @Nullable [] build(Bytes.DataConsumer data) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             DataOutputStream msg = new DataOutputStream(output)) {
            data.accept(msg);
            return output.toByteArray();
        } catch (IOException e) {
            BukkitPlugin.getInstance().warn("构建 BungeeCord 插件消息时出现一个异常", e);
        }
        return null;
    }

    @SuppressWarnings("SameParameterValue")
    private static byte[] buildBungee(byte[] data, String subChannel, String... arguments) {
        ByteArrayDataOutput out = Bytes.newDataOutput();
        out.writeUTF(subChannel);
        for (String argument : arguments) {
            out.writeUTF(argument);
        }
        if (data != null) {
            out.write(data.length);
            out.write(data);
        }
        return out.toByteArray();
    }

    public static BroadcastManager inst() {
        return instanceOf(BroadcastManager.class);
    }
}
