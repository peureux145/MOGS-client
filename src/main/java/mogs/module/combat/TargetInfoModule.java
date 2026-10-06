package mogs.module.combat;

import mogs.module.CombatModule;
import mogs.setting.BoolSetting;
import mogs.util.Colors;
import mogs.util.Render2D;
import mogs.util.Render2D.Row;
import mogs.util.TextUtil;
import mogs.util.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Information about the entity under your own crosshair (the vanilla pick result).
 * Nothing is selected, tracked or targeted automatically.
 */
public class TargetInfoModule extends CombatModule {
	private final BoolSetting showHealth = add(new BoolSetting("Health", "Show the target's health.", true));
	private final BoolSetting showArmor = add(new BoolSetting("Armor", "Show the target's armor points.", true));
	private final BoolSetting showDistance = add(new BoolSetting("Distance", "Show the distance to the target.", true));
	private final BoolSetting showEffects = add(new BoolSetting("Effects", "Show the target's status effects.", true));

	public TargetInfoModule() {
		super("Target Info", "Details about the entity under your crosshair.");
		initHud(300, 6);
	}

	@Override
	public void renderHud(GuiGraphics g) {
		Entity target = mc.crosshairPickEntity;
		if (target == null || mc.player == null) {
			if (isPreview()) {
				renderSample(g);
			}
			return;
		}

		List<Row> rows = new ArrayList<>();
		if (target instanceof LivingEntity living) {
			if (showHealth.on()) {
				rows.add(new Row("Health", TextUtil.fixed(living.getHealth(), 1) + " / " + TextUtil.fixed(living.getMaxHealth(), 0),
						Colors.status(living.getHealth() / Math.max(1f, living.getMaxHealth()))));
			}
			if (showArmor.on()) {
				rows.add(new Row("Armor", Integer.toString(living.getArmorValue()), Theme.TEXT));
			}
		}
		if (showDistance.on()) {
			rows.add(new Row("Distance", TextUtil.fixed(mc.player.distanceTo(target), 1) + "m", Theme.TEXT));
		}
		if (showEffects.on() && target instanceof LivingEntity living) {
			int shown = 0;
			for (MobEffectInstance effect : living.getActiveEffects()) {
				if (shown++ >= 5) {
					break;
				}
				String name = effect.getEffect().value().getDisplayName().getString();
				rows.add(new Row(name + " " + TextUtil.roman(effect.getAmplifier()),
						effect.isInfiniteDuration() ? "inf" : TextUtil.ticksToClock(effect.getDuration()), Theme.gold()));
			}
		}
		Render2D.rows(g, this, target.getName().getString(), rows);
	}

	private void renderSample(GuiGraphics g) {
		List<Row> rows = new ArrayList<>();
		rows.add(new Row("Health", "20.0 / 20", Theme.TEXT));
		rows.add(new Row("Armor", "20", Theme.TEXT));
		rows.add(new Row("Distance", "3.0m", Theme.TEXT));
		rows.add(new Row("Speed II", "0:30", Theme.gold()));
		Render2D.rows(g, this, "Target", rows);
	}
}
