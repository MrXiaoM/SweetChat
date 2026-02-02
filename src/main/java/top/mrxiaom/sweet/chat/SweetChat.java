package top.mrxiaom.sweet.chat;

import top.mrxiaom.pluginbase.BukkitPlugin;
import top.mrxiaom.pluginbase.actions.ActionProviders;
import top.mrxiaom.pluginbase.func.LanguageManager;
import top.mrxiaom.pluginbase.paper.PaperFactory;
import top.mrxiaom.pluginbase.utils.inventory.InventoryFactory;
import top.mrxiaom.pluginbase.utils.item.ItemEditor;
import top.mrxiaom.pluginbase.utils.scheduler.FoliaLibScheduler;
import top.mrxiaom.pluginbase.utils.ClassLoaderWrapper;
import top.mrxiaom.pluginbase.utils.ConfigUtils;
import top.mrxiaom.pluginbase.resolver.DefaultLibraryResolver;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import top.mrxiaom.sweet.chat.actions.ActionSound;
import top.mrxiaom.sweet.chat.api.*;
import top.mrxiaom.sweet.chat.database.MessageDatabase;
import top.mrxiaom.sweet.chat.func.ChatListener;
import top.mrxiaom.sweet.chat.func.FilterManager;
import top.mrxiaom.sweet.chat.func.MessageStyleManager;
import top.mrxiaom.sweet.chat.utils.ComponentUtils;

public class SweetChat extends BukkitPlugin {
    private static Api api;
    public static SweetChat getInstance() {
        return (SweetChat) BukkitPlugin.getInstance();
    }
    public static Api getApi() {
        if (api == null) {
            throw new UnsupportedOperationException("Plugin is not enabled");
        }
        return api;
    }
    public SweetChat() throws Exception {
        super(options()
                .bungee(true)
                .adventure(true)
                .database(true)
                .reconnectDatabaseWhenReloadConfig(false)
                .scanIgnore("top.mrxiaom.sweet.chat.libs")
        );
        this.scheduler = new FoliaLibScheduler(this);

        info("正在检查依赖库状态");
        File librariesDir = ClassLoaderWrapper.isSupportLibraryLoader
                ? new File("libraries")
                : new File(this.getDataFolder(), "libraries");
        DefaultLibraryResolver resolver = new DefaultLibraryResolver(getLogger(), librariesDir);

        YamlConfiguration overrideLibraries = ConfigUtils.load(resolve("./.override-libraries.yml"));
        for (String key : overrideLibraries.getKeys(false)) {
            resolver.getStartsReplacer().put(key, overrideLibraries.getString(key));
        }
        resolver.addResolvedLibrary(BuildConstants.RESOLVED_LIBRARIES);

        List<URL> libraries = resolver.doResolve();
        info("正在添加 " + libraries.size() + " 个依赖库到类加载器");
        for (URL library : libraries) {
            this.classLoader.addURL(library);
        }
    }

    @Override
    public @NotNull ItemEditor initItemEditor() {
        return PaperFactory.createItemEditor();
    }

    @Override
    public @NotNull InventoryFactory initInventoryFactory() {
        return PaperFactory.createInventoryFactory();
    }

    private MessageDatabase messageDatabase;

    public MessageDatabase getMessageDatabase() {
        return messageDatabase;
    }

    @Override
    protected void beforeEnable() {
        ComponentUtils.init(this);
        LanguageManager.inst()
                .setLangFile("messages.yml")
                .register(Messages.class)
                .register(Messages.Commands.class)
                .reload();

        ActionProviders.registerActionProviders(ActionSound.PROVIDER);

        options.registerDatabase(
                messageDatabase = new MessageDatabase(this)
        );
    }

    @Override
    protected void afterEnable() {
        ComponentUtils.afterEnable();
        getLogger().info("SweetChat 加载完毕");
        api = new Api();
    }

    /**
     * SweetChat 开发者接口
     */
    public static class Api {
        private final ChatListener chatListener = ChatListener.inst();
        private final FilterManager filterManager = FilterManager.inst();
        private final MessageStyleManager styleManager = MessageStyleManager.inst();
        private Api() {}

        /**
         * 注册聊天格式部分提供器
         * @param partType 聊天部分类型，即配置中的 <code>type</code>
         * @param provider 通过配置读取聊天格式的实现
         */
        public void registerPart(@NotNull String partType, @NotNull IFormatPartProvider provider) {
            chatListener.registerPart(partType, provider);
        }

        /**
         * 注销聊天格式部分提供器
         * @param partType 聊天部分类型
         * @see Api#registerPart(String, IFormatPartProvider)
         */
        public void unregisterPart(@NotNull String partType) {
            chatListener.unregisterPart(partType);
        }

        /**
         * 注册聊天模式
         * @param modeId 聊天模式 ID
         * @param chatMode 聊天模式实现
         */
        public void registerChatMode(@NotNull String modeId, @NotNull IChatMode chatMode) {
            chatListener.registerChatMode(modeId, chatMode);
        }

        /**
         * 注销聊天模式
         * @param modeId 聊天模式 ID
         */
        public void unregisterChatMode(@NotNull String modeId) {
            chatListener.unregisterChatMode(modeId);
        }

        /**
         * 注册固定的聊天过滤器
         * @param filter 聊天过滤器实现
         */
        public void registerFixedFilter(@NotNull IChatFilter filter) {
            filterManager.registerFixedFilter(filter);
        }

        /**
         * 注销固定的聊天过滤器
         * @param filter 聊天过滤器实现
         */
        public void unregisterFixedFilter(@NotNull IChatFilter filter) {
            filterManager.unregisterFixedFilter(filter);
        }

        /**
         * 注册聊天过滤器提供器
         * @param provider 从配置中读取聊天过滤器的实现
         */
        public void registerFilterProvider(@NotNull IChatFilterProvider provider) {
            filterManager.registerFilterProvider(provider);
        }

        /**
         * 注销聊天过滤器提供器
         * @param provider 从配置中读取聊天过滤器的实现
         */
        public void unregisterFilterProvider(@NotNull IChatFilterProvider provider) {
            filterManager.unregisterFilterProvider(provider);
        }

        /**
         * 注册玩家消息样式预处理器
         * @param processor 文本组件处理实现
         */
        public void registerStylePreProcessor(@NotNull IComponentProcessor processor) {
            styleManager.registerStylePreProcessor(processor);
        }

        /**
         * 注销玩家消息样式预处理器
         * @param processor 文本组件处理实现
         */
        public void unregisterStylePreProcessor(@NotNull IComponentProcessor processor) {
            styleManager.unregisterStylePreProcessor(processor);
        }

        /**
         * 注册玩家消息样式后处理器
         * @param processor 文本组件处理实现
         */
        public void registerStylePostProcessor(@NotNull IComponentProcessor processor) {
            styleManager.registerStylePostProcessor(processor);
        }

        /**
         * 注销玩家消息样式后处理器
         * @param processor 文本组件处理实现
         */
        public void unregisterStylePostProcessor(@NotNull IComponentProcessor processor) {
            styleManager.unregisterStylePostProcessor(processor);
        }
    }
}
