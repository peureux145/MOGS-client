package mogs.module.combat;

import mogs.module.CombatModule;
import mogs.setting.BoolSetting;
import mogs.util.Colors;
import mogs.util.CombatTracker;
import mogs.util.Render2D;
import mogs.util.Render2D.Row;
import mogs.util.TextUtil;
import mogs.util.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

import java.util.ArrayList;
import java.util.List;

/** One compact panel with the most useful combat numbers. */
public class CombatHudModule extends CombatModule {
	private final BoolSetting showHealth = add(new BoolSetting("Health", "Show your health.", true));
	private final BoolSetting showCooldown = add(new BoolSetting("Attack Cooldown", "Show your attack charge.", true));
	private final BoolSetting showCps = add(new BoolSetting("CPS", "Show left / right clicks per second.", true));
	private final BoolSetting showAccuracy = add(new BoolSetting("Accuracy", "Show your hit accuracy this session.", true));
	private final BoolSetting showTotems = add(new BoolSetting("Totems", "Show how many totems you carry.", true));

	public CombatHudModule() {
		super("Combat HUD", "One compact panel with your key combat stats.");
		initHud(6, 34);
	}

	@Override
	public void renderHud(GuiGraphics g) {
		LocalPlayer player = mc.player;
		if (player == null) {
			return;
		}
		List<Row> rows = new ArrayList<>();
		if (showHealth.on()) {
			float health = player.getHealth() + player.getAbsorptionAmount();
			rows.add(new Row("Health", TextUtil.fixed(health, 1), Colors.status(player.getHealth() / player.getMaxHealth())));
		}
		if (showCooldown.on()) {
			float charge = player.getAttackStrengthScale(0f);
			rows.add(new Row("Cooldown", Math.round(charge * 100) + "%", charge >= 1f ? Theme.gold() : Theme.TEXT));
		}
		if (showCps.on()) {
			rows.add(new Row("CPS", Math.round(CombatTracker.leftCps(1.0)) + " | " + Math.round(CombatTracker.rightCps(1.0)),
					Theme.TEXT));
		}
		if (showAccuracy.on()) {
			rows.add(new Row("Accuracy", TextUtil.fixed(CombatTracker.accuracy(), 0) + "%", Theme.TEXT));
		}
		if (showTotems.on()) {
			int totems = CombatTracker.totems(mc);
			rows.add(new Row("Totems", Integer.toString(totems), totems == 0 ? Theme.red() : Theme.TEXT));
		}
		if (rows.isEmpty()) {
			rows.add(new Row("Nothing enabled", "", Theme.MUTED));
		}
		Render2D.rows(g, this, "Combat", rows);
	}
}
