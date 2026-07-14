package io.github.haykam821.parkourrun.game.phase;

import io.github.haykam821.parkourrun.game.ParkourRunConfig;
import io.github.haykam821.parkourrun.game.ParkourRunSpawnLogic;
import io.github.haykam821.parkourrun.game.map.ParkourRunChunkGenerator;
import io.github.haykam821.parkourrun.game.map.ParkourRunMap;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
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

import java.util.ArrayList;

public class ParkourRunWaitingPhase {
	private final GameSpace gameSpace;
	private final ServerLevel level;
	private final ParkourRunSpawnLogic spawnLogic;
	private final ParkourRunConfig config;
	private final ArrayList<BoundingBox> areaBoundingBoxes;
	public ParkourRunWaitingPhase(GameSpace gameSpace, ServerLevel level, ParkourRunMap map, ParkourRunConfig config, ArrayList<BoundingBox> areaBoundingBoxes) {
		this.gameSpace = gameSpace;
		this.level = level;
		this.spawnLogic = new ParkourRunSpawnLogic(map, this.level, config.playerCollisions());
		this.config = config;
		this.areaBoundingBoxes = areaBoundingBoxes;
	}

	public static GameOpenProcedure open(GameOpenContext<ParkourRunConfig> context) {
		ParkourRunConfig config = context.config();
		ParkourRunMap map = new ParkourRunMap(config.mapConfig());
		ParkourRunChunkGenerator generator = map.createGenerator(context.server());
		RuntimeLevelConfig levelConfig = new RuntimeLevelConfig()
			.setGenerator(generator);

		return context.openWithLevel(levelConfig, (activity, level) -> {
			ParkourRunWaitingPhase phase = new ParkourRunWaitingPhase(activity.getGameSpace(), level, map, config, generator.getAreaBoundingBoxes());
			GameWaitingLobby.addTo(activity, config.playerConfig());

			ParkourRunActivePhase.setRules(activity);

			// Listeners
			activity.listen(GamePlayerEvents.ACCEPT, phase::onAcceptPlayers);
			activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
			activity.listen(GamePlayerEvents.LEAVE, phase.spawnLogic::onPlayerLeave);
			activity.listen(PlayerDeathEvent.EVENT, phase::onPlayerDeath);
			activity.listen(GameActivityEvents.REQUEST_START, phase::requestStart);
			activity.listen(GameActivityEvents.TICK, phase::onTick);
		});
	}

	public GameResult requestStart() {
		ParkourRunActivePhase.open(this.gameSpace, this.level, this.spawnLogic, this.config, this.areaBoundingBoxes);
		return GameResult.ok();
	}

	public void onTick() {
		this.spawnLogic.teleportWithinBounds(this.gameSpace.getPlayers());
	}

	public JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		return this.spawnLogic.acceptPlayers(acceptor).thenRunForEach(player -> {
			player.setGameMode(GameType.ADVENTURE);
		});
	}

	public EventResult onPlayerDeath(ServerPlayer player, DamageSource source) {
		// Respawn player at the start
		this.spawnLogic.spawnPlayer(player);
		return EventResult.DENY;
	}
}
