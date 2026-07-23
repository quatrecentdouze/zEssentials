package fr.maxlego08.essentials.api.utils.inventory;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Consumer;

public class OfflineEnderChestHolder implements InventoryHolder {

    private final UUID targetUniqueId;
    private final ItemStack[] contents;
    private final Consumer<Inventory> saveAction;
    private Inventory inventory;

    public OfflineEnderChestHolder(UUID targetUniqueId, ItemStack[] contents, Consumer<Inventory> saveAction) {
        this.targetUniqueId = targetUniqueId;
        this.contents = contents;
        this.saveAction = saveAction;
    }

    public UUID getTargetUniqueId() {
        return targetUniqueId;
    }

    public void save() {
        saveAction.accept(getInventory());
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory != null) return inventory;
        inventory = Bukkit.createInventory(this, 27);
        inventory.setContents(contents);
        return inventory;
    }
}
