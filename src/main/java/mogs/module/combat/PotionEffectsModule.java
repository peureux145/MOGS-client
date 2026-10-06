package mogs.module.combat;

import mogs.module.CombatModule;
import mogs.setting.BoolSetting;
import mogs.setting.NumberSetting;
import mogs.util.Render2D;
import mogs.util.TextUtil;
import mogs.util.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

public class PotionEffectsModule extends CombatModule {
	private record Line(String label, String time, int color) {
	}

	private final BoolSetting beneficial = add(new BoolSetting("Beneficial", "Show helpful effects.", true));
	private final BoolSetting harmful = add(new BoolSetting("Harmful", "Show harmful effects.", true));
	private final BoolSetting showAmplifier = add(new BoolSetting("Amplifier", "Show the level (II, III ...).", true));
	private final BoolSetting showDuration = add(new BoolSetting("Duration", "Show the remaining time.", true));
	private final NumberSetting maxRows = add(new NumberSetting("Max Rows", "Maximum number of effects shown.", 6, 1, 12, 1));

	public PotionEffectsModule() {
		super("Potion Effects", "Your active effects with level and remaining time.");
		initHud(300, 60);
	}

	@Override
	public void renderHud(GuiGraphics g) {
		if (mc.player == null) {
			return;
		}
		List<Line> lines = new ArrayList<>();
		for (MobEffectInstance effect : mc.player.getActiveEffects()) {
			MobEffectCategory category = effect.getEffect().value().getCategory();
			boolean good = category == MobEffectCategory.BENEFICIAL;
			boolean bad = category == MobEffectCategory.HARMFUL;
			if ((good && !beneficial.on()) || (bad && !harmful.on())) {
				continue;
			}
			String name = effect.getEffect().value().getDisplayName().getString();
			if (showAmplifier.on()) {
				name += " " + TextUtil.roman(effect.getAmplifier());
			}
			String time = effect.isInfiniteDuration() ? "inf" : TextUtil.ticksToClock(effect.getDuration());
			lines.add(new Line(name, time, bad ? Theme.red() : good ? Theme.gold() : Theme.TEXT));
			if (lines.size() >= maxRows.asInt()) {
				break;
			}
		}
		if (lines.isEmpty() && isPreview()) {
			lines.add(new Line("Strength II", "1:30", Theme.gold()));
			lines.add(new Line("Weakness", "0:20", Theme.red()));
		}
		if (lines.isEmpty()) {
			return;
		}

		int width = 0;
		for (Line line : lines) {
			int w = TextUtil.width(line.label()) + (showDuration.on() ? 10 + TextUtil.width(line.time()) : 0);
			width = Math.max(width, w);
		}
		width += 14;
		int height = lines.size() * Render2D.LINE + 8;
		Render2D.panel(g, this, width, height);
		float y = 4;
		for (Line line : lines) {
			Render2D.text(g, line.label(), 8, y, line.color());
			if (showDuration.on()) {
				Render2D.text(g, line.time(), 8 + TextUtil.width(line.label()) + 10, y, Theme.MUTED);
			}
			y += Render2D.LINE;
		}
		setHudSize(width, height);
	}
}
