package mogs.util;

public final class Colors {
	private Colors() {
	}

	/** Colour for a 0..1 "how healthy is this" value: white when fine, gold when getting low, red when critical. */
	public static int status(float fraction) {
		if (fraction > 0.5f) {
			return Theme.TEXT;
		}
		if (fraction > 0.25f) {
			return Theme.gold();
		}
		return Theme.red();
	}
}
