package top.mrxiaom.sweet.chat;

import top.mrxiaom.pluginbase.func.language.Language;
import top.mrxiaom.pluginbase.func.language.Message;

import static top.mrxiaom.pluginbase.func.language.LanguageFieldAutoHolder.field;

@Language(prefix="messages.")
public class Messages {

    public static final Message player__not_online = field("&e玩家 %player% 不在线 (或不存在)");
    public static final Message player__only = field("该命令只能由玩家执行");
    public static final Message chat_exception = field("&e聊天消息发送失败，日志已输出到控制台，请联系服务器管理员");

    @Language(prefix="messages.commands.")
    public static class Commands {
        public static final Message no_permission = field("&c你没有执行该命令的权限");

        public static final Message mode__not_found = field("&e找不到指定的聊天模式 &7(或无权限)");
        public static final Message mode__set = field("&a已设置默认聊天模式为 &e%mode%");
        public static final Message mode__clear = field("&a已清除默认聊天模式设置");

        public static final Message reload__config = field("&a配置文件已重载");
        public static final Message reload__database = field("&a已重载并重新连接数据库");
    }
}
