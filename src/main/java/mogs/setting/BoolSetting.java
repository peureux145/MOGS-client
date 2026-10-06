package mogs.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BoolSetting extends Setting<Boolean> {
	public BoolSetting(String name, String description, boolean defaultValue) {
		super(name, description, defaultValue);
	}

	public boolean on() {
		return value;
	}

	public void toggle() {
		value = !value;
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(value);
	}

	@Override
	public void load(JsonElement element) {
		if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
			value = element.getAsBoolean();
		}
	}
}
