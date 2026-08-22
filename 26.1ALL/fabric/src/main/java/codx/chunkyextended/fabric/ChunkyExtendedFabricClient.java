package codx.chunkyextended.fabric;

import codx.chunkyextended.client.CeMenuClient;
import codx.codxlib.api.command.CodxCommands;
import net.fabricmc.api.ClientModInitializer;

public class ChunkyExtendedFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CeMenuClient.install();
        CodxCommands.registerClient(() -> CodxCommands.literal("cemenu")
                .executes(ctx -> {
                    CeMenuClient.requestAndOpen();
                    return 1;
                }));
    }
}
