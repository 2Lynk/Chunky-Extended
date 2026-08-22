package codx.chunkyextended.neoforge;

import codx.chunkyextended.ChunkyExtendedCommon;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(ChunkyExtendedCommon.MOD_ID)
public class ChunkyExtendedNeoForge {
    public ChunkyExtendedNeoForge(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);

        if (FMLEnvironment.getDist().isClient()) {
            ChunkyExtendedNeoForgeClient.init();
        }

        ChunkyExtendedCommon.logLoaded("NeoForge");
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        ChunkyExtendedCommon.registerCommands(event.getDispatcher());
    }

    private void onServerStarted(ServerStartedEvent event) {
        ChunkyExtendedCommon.onServerStarted(event.getServer());
    }

    private void onServerStopping(ServerStoppingEvent event) {
        ChunkyExtendedCommon.onServerStopping();
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().getServer() != null) {
            ChunkyExtendedCommon.onPlayerJoin(event.getEntity().level().getServer());
        }
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().level().getServer() != null) {
            ChunkyExtendedCommon.onPlayerDisconnect(event.getEntity().level().getServer());
        }
    }

    private void onServerTick(ServerTickEvent.Post event) {
        ChunkyExtendedCommon.onServerTick(event.getServer());
    }
}