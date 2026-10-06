package mogs.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

/** A button in the settings panel. It holds no value and is never saved. */
public class ActionSetting extends Setting<Boolean> {
	private final Runnable action;

	public ActionSetting(String name, String description, Runnable action) {
		super(name, description, Boolean.FALSE);
		this.action = action;
	}

	public void run() {
		action.run();
	}

	@Override
	public boolean isPersistent() {
		return false;
	}

	@Override
	public void reset() {
		// nothing to reset
	}

	@Override
	public JsonElement save() {
		return JsonNull.INSTANCE;
	}

	@Override
	public void load(JsonElement element) {
		// nothing to load
	}
}
