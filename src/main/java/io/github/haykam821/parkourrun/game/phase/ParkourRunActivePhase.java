package io.github.haykam821.parkourrun.game.phase;

import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Sets;

import io.github.haykam821.parkourrun.Main;
import io.github.haykam821.parkourrun.game.ParkourRunConfig;
import io.github.haykam821.parkourrun.game.ParkourRunSpawnLogic;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
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
	private final ServerLevel level;
	private final ParkourRunSpawnLogic spawnLogic;
	private final ParkourRunConfig config;

	private final Set<ServerPlayer> players;
	private final long startTime;

	private int ticksUntilClose = -1;

	public ParkourRunActivePhase(GameSpace gameSpace, ServerLevel level, ParkourRunSpawnLogic spawnLogic, ParkourRunConfig config) {
		this.gameSpace = gameSpace;
		this.level = level;
		this.spawnLogic = spawnLogic;
		this.config = config;

		this.players = Sets.newHashSet(gameSpace.getPlayers().participants());
		this.startTime = level.getGameTime();
	}

	public static void setRules(GameActivity activity) {
		activity.deny(GameRuleType.CRAFTING);
		activity.deny(GameRuleType.PORTALS);
		activity.deny(GameRuleType.PVP);
		activity.deny(GameRuleType.FALL_DAMAGE);
		activity.deny(GameRuleType.HUNGER);
		activity.deny(GameRuleType.INTERACTION);
	}

	public static void open(GameSpace gameSpace, ServerLevel level, ParkourRunSpawnLogic spawnLogic, ParkourRunConfig config) {
		ParkourRunActivePhase phase = new ParkourRunActivePhase(gameSpace, level, spawnLogic, config);

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
		for (ServerPlayer player : this.gameSpace.getPlayers().spectators()) {
			this.spawnLogic.spawnPlayer(player);
			player.setGameMode(GameType.SPECTATOR);
		}
	}

	public void tick() {
		// Decrease ticks until game end to zero
		if (this.isGameEnding()) {
			if (this.ticksUntilClose == 0) {
				for (ServerPlayer player : players) {
					this.spawnLogic.onPlayerLeave(player);
				}
				this.gameSpace.close(GameCloseReason.FINISHED);
			} else {
				this.ticksUntilClose -= 1;
			}

			return;
		}

		Iterator<ServerPlayer> iterator = this.players.iterator();
		while (iterator.hasNext()) {
			ServerPlayer player = iterator.next();
			BlockState state = player.getBlockStateOn();
			if (state.is(Main.ENDING_PLATFORMS)) {
				ParkourRunResult result = new ParkourRunResult(player, this.level.getGameTime() - this.startTime);
				result.announceTo(this.gameSpace.getPlayers());

				this.endGame();
				return;
			}

			if (this.config.invisiblePlayers()) {
				player.forceAddEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20, 1, false, false), null);
			}
		}
	}

	public JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		return this.spawnLogic.acceptPlayers(acceptor).thenRunForEach(player -> {
			if (this.players.contains(player) && !this.isGameEnding()) {
				player.setGameMode(GameType.ADVENTURE);
			} else {
				player.setGameMode(GameType.SPECTATOR);
			}
		});
	}

	public EventResult onPlayerDeath(ServerPlayer player, DamageSource source) {
		// Respawn player at the start
		this.spawnLogic.spawnPlayer(player);
		return EventResult.DENY;
	}

	private void endGame() {
		this.ticksUntilClose = this.config.ticksUntilClose();
	}

	private boolean isGameEnding() {
		return this.ticksUntilClose >= 0;
	}
}
