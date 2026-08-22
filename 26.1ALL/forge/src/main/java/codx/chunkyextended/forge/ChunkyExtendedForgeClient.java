package codx.chunkyextended.forge;

import codx.chunkyextended.client.CeMenuClient;
import codx.codxlib.api.command.CodxCommands;

/**
 * Client-only init for Forge. Called from the mod constructor when running on a
 * client (see {@link ChunkyExtendedForge}). CodxLib materializes the registered
 * client command into Forge's client dispatcher on the next world join.
 */
public final class ChunkyExtendedForgeClient {

    private ChunkyExtendedForgeClient() {
    }

    public static void init() {
        CeMenuClient.install();
        CodxCommands.registerClient(() -> CodxCommands.literal("cemenu")
                .executes(ctx -> {
                    CeMenuClient.requestAndOpen();
                    return 1;
                }));
    }
}
