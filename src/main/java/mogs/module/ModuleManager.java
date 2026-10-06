package mogs.module;

import mogs.config.ConfigManager;
import mogs.gui.hud.HudEditorScreen;
import mogs.module.client.ClickGuiModule;
import mogs.module.client.NotificationsModule;
import mogs.module.combat.ArmorDurabilityModule;
import mogs.module.combat.AttackCooldownModule;
import mogs.module.combat.CombatCrosshairModule;
import mogs.module.combat.CombatHudModule;
import mogs.module.combat.CombatNotificationsModule;
import mogs.module.combat.CpsCounterModule;
import mogs.module.combat.HitParticlesModule;
import mogs.module.combat.HitStatisticsModule;
import mogs.module.combat.PotionEffectsModule;
import mogs.module.combat.TargetHighlightModule;
import mogs.module.combat.TargetInfoModule;
import mogs.module.combat.TotemCounterModule;
import mogs.module.combat.WeaponDurabilityModule;
import mogs.module.misc.CoordSnapperModule;
import mogs.module.misc.CoordinatesModule;
import mogs.module.misc.CustomCrosshairModule;
import mogs.module.misc.WeatherNotifierModule;
import mogs.module.misc.SessionClockModule;
import mogs.module.render.FullBrightModule;
import mogs.module.render.HeldItemModule;
import mogs.module.visuals.WatermarkModule;
import mogs.util.Compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ModuleManager {
	private static final List<Module> MODULES = new ArrayList<>();
	private static final Map<Module, Boolean> KEY_STATE = new HashMap<>();

	private ModuleManager() {
	}

	/** Registers every module. Adding a new module to MOGS means adding one line here. */
	public static void init() {
		MODULES.clear();

		// Combat
		register(new CombatHudModule());
		register(new CpsCounterModule());
		register(new AttackCooldownModule());
		register(new TargetInfoModule());
		register(new TargetHighlightModule());
		register(new HitStatisticsModule());
		register(new CombatNotificationsModule());
		register(new WeaponDurabilityModule());
		register(new ArmorDurabilityModule());
		register(new TotemCounterModule());
		register(new PotionEffectsModule());
		register(new CombatCrosshairModule());
		register(new HitParticlesModule());

		// Misc
		register(new CoordinatesModule());
		register(new SessionClockModule());
		register(new CustomCrosshairModule());
		register(new CoordSnapperModule());
		register(new WeatherNotifierModule());

		// Render
		register(new HeldItemModule());
		register(new FullBrightModule());

		// Visuals
		register(new WatermarkModule());

		// Client
		register(new ClickGuiModule());
		register(new NotificationsModule());
	}

	public static void register(Module module) {
		for (Module existing : MODULES) {
			if (existing.name().equalsIgnoreCase(module.name())) {
				throw new IllegalStateException("Duplicate module name: " + module.name());
			}
		}
		MODULES.add(module);
	}

	public static List<Module> all() {
		return Collections.unmodifiableList(MODULES);
	}

	public static List<Module> byCategory(Category category) {
		List<Module> result = new ArrayList<>();
		for (Module module : MODULES) {
			if (module.category() == category) {
				result.add(module);
			}
		}
		return result;
	}

	public static List<Module> search(String query) {
		String needle = query.toLowerCase(Locale.ROOT).trim();
		List<Module> result = new ArrayList<>();
		for (Module module : MODULES) {
			if (needle.isEmpty()
					|| module.name().toLowerCase(Locale.ROOT).contains(needle)
					|| module.description().toLowerCase(Locale.ROOT).contains(needle)) {
				result.add(module);
			}
		}
		return result;
	}

	public static <T extends Module> T get(Class<T> type) {
		for (Module module : MODULES) {
			if (type.isInstance(module)) {
				return type.cast(module);
			}
		}
		return null;
	}

	public static Module byName(String name) {
		for (Module module : MODULES) {
			if (module.name().equals(name)) {
				return module;
			}
		}
		return null;
	}

	public static void resetAll() {
		for (Module module : MODULES) {
			module.resetSettings();
		}
	}

	// ---------------------------------------------------------------- per-tick / per-frame dispatch

	public static void tick(Minecraft mc) {
		for (Module module : MODULES) {
			int key = module.keybind().key();
			boolean down = key >= 0 && Compat.isKeyDown(key);
			boolean was = KEY_STATE.getOrDefault(module, false);
			if (mc.screen == null && down && !was && !module.isSettingsOnly()) {
				module.toggle();
			}
			KEY_STATE.put(module, down);

			if (module.isEnabled()) {
				module.onTick();
			}
		}
		ConfigManager.tick();
	}

	public static void renderHud(GuiGraphics graphics, float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui || mc.screen instanceof HudEditorScreen) {
			return;
		}
		for (Module module : MODULES) {
			if (!module.isEnabled()) {
				continue;
			}
			renderHudElement(graphics, module);
			module.onRender2D(graphics, partialTick);
		}
	}

	/** Draws one module's HUD element at its stored position and scale. */
	public static void renderHudElement(GuiGraphics graphics, Module module) {
		if (!module.hasHud() || !module.hudVisible()) {
			return;
		}
		graphics.pose().pushMatrix();
		graphics.pose().translate((float) module.hudX(), (float) module.hudY());
		graphics.pose().scale(module.hudScale(), module.hudScale());
		module.renderHud(graphics);
		graphics.pose().popMatrix();
	}

	public static void onAttackEntity(Entity target) {
		for (Module module : MODULES) {
			if (module.isEnabled()) {
				module.onAttackEntity(target);
			}
		}
	}
}
