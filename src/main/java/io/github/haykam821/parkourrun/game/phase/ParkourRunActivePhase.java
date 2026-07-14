package io.github.haykam821.parkourrun.game.phase;

import java.util.*;

import com.google.common.collect.Sets;

import io.github.haykam821.parkourrun.Main;
import io.github.haykam821.parkourrun.game.ParkourRunConfig;
import io.github.haykam821.parkourrun.game.ParkourRunSpawnLogic;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.widget.SidebarWidget;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.game.stats.GameStatisticBundle;
import xyz.nucleoid.plasmid.api.game.stats.StatisticKey;
import xyz.nucleoid.plasmid.api.util.PlayerUtil;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

import static io.github.haykam821.parkourrun.Main.MOD_ID;

public class ParkourRunActivePhase {
	private static final StatisticKey<Integer> TIME_KEY = new StatisticKey<>(Main.identifier("time"), StatisticKey.ValueType.INT, false);
	private final GameSpace gameSpace;
	private final ServerLevel level;
	private final ParkourRunSpawnLogic spawnLogic;
	private final ParkourRunConfig config;
	private final ArrayList<BoundingBox> areaBoundingBoxes;
	private final Set<ServerPlayer> players;
	private final HashMap<ServerPlayer, ArrayList<BoundingBox>> areasPlayerPassed = new HashMap<>();
	private final long startTime;
	private final GameStatisticBundle stats;
	private final LinkedHashMap<ServerPlayer, Integer> playerCompleteTimes = new LinkedHashMap<>();
	private final HashMap<ServerPlayer, Integer> areaPassedCooldown = new HashMap<>();
	private int ticksUntilClose = -1;
	private final int mapLength;
	SidebarWidget sidebar = null;
	public ParkourRunActivePhase(GameSpace gameSpace, ServerLevel level, ParkourRunSpawnLogic spawnLogic, ParkourRunConfig config, ArrayList<BoundingBox> areaBoundingBoxes) {
		this.gameSpace = gameSpace;
		this.level = level;
		this.spawnLogic = spawnLogic;
		this.config = config;
		this.areaBoundingBoxes = areaBoundingBoxes;
		this.mapLength = this.areaBoundingBoxes.getLast().maxX();
		this.stats = gameSpace.getStatistics().bundle(MOD_ID);
		this.players = Sets.newHashSet(gameSpace.getPlayers().participants());
		for (ServerPlayer player : this.players) {
			fillAreasPassed(player);
		}
		this.startTime = level.getGameTime();
	}
	public void setWidgets(SidebarWidget sidebar) {
		this.sidebar = sidebar;
		sidebar.setTitle(Component.translatable("gameType.parkourrun.parkour_run").withStyle(ChatFormatting.GOLD));
		updateWidgets();
	}
	private void updateWidgets() {
		sidebar.set((builder) -> {
			builder.add(Component.translatable("parkourrun.player_times"));
			this.playerCompleteTimes.forEach((player, time) -> {
				long minutes = time / 60;
				String seconds = Long.toString(time % 60);
				builder.add(player.getName().copy().withStyle(ChatFormatting.BLUE).append(": ").append(Component.literal(minutes + "m " + seconds + "s").withStyle(ChatFormatting.WHITE)));
			});
			ArrayList<ServerPlayer> playersLeft = new ArrayList<>(gameSpace.getPlayers().participants().stream()
					.filter(player -> !playerCompleteTimes.containsKey(player)).toList());
			playersLeft.sort((player1, player2) -> (int) (player2.getX() - player1.getX()));
			for (ServerPlayer serverPlayer : playersLeft) {
				builder.add(serverPlayer.getName());
			}
		});
		long time = (level.getGameTime() - this.startTime) / SharedConstants.TICKS_PER_SECOND;
		for (ServerPlayer player : gameSpace.getPlayers()) {
			var val = areaPassedCooldown.compute(player, (_player, playerTime) -> {
				if (playerTime == null || playerTime == 0) return null;
				return playerTime - 1;
			});
			if (val != null) continue;
			long minutes = time / 60;
			String seconds = Long.toString(time % 60);
			player.sendSystemMessage(Component.translatable("parkourrun.current_time").append(minutes + "m " + seconds + "s"), true);
		}
	}
	public static void setRules(GameActivity activity) {
		activity.deny(GameRuleType.CRAFTING);
		activity.deny(GameRuleType.PORTALS);
		activity.deny(GameRuleType.PVP);
		activity.deny(GameRuleType.FALL_DAMAGE);
		activity.deny(GameRuleType.HUNGER);
		activity.deny(GameRuleType.INTERACTION);
	}

	private void fillAreasPassed(ServerPlayer player) {
		this.areasPlayerPassed.put(player, new ArrayList<>());
	}

	public static void open(GameSpace gameSpace, ServerLevel level, ParkourRunSpawnLogic spawnLogic, ParkourRunConfig config, ArrayList<BoundingBox> areaBoundingBoxes) {
		ParkourRunActivePhase phase = new ParkourRunActivePhase(gameSpace, level, spawnLogic, config, areaBoundingBoxes);

		gameSpace.setActivity(activity -> {
			ParkourRunActivePhase.setRules(activity);
			GlobalWidgets widgets = GlobalWidgets.addTo(activity);
			phase.setWidgets(widgets.addSidebar());
			// Listeners
			activity.listen(GameActivityEvents.ENABLE, phase::enable);
			activity.listen(GameActivityEvents.TICK, phase::tick);
			activity.listen(GamePlayerEvents.ACCEPT, phase::onAcceptPlayers);
			activity.listen(GamePlayerEvents.OFFER, JoinOffer::acceptSpectators);
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
		updateWidgets();
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
			player.setExperienceLevels(99);
			player.setExperiencePoints(Math.round(mapRange((float) player.getX(), 0, mapLength, 0, player.getXpNeededForNextLevel() - 1)));
			if (state.is(Main.ENDING_PLATFORMS) && !playerCompleteTimes.containsKey(player)) {
				int finishTime = Math.toIntExact((this.level.getGameTime() - this.startTime) / 20);
				stats.forPlayer(player).set(ParkourRunActivePhase.TIME_KEY, finishTime);
				playerCompleteTimes.put(player, finishTime);
				ParkourRunResult result = new ParkourRunResult(player, this.level.getGameTime() - this.startTime);
				result.announceTo(this.gameSpace.getPlayers());
				if (this.playerCompleteTimes.size() >= this.gameSpace.getPlayers().participants().size()) {
					this.endGame();
				}
				return;
			}
			ArrayList<BoundingBox> areas = this.areasPlayerPassed.get(player);
			Vec3i pos = player.blockPosition();
			if (areas != null) {
				for (BoundingBox boundingBox : areaBoundingBoxes) {
					if (!areas.contains(boundingBox) && boundingBox.isInside(pos)) {
						areas.add(boundingBox);
						player.sendSystemMessage(Component.translatable("parkourrun.passed_area").withStyle(ChatFormatting.GREEN), true);
						PlayerUtil.playSoundToPlayer(player, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.UI, 0.6f, 1);
						level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.getX(), pos.getY(), pos.getZ(), 16, 0.1, 0.1, 0.1, 1);
						areaPassedCooldown.put(player, 3 * SharedConstants.TICKS_PER_SECOND);
					}
				}
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
				fillAreasPassed(player);
			} else {
				player.setGameMode(GameType.SPECTATOR);
			}
		});
	}

	public static float mapRange(float value, float inMin, float inMax, float outMin, float outMax) {
		return (value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
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
