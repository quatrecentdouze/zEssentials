package fr.maxlego08.essentials.commands.commands.enderchest;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.api.nms.PlayerUtil;
import fr.maxlego08.essentials.api.utils.inventory.OfflineEnderChestSessions;
import fr.maxlego08.essentials.zutils.utils.ZUtils;
import fr.maxlego08.menu.common.utils.nms.NmsVersion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.lang.reflect.Constructor;
import java.util.UUID;

public class EnderChestAccess extends ZUtils {

    private final EssentialsPlugin plugin;

    public EnderChestAccess(EssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, UUID targetUniqueId, String targetName) {
        this.plugin.getScheduler().runAtEntity(viewer, task -> openNow(viewer, targetUniqueId, targetName));
    }

    private void openNow(Player viewer, UUID targetUniqueId, String targetName) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetUniqueId);
        Player target = offlinePlayer.getPlayer();
        if (target != null && target.isOnline()) {
            openOnline(viewer, target, targetName);
            return;
        }
        if (!OfflineEnderChestSessions.acquire(viewer.getUniqueId(), targetUniqueId)) {
            message(viewer, Message.COMMAND_ENDERSEE_ALREADY_OPEN, "%player%", targetName);
            return;
        }

        String version = NmsVersion.getCurrentVersion().name().replace("V_", "v");
        String className = String.format("fr.maxlego08.essentials.nms.%s.PlayerUtils", version);
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getConstructor(EssentialsPlugin.class);
            PlayerUtil playerUtil = (PlayerUtil) constructor.newInstance(this.plugin);
            if (playerUtil.openEnderChest(viewer, offlinePlayer)) {
                OfflineEnderChestSessions.bind(viewer.getUniqueId(), viewer.getOpenInventory().getTopInventory());
                Player connectedTarget = Bukkit.getPlayer(targetUniqueId);
                if (connectedTarget != null && connectedTarget.isOnline()) {
                    OfflineEnderChestSessions.releaseTarget(targetUniqueId);
                    openOnline(viewer, connectedTarget, targetName);
                }
                return;
            }
        } catch (Exception exception) {
            this.plugin.getLogger().severe("Cannot create a new instance for the class " + className);
            this.plugin.getLogger().severe(String.valueOf(exception.getMessage()));
        }
        OfflineEnderChestSessions.releaseViewer(viewer.getUniqueId());
        message(viewer, Message.COMMAND_ENDERSEE_ERROR, "%player%", targetName);
    }

    private void openOnline(Player viewer, Player target, String targetName) {
        this.plugin.getScheduler().runAtEntityWithFallback(target, task -> {
            Inventory inventory = target.getEnderChest();
            this.plugin.getScheduler().runAtEntity(viewer, viewerTask -> {
                if (Bukkit.getPlayer(target.getUniqueId()) == null) {
                    openNow(viewer, target.getUniqueId(), targetName);
                    return;
                }
                viewer.openInventory(inventory);
            });
        }, () -> open(viewer, target.getUniqueId(), targetName));
    }
}
