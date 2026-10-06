package mogs.module.combat;

import mogs.module.CombatModule;
import mogs.setting.ActionSetting;
import mogs.setting.BoolSetting;
import mogs.setting.NumberSetting;
import mogs.util.CombatTracker;
import mogs.util.Render2D;
import mogs.util.Render2D.Row;
import mogs.util.TextUtil;
import mogs.util.Theme;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public class CpsCounterModule extends CombatModule {
	private final NumberSetting window = add(new NumberSetting("Window",
			"Seconds the click rate is averaged over.", 1.0, 0.5, 5.0, 0.5));
	private final BoolSetting showRight = add(new BoolSetting("Right Click", "Also show right-click CPS.", true));
	private final BoolSetting showPeak = add(new BoolSetting("Peak", "Show the highest CPS reached.", false));
	private final ActionSetting resetPeak = add(new ActionSetting("Reset Peak", "Clear the peak values.", this::clearPeaks));

	private double peakLeft;
	private double peakRight;

	public CpsCounterModule() {
		super("CPS Counter", "Left and right clicks per second.");
		initHud(6, 96);
	}

	private void clearPeaks() {
		peakLeft = 0;
		peakRight = 0;
	}

	@Override
	protected void onEnable() {
		clearPeaks();
	}

	@Override
	protected void onSettingsReset() {
		clearPeaks();
	}

	@Override
	public void onTick() {
		peakLeft = Math.max(peakLeft, CombatTracker.leftCps(window.asDouble()));
		peakRight = Math.max(peakRight, CombatTracker.rightCps(window.asDouble()));
	}

	@Override
	public void renderHud(GuiGraphics g) {
		double w = window.asDouble();
		List<Row> rows = new ArrayList<>();
		rows.add(new Row("LMB", TextUtil.fixed(CombatTracker.leftCps(w), 1), Theme.TEXT));
		if (showRight.on()) {
			rows.add(new Row("RMB", TextUtil.fixed(CombatTracker.rightCps(w), 1), Theme.TEXT));
		}
		if (showPeak.on()) {
			String peak = showRight.on()
					? TextUtil.fixed(peakLeft, 1) + " | " + TextUtil.fixed(peakRight, 1)
					: TextUtil.fixed(peakLeft, 1);
			rows.add(new Row("Peak", peak, Theme.gold()));
		}
		Render2D.rows(g, this, "CPS", rows);
	}
}
