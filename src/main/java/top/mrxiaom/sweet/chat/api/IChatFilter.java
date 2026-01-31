package top.mrxiaom.sweet.chat.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 聊天过滤器接口
 */
public interface IChatFilter {
    /**
     * 匹配过滤器
     * @param ctx 聊天上下文
     * @return 如果匹配，返回已匹配数据实例；如果不匹配，返回 <code>null</code>
     */
    @Nullable Matched match(@NotNull ChatContext ctx);
    interface Matched {
        /**
         * 执行惩罚操作
         * @return 是否阻止聊天消息的发送，如果返回 <code>true</code>，也会阻止剩余聊天过滤器的匹配与惩罚执行
         */
        boolean punish();
    }
}
