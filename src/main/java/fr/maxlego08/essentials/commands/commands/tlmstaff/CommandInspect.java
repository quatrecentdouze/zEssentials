package fr.maxlego08.essentials.commands.commands.tlmstaff;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.commands.CommandResultType;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.module.modules.tlmstaff.TLMStaffModule;
import fr.maxlego08.essentials.zutils.utils.commands.VCommand;
import org.bukkit.entity.Player;

public class CommandInspect extends VCommand {

    public CommandInspect(EssentialsPlugin plugin) {
        super(plugin);
        this.setPermission(Permission.ESSENTIALS_TLMSTAFF_INSPECT);
        this.setDescription(Message.DESCRIPTION_TLMSTAFF_INSPECT);
        this.setModule(TLMStaffModule.class);
        this.addRequirePlayerNameArg();
        this.onlyPlayers();
    }

    @Override
    protected CommandResultType perform(EssentialsPlugin plugin) {
        TLMStaffModule module = plugin.getModuleManager().getModule(TLMStaffModule.class);
        Player target = this.argAsPlayer(0);
        if (target == null) return CommandResultType.SYNTAX_ERROR;
        module.openInspect(this.player, target);
        return CommandResultType.SUCCESS;
    }
}
