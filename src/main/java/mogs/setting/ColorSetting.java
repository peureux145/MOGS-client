package mogs.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Stores an opaque RGB colour. Alpha is applied separately by the module that uses it. */
public class ColorSetting extends Setting<Integer> {
	/** Preset swatches shown in the GUI (kept small and on-theme). */
	public static final int[] PRESETS = {
			0xD1202F, 0xD4AF37, 0xFFFFFF, 0xB0B0B8, 0xFF7A1A, 0x3DDC84, 0x35B6FF, 0x9B5CFF
	};

	public ColorSetting(String name, String description, int rgb) {
		super(name, description, rgb & 0xFFFFFF);
	}

	@Override
	protected Integer sanitize(Integer newValue) {
		return newValue & 0xFFFFFF;
	}

	/** Opaque ARGB value. */
	public int argb() {
		return 0xFF000000 | value;
	}

	public int withAlpha(int alpha) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | value;
	}

	public float hue() {
		float[] hsb = java.awt.Color.RGBtoHSB((value >> 16) & 255, (value >> 8) & 255, value & 255, null);
		return hsb[0];
	}

	public void setHue(float hue) {
		value = java.awt.Color.HSBtoRGB(Math.max(0f, Math.min(1f, hue)), 0.85f, 1.0f) & 0xFFFFFF;
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(String.format("#%06X", value));
	}

	@Override
	public void load(JsonElement element) {
		if (element == null || !element.isJsonPrimitive()) {
			return;
		}
		try {
			String text = element.getAsString().replace("#", "");
			value = Integer.parseInt(text, 16) & 0xFFFFFF;
		} catch (NumberFormatException ignored) {
			// keep current value
		}
	}
}
