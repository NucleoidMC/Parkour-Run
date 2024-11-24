package io.github.haykam821.parkourrun.game.map;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.haykam821.parkourrun.Main;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.structure.pool.SinglePoolElement;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.game.world.generator.GameChunkGenerator;

public class ParkourRunChunkGenerator extends GameChunkGenerator {
	private static final Identifier STARTS_ID = Main.identifier("starts");
	private static final Identifier AREAS_ID = Main.identifier("areas");
	private static final Identifier CONNECTORS_ID = Main.identifier("connectors");
	private static final Identifier ENDINGS_ID = Main.identifier("endings");

	private final ParkourRunMap map;
	private final StructureTemplateManager structureTemplateManager;
	private final Long2ObjectMap<List<PoolStructurePiece>> piecesByChunk = new Long2ObjectOpenHashMap<>();

	private final StructurePool starts;
	private final StructurePool areas;
	private final StructurePool connectors;
	private final StructurePool endings;

	public ParkourRunChunkGenerator(MinecraftServer server, ParkourRunMap map) {
		super(server);
		this.map = map;
		this.structureTemplateManager = server.getStructureTemplateManager();

		Registry<StructurePool> poolRegistry = server.getRegistryManager().get(RegistryKeys.TEMPLATE_POOL);
		this.starts = poolRegistry.get(STARTS_ID);
		this.areas = poolRegistry.get(AREAS_ID);
		this.connectors = poolRegistry.get(CONNECTORS_ID);
		this.endings = poolRegistry.get(ENDINGS_ID);

		for (PoolStructurePiece piece : this.generatePieces()) {
			BlockBox box = piece.getBoundingBox();
			int minChunkX = box.getMinX() >> 4;
			int minChunkZ = box.getMinZ() >> 4;
			int maxChunkX = box.getMaxX() >> 4;
			int maxChunkZ = box.getMaxZ() >> 4;

			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
					long chunkPos = ChunkPos.toLong(chunkX, chunkZ);
					List<PoolStructurePiece> piecesByChunk = this.piecesByChunk.computeIfAbsent(chunkPos, p -> new ArrayList<>());
					piecesByChunk.add(piece);
				}
			}
		}
	}

	private void generatePiece(String marker, Set<PoolStructurePiece> pieces, StructurePoolElement element, Random random, BlockPos.Mutable pos) {
		if (!(element instanceof SinglePoolElement)) return;
		StructureTemplate structure = ((SinglePoolElement) element).getStructure(this.structureTemplateManager);

		BlockBox box = BlockBox.create(pos, pos.add(structure.getSize()));
		PoolStructurePiece piece = new PoolStructurePiece(this.structureTemplateManager, element, pos.toImmutable(), 0, BlockRotation.NONE, box);
		pieces.add(piece);

		this.map.getTemplate().getMetadata().addRegion(marker, BlockBounds.of(pos, pos.add(structure.getSize())));
		pos.move(Direction.EAST, structure.getSize().getX());
	}

	private Set<PoolStructurePiece> generatePieces() {
		Set<PoolStructurePiece> pieces = new HashSet<>();

		BlockPos.Mutable pos = this.map.getOrigin().mutableCopy();
		Random random = Random.createLocal();
		int areaCount = this.map.getConfig().getAreaCount();

		this.generatePiece("start", pieces, this.starts.getRandomElement(random), random, pos);
		for (int index = 0; index < areaCount; index++) {
			this.generatePiece("area", pieces, this.areas.getRandomElement(random), random, pos);
			if (index + 1 < areaCount) {
				this.generatePiece("connector", pieces, this.connectors.getRandomElement(random), random, pos);
			}
		}
		this.generatePiece("ending", pieces, this.endings.getRandomElement(random), random, pos);

		return pieces;
	}

	@Override
	public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structures) {
		if (this.piecesByChunk.isEmpty()) {
			return;
		}

		ChunkPos chunkPos = chunk.getPos();
		List<PoolStructurePiece> pieces = this.piecesByChunk.remove(chunkPos.toLong());

		if (pieces != null) {
			BlockBox chunkBox = new BlockBox(chunkPos.getStartX(), world.getBottomY(), chunkPos.getStartZ(), chunkPos.getEndX(), world.getTopY(), chunkPos.getEndZ());
			for (PoolStructurePiece piece : pieces) {
				piece.generate(world, structures, this, world.getRandom(), chunkBox, this.map.getOrigin(), false);
			}
		}
	}
}