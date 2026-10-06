package mogs.module.combat;

import mogs.module.CombatModule;
import mogs.setting.ModeSetting;
import mogs.setting.NumberSetting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client-side cosmetic particles that play where you manually hit an entity.
 * They are only visual (spawned in the local world) and carry no gameplay effect.
 */
public class HitParticlesModule extends CombatModule {
	private static final class Burst {
		final Vec3 center;
		final int lifetime;
		int age;

		Burst(Vec3 center, int lifetime) {
			this.center = center;
			this.lifetime = lifetime;
		}
	}

	private final ModeSetting style = add(new ModeSetting("Style", "Motion pattern of the particles.",
			"Burst", "Burst", "Ring", "Spiral", "Fountain"));
	private final ModeSetting particle = add(new ModeSetting("Particle", "Which particle is used.",
			"Crit", "Crit", "Sparkle", "Flame", "Heart", "End Rod"));
	private final NumberSetting amount = add(new NumberSetting("Amount", "Total particles per hit.", 20, 4, 80, 2));
	private final NumberSetting size = add(new NumberSetting("Size", "Spread radius of the effect (blocks).", 0.8, 0.2, 2.0, 0.1));
	private final NumberSetting lifetime = add(new NumberSetting("Lifetime", "Ticks the effect keeps emitting (20 = 1s).", 8, 1, 40, 1));

	private final List<Burst> active = new ArrayList<>();

	public HitParticlesModule() {
		super("Hit Particles", "Custom particle effects when you hit an entity.");
	}

	@Override
	protected void onDisable() {
		active.clear();
	}

	@Override
	public void onAttackEntity(Entity target) {
		if (mc.level == null) {
			return;
		}
		Vec3 center = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ());
		active.add(new Burst(center, lifetime.asInt()));
	}

	@Override
	public void onTick() {
		if (mc.level == null) {
			active.clear();
			return;
		}
		int perTick = Math.max(1, (int) Math.ceil(amount.asDouble() / lifetime.asDouble()));
		Iterator<Burst> iterator = active.iterator();
		while (iterator.hasNext()) {
			Burst burst = iterator.next();
			emit(burst, perTick);
			if (++burst.age >= burst.lifetime) {
				iterator.remove();
			}
		}
	}

	private ParticleOptions options() {
		return switch (particle.get()) {
			case "Sparkle" -> ParticleTypes.ENCHANTED_HIT;
			case "Flame" -> ParticleTypes.FLAME;
			case "Heart" -> ParticleTypes.HEART;
			case "End Rod" -> ParticleTypes.END_ROD;
			default -> ParticleTypes.CRIT;
		};
	}

	private void emit(Burst burst, int count) {
		RandomSource random = mc.level.random;
		ParticleOptions options = options();
		double radius = size.asDouble();
		Vec3 c = burst.center;

		for (int i = 0; i < count; i++) {
			switch (style.get()) {
				case "Ring" -> {
					double angle = random.nextDouble() * Math.PI * 2;
					double cos = Math.cos(angle);
					double sin = Math.sin(angle);
					mc.level.addParticle(options, c.x + cos * radius * 0.5, c.y, c.z + sin * radius * 0.5,
							cos * 0.12 * radius, 0.02, sin * 0.12 * radius);
				}
				case "Spiral" -> {
					double angle = burst.age * 0.7 + i * (Math.PI * 2 / count);
					double rise = ((burst.age / (double) burst.lifetime) - 0.5) * radius;
					mc.level.addParticle(options, c.x + Math.cos(angle) * radius * 0.5, c.y + rise,
							c.z + Math.sin(angle) * radius * 0.5, 0, 0.03, 0);
				}
				case "Fountain" -> mc.level.addParticle(options, c.x, c.y, c.z,
						(random.nextDouble() - 0.5) * 0.15 * radius,
						0.2 * radius + random.nextDouble() * 0.1,
						(random.nextDouble() - 0.5) * 0.15 * radius);
				default -> mc.level.addParticle(options, c.x, c.y, c.z,
						(random.nextDouble() - 0.5) * radius,
						(random.nextDouble() - 0.3) * radius,
						(random.nextDouble() - 0.5) * radius);
			}
		}
	}
}
