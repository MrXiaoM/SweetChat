package top.mrxiaom.sweet.chat.depend;

import com.github.retrooper.packetevents.protocol.chat.message.ChatMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NBTType;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.function.Consumer;

public class PacketChatMessage extends WrapperPlayServerChatMessage {
    public PacketChatMessage(ChatMessage message) {
        super(message);
    }

    public void writeComponentAsJSON(@NonNull Component component) {
        JsonElement element = this.getSerializers().asJsonTree(component);
        fetchAll(element, "hoverEvent", obj -> {
            JsonElement elAction = obj.get("action");
            JsonElement elValue = obj.get("value");
            if (elAction == null || !elAction.isJsonPrimitive()) return;
            if (elValue == null || !elValue.isJsonPrimitive()) return;
            String action = elAction.getAsString();
            String value = elValue.getAsString();
            if (action.equalsIgnoreCase("show_item")) {
                try {
                    ReadWriteNBT nbt = NBT.parseNBT(value);
                    ReadWriteNBT tag = nbt.getCompound("tag");
                    if (tag == null) return;
                    if (tag.hasTag("Damage", NBTType.NBTTagShort)) {
                        tag.removeKey("Damage");
                        nbt.setShort("Damage", tag.getShort("Damage"));
                        obj.addProperty("value", nbt.toString());
                    }
                } catch (Throwable ignored) {}
            }
        });
        String jsonString = element.toString();
        this.writeString(jsonString, this.getMaxMessageLength());
    }

    private static void fetchAll(JsonElement element, String targetKey, Consumer<JsonObject> consumer) {
        if (element == null) return;
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                if (child.isJsonObject()) {
                    fetchAll(child, targetKey, consumer);
                }
            }
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                String key = entry.getKey();
                if (entry.getValue().isJsonObject()) {
                    JsonObject child = entry.getValue().getAsJsonObject();
                    if (key.equalsIgnoreCase(targetKey)) {
                        consumer.accept(child);
                    } else {
                        fetchAll(child, targetKey, consumer);
                    }
                }
                if (entry.getValue().isJsonArray()) {
                    fetchAll(entry.getValue(), targetKey, consumer);
                }
            }
        }
    }

}
