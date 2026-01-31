package top.mrxiaom.sweet.chat.config.replacements;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * 物品来源枚举，用于从玩家背包的某格获取物品
 */
@SuppressWarnings("deprecation")
public enum EnumItemSource {
    /**
     * 主手物品
     */
    HAND("hand", PlayerInventory::getItemInHand),
    /**
     * 副手物品
     */
    OFF_HAND("offhand", OffHandAccessor::getItemInOffHand),
    /**
     * 快捷栏左数第1个物品
     */
    HOTBAR_1("hotbar1", inv -> inv.getItem(hotbar(1))),
    /**
     * 快捷栏左数第2个物品
     */
    HOTBAR_2("hotbar2", inv -> inv.getItem(hotbar(2))),
    /**
     * 快捷栏左数第3个物品
     */
    HOTBAR_3("hotbar3", inv -> inv.getItem(hotbar(3))),
    /**
     * 快捷栏左数第4个物品
     */
    HOTBAR_4("hotbar4", inv -> inv.getItem(hotbar(4))),
    /**
     * 快捷栏左数第5个物品
     */
    HOTBAR_5("hotbar5", inv -> inv.getItem(hotbar(5))),
    /**
     * 快捷栏左数第6个物品
     */
    HOTBAR_6("hotbar6", inv -> inv.getItem(hotbar(6))),
    /**
     * 快捷栏左数第7个物品
     */
    HOTBAR_7("hotbar7", inv -> inv.getItem(hotbar(7))),
    /**
     * 快捷栏左数第8个物品
     */
    HOTBAR_8("hotbar8", inv -> inv.getItem(hotbar(8))),
    /**
     * 快捷栏左数第9个物品
     */
    HOTBAR_9("hotbar9", inv -> inv.getItem(hotbar(9))),
    ;
    private static int hotbar(int slot) {
        return -1 + slot;
    }
    private final String tagName;
    private final Function<PlayerInventory, ItemStack> impl;
    EnumItemSource(String tagSuffix, Function<PlayerInventory, ItemStack> impl) {
        this.impl = impl;
        this.tagName = "sweet-chat-item-" + tagSuffix;
    }

    /**
     * 获取其代表的 MiniMessage 标签名
     */
    public String tagName() {
        return tagName;
    }

    /**
     * 获取物品实例，如果无法获取，则返回 AIR 物品
     */
    @NotNull
    public ItemStack get(PlayerInventory inv) {
        try {
            ItemStack item = impl.apply(inv);
            if (item != null) return item;
        } catch (LinkageError ignored) {}
        return new ItemStack(Material.AIR);
    }
}
