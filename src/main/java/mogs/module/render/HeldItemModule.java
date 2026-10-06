package mogs.module.render;

import mogs.module.Category;
import mogs.module.Module;
import mogs.setting.BoolSetting;
import mogs.util.Colors;
import mogs.util.Render2D;
import mogs.util.TextUtil;
import mogs.util.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class HeldItemModule extends Module {
	private final BoolSetting showCount = add(new BoolSetting("Count", "Show the stack size.", true));
	private final BoolSetting showDurability = add(new BoolSetting("Durability",
			"Show remaining durability when the item has any.", true));

	public HeldItemModule() {
		super("Held Item", "Icon, name, count and durability of the item in your hand.", Category.RENDER);
		initHud(140, 74);
	}

	@Override
	public void renderHud(GuiGraphics g) {
		if (mc.player == null) {
			return;
		}
		ItemStack stack = mc.player.getMainHandItem();
		if (stack.isEmpty()) {
			if (!isPreview()) {
				return;
			}
			stack = new ItemStack(Items.GOLDEN_APPLE, 3);
		}

		String name = stack.getHoverName().getString();
		String detail = "";
		int color = Theme.MUTED;
		if (showCount.on() && stack.getCount() > 1) {
			detail = "x" + stack.getCount();
		}
		if (showDurability.on() && stack.isDamageableItem()) {
			int left = stack.getMaxDamage() - stack.getDamageValue();
			detail = (detail.isEmpty() ? "" : detail + "  ") + left + "/" + stack.getMaxDamage();
			color = Colors.status(left / (float) stack.getMaxDamage());
		}

		int textWidth = Math.max(TextUtil.width(name), TextUtil.width(detail));
		int width = 6 + 16 + 6 + textWidth + 8;
		int height = 24;
		Render2D.panel(g, this, width, height);
		g.renderItem(stack, 6, 4);
		Render2D.text(g, name, 28, detail.isEmpty() ? 8 : 4, Theme.TEXT);
		if (!detail.isEmpty()) {
			Render2D.text(g, detail, 28, 14, color);
		}
		setHudSize(width, height);
	}
}
