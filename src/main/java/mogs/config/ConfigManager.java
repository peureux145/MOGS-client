package mogs.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mogs.MogsClient;
import mogs.module.Category;
import mogs.module.Module;
import mogs.module.ModuleManager;
import mogs.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Persists everything to .minecraft/config/mogs/config.json: enabled modules, keybinds, all settings
 * (including HUD positions / scale and theme colours), GUI position and the last selected category.
 * The directory is created automatically and nothing has to be edited by hand.
 */
public final class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int AUTOSAVE_TICKS = 40;

	private static Path directory;
	private static Path file;
	private static boolean dirty;
	private static int ticksSinceDirty;

	/** ClickGUI window position in screen pixels; -1 means "centre on the screen". */
	public static int guiX = -1;
	public static int guiY = -1;
	public static Category lastCategory = Category.COMBAT;

	private ConfigManager() {
	}

	public static void init() {
		directory = FabricLoader.getInstance().getConfigDir().resolve("mogs");
		file = directory.resolve("config.json");
		try {
			Files.createDirectories(directory);
		} catch (IOException e) {
			MogsClient.LOGGER.error("Could not create the MOGS config directory", e);
		}
	}

	public static Path directory() {
		return directory;
	}

	public static void load() {
		if (file == null || !Files.exists(file)) {
			save();
			return;
		}
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

			if (root.has("gui") && root.get("gui").isJsonObject()) {
				JsonObject gui = root.getAsJsonObject("gui");
				guiX = gui.has("x") ? gui.get("x").getAsInt() : -1;
				guiY = gui.has("y") ? gui.get("y").getAsInt() : -1;
				if (gui.has("category")) {
					try {
						lastCategory = Category.valueOf(gui.get("category").getAsString());
					} catch (IllegalArgumentException ignored) {
						lastCategory = Category.COMBAT;
					}
				}
			}

			JsonObject modules = root.has("modules") && root.get("modules").isJsonObject()
					? root.getAsJsonObject("modules") : new JsonObject();
			for (Module module : ModuleManager.all()) {
				if (!modules.has(module.name()) || !modules.get(module.name()).isJsonObject()) {
					continue;
				}
				JsonObject data = modules.getAsJsonObject(module.name());
				if (data.has("key")) {
					module.keybind().load(data.get("key"));
				}
				if (data.has("settings") && data.get("settings").isJsonObject()) {
					JsonObject saved = data.getAsJsonObject("settings");
					for (Setting<?> setting : module.settings()) {
						if (setting.isPersistent() && saved.has(setting.name)) {
							setting.load(saved.get(setting.name));
						}
					}
				}
				if (data.has("enabled") && !module.isSettingsOnly()) {
					module.setEnabled(data.get("enabled").getAsBoolean(), false);
				}
			}
		} catch (Exception e) {
			// A corrupted file must never stop the game: keep defaults and rewrite it.
			MogsClient.LOGGER.error("Could not read the MOGS config, using defaults", e);
			save();
		}
		dirty = false;
	}

	public static void save() {
		if (file == null) {
			return;
		}
		JsonObject root = new JsonObject();
		root.addProperty("client", MogsClient.NAME);
		root.addProperty("minecraft", MogsClient.MINECRAFT_VERSION);

		JsonObject gui = new JsonObject();
		gui.addProperty("x", guiX);
		gui.addProperty("y", guiY);
		gui.addProperty("category", lastCategory.name());
		root.add("gui", gui);

		JsonObject modules = new JsonObject();
		for (Module module : ModuleManager.all()) {
			JsonObject data = new JsonObject();
			data.addProperty("enabled", module.isEnabled());
			data.addProperty("key", module.keybind().key());
			JsonObject settings = new JsonObject();
			for (Setting<?> setting : module.settings()) {
				if (setting.isPersistent()) {
					settings.add(setting.name, setting.save());
				}
			}
			data.add("settings", settings);
			modules.add(module.name(), data);
		}
		root.add("modules", modules);

		try {
			Files.createDirectories(directory);
			Path temp = directory.resolve("config.json.tmp");
			Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			MogsClient.LOGGER.error("Could not save the MOGS config", e);
		}
		dirty = false;
		ticksSinceDirty = 0;
	}

	public static void markDirty() {
		dirty = true;
	}

	/** Debounced autosave, called from the client tick. */
	public static void tick() {
		if (!dirty) {
			return;
		}
		if (++ticksSinceDirty >= AUTOSAVE_TICKS) {
			save();
		}
	}

	/** Resets every module, keybind, HUD position and the GUI position to the defaults and saves. */
	public static void resetAll() {
		ModuleManager.resetAll();
		guiX = -1;
		guiY = -1;
		lastCategory = Category.COMBAT;
		save();
	}
}
