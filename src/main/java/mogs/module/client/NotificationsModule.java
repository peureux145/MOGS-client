package mogs.module.client;

import mogs.gui.hud.NotificationManager;
import mogs.module.Category;
import mogs.module.Module;
import mogs.setting.BoolSetting;
import mogs.setting.ModeSetting;
import mogs.setting.NumberSetting;

/** Controls the toast shown when a module is enabled or disabled. */
public class NotificationsModule extends Module {
	private final BoolSetting toggleAlerts = add(new BoolSetting("Toggle Alerts",
			"Show a notification when a module is enabled or disabled.", true));
	private final NumberSetting duration = add(new NumberSetting("Duration",
			"Seconds a notification stays on screen.", 2.5, 1.0, 8.0, 0.5));
	private final ModeSetting position = add(new ModeSetting("Position",
			"Screen corner for notifications.", "Top Right", NotificationManager.ANCHORS));

	public NotificationsModule() {
		super("Notifications", "Enable / disable alerts for modules.", Category.CLIENT);
		enabledByDefault();
	}

	public boolean toggleAlerts() {
		return toggleAlerts.on();
	}

	public long durationMs() {
		return (long) (duration.asDouble() * 1000.0);
	}

	public int anchor() {
		return position.index();
	}
}
