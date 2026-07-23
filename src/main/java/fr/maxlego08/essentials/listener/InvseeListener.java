package fr.maxlego08.essentials.listener;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.utils.inventory.EnderChestHolder;
import fr.maxlego08.essentials.api.utils.inventory.OfflineEnderChestSessions;
import fr.maxlego08.essentials.api.utils.inventory.OfflineEnderChestHolder;
import fr.maxlego08.essentials.api.utils.inventory.PlayerInventoryHolder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class InvseeListener implements Listener {

    private final EssentialsPlugin plugin;

    public InvseeListener(EssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof EnderChestHolder enderChestHolder) {
            Player targetPlayer = enderChestHolder.player();
            UUID viewerUniqueId = event.getPlayer().getUniqueId();
            if (targetPlayer != null && OfflineEnderChestSessions.owns(viewerUniqueId, targetPlayer.getUniqueId())) {
                try {
                    copyEnderChest(event.getInventory(), targetPlayer);
                    targetPlayer.saveData();
                } finally {
                    OfflineEnderChestSessions.releaseViewer(viewerUniqueId);
                }
            }
        } else if (event.getInventory().getHolder() instanceof OfflineEnderChestHolder offlineEnderChestHolder) {
            UUID viewerUniqueId = event.getPlayer().getUniqueId();
            if (OfflineEnderChestSessions.owns(viewerUniqueId, offlineEnderChestHolder.getTargetUniqueId())) {
                try {
                    offlineEnderChestHolder.save();
                } finally {
                    OfflineEnderChestSessions.releaseViewer(viewerUniqueId);
                }
            }
        } else if (event.getInventory().getHolder() instanceof PlayerInventoryHolder playerInventoryHolder) {
            Player targetPlayer = playerInventoryHolder.player();
            if (targetPlayer == null || !targetPlayer.isOnline()) return;
            for (int slot = 0; slot < 36; slot++) targetPlayer.getInventory().setItem(slot, event.getInventory().getItem(slot));
            targetPlayer.saveData();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        OfflineEnderChestSessions.releaseTarget(event.getPlayer().getUniqueId()).ifPresent(session -> {
            if (session.inventory() != null) copyEnderChest(session.inventory(), event.getPlayer());
            Player viewer = Bukkit.getPlayer(session.viewerUniqueId());
            if (viewer != null) this.plugin.getScheduler().runAtEntity(viewer, task -> viewer.closeInventory());
        });
    }

    private void copyEnderChest(Inventory source, Player target) {
        int slot = 0;
        for (ItemStack content : source.getContents()) target.getEnderChest().setItem(slot++, content);
    }
}
