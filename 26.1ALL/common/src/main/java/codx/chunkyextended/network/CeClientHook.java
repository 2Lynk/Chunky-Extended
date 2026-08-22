package codx.chunkyextended.network;

/**
 * Server-safe seam between the clientbound {@link CeMenuPackets.State} handler
 * (registered in common, on both sides) and the client-only screen code.
 *
 * <p>The clientbound payload's type + codec must be registered during common init
 * so the <em>server</em> can encode and send it. But the handler must not reference
 * any {@code net.minecraft.client.*} class at registration time, or creating that
 * method-reference lambda would classload the client screen on a dedicated server
 * and crash. So common registers {@link #onState} here (no client imports), and the
 * client installs the real behaviour via {@link #setHandler} at client init.
 */
public final class CeClientHook {

    @FunctionalInterface
    public interface StateHandler {
        void accept(CeMenuPackets.State state);
    }

    private static volatile StateHandler handler = state -> {
    };

    private CeClientHook() {
    }

    /** Called by the client entrypoint to wire the screen up. */
    public static void setHandler(StateHandler newHandler) {
        handler = newHandler;
    }

    /** Registered as the clientbound handler; only does anything on a client. */
    public static void onState(CeMenuPackets.State state) {
        handler.accept(state);
    }
}
