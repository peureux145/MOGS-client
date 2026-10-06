package mogs.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Projects a world-space box to GUI-scaled screen coordinates (first person only).
 * The projection uses the FOV option, so it can drift slightly while a dynamic FOV effect
 * (sprinting, speed potions) is active. This is purely visual and never influences gameplay.
 */
public final class Projection {
	private Projection() {
	}

	/** Returns {minX, minY, maxX, maxY} in GUI pixels, or null if the box is not fully in front of the camera. */
	public static float[] project(Minecraft mc, AABB box, float partialTick, int guiWidth, int guiHeight) {
		LocalPlayer player = mc.player;
		if (player == null) {
			return null;
		}
		Vec3 eye = player.getEyePosition(partialTick);
		Vec3 look = player.getViewVector(partialTick).normalize();
		Vec3 right = look.cross(new Vec3(0, 1, 0));
		if (right.lengthSqr() < 1.0E-6) {
			return null;
		}
		right = right.normalize();
		Vec3 up = right.cross(look);

		double tanHalf = Math.tan(Math.toRadians(mc.options.fov().get()) / 2.0);
		double aspect = guiWidth / (double) guiHeight;

		float minX = Float.MAX_VALUE;
		float minY = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE;
		float maxY = -Float.MAX_VALUE;
		for (int i = 0; i < 8; i++) {
			double cx = (i & 1) == 0 ? box.minX : box.maxX;
			double cy = (i & 2) == 0 ? box.minY : box.maxY;
			double cz = (i & 4) == 0 ? box.minZ : box.maxZ;
			Vec3 d = new Vec3(cx, cy, cz).subtract(eye);
			double depth = d.dot(look);
			if (depth < 0.05) {
				return null;
			}
			float sx = (float) (guiWidth / 2.0 + (d.dot(right) / depth) / (tanHalf * aspect) * guiWidth / 2.0);
			float sy = (float) (guiHeight / 2.0 - (d.dot(up) / depth) / tanHalf * guiHeight / 2.0);
			minX = Math.min(minX, sx);
			minY = Math.min(minY, sy);
			maxX = Math.max(maxX, sx);
			maxY = Math.max(maxY, sy);
		}
		return new float[]{minX, minY, maxX, maxY};
	}
}
