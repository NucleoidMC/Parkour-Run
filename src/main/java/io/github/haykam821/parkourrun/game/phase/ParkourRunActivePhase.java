package io.github.haykam821.parkourrun.game.phase;

import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Sets;

import io.github.haykam821.parkourrun.Main;
import io.github.haykam821.parkourrun.game.ParkourRunSpawnLogic;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class ParkourRunActivePhase {
	private final GameSpace gameSpace;
	private final ServerWorld world;
	private final ParkourRunSpawnLogic spawnLogic;
	private final Set<ServerPlayerEntity> players;
	private final long startTime;

	public ParkourRunActivePhase(GameSpace gameSpace, ServerWorld world, ParkourRunSpawnLogic spawnLogic) {
		this.gameSpace = gameSpace;
		this.world = world;
		this.spawnLogic = spawnLogic;
		this.players = Sets.newHashSet(gameSpace.getPlayers().participants());
		this.startTime = world.getTime();
	}

	public static void setRules(GameActivity activity) {
		activity.deny(GameRuleType.CRAFTING);
		activity.deny(GameRuleType.PORTALS);
		activity.deny(GameRuleType.PVP);
		activity.deny(GameRuleType.FALL_DAMAGE);
		activity.deny(GameRuleType.HUNGER);
	}

	public static void open(GameSpace gameSpace, ServerWorld world, ParkourRunSpawnLogic spawnLogic) {
		ParkourRunActivePhase phase = new ParkourRunActivePhase(gameSpace, world, spawnLogic);

		gameSpace.setActivity(activity -> {
			ParkourRunActivePhase.setRules(activity);

			// Listeners
			activity.listen(GameActivityEvents.ENABLE, phase::enable);
			activity.listen(GameActivityEvents.TICK, phase::tick);
			activity.listen(GamePlayerEvents.ACCEPT, phase::onAcceptPlayers);
			activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
			activity.listen(PlayerDeathEvent.EVENT, phase::onPlayerDeath);
		});
	}

	public void enable() {
		for (ServerPlayerEntity player : this.gameSpace.getPlayers().spectators()) {
			this.spawnLogic.spawnPlayer(player);
			player.changeGameMode(GameMode.SPECTATOR);
		}
	}

	public void tick() {
		Iterator<ServerPlayerEntity> iterator = this.players.iterator();
		while (iterator.hasNext()) {
			ServerPlayerEntity player = iterator.next();

			BlockState state = player.getSteppingBlockState();
			if (state.isIn(Main.ENDING_PLATFORMS)) {
				ParkourRunResult result = new ParkourRunResult(player, this.world.getTime() - this.startTime);
				this.gameSpace.getPlayers().forEach(result::announce);

				this.gameSpace.close(GameCloseReason.FINISHED);
				return;
			}
		}
	}

	public JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		return acceptor.teleport(this.world, this.spawnLogic.getSpawnPos()).thenRunForEach(player -> {
			if (this.players.contains(player)) {
				player.changeGameMode(GameMode.ADVENTURE);
			} else {
				player.changeGameMode(GameMode.SPECTATOR);
			}
		});
	}

	public EventResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player at the start
		this.spawnLogic.spawnPlayer(player);
		return EventResult.DENY;
	}
}
