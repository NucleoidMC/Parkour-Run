package io.github.haykam821.parkourrun.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.parkourrun.game.map.ParkourRunMapConfig;
import net.minecraft.SharedConstants;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.IntProvider;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;

public record ParkourRunConfig(ParkourRunMapConfig mapConfig, WaitingLobbyConfig playerConfig, boolean playerCollisions, boolean invisiblePlayers, IntProvider ticksUntilClose) {
	private static final IntProvider DEFAULT_TICKS_UNTIL_CLOSE = ConstantIntProvider.create(SharedConstants.TICKS_PER_SECOND * 5);

	public static final MapCodec<ParkourRunConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> {
		return instance.group(
			ParkourRunMapConfig.CODEC.fieldOf("map").forGetter(ParkourRunConfig::mapConfig),
			WaitingLobbyConfig.CODEC.fieldOf("players").forGetter(ParkourRunConfig::playerConfig),
			Codec.BOOL.optionalFieldOf("player_collisions", true).forGetter(ParkourRunConfig::playerCollisions),
			Codec.BOOL.optionalFieldOf("invisible_players", true).forGetter(ParkourRunConfig::invisiblePlayers),
			IntProvider.NON_NEGATIVE_CODEC.optionalFieldOf("ticks_until_close", DEFAULT_TICKS_UNTIL_CLOSE).forGetter(ParkourRunConfig::ticksUntilClose)
		).apply(instance, ParkourRunConfig::new);
	});
}