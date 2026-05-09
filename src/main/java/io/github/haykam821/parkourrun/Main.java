package io.github.haykam821.parkourrun;

import io.github.haykam821.parkourrun.game.ParkourRunConfig;
import io.github.haykam821.parkourrun.game.phase.ParkourRunWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.Identifier;
import xyz.nucleoid.plasmid.api.game.GameType;
import xyz.nucleoid.plasmid.api.game.GameTypes;

public class Main implements ModInitializer {
	private static final String MOD_ID = "parkourrun";

	private static final Identifier ENDING_PLATFORMS_ID = Main.identifier("ending_platforms");
	public static final TagKey<Block> ENDING_PLATFORMS = TagKey.create(Registries.BLOCK, ENDING_PLATFORMS_ID);

	private static final Identifier PARKOUR_RUN_ID = Main.identifier("parkour_run");
	public static final GameType<ParkourRunConfig> PARKOUR_RUN_TYPE = GameTypes.register(PARKOUR_RUN_ID, ParkourRunConfig.CODEC, ParkourRunWaitingPhase::open);

	@Override
	public void onInitialize() {
		return;
	}

	public static Identifier identifier(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}