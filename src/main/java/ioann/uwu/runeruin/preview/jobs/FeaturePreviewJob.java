package ioann.uwu.runeruin.preview.jobs;

import ioann.uwu.runeruin.preview.PreviewArgs;
import ioann.uwu.runeruin.preview.PreviewJob;
import ioann.uwu.runeruin.preview.PreviewJobs;
import ioann.uwu.runeruin.preview.PreviewWorld;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

/**
 * Any configured feature by id, placed on a prepared surface. Like a placed feature's scan, the origin is
 * the surface block shifted by {@code offset}. Fails when the feature changes no block.
 */
public final class FeaturePreviewJob implements PreviewJob {

    /** The prepared surface and the default origin offset from its surface block. */
    public enum Surface {
        /** Ground at y 60-63; default origin is the air above it. */
        FLOOR(1),
        /** Ceiling at y 65-68; default origin is the air below it. */
        CEILING(-1),
        /** Ground at y 53-56 under water up to y 63; default origin is the air above the water. */
        WATER(1),
        /** Same pool; the surface block is the pool floor, default origin the water above it. */
        UNDERWATER(1),
        /** Wall at x 1-4; the default origin is the wall block at 1 64 0, as wall placements expect. */
        WALL(0),
        /** Floor at y 53-56 and ceiling at y 72-75; the default origin is the air at 0 64 0. */
        CAVE(0);

        private final int defaultOffset;

        Surface(int defaultOffset) {
            this.defaultOffset = defaultOffset;
        }

        public int defaultOffset() {
            return defaultOffset;
        }
    }

    /** The world after placing the feature and how many blocks differ from the prepared surface. */
    public record Placement(PreviewWorld world, BlockPos origin, int changedBlocks) {}

    @Override
    public String id() {
        return "feature";
    }

    @Override
    public String description() {
        return "Any configured feature. params: id=<namespace:path>, surface=floor|ceiling|water|underwater|wall|cave, "
            + "ground=<block id>, offset=<dy from the surface block>, radius";
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args) throws IOException {
        throw new IOException("The feature preview needs the server registries");
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args, MinecraftServer server) throws IOException {
        String id = args.get("id", "");
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Pass id=<namespace:path>, e.g. -Parg.id=runeruin:inverted_tree");
        }
        Surface surface = Surface.valueOf(args.get("surface", "floor").toUpperCase(Locale.ROOT));
        String ground = args.get("ground", "minecraft:stone");
        int offset = args.getInt("offset", surface.defaultOffset());
        int radius = args.getInt("radius", 16);
        Placement placement = place(server, Identifier.parse(id), surface,
            BuiltInRegistries.BLOCK.getValue(Identifier.parse(ground)).defaultBlockState(), offset, args.seed(), radius);
        if (placement.changedBlocks() == 0) {
            throw new IOException(id + " changed no block on " + surface + " of " + ground + " at offset " + offset
                + "; try another surface=, ground= or offset=");
        }
        PreviewWorld world = placement.world();
        BlockPos origin = placement.origin();
        return PreviewJobs.export(world, PreviewJobs.paddedOccupied(world, 1),
            args.name("preview_" + Identifier.parse(id).getPath()), args.exportDir(), List.of(
                "job: " + id(),
                "feature: " + id,
                "seed: " + args.seed(),
                "surface: " + surface + " of " + ground + ", offset " + offset + ", radius " + radius,
                "changed blocks: " + placement.changedBlocks(),
                "origin: " + origin.getX() + " " + origin.getY() + " " + origin.getZ()
            ));
    }

    public static Placement place(MinecraftServer server, Identifier id, Surface surface, BlockState ground,
                                  int offset, long seed, int radius) {
        ConfiguredFeature<?, ?> feature = server.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE)
            .getOptional(id)
            .orElseThrow(() -> new IllegalArgumentException("Unknown configured feature " + id));

        List<BoundingBox> groundBoxes = switch (surface) {
            case FLOOR -> List.of(layer(60, radius));
            case CEILING -> List.of(layer(65, radius));
            case WATER, UNDERWATER -> List.of(layer(53, radius));
            case WALL -> List.of(new BoundingBox(1, 48, -radius, 4, 80, radius));
            case CAVE -> List.of(layer(53, radius), layer(72, radius));
        };
        @Nullable BoundingBox waterBox = surface == Surface.WATER || surface == Surface.UNDERWATER
            ? new BoundingBox(-radius, 57, -radius, radius, 63, radius)
            : null;
        BlockPos surfaceBlock = switch (surface) {
            case FLOOR, WATER -> new BlockPos(0, 63, 0);
            case CEILING -> new BlockPos(0, 65, 0);
            case UNDERWATER -> new BlockPos(0, 56, 0);
            case WALL -> new BlockPos(1, 64, 0);
            case CAVE -> new BlockPos(0, 64, 0);
        };
        BlockPos origin = surfaceBlock.above(offset);

        PreviewWorld world = PreviewWorld.create(seed, server.overworld());
        groundBoxes.forEach(box -> world.fillBox(box, ground));
        if (waterBox != null) {
            world.fillBox(waterBox, Blocks.WATER.defaultBlockState());
        }
        // Selectors place nested placed features, whose PlacementContext needs a generator.
        feature.place(world.asLevel(), server.overworld().getChunkSource().getGenerator(), world.random(), origin);
        return new Placement(world, origin, world.changedCount());
    }

    /** Four blocks thick: features check that their base is embedded in solid ground. */
    private static BoundingBox layer(int minY, int radius) {
        return new BoundingBox(-radius, minY, -radius, radius, minY + 3, radius);
    }
}
