package io.github.haykam821.parkourrun.game.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ParkourRunMapConfig(int areaCount) {
	public static final Codec<ParkourRunMapConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Codec.INT.fieldOf("areaCount").forGetter(ParkourRunMapConfig::areaCount)
		).apply(instance, ParkourRunMapConfig::new);
	});
}
