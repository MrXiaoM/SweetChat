package top.mrxiaom.sweet.chat.func;

import top.mrxiaom.sweet.chat.SweetChat;

@SuppressWarnings({"unused"})
public abstract class AbstractPluginHolder extends top.mrxiaom.pluginbase.func.AbstractPluginHolder<SweetChat> {
    public AbstractPluginHolder(SweetChat plugin) {
        super(plugin);
    }

    public AbstractPluginHolder(SweetChat plugin, boolean register) {
        super(plugin, register);
    }
}
