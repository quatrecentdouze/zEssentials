package fr.maxlego08.essentials.api.utils.inventory;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class EnderChestHolder implements InventoryHolder {

    private final Player player;
    private Inventory inventory;

    public EnderChestHolder(Player player) {
        this.player = player;
    }

    public Player player() {
        return player;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory != null) return inventory;
        Inventory enderChestInventory = player.getEnderChest();
        inventory = Bukkit.createInventory(this, 27, enderChestInventory.getType().defaultTitle());
        int slot = 0;
        for (ItemStack content : enderChestInventory.getContents()) inventory.setItem(slot++, content);
        return inventory;
    }
}
