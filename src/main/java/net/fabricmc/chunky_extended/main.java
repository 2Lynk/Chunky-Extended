package net.fabricmc.chunky_extended;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.chunky_extended.event.OnPlayerJoinHandler;
import net.fabricmc.chunky_extended.event.OnPlayerDisconnectHandler;
import net.fabricmc.chunky_extended.util.ModRegistries;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class main implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("Chunky-Extended");
	public static final String MOD_ID = "Chunky extension mod";

	@Override
	public void onInitialize() {
		LOGGER.info("Chunky extended loaded");

		ModRegistries.registerCommands();

		ServerPlayConnectionEvents.JOIN.register(new OnPlayerJoinHandler());
		ServerPlayConnectionEvents.DISCONNECT.register(new OnPlayerDisconnectHandler());

	}
}
