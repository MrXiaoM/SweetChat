package top.mrxiaom.sweet.chat.config;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class OffHandAccessor {
    public static ItemStack getItemInOffHand(PlayerInventory inv) {
        return inv.getItemInOffHand();
    }
}
