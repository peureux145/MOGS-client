package mogs.event;

import mogs.config.ConfigManager;
import mogs.gui.clickgui.ClickGuiScreen;
import mogs.gui.hud.NotificationManager;
import mogs.module.ModuleManager;
import mogs.module.client.ClickGuiModule;
import mogs.module.misc.CustomCrosshairModule;
import mogs.util.Compat;
import mogs.util.CombatTracker;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;

/** Wires Fabric events to the module system. Only observes: nothing here ever acts for the player. */
public final class ClientEvents {
	private static boolean openWasDown;

	private ClientEvents() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			// Open chord: hold Esc and press the GUI key (Right Shift by default). Works from in-game and from the pause screen
			// that Esc opens, so it does not clash with Lunar's own Right Shift menu.
			boolean openDown = Compat.isKeyDown(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) && Compat.isKeyDown(ClickGuiModule.openKey());
			if (openDown && !openWasDown && (mc.screen == null || mc.screen instanceof PauseScreen)) {
				mc.setScreen(new ClickGuiScreen());
			}
			openWasDown = openDown;
			if (mc.player == null || mc.level == null) {
				return;
			}
			CombatTracker.tick(mc);
			ModuleManager.tick(mc);
		});

		HudElementRegistry.attachElementAfter(
				VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath("mogs", "hud"),
				(graphics, deltaTracker) -> {
					Minecraft mc = Minecraft.getInstance();
					if (mc.options.hideGui) {
						return;
					}
					ModuleManager.renderHud(graphics, deltaTracker.getGameTimeDeltaPartialTick(false));
					NotificationManager.render(graphics);
				});

		// Lets CustomCrosshair hide the vanilla crosshair; otherwise it is drawn exactly as before.
		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (graphics, deltaTracker) -> {
			if (!CustomCrosshairModule.hidesVanilla()) {
				original.render(graphics, deltaTracker);
			}
		});

		// Fires when the local player manually attacks an entity.
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (level.isClientSide() && player == Minecraft.getInstance().player) {
				CombatTracker.onHit(entity);
				ModuleManager.onAttackEntity(entity);
			}
			return InteractionResult.PASS;
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> ConfigManager.save());
	}
}
