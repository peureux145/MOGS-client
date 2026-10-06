package mogs.module.combat;

import mogs.MogsClient;
import mogs.gui.hud.NotificationManager;
import mogs.module.CombatModule;
import mogs.setting.BoolSetting;
import mogs.setting.ModeSetting;
import mogs.setting.NumberSetting;
import mogs.util.CombatTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Optional toast notifications for combat events. They only report; they never react for you. */
public class CombatNotificationsModule extends CombatModule {
	private final BoolSetting lowHealth = add(new BoolSetting("Low Health", "Warn when your health gets low.", true));
	private final NumberSetting healthThreshold = add(new NumberSetting("Health Threshold",
			"Warn at or below this many health points (2 points = 1 heart).", 6, 2, 18, 1));
	private final BoolSetting totemChange = add(new BoolSetting("Totem Count", "Notify when your totem count changes.", true));
	private final BoolSetting weaponDurability = add(new BoolSetting("Weapon Durability",
			"Warn when the held weapon is nearly broken.", true));
	private final NumberSetting durabilityThreshold = add(new NumberSetting("Durability Threshold",
			"Warn at or below this percentage.", 15, 5, 50, 5));
	private final BoolSetting hitConfirm = add(new BoolSetting("Hit Confirm", "Notify when you hit an entity.", false));
	private final NumberSetting duration = add(new NumberSetting("Duration", "Seconds a notification stays.", 2.5, 1.0, 8.0, 0.5));
	private final ModeSetting position = add(new ModeSetting("Position", "Screen corner for these notifications.",
			"Top Right", NotificationManager.ANCHORS));

	private boolean healthWarned;
	private boolean durabilityWarned;
	private int lastTotems = -1;
	private ItemStack lastWeapon = ItemStack.EMPTY;

	public CombatNotificationsModule() {
		super("Combat Notifications", "Optional alerts for low health, totems and weapon wear.");
	}

	@Override
	protected void onEnable() {
		lastTotems = -1;
		healthWarned = false;
		durabilityWarned = false;
	}

	private void notify(String text) {
		NotificationManager.push(MogsClient.NAME + " Combat", text, (long) (duration.asDouble() * 1000.0), position.index());
	}

	@Override
	public void onTick() {
		if (mc.player == null) {
			return;
		}

		if (lowHealth.on()) {
			boolean low = mc.player.getHealth() <= healthThreshold.asFloat() && mc.player.getHealth() > 0;
			if (low && !healthWarned) {
				notify("Low health: " + Math.round(mc.player.getHealth()) + " HP");
			}
			healthWarned = low;
		}

		int totems = CombatTracker.totems(mc);
		if (totemChange.on() && lastTotems >= 0 && totems != lastTotems) {
			notify("Totems: " + lastTotems + " -> " + totems);
		}
		lastTotems = totems;

		if (weaponDurability.on()) {
			ItemStack held = mc.player.getMainHandItem();
			if (held.isDamageableItem()) {
				float percent = 100f * (held.getMaxDamage() - held.getDamageValue()) / held.getMaxDamage();
				boolean low = percent <= durabilityThreshold.asFloat();
				boolean sameItem = ItemStack.isSameItem(held, lastWeapon);
				if (low && (!durabilityWarned || !sameItem)) {
					notify(held.getHoverName().getString() + " at " + Math.round(percent) + "%");
				}
				durabilityWarned = low;
				lastWeapon = held;
			} else {
				durabilityWarned = false;
				lastWeapon = ItemStack.EMPTY;
			}
		}
	}

	@Override
	public void onAttackEntity(Entity target) {
		if (hitConfirm.on()) {
			notify("Hit " + target.getName().getString());
		}
	}
}
