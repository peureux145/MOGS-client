package mogs.module.combat;

import mogs.module.CombatModule;
import mogs.setting.ColorSetting;
import mogs.setting.ModeSetting;
import mogs.setting.NumberSetting;
import mogs.util.Projection;
import mogs.util.Render2D;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a frame around the entity that is already under your crosshair. It never picks a target for you
 * and never moves your view; it only decorates what vanilla already considers targeted.
 */
public class TargetHighlightModule extends CombatModule {
	private final ModeSetting style = add(new ModeSetting("Style", "Shape of the highlight.",
			"Corners", "Box", "Corners", "Underline"));
	private final NumberSetting size = add(new NumberSetting("Size", "Padding around the entity in pixels.", 3, 0, 12, 1));
	private final NumberSetting thickness = add(new NumberSetting("Thickness", "Line width in pixels.", 1, 1, 4, 1));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", "Highlight opacity in percent.", 80, 10, 100, 5));
	private final ColorSetting color = add(new ColorSetting("Color", "Highlight colour.", 0xD1202F));

	public TargetHighlightModule() {
		super("Target Highlight", "Frames the entity under your crosshair.");
	}

	@Override
	public void onRender2D(GuiGraphics g, float partialTick) {
		if (mc.player == null || mc.level == null || mc.screen != null || !mc.options.getCameraType().isFirstPerson()) {
			return;
		}
		Entity target = mc.crosshairPickEntity;
		if (target == null) {
			return;
		}
		Vec3 offset = target.getPosition(partialTick).subtract(target.position());
		AABB box = target.getBoundingBox().move(offset);
		float[] b = Projection.project(mc, box, partialTick, g.guiWidth(), g.guiHeight());
		if (b == null) {
			return;
		}

		float pad = size.asFloat();
		float x = b[0] - pad;
		float y = b[1] - pad;
		float w = (b[2] - b[0]) + pad * 2;
		float h = (b[3] - b[1]) + pad * 2;
		int t = thickness.asInt();
		int argb = color.withAlpha(Math.round(opacity.asFloat() * 2.55f));

		switch (style.get()) {
			case "Box" -> Render2D.outline(g, x, y, w, h, argb, t);
			case "Underline" -> Render2D.rect(g, x, y + h, w, t, argb);
			default -> {
				float len = Math.max(4f, Math.min(14f, Math.min(w, h) * 0.3f));
				// top-left, top-right, bottom-left, bottom-right
				Render2D.rect(g, x, y, len, t, argb);
				Render2D.rect(g, x, y, t, len, argb);
				Render2D.rect(g, x + w - len, y, len, t, argb);
				Render2D.rect(g, x + w - t, y, t, len, argb);
				Render2D.rect(g, x, y + h - t, len, t, argb);
				Render2D.rect(g, x, y + h - len, t, len, argb);
				Render2D.rect(g, x + w - len, y + h - t, len, t, argb);
				Render2D.rect(g, x + w - t, y + h - len, t, len, argb);
			}
		}
	}
}
