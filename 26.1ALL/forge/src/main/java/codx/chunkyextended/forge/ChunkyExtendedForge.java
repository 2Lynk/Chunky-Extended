package codx.chunkyextended.forge;

import codx.chunkyextended.ChunkyExtendedCommon;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(ChunkyExtendedCommon.MOD_ID)
public class ChunkyExtendedForge {
    public ChunkyExtendedForge() {
        MinecraftForge.EVENT_BUS.register(this);
        if (FMLEnvironment.dist.isClient()) {
            ChunkyExtendedForgeClient.init();
        }
        ChunkyExtendedCommon.logLoaded("Forge");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ChunkyExtendedCommon.registerCommands(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        ChunkyExtendedCommon.onServerStarted(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        ChunkyExtendedCommon.onServerStopping();
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().getServer() != null) {
            ChunkyExtendedCommon.onPlayerJoin(event.getEntity().level().getServer());
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().level().getServer() != null) {
            ChunkyExtendedCommon.onPlayerDisconnect(event.getEntity().level().getServer());
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent.Post event) {
        ChunkyExtendedCommon.onServerTick(event.server());
    }
}