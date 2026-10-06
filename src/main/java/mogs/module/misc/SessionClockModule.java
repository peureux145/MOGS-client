package mogs.module.misc;

import mogs.module.Category;
import mogs.module.Module;
import mogs.setting.BoolSetting;
import mogs.util.Render2D;
import mogs.util.Render2D.Row;
import mogs.util.Theme;
import net.minecraft.client.gui.GuiGraphics;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SessionClockModule extends Module {
	private final BoolSetting twentyFourHour = add(new BoolSetting("24 Hour", "Use a 24 hour clock.", true));
	private final BoolSetting seconds = add(new BoolSetting("Seconds", "Show seconds on the clock.", false));
	private final BoolSetting showSession = add(new BoolSetting("Session Time",
			"Show how long Minecraft has been running.", true));

	private final long startMs = System.currentTimeMillis();

	public SessionClockModule() {
		super("Session Clock", "Local time and how long you have been playing.", Category.MISC);
		initHud(140, 40);
	}

	@Override
	public void renderHud(GuiGraphics g) {
		String pattern = (twentyFourHour.on() ? "HH:mm" : "hh:mm") + (seconds.on() ? ":ss" : "")
				+ (twentyFourHour.on() ? "" : " a");
		List<Row> rows = new ArrayList<>();
		rows.add(new Row("Time", LocalTime.now().format(DateTimeFormatter.ofPattern(pattern)), Theme.TEXT));
		if (showSession.on()) {
			long total = (System.currentTimeMillis() - startMs) / 1000L;
			String text = total >= 3600
					? String.format("%dh %02dm", total / 3600, (total % 3600) / 60)
					: String.format("%d:%02d", total / 60, total % 60);
			rows.add(new Row("Session", text, Theme.TEXT));
		}
		Render2D.rows(g, this, null, rows);
	}
}
