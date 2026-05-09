package io.github.haykam821.parkourrun.game.map;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.haykam821.parkourrun.Main;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.StructureManager;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.api.game.level.generator.GameChunkGenerator;

public class ParkourRunChunkGenerator extends GameChunkGenerator {
	private static final Identifier STARTS_ID = Main.identifier("starts");
	private static final Identifier AREAS_ID = Main.identifier("areas");
	private static final Identifier CONNECTORS_ID = Main.identifier("connectors");
	private static final Identifier ENDINGS_ID = Main.identifier("endings");

	private final ParkourRunMap map;
	private final StructureTemplateManager structureTemplateManager;
	private final Long2ObjectMap<List<PoolElementStructurePiece>> piecesByChunk = new Long2ObjectOpenHashMap<>();

	private final StructureTemplatePool starts;
	private final StructureTemplatePool areas;
	private final StructureTemplatePool connectors;
	private final StructureTemplatePool endings;

	public ParkourRunChunkGenerator(MinecraftServer server, ParkourRunMap map) {
		super(server);
		this.map = map;
		this.structureTemplateManager = server.getStructureManager();

		Registry<StructureTemplatePool> poolRegistry = server.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
		this.starts = poolRegistry.getValue(STARTS_ID);
		this.areas = poolRegistry.getValue(AREAS_ID);
		this.connectors = poolRegistry.getValue(CONNECTORS_ID);
		this.endings = poolRegistry.getValue(ENDINGS_ID);

		for (PoolElementStructurePiece piece : this.generatePieces()) {
			BoundingBox box = piece.getBoundingBox();
			int minChunkX = box.minX() >> 4;
			int minChunkZ = box.minZ() >> 4;
			int maxChunkX = box.maxX() >> 4;
			int maxChunkZ = box.maxZ() >> 4;

			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
					long chunkPos = ChunkPos.pack(chunkX, chunkZ);
					List<PoolElementStructurePiece> piecesByChunk = this.piecesByChunk.computeIfAbsent(chunkPos, p -> new ArrayList<>());
					piecesByChunk.add(piece);
				}
			}
		}
	}

	private void generatePiece(String marker, Set<PoolElementStructurePiece> pieces, StructurePoolElement element, RandomSource random, BlockPos.MutableBlockPos pos) {
		if (!(element instanceof SinglePoolElement)) return;
		StructureTemplate structure = ((SinglePoolElement) element).getTemplate(this.structureTemplateManager);

		BlockPos endPos = pos.offset(structure.getSize()).offset(-1, -1, -1);

		BoundingBox box = BoundingBox.fromCorners(pos, endPos);
		PoolElementStructurePiece piece = new PoolElementStructurePiece(this.structureTemplateManager, element, pos.immutable(), 0, Rotation.NONE, box, LiquidSettings.APPLY_WATERLOGGING);
		pieces.add(piece);

		this.map.getTemplate().getMetadata().addRegion(marker, BlockBounds.of(pos, endPos));
		pos.move(Direction.EAST, structure.getSize().getX());
	}

	private Set<PoolElementStructurePiece> generatePieces() {
		Set<PoolElementStructurePiece> pieces = new HashSet<>();

		BlockPos.MutableBlockPos pos = this.map.getOrigin().mutable();
		RandomSource random = RandomSource.createThreadLocalInstance();
		int areaCount = this.map.getConfig().areaCount();

		this.generatePiece("start", pieces, this.starts.getRandomTemplate(random), random, pos);
		for (int index = 0; index < areaCount; index++) {
			this.generatePiece("area", pieces, this.areas.getRandomTemplate(random), random, pos);
			if (index + 1 < areaCount) {
				this.generatePiece("connector", pieces, this.connectors.getRandomTemplate(random), random, pos);
			}
		}
		this.generatePiece("ending", pieces, this.endings.getRandomTemplate(random), random, pos);

		return pieces;
	}

	@Override
	public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structures) {
		if (this.piecesByChunk.isEmpty()) {
			return;
		}

		ChunkPos chunkPos = chunk.getPos();
		List<PoolElementStructurePiece> pieces = this.piecesByChunk.remove(chunkPos.pack());

		if (pieces != null) {
			BoundingBox chunkBox = new BoundingBox(chunkPos.getMinBlockX(), level.getMinY(), chunkPos.getMinBlockZ(), chunkPos.getMaxBlockX(), level.getMaxY(), chunkPos.getMaxBlockZ());
			for (PoolElementStructurePiece piece : pieces) {
				piece.place(level, structures, this, level.getRandom(), chunkBox, this.map.getOrigin(), false);
			}
		}
	}
}