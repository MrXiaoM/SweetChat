package top.mrxiaom.sweet.chat.api;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.sweet.chat.SweetChat;

import java.util.List;

public class ChatPostContext {
    private final ChatContext chatContext;
    private final List<Player> playersToReceiver;
    private final List<Pair<IFormatPart, Component>> parts;
    private final Component finalMessage;

    public ChatPostContext(ChatContext chatContext, List<Player> playersToReceiver, List<Pair<IFormatPart, Component>> parts, Component finalMessage) {
        this.chatContext = chatContext;
        this.playersToReceiver = playersToReceiver;
        this.parts = parts;
        this.finalMessage = finalMessage;
    }

    public ChatContext chatContext() {
        return chatContext;
    }

    public SweetChat plugin() {
        return chatContext.plugin();
    }

    public Player player() {
        return chatContext.player();
    }

    public List<Player> playersToReceiver() {
        return playersToReceiver;
    }

    public List<Pair<IFormatPart, Component>> parts() {
        return parts;
    }

    public Component finalMessage() {
        return finalMessage;
    }
}
