package net.fabricmc.chunky_extended.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.chunky_extended.util.ModUtil;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.*;

public class Commands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        LiteralCommandNode<ServerCommandSource> register = dispatcher.register(CommandManager.literal("chunky-extend")
            .executes(context -> {
                // For versions below 1.19, replace "Text.literal" with "new LiteralText".
                context.getSource().sendMessage(Text.literal("chunky-extend enable - Enable auto stop and start"));
                context.getSource().sendMessage(Text.literal("chunky-extend disable - Disable auto stop and start"));
                context.getSource().sendMessage(Text.literal("chunky-extend status - Displays status"));
                return 0;
            })
            .then(CommandManager.literal("enable").executes(Commands::enable))
            .then(CommandManager.literal("disable").executes(Commands::disable))
            .then(CommandManager.literal("status").executes(Commands::status))

        );
    }

    private static int enable(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {
        if(!ModUtil.returnModEnabled()){
            ModUtil.enableMod();
            serverCommandSourceCommandContext.getSource().getPlayer().sendMessage(Text.of("Ce: enabled!"));
        }
        return 0;
    };

    private static int disable(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {
        if(ModUtil.returnModEnabled()){
            ModUtil.disableMod();
            serverCommandSourceCommandContext.getSource().getPlayer().sendMessage(Text.of("Ce: disabled!"));
        }
        return 0;
    };

    private static int status(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {
        if(ModUtil.returnModEnabled()){
            serverCommandSourceCommandContext.getSource().getPlayer().sendMessage(Text.of("Ce: Chunky extend is enabled"));
        }else{
            serverCommandSourceCommandContext.getSource().getPlayer().sendMessage(Text.of("Ce: Chunky extend is disabled"));
        }

        return 0;
    }
}
