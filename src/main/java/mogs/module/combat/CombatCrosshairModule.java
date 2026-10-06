package mogs.module.combat;

import mogs.module.CombatModule;
import mogs.setting.BoolSetting;
import mogs.setting.ColorSetting;
import mogs.setting.ModeSetting;
import mogs.setting.NumberSetting;
import mogs.util.Render2D;
import mogs.util.TextUtil;
import mogs.util.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;

/** Extra information drawn around the vanilla crosshair (attack charge, distance to the entity you are aiming at). */
public class CombatCrosshairModule extends CombatModule {
	private static final int SEGMENTS = 40;

	private final ModeSetting style = add(new ModeSetting("Style", "How the attack charge is drawn.",
			"Ring", "Ring", "Bar", "Percent"));
	private final NumberSetting size = add(new NumberSetting("Size", "Radius / width in pixels.", 11, 6, 30, 1));
	private final NumberSetting opacity = add(new NumberSetting("Opacity", "Opacity in percent.", 85, 10, 100, 5));
	private final ColorSetting color = add(new ColorSetting("Color", "Colour of the charge indicator.", 0xD1202F));
	private final BoolSetting showTrack = add(new BoolSetting("Track", "Draw the empty part of the ring / bar.", true));
	private final BoolSetting onlyWhenCharging = add(new BoolSetting("Only While Charging",
			"Hide the indicator when your attack is fully charged.", false));
	private final BoolSetting showDistance = add(new BoolSetting("Target Distance",
			"Show the distance to the entity under the crosshair.", true));

	public CombatCrosshairModule() {
		super("Combat Crosshair", "Attack charge and target distance around the crosshair.");
	}

	@Override
	public void onRender2D(GuiGraphics g, float partialTick) {
		if (mc.player == null || mc.screen != null || !mc.options.getCameraType().isFirstPerson()) {
			return;
		}
		float charge = Math.max(0f, Math.min(1f, mc.player.getAttackStrengthScale(0f)));
		float cx = g.guiWidth() / 2f;
		float cy = g.guiHeight() / 2f;
		float radius = size.asFloat();
		int alpha = Math.round(opacity.asFloat() * 2.55f);
		int fill = color.withAlpha(alpha);
		int track = Render2D.withAlpha(Theme.BORDER, Math.round(alpha * 0.7f));
		boolean full = charge >= 1f;

		if (!(onlyWhenCharging.on() && full)) {
			switch (style.get()) {
				case "Bar" -> {
					float w = radius * 2f;
					float y = cy + 10f;
					if (showTrack.on()) {
						Render2D.rect(g, cx - radius, y, w, 2, track);
					}
					Render2D.rect(g, cx - radius, y, w * charge, 2, fill);
				}
				case "Percent" -> Render2D.centered(g, Math.round(charge * 100) + "%", cx, cy + 10f, fill);
				default -> {
					int lit = Math.round(SEGMENTS * charge);
					for (int i = 0; i < SEGMENTS; i++) {
						boolean on = i < lit;
						if (!on && !showTrack.on()) {
							continue;
						}
						double angle = -Math.PI / 2 + 2 * Math.PI * i / SEGMENTS;
						float x = cx + (float) Math.cos(angle) * radius;
						float y = cy + (float) Math.sin(angle) * radius;
						Render2D.rect(g, x - 1, y - 1, 2, 2, on ? fill : track);
					}
				}
			}
		}

		if (showDistance.on()) {
			Entity target = mc.crosshairPickEntity;
			if (target != null) {
				String text = TextUtil.fixed(mc.player.distanceTo(target), 1) + "m";
				float offset = style.is("Ring") ? radius + 6f : 22f;
				Render2D.centered(g, text, cx, cy + offset, Render2D.withAlpha(0xFFFFFF, alpha));
			}
		}
	}
}
