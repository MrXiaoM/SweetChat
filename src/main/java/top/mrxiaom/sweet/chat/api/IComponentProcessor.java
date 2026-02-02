package top.mrxiaom.sweet.chat.api;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * 聊天消息文本组件处理器
 */
public interface IComponentProcessor {
    /**
     * 匹配优先级，数值越小越先匹配
     */
    default int priority() {
        return 1000;
    }

    /**
     * 处理聊天消息文本组件的实现
     * @param component 传入的文本组件
     * @param ctx 聊天上下文
     * @return 修改后的文本组件
     */
    @NotNull Component process(@NotNull Component component, @NotNull ChatContext ctx);
}
