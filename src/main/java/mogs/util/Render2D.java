package mogs.util;

import mogs.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/** Small 2D drawing helpers built on top of GuiGraphics (sharp rectangles only - the MOGS look). */
public final class Render2D {
	public static final int LINE = 11;

	/** One "label  value" row of a HUD panel. */
	public record Row(String label, String value, int valueColor) {
	}

	private Render2D() {
	}

	// Optional "design space" transform used by the ClickGUI: local coordinates are mapped to screen
	// coordinates here instead of through the matrix stack, so scissor clipping always stays in screen space.
	private static float originX;
	private static float originY;
	private static float factor = 1f;

	public static void setTransform(float x, float y, float scale) {
		originX = x;
		originY = y;
		factor = scale;
	}

	public static void resetTransform() {
		originX = 0f;
		originY = 0f;
		factor = 1f;
	}

	/** Clips drawing to a rectangle given in the current local coordinates. Call GuiGraphics#disableScissor to end. */
	public static void clip(GuiGraphics g, float x, float y, float w, float h) {
		g.enableScissor(Math.round(originX + x * factor), Math.round(originY + y * factor),
				Math.round(originX + (x + w) * factor), Math.round(originY + (y + h) * factor));
	}

	public static void rect(GuiGraphics g, float x, float y, float w, float h, int color) {
		g.fill(Math.round(originX + x * factor), Math.round(originY + y * factor),
				Math.round(originX + (x + w) * factor), Math.round(originY + (y + h) * factor), color);
	}

	/** Text enlarged by an extra factor (used for titles). */
	public static void scaledText(GuiGraphics g, String text, float x, float y, float extra, int color, boolean shadow) {
		g.pose().pushMatrix();
		g.pose().translate(originX + x * factor, originY + y * factor);
		g.pose().scale(factor * extra, factor * extra);
		g.drawString(Minecraft.getInstance().font, text, 0, 0, color, shadow);
		g.pose().popMatrix();
	}

	private static void draw(GuiGraphics g, String text, float x, float y, int color, boolean shadow) {
		if (factor == 1f && originX == 0f && originY == 0f) {
			g.drawString(Minecraft.getInstance().font, text, Math.round(x), Math.round(y), color, shadow);
		} else {
			scaledText(g, text, x, y, 1f, color, shadow);
		}
	}

	public static void outline(GuiGraphics g, float x, float y, float w, float h, int color, int thickness) {
		rect(g, x, y, w, thickness, color);
		rect(g, x, y + h - thickness, w, thickness, color);
		rect(g, x, y + thickness, thickness, h - thickness * 2, color);
		rect(g, x + w - thickness, y + thickness, thickness, h - thickness * 2, color);
	}

	public static void text(GuiGraphics g, String text, float x, float y, int color) {
		draw(g, text, x, y, color, false);
	}

	public static void textShadow(GuiGraphics g, String text, float x, float y, int color) {
		draw(g, text, x, y, color, true);
	}

	public static void centered(GuiGraphics g, String text, float cx, float y, int color) {
		text(g, text, cx - TextUtil.width(text) / 2f, y, color);
	}

	public static boolean inside(double mx, double my, double x, double y, double w, double h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	public static int alpha(int argb, float multiplier) {
		int a = (argb >>> 24) & 0xFF;
		int na = Math.max(0, Math.min(255, Math.round(a * multiplier)));
		return (na << 24) | (argb & 0xFFFFFF);
	}

	public static int withAlpha(int rgb, int alpha) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
	}

	public static int lerp(int from, int to, float t) {
		t = Math.max(0f, Math.min(1f, t));
		int a = Math.round(((from >>> 24) & 255) + (((to >>> 24) & 255) - ((from >>> 24) & 255)) * t);
		int r = Math.round(((from >> 16) & 255) + (((to >> 16) & 255) - ((from >> 16) & 255)) * t);
		int gr = Math.round(((from >> 8) & 255) + (((to >> 8) & 255) - ((from >> 8) & 255)) * t);
		int b = Math.round((from & 255) + ((to & 255) - (from & 255)) * t);
		return (a << 24) | (r << 16) | (gr << 8) | b;
	}

	/** Draws a HUD panel (relative to the already translated/scaled matrix) and reports its size to the module. */
	public static void rows(GuiGraphics g, Module module, String title, List<Row> rows) {
		int width = 0;
		if (title != null) {
			width = TextUtil.width(title);
		}
		for (Row row : rows) {
			int rowWidth = TextUtil.width(row.label()) + (row.value().isEmpty() ? 0 : 8 + TextUtil.width(row.value()));
			width = Math.max(width, rowWidth);
		}
		int w = width + 14;
		int h = (title != null ? 1 : 0) * LINE + rows.size() * LINE + 8;
		panel(g, module, w, h);
		float y = 4;
		if (title != null) {
			text(g, title, 8, y, Theme.gold());
			y += LINE;
		}
		for (Row row : rows) {
			text(g, row.label(), 8, y, Theme.MUTED);
			if (!row.value().isEmpty()) {
				text(g, row.value(), 8 + TextUtil.width(row.label()) + 8, y, row.valueColor());
			}
			y += LINE;
		}
		module.setHudSize(w, h);
	}

	/** Background plate with the red accent bar. */
	public static void panel(GuiGraphics g, Module module, int w, int h) {
		if (module.hudBackground()) {
			rect(g, 0, 0, w, h, Theme.HUD_BG);
			rect(g, 0, 0, 2, h, Theme.red());
		}
	}
}
