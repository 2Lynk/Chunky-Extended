package net.fabricmc.chunky_extended.event;

import net.fabricmc.chunky_extended.util.ModUtil;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;

public class OnPlayerJoinHandler implements ServerPlayConnectionEvents.Join {
    @Override
    public void onPlayReady(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        if(ModUtil.returnModEnabled()){
            if(server.getPlayerManager().getCurrentPlayerCount() == 0){
                Text command = Text.of("Ce : player joined, pausing chunky");
                server.sendMessage(command);
                server.getCommandManager().executeWithPrefix(server.getCommandSource(), "chunky pause");
            }
        }
    }
}


