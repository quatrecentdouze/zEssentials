package fr.maxlego08.essentials.nms.v1_21_8;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.nms.PlayerUtil;
import fr.maxlego08.essentials.api.utils.inventory.OfflineEnderChestHolder;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class PlayerUtils implements PlayerUtil {

    private final EssentialsPlugin plugin;

    public PlayerUtils(EssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean openEnderChest(Player player, OfflinePlayer offlinePlayer) {
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        PlayerDataStorage storage = server.playerDataStorage;
        String name = offlinePlayer.getName() == null ? offlinePlayer.getUniqueId().toString() : offlinePlayer.getName();
        Optional<CompoundTag> optional = storage.load(name, offlinePlayer.getUniqueId().toString(), ProblemReporter.DISCARDING);
        if (optional.isEmpty()) return false;
        CompoundTag playerData = optional.get();
        ItemStack[] contents = loadContents(server, playerData);
        OfflineEnderChestHolder holder = new OfflineEnderChestHolder(offlinePlayer.getUniqueId(), contents, inventory -> queueSave(server, storage, playerData, offlinePlayer, inventory));
        player.openInventory(holder.getInventory());
        return true;
    }

    private ItemStack[] loadContents(MinecraftServer server, CompoundTag playerData) {
        ItemStack[] contents = new ItemStack[27];
        ListTag items = playerData.getListOrEmpty("EnderItems");
        for (int index = 0; index < items.size(); index++) {
            CompoundTag itemData = items.getCompoundOrEmpty(index);
            int slot = itemData.getByteOr("Slot", (byte) -1) & 255;
            if (slot < 0 || slot >= contents.length) continue;
            net.minecraft.world.item.ItemStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, server.registryAccess()), itemData).result().filter(item -> !item.isEmpty()).ifPresent(item -> contents[slot] = item.asBukkitCopy());
        }
        return contents;
    }

    private void queueSave(MinecraftServer server, PlayerDataStorage storage, CompoundTag playerData, OfflinePlayer offlinePlayer, Inventory inventory) {
        ItemStack[] contents = inventory.getContents().clone();
        saveContents(server, storage, playerData, offlinePlayer, contents);
        this.plugin.getScheduler().runNextTick(task -> {
            Player connectedPlayer = Bukkit.getPlayer(offlinePlayer.getUniqueId());
            if (connectedPlayer != null) this.plugin.getScheduler().runAtEntity(connectedPlayer, entityTask -> connectedPlayer.getEnderChest().setContents(contents));
        });
    }

    private void saveContents(MinecraftServer server, PlayerDataStorage storage, CompoundTag playerData, OfflinePlayer offlinePlayer, ItemStack[] contents) {
        try {
            ListTag items = new ListTag();
            for (int slot = 0; slot < contents.length; slot++) {
                ItemStack bukkitItem = contents[slot];
                if (bukkitItem == null || bukkitItem.isEmpty()) continue;
                net.minecraft.world.item.ItemStack item = net.minecraft.world.item.ItemStack.fromBukkitCopy(bukkitItem);
                Tag encoded = net.minecraft.world.item.ItemStack.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, server.registryAccess()), item).result().orElse(null);
                if (!(encoded instanceof CompoundTag itemData)) continue;
                itemData.putByte("Slot", (byte) slot);
                items.add(itemData);
            }
            playerData.put("EnderItems", items);
            Path directory = storage.getPlayerDir().toPath();
            Path temporary = Files.createTempFile(directory, offlinePlayer.getUniqueId() + "-", ".dat");
            NbtIo.writeCompressed(playerData, temporary);
            Path destination = directory.resolve(offlinePlayer.getUniqueId() + ".dat");
            Path backup = directory.resolve(offlinePlayer.getUniqueId() + ".dat_old");
            Util.safeReplaceFile(destination, temporary, backup);
        } catch (Exception exception) {
            this.plugin.getLogger().severe(exception.getMessage());
        }
    }

    @Override
    public boolean openPlayerInventory(Player player, OfflinePlayer offlinePlayer) {
        return false;
    }
}
