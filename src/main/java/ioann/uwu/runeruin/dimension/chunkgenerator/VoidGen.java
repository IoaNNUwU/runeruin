package ioann.uwu.runeruin.dimension.chunkgenerator;

import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.noise.LazyNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;

import static ioann.uwu.runeruin.dimension.Const.CEILING_TERRAIN_HEIGHT;
import static ioann.uwu.runeruin.dimension.Const.CEILING_VOID_Y;
import static ioann.uwu.runeruin.dimension.Const.LOST_CAVES_Y;
import static ioann.uwu.runeruin.dimension.Const.TERRAIN_HEIGHT;

/** The ceiling of the Void: terraces of void stone from {@link #LOWEST_Y} up into the floor of the Lost caves. */
public class VoidGen {

    /** Nothing of the ceiling is lower than this. */
    public static final int LOWEST_Y = CEILING_VOID_Y - CEILING_TERRAIN_HEIGHT;
    // Deepslate that always stays under the surface of the Lost caves floor.
    private static final int CRUST = 6;
    /** The highest {@link #roofY}. */
    public static final int HIGHEST_Y = LOST_CAVES_Y + TERRAIN_HEIGHT - CRUST;

    private static final LazyNoise reliefNoise = LazyNoise.single("voidCeilingRelief", 9f);

    private static final TerracedCeiling CEILING = new TerracedCeiling("voidCeiling", LOWEST_Y, HIGHEST_Y, 3,
            (chunkX, chunkZ, randomState) -> (x, z) -> {
                int roof = roofY(x, z, randomState.sampler());
                float bottom = LOWEST_Y + (reliefNoise.getOrCreateNoise(randomState).noise(x, z) - 0.5f) * 3f;
                // The highest terrace is 2 blocks under the roof. A block reads its biome up to 2 blocks above itself
                // and 5 blocks aside, where the roof may be a block lower: the air stays in the Void biome.
                return new TerracedCeiling.Column(bottom, roof - 2 - bottom, roof, 1);
            });

    /** The highest block of the ceiling: void stone up to here, deepslate from here up to the floor of the Lost caves. */
    public static int roofY(int x, int z, Climate.Sampler sampler) {
        // The floor reads its noise 8 blocks aside.
        return DeepCavesAndLostCavesGen.lostCavesFloorY(x + 8, z + 8, sampler) - CRUST;
    }

    /** The lowest block of the Void ceiling in this column. */
    public static int ceilingSurfaceY(int x, int z, RandomState randomState) {
        return CEILING.chunk(x >> 4, z >> 4, randomState).lowestY(x, z);
    }

    /** Whether a block is a part of the Void ceiling or lies under it: what is buried in the Lost caves floor stops above. */
    public static boolean reaches(BlockGetter level, BlockPos pos) {
        BlockPos.MutableBlockPos above = pos.mutable();
        while (above.getY() <= HIGHEST_Y) {
            if (level.getBlockState(above).is(RRBlocks.VOID_STONE)) {
                return true;
            }
            above.move(Direction.UP);
        }
        return false;
    }

    public static void generateVoidCeiling(TerrainWriter terrain, RandomState randomState) {
        ChunkAccess chunk = terrain.chunk();
        ChunkPos pos = chunk.getPos();
        TerracedCeiling.Chunk ceiling = CEILING.chunk(pos.x(), pos.z(), randomState);
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = pos.getMinBlockX() + x;
                int worldZ = pos.getMinBlockZ() + z;
                int roof = roofY(worldX, worldZ, randomState.sampler());
                // The whole ceiling is made of the biome's block, not only its surface.
                BlockState stone = RRTerrainSurfaces.ceilingAt(chunk, worldX, LOWEST_Y, worldZ, randomState);

                for (int y = LOWEST_Y; y <= roof; y++) {
                    if (ceiling.stone(x, y, z)) {
                        terrain.set(x, y, z, stone);
                    } else if (y > CEILING_VOID_Y) {
                        // Above the Void the ceiling is dug out of the rock under the Lost caves.
                        terrain.set(x, y, z, air);
                    }
                }
            }
        }
    }
}
