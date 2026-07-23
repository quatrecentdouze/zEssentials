package fr.maxlego08.essentials.module.modules.tlmstaff;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class InspectHolder implements InventoryHolder {

    private final UUID targetUniqueId;
    private Inventory inventory;

    public InspectHolder(UUID targetUniqueId) {
        this.targetUniqueId = targetUniqueId;
    }

    public UUID getTargetUniqueId() {
        return targetUniqueId;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
