package mogs.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

/** Base class for every configurable value of a module. */
public abstract class Setting<T> {
	public final String name;
	public final String description;
	protected final T defaultValue;
	protected T value;
	private boolean hidden;
	private BooleanSupplier visibleWhen = () -> true;

	protected Setting(String name, String description, T defaultValue) {
		this.name = name;
		this.description = description;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	public T get() {
		return value;
	}

	public void set(T newValue) {
		this.value = sanitize(newValue);
	}

	protected T sanitize(T newValue) {
		return newValue;
	}

	public void reset() {
		this.value = defaultValue;
	}

	/** Hidden settings are saved to the config but never shown in the GUI (e.g. HUD coordinates). */
	public void hide() {
		this.hidden = true;
	}

	public void visibleWhen(BooleanSupplier supplier) {
		this.visibleWhen = supplier;
	}

	public boolean isVisible() {
		return !hidden && visibleWhen.getAsBoolean();
	}

	/** Whether this setting is written to the config file. */
	public boolean isPersistent() {
		return true;
	}

	public abstract JsonElement save();

	public abstract void load(JsonElement element);
}
