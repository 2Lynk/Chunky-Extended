package codx.chunkyextended.client;

import codx.chunkyextended.network.CeClientHook;
import codx.chunkyextended.network.CeMenuPackets;
import codx.codxlib.api.network.CodxNetwork;
import net.minecraft.client.Minecraft;

/**
 * Client-side glue for the {@code /cemenu} screen. Loaded only on the client (the
 * loader client entrypoints call {@link #install()} and {@link #requestAndOpen()}),
 * so it may freely touch {@code net.minecraft.client.*}.
 */
public final class CeMenuClient {

    private static boolean awaitingOpen;

    private CeMenuClient() {
    }

    /** Wire the clientbound state handler. Call once from client init. */
    public static void install() {
        CeClientHook.setHandler(CeMenuClient::onState);
    }

    /** Ask the server for the current state; the screen opens once it replies. */
    public static void requestAndOpen() {
        awaitingOpen = true;
        CodxNetwork.sendToServer(new CeMenuPackets.Request());
    }

    private static void onState(CeMenuPackets.State state) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.screen instanceof CeMenuScreen screen) {
                screen.applyState(state);
            } else if (awaitingOpen) {
                awaitingOpen = false;
                mc.setScreen(new CeMenuScreen(mc.screen, state));
            }
        });
    }
}
