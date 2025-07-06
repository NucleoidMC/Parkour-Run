package io.github.haykam821.parkourrun.game;

import java.util.Set;

import io.github.haykam821.parkourrun.game.map.ParkourRunMap;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.api.game.GameSpacePlayers;

public class ParkourRunSpawnLogic {
	private final ParkourRunMap map;
	private final ServerWorld world;

	public ParkourRunSpawnLogic(ParkourRunMap map, ServerWorld world) {
		this.map = map;
		this.world = world;
	}

	public Vec3d getSpawnPos() {
		BlockBounds spawn = this.map.getSpawn();
		return spawn == null ? Vec3d.ZERO : spawn.center();
	}

	public void spawnPlayer(ServerPlayerEntity player) {
		Vec3d pos = this.getSpawnPos();
		player.teleport(this.world, pos.getX(), pos.getY(), pos.getZ(), Set.of(), player.getYaw(), player.getPitch(), true);
	}

	public void teleportWithinBounds(GameSpacePlayers players) {
		BlockBounds start = this.map.getTemplate().getMetadata().getFirstRegionBounds("start");

		if (start != null) {
			for (ServerPlayerEntity player : players) {
				if (!start.contains(player.getBlockPos())) {
					this.spawnPlayer(player);
				}
			}
		}
	}
}