package mogs.module;

/**
 * Base class of all Combat modules.
 * <p>
 * Combat modules in MOGS are strictly informational / cosmetic: they read game state and draw
 * something. They never send input, never switch items, never modify reach, hitboxes or packets.
 * Every attack, click, block placement and item switch stays manual.
 */
public abstract class CombatModule extends Module {
	protected CombatModule(String name, String description) {
		super(name, description, Category.COMBAT);
	}
}
