package io.github.haykam821.parkourrun.game;

import java.util.Set;

import io.github.haykam821.parkourrun.game.map.ParkourRunMap;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.api.game.GameSpacePlayers;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;

public class ParkourRunSpawnLogic {
	private final ParkourRunMap map;
	private final ServerLevel world;
	private final PlayerTeam runnerTeam;
	private final boolean disableCollision;

	public ParkourRunSpawnLogic(ParkourRunMap map, ServerLevel world, boolean disableCollision) {
		this.map = map;
		this.world = world;
		this.runnerTeam = new PlayerTeam(new Scoreboard(), "");
		this.disableCollision = disableCollision;
		runnerTeam.setCollisionRule(Team.CollisionRule.NEVER);
	}

	public Vec3 getSpawnPos() {
		BlockBounds spawn = this.map.getSpawn();
		return spawn == null ? Vec3.ZERO : spawn.center();
	}

	public void spawnPlayer(ServerPlayer player) {
		Vec3 pos = this.getSpawnPos();
		player.teleportTo(this.world, pos.x(), pos.y(), pos.z(), Set.of(), -90, 0, true);
		if (disableCollision) {
			player.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(runnerTeam, true));
			player.connection.send(ClientboundSetPlayerTeamPacket.createPlayerPacket(runnerTeam, player.getGameProfile().name(), ClientboundSetPlayerTeamPacket.Action.ADD));
		}
	}

	public JoinAcceptorResult.Teleport acceptPlayers(JoinAcceptor acceptor) {
		return acceptor.teleport(this.world, this.getSpawnPos(), -90, 0).thenRunForEach((player) -> {
			if (disableCollision) {
				player.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(runnerTeam, true));
				player.connection.send(ClientboundSetPlayerTeamPacket.createPlayerPacket(runnerTeam, player.getGameProfile().name(), ClientboundSetPlayerTeamPacket.Action.ADD));
			}
		});
	}

	public void onPlayerLeave(ServerPlayer player) {
		if (disableCollision) {
			PlayerTeam playerTeam = player.getTeam();
			if (playerTeam != null) {
				player.connection.send(ClientboundSetPlayerTeamPacket.createPlayerPacket(playerTeam, player.getGameProfile().name(), ClientboundSetPlayerTeamPacket.Action.ADD));
			} else {
				player.connection.send(ClientboundSetPlayerTeamPacket.createPlayerPacket(runnerTeam, player.getGameProfile().name(), ClientboundSetPlayerTeamPacket.Action.REMOVE));
			}
		}
	}

	public void teleportWithinBounds(GameSpacePlayers players) {
		BlockBounds start = this.map.getTemplate().getMetadata().getFirstRegionBounds("start");

		if (start != null) {
			for (ServerPlayer player : players) {
				if (!start.contains(player.blockPosition())) {
					this.spawnPlayer(player);
				}
			}
		}
	}
}