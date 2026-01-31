package top.mrxiaom.sweet.chat.config.replacements;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class OffHandAccessor {
    public static ItemStack getItemInOffHand(PlayerInventory inv) {
        return inv.getItemInOffHand();
    }
}
