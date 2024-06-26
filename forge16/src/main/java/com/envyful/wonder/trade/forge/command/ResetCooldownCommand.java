package com.envyful.wonder.trade.forge.command;

import com.envyful.api.command.annotate.Command;
import com.envyful.api.command.annotate.executor.CommandProcessor;
import com.envyful.api.command.annotate.executor.Sender;
import com.envyful.api.command.annotate.permission.Permissible;
import com.envyful.api.forge.chat.UtilChatColour;
import com.envyful.wonder.trade.forge.WonderTradeForge;
import com.envyful.wonder.trade.forge.data.WonderTradeAttribute;
import net.minecraft.command.ICommandSource;
import net.minecraft.util.Util;

@Command(
        value = {
                "resetcooldown",
                "rc"
        }
)
@Permissible("wonder.trade.command.reset.cooldown")
public class ResetCooldownCommand {

    @CommandProcessor
    public void onCommand(@Sender ICommandSource sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(UtilChatColour.colour("&c&l(!) &cNo player specified."), Util.NIL_UUID);
            return;
        }

        var target = WonderTradeForge.getInstance().getPlayerManager().getOnlinePlayerCaseInsensitive(args[0]);

        if (target == null) {
            sender.sendMessage(UtilChatColour.colour("&c&l(!) &cCannot find that player"), Util.NIL_UUID);
            return;
        }

        var attribute = target.getAttributeNow(WonderTradeAttribute.class);

        if (attribute == null) {
            sender.sendMessage(UtilChatColour.colour("&c&l(!) &cPlease wait! That player has not loaded yet"), Util.NIL_UUID);
            return;
        }

        attribute.resetCooldown();
        sender.sendMessage(UtilChatColour.colour("&e&l(!) &eSuccessfully reset their cooldown"), Util.NIL_UUID);

    }
}
