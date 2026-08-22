package codx.chunkyextended.neoforge;

import codx.chunkyextended.client.CeMenuClient;
import codx.codxlib.api.command.CodxCommands;

/**
 * Client-only init for NeoForge. Called from the mod constructor when running on a
 * client (see {@link ChunkyExtendedNeoForge}). CodxLib materializes the registered
 * client command into NeoForge's client dispatcher on the next world join.
 */
public final class ChunkyExtendedNeoForgeClient {

    private ChunkyExtendedNeoForgeClient() {
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
