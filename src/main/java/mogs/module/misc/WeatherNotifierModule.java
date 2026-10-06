package mogs.module.misc;

import mogs.MogsClient;
import mogs.gui.hud.NotificationManager;
import mogs.module.Category;
import mogs.module.Module;
import mogs.setting.BoolSetting;

/** A toast when the weather changes. */
public class WeatherNotifierModule extends Module {
	private final BoolSetting rain = add(new BoolSetting("Rain", "Notify when rain starts or stops.", true));
	private final BoolSetting thunder = add(new BoolSetting("Thunder", "Notify when a thunderstorm starts or stops.", true));

	/** -1 unknown, 0 clear, 1 rain, 2 thunderstorm */
	private int state = -1;

	public WeatherNotifierModule() {
		super("WeatherNotifier", "Themed toast when the weather changes.", Category.MISC);
	}

	@Override
	protected void onEnable() {
		state = -1;
	}

	@Override
	public void onTick() {
		if (mc.level == null) {
			state = -1;
			return;
		}
		int now = mc.level.isThundering() ? 2 : mc.level.isRaining() ? 1 : 0;
		if (state != -1 && now != state) {
			boolean allowed = ((now == 2 || state == 2) && thunder.on()) || ((now == 1 || state == 1) && rain.on());
			if (allowed) {
				String text;
				if (now == 2) {
					text = "Thunderstorm started";
				} else if (now == 1) {
					text = state == 2 ? "Thunder stopped, still raining" : "Rain started";
				} else {
					text = "Weather cleared";
				}
				NotificationManager.push(MogsClient.NAME, text, 3000, 1);
			}
		}
		state = now;
	}
}
