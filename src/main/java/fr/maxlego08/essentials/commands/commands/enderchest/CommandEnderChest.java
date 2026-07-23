package fr.maxlego08.essentials.commands.commands.enderchest;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;

import java.util.List;
import java.util.UUID;

public class CommandEnderChest extends VCommand {

    public CommandEnderChest(EssentialsPlugin plugin) {
        super(plugin);
        this.setDescription(Message.DESCRIPTION_ENDERCHEST);
        this.addOptionalArg("player", (sender, args) -> plugin.getStorageManager() == null ? List.of() : plugin.getStorageManager().getStorage().getPlayerNames());
        this.onlyPlayers();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        String identifier = this.argAsString(0, null);
        if (identifier == null) {
            if (!hasPermission(this.sender, Permission.ESSENTIALS_ENDERCHEST)) return CommandResultType.NO_PERMISSION;
            this.player.openInventory(this.player.getEnderChest());
            return CommandResultType.SUCCESS;
        }
        if (!hasPermission(this.sender, Permission.ESSENTIALS_ENDERSEE)) return CommandResultType.NO_PERMISSION;
        EnderChestAccess access = new EnderChestAccess(plugin);
        try {
            UUID uniqueId = UUID.fromString(identifier);
            if (!uniqueId.equals(this.player.getUniqueId()) && !hasPermission(this.sender, Permission.ESSENTIALS_ENDERSEE_OFFLINE) && plugin.getServer().getPlayer(uniqueId) == null) return CommandResultType.NO_PERMISSION;
            access.open(this.player, uniqueId, identifier);
        } catch (IllegalArgumentException exception) {
            fetchUniqueId(identifier, uniqueId -> {
                if (!uniqueId.equals(this.player.getUniqueId()) && !hasPermission(this.sender, Permission.ESSENTIALS_ENDERSEE_OFFLINE) && plugin.getServer().getPlayer(uniqueId) == null) {
                    message(this.sender, Message.COMMAND_NO_PERMISSION);
                    return;
                }
                access.open(this.player, uniqueId, identifier);
            });
        }
        return CommandResultType.SUCCESS;
    }
}
