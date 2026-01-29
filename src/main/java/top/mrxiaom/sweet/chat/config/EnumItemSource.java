package top.mrxiaom.sweet.chat.config;

import net.kyori.adventure.text.minimessage.tag.TagPattern;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

@SuppressWarnings("deprecation")
public enum EnumItemSource {
    HAND("hand", PlayerInventory::getItemInHand),
    OFF_HAND("offhand", PlayerInventory::getItemInOffHand),
    HOTBAR_1("hotbar1", inv -> inv.getItem(hotbar(1))),
    HOTBAR_2("hotbar2", inv -> inv.getItem(hotbar(2))),
    HOTBAR_3("hotbar3", inv -> inv.getItem(hotbar(3))),
    HOTBAR_4("hotbar4", inv -> inv.getItem(hotbar(4))),
    HOTBAR_5("hotbar5", inv -> inv.getItem(hotbar(5))),
    HOTBAR_6("hotbar6", inv -> inv.getItem(hotbar(6))),
    HOTBAR_7("hotbar7", inv -> inv.getItem(hotbar(7))),
    HOTBAR_8("hotbar8", inv -> inv.getItem(hotbar(8))),
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

    public String tagName() {
        return tagName;
    }

    @NotNull
    public ItemStack get(PlayerInventory inv) {
        try {
            ItemStack item = impl.apply(inv);
            if (item != null) return item;
        } catch (LinkageError ignored) {}
        return new ItemStack(Material.AIR);
    }
}
