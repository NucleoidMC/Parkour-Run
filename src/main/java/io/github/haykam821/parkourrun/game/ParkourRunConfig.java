package io.github.haykam821.parkourrun.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.parkourrun.game.map.ParkourRunMapConfig;
import net.minecraft.SharedConstants;
import net.minecraft.util.ExtraCodecs;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;

public record ParkourRunConfig(ParkourRunMapConfig mapConfig, WaitingLobbyConfig playerConfig, boolean playerCollisions, boolean invisiblePlayers, int ticksUntilClose) {
	private static final int DEFAULT_TICKS_UNTIL_CLOSE = SharedConstants.TICKS_PER_SECOND * 5;
	public static final MapCodec<ParkourRunConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> {
		return instance.group(
			ParkourRunMapConfig.CODEC.fieldOf("map").forGetter(ParkourRunConfig::mapConfig),
			WaitingLobbyConfig.CODEC.fieldOf("players").forGetter(ParkourRunConfig::playerConfig),
			Codec.BOOL.optionalFieldOf("disable_player_collisions", true).forGetter(ParkourRunConfig::playerCollisions),
			Codec.BOOL.optionalFieldOf("invisible_players", false).forGetter(ParkourRunConfig::invisiblePlayers),
			ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("ticks_until_close", DEFAULT_TICKS_UNTIL_CLOSE).forGetter(ParkourRunConfig::ticksUntilClose)
		).apply(instance, ParkourRunConfig::new);
	});
}