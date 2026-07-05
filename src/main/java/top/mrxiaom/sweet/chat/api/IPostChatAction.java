package top.mrxiaom.sweet.chat.api;

import top.mrxiaom.pluginbase.api.WithPriority;

public interface IPostChatAction extends WithPriority {
    void execute(ChatPostContext ctx);
}
