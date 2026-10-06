package mogs.module.render;

import mogs.module.Category;
import mogs.module.Module;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Maximum brightness everywhere. Uses a purely client-side night vision effect (hidden icon and particles),
 * so no mixin is needed. It is removed again when the module is switched off.
 */
public class FullBrightModule extends Module {
	public FullBrightModule() {
		super("FullBright", "Maximum brightness everywhere.", Category.RENDER);
	}

	@Override
	protected void onDisable() {
		if (mc.player != null) {
			mc.player.removeEffect(MobEffects.NIGHT_VISION);
		}
	}

	@Override
	public void onTick() {
		if (mc.player == null) {
			return;
		}
		if (!mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
			mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1_000_000, 0, false, false, false));
		}
	}
}
