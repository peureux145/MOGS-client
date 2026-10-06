package mogs.module.combat;

import mogs.module.CombatModule;
import mogs.setting.BoolSetting;
import mogs.setting.ModeSetting;
import mogs.util.Colors;
import mogs.util.Render2D;
import mogs.util.TextUtil;
import mogs.util.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class WeaponDurabilityModule extends CombatModule {
	private final ModeSetting style = add(new ModeSetting("Style", "How the durability is shown.",
			"Icon + Bar", "Text", "Bar", "Icon", "Icon + Bar"));
	private final BoolSetting showPercent = add(new BoolSetting("Percent", "Show a percentage instead of exact values.", false));

	public WeaponDurabilityModule() {
		super("Weapon Durability", "Durability of the item in your main hand.");
		initHud(6, 214);
	}

	@Override
	public void renderHud(GuiGraphics g) {
		if (mc.player == null) {
			return;
		}
		ItemStack stack = mc.player.getMainHandItem();
		boolean sample = false;
		if (!stack.isDamageableItem()) {
			if (!isPreview()) {
				return;
			}
			stack = new ItemStack(Items.DIAMOND_SWORD);
			sample = true;
		}

		int max = stack.getMaxDamage();
		int left = max - (sample ? 0 : stack.getDamageValue());
		float fraction = left / (float) max;
		String text = showPercent.on() ? Math.round(fraction * 100) + "%" : left + "/" + max;

		boolean icon = style.is("Icon") || style.is("Icon + Bar");
		boolean bar = style.is("Bar") || style.is("Icon + Bar");
		boolean showText = true;

		int offset = icon ? 26 : 8;
		int contentWidth = Math.max(TextUtil.width(text), bar ? 52 : 0);
		int width = offset + contentWidth + 8;
		int height = icon ? 24 : 8 + (showText ? Render2D.LINE : 0) + (bar ? 6 : 0);

		Render2D.panel(g, this, width, height);
		if (icon) {
			g.renderItem(stack, 5, (height - 16) / 2);
		}
		float y = icon ? (bar ? 4 : 8) : 4;
		if (showText) {
			Render2D.text(g, text, offset, y, Colors.status(fraction));
			y += Render2D.LINE;
		}
		if (bar) {
			Render2D.rect(g, offset, y + (icon ? 0 : 1), contentWidth, 3, Theme.FIELD);
			Render2D.rect(g, offset, y + (icon ? 0 : 1), contentWidth * fraction, 3, Colors.status(fraction) == Theme.TEXT
					? Theme.red() : Colors.status(fraction));
		}
		setHudSize(width, height);
	}
}
