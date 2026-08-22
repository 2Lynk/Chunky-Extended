package codx.chunkyextended;

import net.fabricmc.api.ModInitializer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import codx.chunkyextended.util.ModRegistries;
import codx.chunkyextended.util.ModUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.ServerCommandSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ChunkyExtended implements ModInitializer {
	public static final String MOD_ID = "chunkyextended";
	private static final Path DATA_PATH = Paths.get("ce.txt");

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Chunky Extended loaded");

		ModRegistries.registerCommands();

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (server.getPlayerManager().getCurrentPlayerCount() == 0) {
				String command = "chunky pause";

				CommandDispatcher<ServerCommandSource> dispatcher = server.getCommandManager().getDispatcher();
				ParseResults<ServerCommandSource> parseResults = dispatcher.parse(command, server.getCommandSource());

				try {
					dispatcher.execute(parseResults);
				} catch (CommandSyntaxException e) {
					LOGGER.error("Failed to execute command: {}", command, e);
				}
			}
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			if (ModUtil.returnModEnabled() && server.getPlayerManager().getCurrentPlayerCount() == 1) {
				String command = "chunky continue";

				CommandDispatcher<ServerCommandSource> dispatcher = server.getCommandManager().getDispatcher();
				ParseResults<ServerCommandSource> parseResults = dispatcher.parse(command, server.getCommandSource());

				try {
					dispatcher.execute(parseResults);
				} catch (CommandSyntaxException e) {
					LOGGER.error("Failed to execute command: {}", command, e);
				}
			}
		});

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			try {
				if (!Files.exists(DATA_PATH)) {
					Files.createFile(DATA_PATH);
				} else {
					String ceFile = Files.readString(DATA_PATH);
					if (ceFile.equals("true")) {
						ModUtil.enableMod();
					} else {
						ModUtil.disableMod();
					}
				}
			} catch (IOException e) {
				LOGGER.error("Failed to load CE state file", e);
			}
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			try {
				if (ModUtil.returnModEnabled()) {
					Files.write(DATA_PATH, "true".getBytes());
				} else {
					Files.write(DATA_PATH, "false".getBytes());
				}
			} catch (IOException e) {
				LOGGER.error("Failed to persist CE state file", e);
			}
		});
	}
}