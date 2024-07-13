package net.fabricmc.chunky_extended;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.chunky_extended.util.ModRegistries;
import net.fabricmc.chunky_extended.util.ModUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class main implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("Chunky-Extended");
	public static final String MOD_ID = "Chunky extension mod";
	private static final Path DATA_PATH = Paths.get("ce.txt");

	@Override
	public void onInitialize() {
		LOGGER.info("Chunky extended loaded");

		ModRegistries.registerCommands();

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if(server.getPlayerManager().getCurrentPlayerCount() == 0) {
				System.out.println("Ce : player joined, pausing chunky");
				String command = "chunky pause";

				CommandDispatcher<ServerCommandSource> dispatcher = server.getCommandManager().getDispatcher();
				ParseResults<ServerCommandSource> parseResults = dispatcher.parse(command, server.getCommandSource());

				try {
					dispatcher.execute(parseResults);
				} catch (CommandSyntaxException e) {
					e.printStackTrace();
				}
			}
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			if(ModUtil.returnModEnabled()) {
				if (server.getPlayerManager().getCurrentPlayerCount() == 1) {
					System.out.println("Ce : Last player left, resuming chunky");
					String command = "chunky continue";

					CommandDispatcher<ServerCommandSource> dispatcher = server.getCommandManager().getDispatcher();
					ParseResults<ServerCommandSource> parseResults = dispatcher.parse(command, server.getCommandSource());

					try {
						dispatcher.execute(parseResults);
					} catch (CommandSyntaxException e) {
						e.printStackTrace();
					}
				}
			}
		});

		ServerLifecycleEvents.SERVER_STARTED.register((server -> {
			try {
				if (!Files.exists(DATA_PATH)) {
					Files.createFile(DATA_PATH);
				}else{
					String ceFile = Files.readString(DATA_PATH);
					if(ceFile.equals("true")){
						ModUtil.enableMod();
					}else{
						ModUtil.disableMod();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				// Handle error or create the file if it doesn't exist
			}
		}));

		ServerLifecycleEvents.SERVER_STOPPING.register((server) ->{
			try {
				if(ModUtil.returnModEnabled()){
					Files.write(DATA_PATH, "true".getBytes());
				}else{
					Files.write(DATA_PATH, "false".getBytes());
				}
			} catch (IOException e) {
				e.printStackTrace();
				// Handle failure to write to the file
			}
		});
	}
}
