package io.github.haykam821.parkourrun.game.phase;

import java.util.Locale;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import xyz.nucleoid.plasmid.api.game.player.PlayerSet;

public class ParkourRunResult {
	private final Player winner;
	private final long time;

	public ParkourRunResult(Player winner, long time) {
		this.winner = winner;
		this.time = time;
	}

	public Player getWinner() {
		return this.winner;
	}

	public float getTimeInSeconds() {
		return this.time / 20f;
	}

	public Component getMessage() {
		return this.winner.getDisplayName().copy().append(String.format(" has won Parkour Run in %,d seconds!", (long) this.getTimeInSeconds(), Locale.ROOT)).withStyle(ChatFormatting.GOLD);
	}

	public void announceTo(PlayerSet players) {
		players.sendMessage(this.getMessage());
	}
}