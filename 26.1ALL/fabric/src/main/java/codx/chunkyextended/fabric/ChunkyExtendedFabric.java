package codx.chunkyextended.fabric;

import codx.chunkyextended.ChunkyExtendedCommon;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class ChunkyExtendedFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) ->
                ChunkyExtendedCommon.registerCommands(dispatcher));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ChunkyExtendedCommon.onPlayerJoin(server));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                ChunkyExtendedCommon.onPlayerDisconnect(server));

        ServerLifecycleEvents.SERVER_STARTED.register(ChunkyExtendedCommon::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> ChunkyExtendedCommon.onServerStopping());
                ServerTickEvents.END_SERVER_TICK.register(ChunkyExtendedCommon::onServerTick);

        ChunkyExtendedCommon.logLoaded("Fabric");
    }
}