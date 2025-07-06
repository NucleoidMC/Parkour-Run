package io.github.haykam821.parkourrun.game;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.parkourrun.game.map.ParkourRunMapConfig;
import net.minecraft.SharedConstants;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.IntProvider;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;

public class ParkourRunConfig {
	private static final IntProvider DEFAULT_TICKS_UNTIL_CLOSE = ConstantIntProvider.create(SharedConstants.TICKS_PER_SECOND * 5);

	public static final MapCodec<ParkourRunConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> {
		return instance.group(
			ParkourRunMapConfig.CODEC.fieldOf("map").forGetter(ParkourRunConfig::getMapConfig),
			WaitingLobbyConfig.CODEC.fieldOf("players").forGetter(ParkourRunConfig::getPlayerConfig),
			IntProvider.NON_NEGATIVE_CODEC.optionalFieldOf("ticks_until_close", DEFAULT_TICKS_UNTIL_CLOSE).forGetter(ParkourRunConfig::getTicksUntilClose)
		).apply(instance, ParkourRunConfig::new);
	});

	private final ParkourRunMapConfig mapConfig;
	private final WaitingLobbyConfig playerConfig;
	private final IntProvider ticksUntilClose;

	public ParkourRunConfig(ParkourRunMapConfig mapConfig, WaitingLobbyConfig playerConfig, IntProvider ticksUntilClose) {
		this.mapConfig = mapConfig;
		this.playerConfig = playerConfig;
		this.ticksUntilClose = ticksUntilClose;
	}

	public ParkourRunMapConfig getMapConfig() {
		return this.mapConfig;
	}

	public WaitingLobbyConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public IntProvider getTicksUntilClose() {
		return this.ticksUntilClose;
	}
}