package mogs.module.misc;

import mogs.MogsClient;
import mogs.gui.hud.NotificationManager;
import mogs.module.Category;
import mogs.module.Module;
import mogs.setting.KeybindSetting;
import mogs.setting.ModeSetting;
import mogs.util.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

/** Copies the coordinates of the block you are looking at to the clipboard with one key. */
public class CoordSnapperModule extends Module {
	private final KeybindSetting copyKey = add(new KeybindSetting("Copy Key",
			"Press while looking at a block to copy its coordinates.", GLFW.GLFW_KEY_K));
	private final ModeSetting format = add(new ModeSetting("Format", "How the coordinates are written.",
			"X Y Z", "X Y Z", "X, Y, Z", "/tp X Y Z"));

	private boolean wasDown;

	public CoordSnapperModule() {
		super("CoordSnapper", "Copies looked-at coordinates with one key.", Category.MISC);
	}

	@Override
	protected void onEnable() {
		wasDown = false;
	}

	@Override
	public void onTick() {
		boolean down = mc.screen == null && Compat.isKeyDown(copyKey.key());
		if (down && !wasDown) {
			copy();
		}
		wasDown = down;
	}

	private void copy() {
		if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
			NotificationManager.push(MogsClient.NAME, "Look at a block first", 2000, 1);
			return;
		}
		BlockPos pos = hit.getBlockPos();
		String text = switch (format.get()) {
			case "X, Y, Z" -> pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
			case "/tp X Y Z" -> "/tp " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
			default -> pos.getX() + " " + pos.getY() + " " + pos.getZ();
		};
		mc.keyboardHandler.setClipboard(text);
		NotificationManager.push(MogsClient.NAME, "Copied " + text, 2500, 1);
	}
}
