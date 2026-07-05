package top.mrxiaom.sweet.chat.depend;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Modules.Chat.ChatBubbleManager;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.MemoryConfiguration;
import top.mrxiaom.pluginbase.func.AutoRegister;
import top.mrxiaom.pluginbase.utils.AdventureUtil;
import top.mrxiaom.pluginbase.utils.Pair;
import top.mrxiaom.pluginbase.utils.Util;
import top.mrxiaom.sweet.chat.SweetChat;
import top.mrxiaom.sweet.chat.api.ChatPostContext;
import top.mrxiaom.sweet.chat.api.IFormatPart;
import top.mrxiaom.sweet.chat.api.IPostChatAction;
import top.mrxiaom.sweet.chat.func.AbstractPluginHolder;
import top.mrxiaom.sweet.chat.func.ChatListener;
import top.mrxiaom.sweet.chat.impl.format.PartPlayerMessage;

import java.util.HashSet;

@AutoRegister(requirePlugins = "CMI")
public class CMIChatBubble extends AbstractPluginHolder implements IPostChatAction {
    public CMIChatBubble(SweetChat plugin) {
        super(plugin);
        if (Util.isPresent("com.Zrips.CMI.Modules.Chat.ChatBubbleManager")) {
            register();
        }
    }

    @Override
    public void reloadConfig(MemoryConfiguration config) {
        if (config.getBoolean("interaction.CMI.chat-bubble", false)) {
            ChatListener.inst().postChatRegistry().register(this);
        } else {
            ChatListener.inst().postChatRegistry().unregister(this);
        }
    }

    @Override
    public void execute(ChatPostContext ctx) {
        Component playerMessage = null;
        for (Pair<IFormatPart, Component> part : ctx.parts()) {
            if (part.key() instanceof PartPlayerMessage) {
                playerMessage = part.value();
            }
        }
        if (playerMessage == null) return;
        ChatBubbleManager manager = CMI.getInstance().getChatBubbleManager();
        String text = AdventureUtil.legacySection(playerMessage);
        manager.showBubble(ctx.player(), text, new HashSet<>(ctx.playersToReceiver()));
    }
}
