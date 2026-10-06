package mogs.module.combat;

import mogs.module.CombatModule;
import mogs.setting.BoolSetting;
import mogs.util.CombatTracker;
import mogs.util.Render2D;
import mogs.util.TextUtil;
import mogs.util.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Counts the totems you carry (inventory + off-hand). Reads the inventory each frame, so it is always current. */
public class TotemCounterModule extends CombatModule {
	private final BoolSetting hideWhenZero = add(new BoolSetting("Hide At Zero", "Hide the counter when you have none.", false));

	private ItemStack icon;

	public TotemCounterModule() {
		super("Totem Counter", "How many totems of undying you carry.");
		initHud(6, 190);
	}

	@Override
	public void renderHud(GuiGraphics g) {
		int count = CombatTracker.totems(mc);
		if (count == 0 && hideWhenZero.on() && !isPreview()) {
			return;
		}
		if (icon == null) {
			icon = new ItemStack(Items.TOTEM_OF_UNDYING);
		}
		String text = "x" + count;
		int width = 6 + 16 + 5 + TextUtil.width(text) + 8;
		int height = 24;
		Render2D.panel(g, this, width, height);
		g.renderItem(icon, 6, 4);
		Render2D.text(g, text, 27, 8, count == 0 ? Theme.red() : Theme.TEXT);
		setHudSize(width, height);
	}
}
