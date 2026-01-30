package top.mrxiaom.sweet.chat;

import top.mrxiaom.pluginbase.BukkitPlugin;
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
import top.mrxiaom.sweet.chat.database.MessageDatabase;
import top.mrxiaom.sweet.chat.utils.ComponentUtils;

public class SweetChat extends BukkitPlugin {
    public static SweetChat getInstance() {
        return (SweetChat) BukkitPlugin.getInstance();
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

        options.registerDatabase(
                messageDatabase = new MessageDatabase(this)
        );
    }

    @Override
    protected void afterEnable() {
        ComponentUtils.afterEnable();
        getLogger().info("SweetChat 加载完毕");
    }
}
