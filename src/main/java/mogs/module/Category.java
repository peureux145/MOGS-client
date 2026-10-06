package mogs.module;

public enum Category {
	COMBAT("Combat"),
	MISC("Misc"),
	RENDER("Render"),
	VISUALS("Visuals"),
	CLIENT("Client");

	private final String display;

	Category(String display) {
		this.display = display;
	}

	public String display() {
		return display;
	}

	public Category next() {
		return values()[(ordinal() + 1) % values().length];
	}

	public Category previous() {
		return values()[(ordinal() - 1 + values().length) % values().length];
	}
}
