package fr.maxlego08.essentials.commands.commands.tlmstaff;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.module.modules.tlmstaff.TLMStaffModule;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;

public class CommandStaffMode extends VCommand {

    public CommandStaffMode(EssentialsPlugin plugin) {
        super(plugin);
        this.setPermission(Permission.ESSENTIALS_TLMSTAFF_STAFF);
        this.setDescription(Message.DESCRIPTION_TLMSTAFF_STAFF);
        this.setModule(TLMStaffModule.class);
        this.onlyPlayers();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        TLMStaffModule module = plugin.getModuleManager().getModule(TLMStaffModule.class);
        module.toggleStaff(this.player);
        return CommandResultType.SUCCESS;
    }
}
