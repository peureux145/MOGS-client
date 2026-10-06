package mogs.module.combat;

import mogs.module.CombatModule;
import mogs.setting.ActionSetting;
import mogs.setting.BoolSetting;
import mogs.util.CombatTracker;
import mogs.util.Render2D;
import mogs.util.Render2D.Row;
import mogs.util.TextUtil;
import mogs.util.Theme;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/** Session statistics of the hits and misses you performed yourself. */
public class HitStatisticsModule extends CombatModule {
	private final BoolSetting showStreak = add(new BoolSetting("Streak", "Show current and best hit streak.", true));
	private final ActionSetting reset = add(new ActionSetting("Reset Statistics",
			"Set hits, misses and streaks back to zero.", CombatTracker::resetStats));

	public HitStatisticsModule() {
		super("Hit Statistics", "Hits, misses and accuracy of your swings this session.");
		initHud(6, 124);
	}

	@Override
	public void renderHud(GuiGraphics g) {
		List<Row> rows = new ArrayList<>();
		rows.add(new Row("Hits", Integer.toString(CombatTracker.hits()), Theme.TEXT));
		rows.add(new Row("Misses", Integer.toString(CombatTracker.misses()), Theme.TEXT));
		rows.add(new Row("Accuracy", TextUtil.fixed(CombatTracker.accuracy(), 1) + "%", Theme.gold()));
		if (showStreak.on()) {
			rows.add(new Row("Streak", CombatTracker.streak() + " (best " + CombatTracker.bestStreak() + ")", Theme.TEXT));
		}
		Render2D.rows(g, this, "Hit Stats", rows);
	}
}
