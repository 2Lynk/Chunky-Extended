package net.fabricmc.chunky_extended.event;

import net.fabricmc.chunky_extended.util.ModUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;

public class OnPlayerDisconnectHandler implements ServerPlayConnectionEvents.Disconnect {
    @Override
    public void onPlayDisconnect(ServerPlayNetworkHandler handler, MinecraftServer server) {
        if(ModUtil.returnModEnabled()) {
            if (server.getPlayerManager().getCurrentPlayerCount() == 1) {
                Text command = Text.of("Ce : Last player left, resuming chunky");
                server.sendMessage(command);
            }
            server.getCommandManager().executeWithPrefix(server.getCommandSource(), "chunky continue");
        }
    }
}
