package mogs.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Passive bookkeeping of what the player did manually: click timestamps and connected hits / swings in the air.
 * It only observes input; it never produces any.
 */
public final class CombatTracker {
	private static final long KEEP_MS = 10_000L;

	private static final Deque<Long> LEFT = new ArrayDeque<>();
	private static final Deque<Long> RIGHT = new ArrayDeque<>();
	private static boolean leftWasDown;
	private static boolean rightWasDown;

	private static int hits;
	private static int misses;
	private static int streak;
	private static int bestStreak;

	private CombatTracker() {
	}

	public static void tick(Minecraft mc) {
		long now = System.currentTimeMillis();
		boolean leftDown = mc.screen == null && mc.options.keyAttack.isDown();
		boolean rightDown = mc.screen == null && mc.options.keyUse.isDown();

		if (leftDown && !leftWasDown) {
			LEFT.addLast(now);
			HitResult result = mc.hitResult;
			// Only swings into thin air count as a miss; mining a block or hitting an entity do not.
			if (result != null && result.getType() == HitResult.Type.MISS) {
				misses++;
				streak = 0;
			}
		}
		if (rightDown && !rightWasDown) {
			RIGHT.addLast(now);
		}
		leftWasDown = leftDown;
		rightWasDown = rightDown;

		prune(LEFT, now);
		prune(RIGHT, now);
	}

	/** Called when the player attacks an entity (Fabric AttackEntityCallback). */
	public static void onHit(Entity target) {
		hits++;
		streak++;
		bestStreak = Math.max(bestStreak, streak);
	}

	private static void prune(Deque<Long> queue, long now) {
		while (!queue.isEmpty() && now - queue.peekFirst() > KEEP_MS) {
			queue.removeFirst();
		}
	}

	private static double cps(Deque<Long> queue, double windowSeconds) {
		long now = System.currentTimeMillis();
		long windowMs = (long) (windowSeconds * 1000.0);
		int count = 0;
		for (long time : queue) {
			if (now - time <= windowMs) {
				count++;
			}
		}
		return count / windowSeconds;
	}

	public static double leftCps(double windowSeconds) {
		return cps(LEFT, windowSeconds);
	}

	public static double rightCps(double windowSeconds) {
		return cps(RIGHT, windowSeconds);
	}

	public static int hits() {
		return hits;
	}

	public static int misses() {
		return misses;
	}

	public static int streak() {
		return streak;
	}

	public static int bestStreak() {
		return bestStreak;
	}

	public static double accuracy() {
		int total = hits + misses;
		return total == 0 ? 0 : hits * 100.0 / total;
	}

	public static void resetStats() {
		hits = 0;
		misses = 0;
		streak = 0;
		bestStreak = 0;
	}

	/** Totems in the main inventory plus the off-hand. */
	public static int totems(Minecraft mc) {
		if (mc.player == null) {
			return 0;
		}
		int count = 0;
		for (int i = 0; i < 36; i++) {
			ItemStack stack = mc.player.getInventory().getItem(i);
			if (stack.is(Items.TOTEM_OF_UNDYING)) {
				count += stack.getCount();
			}
		}
		ItemStack offhand = mc.player.getOffhandItem();
		if (offhand.is(Items.TOTEM_OF_UNDYING)) {
			count += offhand.getCount();
		}
		return count;
	}
}
