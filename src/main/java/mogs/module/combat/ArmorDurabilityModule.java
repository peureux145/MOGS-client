package mogs.module.combat;

import mogs.module.CombatModule;
import mogs.setting.BoolSetting;
import mogs.util.Colors;
import mogs.util.Render2D;
import mogs.util.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ArmorDurabilityModule extends CombatModule {
	private static final EquipmentSlot[] SLOTS = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	private final BoolSetting showPercent = add(new BoolSetting("Percent", "Show the remaining percentage.", true));
	private final BoolSetting showBar = add(new BoolSetting("Bar", "Show a durability bar.", true));
	private final BoolSetting showEmpty = add(new BoolSetting("Empty Slots", "Keep a row for empty armor slots.", false));

	public ArmorDurabilityModule() {
		super("Armor Durability", "Durability of your four armor pieces.");
		initHud(300, 150);
	}

	@Override
	public void renderHud(GuiGraphics g) {
		if (mc.player == null) {
			return;
		}
		List<ItemStack> pieces = new ArrayList<>();
		for (EquipmentSlot slot : SLOTS) {
			ItemStack stack = mc.player.getItemBySlot(slot);
			if (!stack.isEmpty() || showEmpty.on()) {
				pieces.add(stack);
			}
		}
		if (pieces.isEmpty() && isPreview()) {
			pieces.add(new ItemStack(Items.NETHERITE_HELMET));
			pieces.add(new ItemStack(Items.NETHERITE_CHESTPLATE));
		}
		if (pieces.isEmpty()) {
			return;
		}

		int width = 6 + 16 + 6 + (showBar.on() ? 44 : 0) + (showBar.on() && showPercent.on() ? 4 : 0)
				+ (showPercent.on() ? 26 : 0) + 6;
		int height = pieces.size() * 18 + 4;
		Render2D.panel(g, this, width, height);

		int y = 4;
		for (ItemStack stack : pieces) {
			if (!stack.isEmpty()) {
				g.renderItem(stack, 6, y);
			}
			float x = 28;
			if (!stack.isEmpty() && stack.isDamageableItem()) {
				float fraction = (stack.getMaxDamage() - stack.getDamageValue()) / (float) stack.getMaxDamage();
				int color = Colors.status(fraction);
				if (showBar.on()) {
					Render2D.rect(g, x, y + 6, 44, 3, Theme.FIELD);
					Render2D.rect(g, x, y + 6, 44 * fraction, 3, color == Theme.TEXT ? Theme.red() : color);
					x += 48;
				}
				if (showPercent.on()) {
					Render2D.text(g, Math.round(fraction * 100) + "%", x, y + 4, color);
				}
			}
			y += 18;
		}
		setHudSize(width, height);
	}
}
