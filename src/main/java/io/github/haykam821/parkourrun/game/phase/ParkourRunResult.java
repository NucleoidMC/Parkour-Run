package io.github.haykam821.parkourrun.game.phase;

import java.util.Locale;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.api.game.player.PlayerSet;

public class ParkourRunResult {
	private final PlayerEntity winner;
	private final long time;

	public ParkourRunResult(PlayerEntity winner, long time) {
		this.winner = winner;
		this.time = time;
	}

	public PlayerEntity getWinner() {
		return this.winner;
	}

	public float getTimeInSeconds() {
		return this.time / 20f;
	}

	public Text getMessage() {
		return this.winner.getDisplayName().copy().append(String.format(" has won Parkour Run in %,d seconds!", (long) this.getTimeInSeconds(), Locale.ROOT)).formatted(Formatting.GOLD);
	}

	public void announceTo(PlayerSet players) {
		players.sendMessage(this.getMessage());
	}
}