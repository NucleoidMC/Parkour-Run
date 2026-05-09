package io.github.haykam821.parkourrun.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;

public class ParkourRunMap {
	private final BlockPos origin = new BlockPos(0, 0, 0);
	private final BlockBounds spawn = BlockBounds.ofBlock(new BlockPos(4, 1, 5));
	private final MapTemplate template = MapTemplate.createEmpty();
	private final ParkourRunMapConfig config;

	public ParkourRunMap(ParkourRunMapConfig config) {
		this.config = config;
	}

	public BlockPos getOrigin() {
		return this.origin;
	}

	public BlockBounds getSpawn() {
		return this.spawn;
	}

	public MapTemplate getTemplate() {
		return this.template;
	}

	public ParkourRunMapConfig getConfig() {
		return this.config;
	}

	public ChunkGenerator createGenerator(MinecraftServer server) {
		return new ParkourRunChunkGenerator(server, this);
	}
}
