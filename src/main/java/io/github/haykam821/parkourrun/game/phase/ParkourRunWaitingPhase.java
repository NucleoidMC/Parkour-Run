package io.github.haykam821.parkourrun.game.phase;

import io.github.haykam821.parkourrun.game.ParkourRunConfig;
import io.github.haykam821.parkourrun.game.ParkourRunSpawnLogic;
import io.github.haykam821.parkourrun.game.map.ParkourRunMap;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameResult;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class ParkourRunWaitingPhase {
	private final GameSpace gameSpace;
	private final ServerWorld world;
	private final ParkourRunSpawnLogic spawnLogic;

	public ParkourRunWaitingPhase(GameSpace gameSpace, ServerWorld world, ParkourRunMap map) {
		this.gameSpace = gameSpace;
		this.world = world;
		this.spawnLogic = new ParkourRunSpawnLogic(map, this.world);
	}

	public static GameOpenProcedure open(GameOpenContext<ParkourRunConfig> context) {
		ParkourRunMap map = new ParkourRunMap(context.config().getMapConfig());
		RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
			.setGenerator(map.createGenerator(context.server()));

		return context.openWithWorld(worldConfig, (activity, world) -> {
			ParkourRunWaitingPhase phase = new ParkourRunWaitingPhase(activity.getGameSpace(), world, map);
			GameWaitingLobby.addTo(activity, context.config().getPlayerConfig());

			ParkourRunActivePhase.setRules(activity);

			// Listeners
			activity.listen(GamePlayerEvents.ACCEPT, phase::onAcceptPlayers);
			activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
			activity.listen(PlayerDeathEvent.EVENT, phase::onPlayerDeath);
			activity.listen(GameActivityEvents.REQUEST_START, phase::requestStart);
			activity.listen(GameActivityEvents.TICK, phase::onTick);
		});
	}

	public GameResult requestStart() {
		ParkourRunActivePhase.open(this.gameSpace, this.world, this.spawnLogic);
		return GameResult.ok();
	}

	public void onTick() {
		this.spawnLogic.teleportWithinBounds(this.gameSpace.getPlayers());
	}

	public JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		return this.spawnLogic.acceptPlayers(acceptor).thenRunForEach(player -> {
			player.changeGameMode(GameMode.ADVENTURE);
		});
	}

	public EventResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player at the start
		this.spawnLogic.spawnPlayer(player);
		return EventResult.DENY;
	}
}
