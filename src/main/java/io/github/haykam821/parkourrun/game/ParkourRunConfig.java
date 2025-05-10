package io.github.haykam821.parkourrun.game;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.parkourrun.game.map.ParkourRunMapConfig;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;

public class ParkourRunConfig {
	public static final MapCodec<ParkourRunConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> {
		return instance.group(
			ParkourRunMapConfig.CODEC.fieldOf("map").forGetter(ParkourRunConfig::getMapConfig),
			WaitingLobbyConfig.CODEC.fieldOf("players").forGetter(ParkourRunConfig::getPlayerConfig)
		).apply(instance, ParkourRunConfig::new);
	});

	private final ParkourRunMapConfig mapConfig;
	private final WaitingLobbyConfig playerConfig;

	public ParkourRunConfig(ParkourRunMapConfig mapConfig, WaitingLobbyConfig playerConfig) {
		this.mapConfig = mapConfig;
		this.playerConfig = playerConfig;
	}

	public ParkourRunMapConfig getMapConfig() {
		return this.mapConfig;
	}

	public WaitingLobbyConfig getPlayerConfig() {
		return this.playerConfig;
	}
}