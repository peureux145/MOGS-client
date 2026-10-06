package mogs.module.misc;

import mogs.module.Category;
import mogs.module.Module;
import mogs.setting.BoolSetting;
import mogs.setting.NumberSetting;
import mogs.util.Render2D;
import mogs.util.Render2D.Row;
import mogs.util.TextUtil;
import mogs.util.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

import java.util.ArrayList;
import java.util.List;

public class CoordinatesModule extends Module {
	private final NumberSetting decimals = add(new NumberSetting("Decimals",
			"Digits after the comma.", 1, 0, 2, 1));
	private final BoolSetting showFacing = add(new BoolSetting("Facing",
			"Show the compass direction you are looking at.", true));

	public CoordinatesModule() {
		super("Coordinates", "Your position and facing direction.", Category.MISC);
		initHud(140, 6);
	}

	@Override
	public void renderHud(GuiGraphics g) {
		LocalPlayer player = mc.player;
		if (player == null) {
			return;
		}
		int d = decimals.asInt();
		List<Row> rows = new ArrayList<>();
		rows.add(new Row("XYZ", TextUtil.fixed(player.getX(), d) + " " + TextUtil.fixed(player.getY(), d)
				+ " " + TextUtil.fixed(player.getZ(), d), Theme.TEXT));
		if (showFacing.on()) {
			rows.add(new Row("Facing", TextUtil.capitalize(player.getDirection().getName()), Theme.TEXT));
		}
		Render2D.rows(g, this, null, rows);
	}
}
