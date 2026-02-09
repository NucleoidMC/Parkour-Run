package io.github.haykam821.parkourrun.game.phase;

import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Sets;

import io.github.haykam821.parkourrun.Main;
import io.github.haykam821.parkourrun.game.ParkourRunConfig;
import io.github.haykam821.parkourrun.game.ParkourRunSpawnLogic;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.RemoveEntityStatusEffectS2CPacket;
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
	private final ParkourRunConfig config;

	private final Set<ServerPlayerEntity> players;
	private final long startTime;

	private int ticksUntilClose = -1;

	public ParkourRunActivePhase(GameSpace gameSpace, ServerWorld world, ParkourRunSpawnLogic spawnLogic, ParkourRunConfig config) {
		this.gameSpace = gameSpace;
		this.world = world;
		this.spawnLogic = spawnLogic;
		this.config = config;

		this.players = Sets.newHashSet(gameSpace.getPlayers().participants());
		this.startTime = world.getTime();
	}

	public static void setRules(GameActivity activity) {
		activity.deny(GameRuleType.CRAFTING);
		activity.deny(GameRuleType.PORTALS);
		activity.deny(GameRuleType.PVP);
		activity.deny(GameRuleType.FALL_DAMAGE);
		activity.deny(GameRuleType.HUNGER);
		activity.deny(GameRuleType.INTERACTION);
	}

	public static void open(GameSpace gameSpace, ServerWorld world, ParkourRunSpawnLogic spawnLogic, ParkourRunConfig config) {
		ParkourRunActivePhase phase = new ParkourRunActivePhase(gameSpace, world, spawnLogic, config);

		gameSpace.setActivity(activity -> {
			ParkourRunActivePhase.setRules(activity);

			// Listeners
			activity.listen(GameActivityEvents.ENABLE, phase::enable);
			activity.listen(GameActivityEvents.TICK, phase::tick);
			activity.listen(GamePlayerEvents.ACCEPT, phase::onAcceptPlayers);
			activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
			activity.listen(GamePlayerEvents.LEAVE, phase.spawnLogic::onPlayerLeave);
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
		// Decrease ticks until game end to zero
		if (this.isGameEnding()) {
			if (this.ticksUntilClose == 0) {
				for (ServerPlayerEntity player : players) {
					this.spawnLogic.onPlayerLeave(player);
				}
				this.gameSpace.close(GameCloseReason.FINISHED);
			} else {
				this.ticksUntilClose -= 1;
			}

			return;
		}

		Iterator<ServerPlayerEntity> iterator = this.players.iterator();
		while (iterator.hasNext()) {
			ServerPlayerEntity player = iterator.next();
			BlockState state = player.getSteppingBlockState();
			if (state.isIn(Main.ENDING_PLATFORMS)) {
				ParkourRunResult result = new ParkourRunResult(player, this.world.getTime() - this.startTime);
				result.announceTo(this.gameSpace.getPlayers());

				this.endGame();
				return;
			}

			if (this.config.invisiblePlayers()) {
				player.setStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 20, 1, false, false), null);
			}
		}
	}

	public JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		return this.spawnLogic.acceptPlayers(acceptor).thenRunForEach(player -> {
			if (this.players.contains(player) && !this.isGameEnding()) {
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

	private void endGame() {
		this.ticksUntilClose = this.config.ticksUntilClose().get(this.world.getRandom());
	}

	private boolean isGameEnding() {
		return this.ticksUntilClose >= 0;
	}
}
