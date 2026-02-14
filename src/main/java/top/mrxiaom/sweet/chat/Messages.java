package top.mrxiaom.sweet.chat;

import top.mrxiaom.pluginbase.func.language.Language;
import top.mrxiaom.pluginbase.func.language.Message;

import static top.mrxiaom.pluginbase.func.language.LanguageFieldAutoHolder.field;

@Language(prefix="messages.")
public class Messages {

    public static final Message player__not_online = field("玩家 %player% 不在线 (或不存在)");
    public static final Message chat_exception = field("&e聊天消息发送失败，日志已输出到控制台，请联系服务器管理员");

    @Language(prefix="messages.commands.")
    public static class Commands {
        public static final Message reload__config = field("&a配置文件已重载");
        public static final Message reload__database = field("&a已重载并重新连接数据库");
    }
}
