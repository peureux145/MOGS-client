package mogs.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

public class ModeSetting extends Setting<String> {
	public final List<String> modes;

	public ModeSetting(String name, String description, String defaultValue, String... modes) {
		super(name, description, defaultValue);
		this.modes = List.of(modes);
		if (!this.modes.contains(defaultValue)) {
			throw new IllegalArgumentException("Default mode '" + defaultValue + "' is not part of " + this.modes);
		}
	}

	public boolean is(String mode) {
		return value.equals(mode);
	}

	public int index() {
		return Math.max(0, modes.indexOf(value));
	}

	public void next() {
		value = modes.get((index() + 1) % modes.size());
	}

	public void previous() {
		value = modes.get((index() - 1 + modes.size()) % modes.size());
	}

	@Override
	protected String sanitize(String newValue) {
		return modes.contains(newValue) ? newValue : value;
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(value);
	}

	@Override
	public void load(JsonElement element) {
		if (element != null && element.isJsonPrimitive() && modes.contains(element.getAsString())) {
			value = element.getAsString();
		}
	}
}
