package mogs.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting<Double> {
	public final double min;
	public final double max;
	public final double step;

	public NumberSetting(String name, String description, double defaultValue, double min, double max, double step) {
		super(name, description, defaultValue);
		this.min = min;
		this.max = max;
		this.step = step;
		this.value = sanitize(defaultValue);
	}

	@Override
	protected Double sanitize(Double newValue) {
		double v = Math.max(min, Math.min(max, newValue));
		if (step > 0) {
			v = min + Math.round((v - min) / step) * step;
			v = Math.max(min, Math.min(max, v));
			v = Math.round(v * 10000.0) / 10000.0;
		}
		return v;
	}

	public double asDouble() {
		return value;
	}

	public float asFloat() {
		return value.floatValue();
	}

	public int asInt() {
		return (int) Math.round(value);
	}

	/** Position of the current value between min and max, 0..1. */
	public double fraction() {
		return max == min ? 0 : (value - min) / (max - min);
	}

	public void setFraction(double fraction) {
		set(min + Math.max(0, Math.min(1, fraction)) * (max - min));
	}

	public String display() {
		if (step >= 1 && step == Math.floor(step)) {
			return Integer.toString(asInt());
		}
		return String.format(java.util.Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
	}

	@Override
	public JsonElement save() {
		return new JsonPrimitive(value);
	}

	@Override
	public void load(JsonElement element) {
		if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
			set(element.getAsDouble());
		}
	}
}
