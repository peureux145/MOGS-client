package mogs.module.visuals;

import mogs.MogsClient;
import mogs.module.Category;
import mogs.module.Module;
import mogs.setting.BoolSetting;
import mogs.util.Render2D;
import mogs.util.TextUtil;
import mogs.util.Theme;
import net.minecraft.client.gui.GuiGraphics;

public class WatermarkModule extends Module {
	private final BoolSetting showVersion = add(new BoolSetting("Show Version",
			"Show the Minecraft version under the name.", true));

	public WatermarkModule() {
		super("Watermark", "MOGS name and Minecraft version on your HUD.", Category.VISUALS);
		initHud(6, 6);
		enabledByDefault();
	}

	@Override
	public void renderHud(GuiGraphics g) {
		String title = MogsClient.NAME;
		String sub = MogsClient.SUBTITLE;
		int titleWidth = Math.round(TextUtil.width(title) * 1.5f);
		int width = Math.max(titleWidth, showVersion.on() ? TextUtil.width(sub) : 0) + 14;
		int height = 18 + (showVersion.on() ? Render2D.LINE : 0) + 2;

		Render2D.panel(g, this, width, height);
		g.pose().pushMatrix();
		g.pose().translate(8f, 4f);
		g.pose().scale(1.5f, 1.5f);
		Render2D.textShadow(g, title, 0, 0, Theme.gold());
		g.pose().popMatrix();
		if (showVersion.on()) {
			Render2D.text(g, sub, 8, 18, Theme.MUTED);
		}
		setHudSize(width, height);
	}
}
