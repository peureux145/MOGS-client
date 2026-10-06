package mogs.module.combat;

import mogs.module.CombatModule;
import mogs.setting.ModeSetting;
import mogs.util.Render2D;
import mogs.util.Theme;
import net.minecraft.client.gui.GuiGraphics;

/** Shows how charged your next attack is. Purely informational: you still time every swing yourself. */
public class AttackCooldownModule extends CombatModule {
	private final ModeSetting display = add(new ModeSetting("Display",
			"How the cooldown is shown.", "Bar + Percent", "Bar", "Percent", "Text", "Bar + Percent"));

	public AttackCooldownModule() {
		super("Attack Cooldown", "Shows your current attack charge.");
		initHud(6, 150);
	}

	@Override
	public void renderHud(GuiGraphics g) {
		if (mc.player == null) {
			return;
		}
		float charge = Math.max(0f, Math.min(1f, mc.player.getAttackStrengthScale(0f)));
		boolean ready = charge >= 1f;
		boolean bar = display.is("Bar") || display.is("Bar + Percent");
		boolean percent = display.is("Percent") || display.is("Bar + Percent");
		boolean text = display.is("Text");

		int width = 96;
		int height = 8 + ((percent || text) ? Render2D.LINE : 0) + (bar ? 8 : 0);
		Render2D.panel(g, this, width, height);

		float y = 4;
		if (percent || text) {
			String label = text ? (ready ? "Ready" : "Charging") : "Cooldown " + Math.round(charge * 100) + "%";
			Render2D.text(g, label, 8, y, ready ? Theme.gold() : Theme.TEXT);
			y += Render2D.LINE;
		}
		if (bar) {
			Render2D.rect(g, 8, y + 1, width - 16, 4, Theme.FIELD);
			Render2D.rect(g, 8, y + 1, (width - 16) * charge, 4, ready ? Theme.gold() : Theme.red());
		}
		setHudSize(width, height);
	}
}
