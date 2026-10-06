package mogs.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

import java.util.Locale;

/**
 * The only place that talks to Minecraft's input layer directly, so that a version-specific
 * signature change only ever needs to be fixed here.
 */
public final class Compat {
	private Compat() {
	}

	public static boolean isKeyDown(int key) {
		if (key < 0) {
			return false;
		}
		return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key);
	}

	public static String keyName(int key) {
		if (key < 0) {
			return "NONE";
		}
		return InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString().toUpperCase(Locale.ROOT);
	}
}
