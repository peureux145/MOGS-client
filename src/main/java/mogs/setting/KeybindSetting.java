package mogs.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** A GLFW key code, or -1 for "unbound". */
public class KeybindSetting extends Setting<Integer> {
	public static final int NONE = -1;

	public KeybindSetting(String name, String description, int defaultKey) {
		super(name, description, defaultKey);
	}

	public int key() {
		return value;
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(value);
	}

	@Override
	public void load(JsonElement element) {
		if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
			value = element.getAsInt();
		}
	}
}
