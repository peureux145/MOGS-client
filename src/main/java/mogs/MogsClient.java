package mogs;

import mogs.config.ConfigManager;
import mogs.event.ClientEvents;
import mogs.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MogsClient implements ClientModInitializer {
	public static final String NAME = "MOGS";
	public static final String FULL_NAME = "MOGS Client";
	public static final String MINECRAFT_VERSION = "1.21.11";
	public static final String SUBTITLE = "Minecraft " + MINECRAFT_VERSION + " · Fabric";
	public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

	@Override
	public void onInitializeClient() {
		ConfigManager.init();
		ModuleManager.init();
		ConfigManager.load();

		ClientEvents.register();
		LOGGER.info("{} loaded - {} modules registered ({})", FULL_NAME, ModuleManager.all().size(), SUBTITLE);
	}
}
