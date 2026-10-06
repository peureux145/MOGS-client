package mogs.module;

import mogs.config.ConfigManager;
import mogs.gui.hud.NotificationManager;
import mogs.setting.BoolSetting;
import mogs.setting.KeybindSetting;
import mogs.setting.NumberSetting;
import mogs.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class of every MOGS module. A module owns its settings, an enabled state and a keybind,
 * and may optionally expose a movable/scalable HUD element (see {@link #initHud(int, int)}).
 */
public abstract class Module {
	private static boolean preview;

	protected final Minecraft mc = Minecraft.getInstance();

	private final String name;
	private final String description;
	private final Category category;
	private final KeybindSetting keybind = new KeybindSetting("Keybind", "Key that toggles this module.", KeybindSetting.NONE);
	private final List<Setting<?>> settings = new ArrayList<>();

	private boolean enabled;
	private boolean enabledByDefault;
	private boolean settingsOnly;

	// HUD support (null / unused unless initHud is called)
	private boolean hasHud;
	private BoolSetting hudVisible;
	private BoolSetting hudBackground;
	private NumberSetting hudScale;
	private NumberSetting hudX;
	private NumberSetting hudY;
	private int hudWidth = 90;
	private int hudHeight = 24;

	protected Module(String name, String description, Category category) {
		this.name = name;
		this.description = description;
		this.category = category;
	}

	// ---------------------------------------------------------------- identity

	public final String name() {
		return name;
	}

	public final String description() {
		return description;
	}

	public final Category category() {
		return category;
	}

	// ---------------------------------------------------------------- state

	public final boolean isEnabled() {
		return enabled;
	}

	public final void toggle() {
		setEnabled(!enabled);
	}

	public final void setEnabled(boolean value) {
		setEnabled(value, true);
	}

	/** @param announce whether the enable/disable notification should be shown */
	public final void setEnabled(boolean value, boolean announce) {
		if (value == enabled) {
			return;
		}
		enabled = value;
		if (value) {
			onEnable();
		} else {
			onDisable();
		}
		if (announce && !settingsOnly) {
			NotificationManager.moduleToggled(this);
		}
		ConfigManager.markDirty();
	}

	protected final void enabledByDefault() {
		this.enabledByDefault = true;
		this.enabled = true;
	}

	/** Modules that only hold settings (no on/off switch in the GUI). */
	protected final void settingsOnly() {
		this.settingsOnly = true;
		this.enabledByDefault = true;
		this.enabled = true;
	}

	public final boolean isSettingsOnly() {
		return settingsOnly;
	}

	public final KeybindSetting keybind() {
		return keybind;
	}

	// ---------------------------------------------------------------- settings

	protected final <S extends Setting<?>> S add(S setting) {
		settings.add(setting);
		return setting;
	}

	public final List<Setting<?>> settings() {
		return Collections.unmodifiableList(settings);
	}

	public final List<Setting<?>> visibleSettings() {
		List<Setting<?>> visible = new ArrayList<>();
		for (Setting<?> setting : settings) {
			if (setting.isVisible()) {
				visible.add(setting);
			}
		}
		return visible;
	}

	/** Restores every setting (and the keybind / enabled state) to its default. */
	public final void resetSettings() {
		for (Setting<?> setting : settings) {
			setting.reset();
		}
		keybind.reset();
		boolean shouldBeOn = enabledByDefault;
		if (enabled != shouldBeOn) {
			setEnabled(shouldBeOn, false);
		}
		onSettingsReset();
		ConfigManager.markDirty();
	}

	// ---------------------------------------------------------------- HUD

	/** Adds the shared HUD settings: visibility, background, scale and (hidden) position. */
	protected final void initHud(int defaultX, int defaultY) {
		hasHud = true;
		hudVisible = add(new BoolSetting("Visible", "Show or hide this HUD element.", true));
		hudBackground = add(new BoolSetting("Background", "Draw the dark plate behind the element.", true));
		hudScale = add(new NumberSetting("Scale", "Size of the HUD element.", 1.0, 0.5, 2.5, 0.1));
		hudX = add(new NumberSetting("hud_x", "HUD x position.", defaultX, 0, 8192, 1));
		hudY = add(new NumberSetting("hud_y", "HUD y position.", defaultY, 0, 8192, 1));
		hudX.hide();
		hudY.hide();
	}

	public final boolean hasHud() {
		return hasHud;
	}

	public final boolean hudVisible() {
		return hasHud && hudVisible.on();
	}

	public final boolean hudBackground() {
		return !hasHud || hudBackground.on();
	}

	public final float hudScale() {
		return hasHud ? hudScale.asFloat() : 1f;
	}

	public final NumberSetting hudScaleSetting() {
		return hudScale;
	}

	public final int hudX() {
		return hudX.asInt();
	}

	public final int hudY() {
		return hudY.asInt();
	}

	public final void setHudPosition(int x, int y) {
		hudX.set((double) x);
		hudY.set((double) y);
		ConfigManager.markDirty();
	}

	public final void setHudSize(int width, int height) {
		this.hudWidth = width;
		this.hudHeight = height;
	}

	public final int hudWidth() {
		return hudWidth;
	}

	public final int hudHeight() {
		return hudHeight;
	}

	/** True while the HUD editor draws sample content so that elements can be positioned. */
	public static boolean isPreview() {
		return preview;
	}

	public static void setPreview(boolean value) {
		preview = value;
	}

	// ---------------------------------------------------------------- hooks

	protected void onEnable() {
	}

	protected void onDisable() {
	}

	protected void onSettingsReset() {
	}

	/** Called every client tick while the module is enabled and a world is loaded. */
	public void onTick() {
	}

	/** Draws the HUD element. The matrix is already translated to the element position and scaled. */
	public void renderHud(GuiGraphics graphics) {
	}

	/** Free-form 2D drawing in unscaled GUI coordinates (used by crosshair / highlight overlays). */
	public void onRender2D(GuiGraphics graphics, float partialTick) {
	}

	/** Called when the player manually attacks an entity. */
	public void onAttackEntity(Entity target) {
	}
}
