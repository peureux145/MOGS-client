package mogs.util;

import mogs.module.client.ClickGuiModule;

/** MOGS colour palette: black / dark gray base, red for active state, gold used sparingly. */
public final class Theme {
	public static final int BG = 0xFF0A0A0C;
	public static final int SIDEBAR = 0xFF111114;
	public static final int CARD = 0xFF18181C;
	public static final int CARD_HOVER = 0xFF1F1F25;
	public static final int FIELD = 0xFF0F0F12;
	public static final int BORDER = 0xFF2A2A31;
	public static final int TEXT = 0xFFEDEDED;
	public static final int MUTED = 0xFF8B8B95;
	public static final int OFF = 0xFF3A3A42;
	public static final int HUD_BG = 0xB0101014;

	private Theme() {
	}

	public static int red() {
		return ClickGuiModule.accent();
	}

	public static int gold() {
		return ClickGuiModule.goldAccent();
	}
}
