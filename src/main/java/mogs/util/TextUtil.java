package mogs.util;

import net.minecraft.client.Minecraft;

import java.util.Locale;

public final class TextUtil {
	private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

	private TextUtil() {
	}

	public static int width(String text) {
		return Minecraft.getInstance().font.width(text);
	}

	/** Shortens text with ".." so that it fits into maxWidth pixels. */
	public static String fit(String text, int maxWidth) {
		if (width(text) <= maxWidth) {
			return text;
		}
		String cut = text;
		while (cut.length() > 1 && width(cut + "..") > maxWidth) {
			cut = cut.substring(0, cut.length() - 1);
		}
		return cut + "..";
	}

	/** Amplifier 0 -> "I", 1 -> "II", ... */
	public static String roman(int amplifier) {
		int level = amplifier + 1;
		return level >= 1 && level <= ROMAN.length ? ROMAN[level - 1] : Integer.toString(level);
	}

	public static String ticksToClock(int ticks) {
		int seconds = Math.max(0, ticks / 20);
		return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
	}

	public static String capitalize(String text) {
		if (text.isEmpty()) {
			return text;
		}
		return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
	}

	public static String fixed(double value, int decimals) {
		return String.format(Locale.ROOT, "%." + Math.max(0, decimals) + "f", value);
	}
}
