package mogs.module.client;

import mogs.module.Category;
import mogs.module.Module;
import mogs.setting.BoolSetting;
import mogs.setting.ColorSetting;
import mogs.setting.KeybindSetting;
import mogs.setting.NumberSetting;

/** Theme and behaviour settings of the ClickGUI itself. */
public class ClickGuiModule extends Module {
	private static ClickGuiModule instance;

	private final KeybindSetting openKey = add(new KeybindSetting("GUI Key",
			"Hold Esc and press this key to open the menu. Press it alone to close.", org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT));
	private final ColorSetting accent = add(new ColorSetting("Accent Color",
			"Active modules, toggles, selected buttons and highlights.", 0xD1202F));
	private final ColorSetting gold = add(new ColorSetting("Gold Accent",
			"Titles and premium accents. Used sparingly.", 0xD4AF37));
	private final NumberSetting scale = add(new NumberSetting("GUI Scale",
			"Size of the ClickGUI window.", 1.0, 0.75, 1.5, 0.05));
	private final NumberSetting animationSpeed = add(new NumberSetting("Animation Speed",
			"How fast hover, toggle and expand animations run.", 1.0, 0.5, 2.0, 0.25));
	private final BoolSetting animations = add(new BoolSetting("Animations",
			"Smooth hover, toggle and expand animations.", true));
	private final BoolSetting tooltips = add(new BoolSetting("Tooltips",
			"Show a tooltip when hovering a module or setting.", true));

	public ClickGuiModule() {
		super("ClickGUI", "Theme, scale and behaviour of this menu.", Category.CLIENT);
		settingsOnly();
		instance = this;
	}

	public static int openKey() {
		return instance == null ? org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT : instance.openKey.key();
	}

	public static int accent() {
		return instance == null ? 0xFFD1202F : instance.accent.argb();
	}

	public static int goldAccent() {
		return instance == null ? 0xFFD4AF37 : instance.gold.argb();
	}

	public static float guiScale() {
		return instance == null ? 1f : instance.scale.asFloat();
	}

	public static boolean animationsEnabled() {
		return instance == null || instance.animations.on();
	}

	public static float animationSpeed() {
		return instance == null ? 1f : instance.animationSpeed.asFloat();
	}

	public static boolean tooltipsEnabled() {
		return instance == null || instance.tooltips.on();
	}
}
