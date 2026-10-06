package mogs.module.misc;

import mogs.module.Category;
import mogs.module.Module;
import mogs.setting.BoolSetting;
import mogs.setting.ColorSetting;
import mogs.setting.ModeSetting;
import mogs.setting.NumberSetting;
import mogs.util.Render2D;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/** Draws your own crosshair and (optionally) hides the vanilla one. */
public class CustomCrosshairModule extends Module {
	private static CustomCrosshairModule instance;

	private final ModeSetting style = add(new ModeSetting("Style", "Shape of the crosshair.",
			"Cross", "Cross", "Dot", "Circle", "Square"));
	private final NumberSetting size = add(new NumberSetting("Size", "Length of the lines in pixels.", 6, 1, 20, 1));
	private final NumberSetting gap = add(new NumberSetting("Gap", "Empty space in the centre.", 3, 0, 12, 1));
	private final NumberSetting thickness = add(new NumberSetting("Thickness", "Line width in pixels.", 1, 1, 4, 1));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", "Opacity in percent.", 100, 10, 100, 5));
	private final ColorSetting color = add(new ColorSetting("Color", "Crosshair colour.", 0xFFFFFF));
	private final BoolSetting outline = add(new BoolSetting("Outline", "Dark outline so it stays visible.", true));
	private final BoolSetting hideVanilla = add(new BoolSetting("Hide Vanilla", "Hide the default crosshair.", true));

	public CustomCrosshairModule() {
		super("CustomCrosshair", "Draws a custom crosshair.", Category.MISC);
		instance = this;
	}

	/** Used by the HUD hook that replaces the vanilla crosshair. */
	public static boolean hidesVanilla() {
		return instance != null && instance.isEnabled() && instance.hideVanilla.on();
	}

	@Override
	public void onRender2D(GuiGraphics g, float partialTick) {
		if (mc.player == null || mc.screen != null || !mc.options.getCameraType().isFirstPerson()) {
			return;
		}
		float cx = g.guiWidth() / 2f;
		float cy = g.guiHeight() / 2f;
		int t = thickness.asInt();
		int len = size.asInt();
		int gp = gap.asInt();
		float half = t / 2f;
		int alpha = Math.round(opacity.asFloat() * 2.55f);

		List<float[]> boxes = new ArrayList<>();
		switch (style.get()) {
			case "Dot" -> {
				float d = t + 1f;
				boxes.add(new float[]{cx - d / 2f, cy - d / 2f, d, d});
			}
			case "Circle" -> {
				float radius = len + gp;
				for (int i = 0; i < 28; i++) {
					double angle = 2 * Math.PI * i / 28;
					boxes.add(new float[]{cx + (float) Math.cos(angle) * radius - half, cy + (float) Math.sin(angle) * radius - half, t, t});
				}
			}
			case "Square" -> {
				float r = len + gp;
				boxes.add(new float[]{cx - r, cy - r, 2 * r, t});
				boxes.add(new float[]{cx - r, cy + r - t, 2 * r, t});
				boxes.add(new float[]{cx - r, cy - r, t, 2 * r});
				boxes.add(new float[]{cx + r - t, cy - r, t, 2 * r});
			}
			default -> {
				boxes.add(new float[]{cx - gp - len, cy - half, len, t});
				boxes.add(new float[]{cx + gp, cy - half, len, t});
				boxes.add(new float[]{cx - half, cy - gp - len, t, len});
				boxes.add(new float[]{cx - half, cy + gp, t, len});
			}
		}

		if (outline.on()) {
			int shade = Render2D.withAlpha(0x000000, Math.round(alpha * 0.8f));
			for (float[] b : boxes) {
				Render2D.rect(g, b[0] - 1, b[1] - 1, b[2] + 2, b[3] + 2, shade);
			}
		}
		int fill = color.withAlpha(alpha);
		for (float[] b : boxes) {
			Render2D.rect(g, b[0], b[1], b[2], b[3], fill);
		}
	}
}
