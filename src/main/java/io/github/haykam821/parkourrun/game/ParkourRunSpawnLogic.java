package io.github.haykam821.parkourrun.game;

import java.util.Set;

import io.github.haykam821.parkourrun.game.map.ParkourRunMap;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.api.game.GameSpacePlayers;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;

public class ParkourRunSpawnLogic {
	private final ParkourRunMap map;
	private final ServerWorld world;
	private final Team runnerTeam;
	private final boolean disableCollision;

	public ParkourRunSpawnLogic(ParkourRunMap map, ServerWorld world, boolean disableCollision) {
		this.map = map;
		this.world = world;
		this.runnerTeam = new Team(new Scoreboard(), "");
		this.disableCollision = disableCollision;
		runnerTeam.setCollisionRule(AbstractTeam.CollisionRule.NEVER);
	}

	public Vec3d getSpawnPos() {
		BlockBounds spawn = this.map.getSpawn();
		return spawn == null ? Vec3d.ZERO : spawn.center();
	}

	public void spawnPlayer(ServerPlayerEntity player) {
		Vec3d pos = this.getSpawnPos();
		player.teleport(this.world, pos.getX(), pos.getY(), pos.getZ(), Set.of(), -90, 0, true);
		if (disableCollision) {
			player.networkHandler.sendPacket(TeamS2CPacket.updateTeam(runnerTeam, true));
			player.networkHandler.sendPacket(TeamS2CPacket.changePlayerTeam(runnerTeam, player.getGameProfile().name(), TeamS2CPacket.Operation.ADD));
		}
	}

	public JoinAcceptorResult.Teleport acceptPlayers(JoinAcceptor acceptor) {
		return acceptor.teleport(this.world, this.getSpawnPos(), -90, 0).thenRunForEach((player) -> {
			if (disableCollision) {
				player.networkHandler.sendPacket(TeamS2CPacket.updateTeam(runnerTeam, true));
				player.networkHandler.sendPacket(TeamS2CPacket.changePlayerTeam(runnerTeam, player.getGameProfile().name(), TeamS2CPacket.Operation.ADD));
			}
		});
	}

	public void onPlayerLeave(ServerPlayerEntity player) {
		if (disableCollision) {
			Team playerTeam = player.getScoreboardTeam();
			if (playerTeam != null) {
				player.networkHandler.sendPacket(TeamS2CPacket.changePlayerTeam(playerTeam, player.getGameProfile().name(), TeamS2CPacket.Operation.ADD));
			} else {
				player.networkHandler.sendPacket(TeamS2CPacket.changePlayerTeam(runnerTeam, player.getGameProfile().name(), TeamS2CPacket.Operation.REMOVE));
			}
		}
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