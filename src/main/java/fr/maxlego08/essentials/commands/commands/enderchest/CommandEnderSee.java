package fr.maxlego08.essentials.commands.commands.enderchest;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;

import java.util.UUID;

public class CommandEnderSee extends VCommand {

    public CommandEnderSee(EssentialsPlugin plugin) {
        super(plugin);
        this.setPermission(Permission.ESSENTIALS_ENDERSEE);
        this.setDescription(Message.DESCRIPTION_ENDERSEE);
        this.addRequireOfflinePlayerNameArg();
        this.onlyPlayers();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        String identifier = this.argAsString(0);
        EnderChestAccess access = new EnderChestAccess(plugin);
        try {
            UUID uniqueId = UUID.fromString(identifier);
            if (!hasOfflinePermission(plugin, uniqueId)) return CommandResultType.NO_PERMISSION;
            access.open(this.player, uniqueId, identifier);
        } catch (IllegalArgumentException exception) {
            fetchUniqueId(identifier, uniqueId -> {
                if (!hasOfflinePermission(plugin, uniqueId)) {
                    message(this.sender, Message.COMMAND_NO_PERMISSION);
                    return;
                }
                access.open(this.player, uniqueId, identifier);
            });
        }
        return CommandResultType.SUCCESS;
    }

    private boolean hasOfflinePermission(EssentialsPlugin plugin, UUID uniqueId) {
        return plugin.getServer().getPlayer(uniqueId) != null || hasPermission(this.sender, Permission.ESSENTIALS_ENDERSEE_OFFLINE);
    }
}
