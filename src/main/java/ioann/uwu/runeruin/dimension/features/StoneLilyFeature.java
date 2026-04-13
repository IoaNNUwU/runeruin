package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ioann.uwu.runeruin.RR;
import net.minecraft.IdentifierException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.List;

public class StoneLilyFeature extends Feature<StoneLilyFeature.Config> {

    private static final Identifier STONE_LILY = RR.id("stone_lily");

    public StoneLilyFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        Config config = ctx.config();

        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        BlockPos origin = ctx.origin();

        BlockState root = config.rootBlock.getState(level, random, origin);

        BlockState slab = config.flowerSlab.getState(level, random, origin);
        BlockState topSlab = slab.setValue(SlabBlock.TYPE, SlabType.TOP);
        BlockState bottomSlab = slab.setValue(SlabBlock.TYPE, SlabType.BOTTOM);

        BlockState wall = config.trunkWall.getState(level, random, origin);
        BlockState straightWall = wall.setValue(WallBlock.UP, true);
        BlockState wallX = straightWall.setValue(WallBlock.NORTH, WallSide.LOW);
        BlockState wallNX = straightWall.setValue(WallBlock.SOUTH, WallSide.LOW);
        BlockState wallZ = straightWall.setValue(WallBlock.EAST, WallSide.LOW);
        BlockState wallNZ = straightWall.setValue(WallBlock.WEST, WallSide.LOW);

        List<BlockPos> roots = List.of(
                origin,
                origin.above(),
                origin.below(),
                origin.north(),
                origin.south(),
                origin.east(),
                origin.west()
        );

        for (BlockPos rootPos : roots) {
            level.setBlock(rootPos, root, 1);
        }

        BlockPos.MutableBlockPos currentBlockState = origin.mutable();
        currentBlockState.setY(currentBlockState.getY() + 2);

        if (random.nextBoolean()) {
            level.setBlock(currentBlockState, straightWall, 1);
            currentBlockState.setY(currentBlockState.getY() + 1);
        }

        int nSegments = random.nextIntBetweenInclusive(2, 7);

        // --- Trunk ---
        for (int i = 0; i < nSegments; i++) {

            int rand = random.nextIntBetweenInclusive(0, 3);
            var dirs = new Direction[] {
                    Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
            };
            Direction direction = dirs[rand];

            BlockState wallBlock = straightWall.setValue(
                    WallBlock.PROPERTY_BY_DIRECTION.get(direction),
                    WallSide.LOW
            );
            level.setBlock(currentBlockState, wallBlock, 1);

            currentBlockState.move(direction);

            wallBlock = straightWall.setValue(
                    WallBlock.PROPERTY_BY_DIRECTION.get(direction.getOpposite()),
                    WallSide.LOW
            );
            level.setBlock(currentBlockState, wallBlock, 1);

            currentBlockState.move(Direction.UP);

            level.setBlock(currentBlockState, straightWall, 1);
            currentBlockState.move(Direction.UP);
        }

        // --- Lily ---

        StructureTemplateManager manager = level.getLevel().getStructureManager();

        StructureTemplate structureTemplate = manager.get(STONE_LILY).orElseThrow();

        Rotation rotation = Rotation.getRandom(random);
        int rand = random.nextIntBetweenInclusive(0, Mirror.values().length - 1);
        Mirror mirror = Mirror.values()[rand];

        structureTemplate.placeInWorld(
                level,
                currentBlockState,
                currentBlockState,
                new StructurePlaceSettings()
                        .setRotation(rotation)
                        .setMirror(mirror)
                ,
                random,
                1
                );

        return true;
    }

    public record Config(
            BlockStateProvider rootBlock,
            BlockStateProvider trunkWall,
            BlockStateProvider flowerSlab,
            BlockStateProvider mossCarpet
    ) implements FeatureConfiguration {

        public static final Codec<Config> CODEC = RecordCodecBuilder.create(codec ->
                codec.group(
                        BlockStateProvider.CODEC.fieldOf("root_block").forGetter(Config::rootBlock),
                        BlockStateProvider.CODEC.fieldOf("trunk_block").forGetter(Config::trunkWall),
                        BlockStateProvider.CODEC.fieldOf("flower_block").forGetter(Config::flowerSlab),
                        BlockStateProvider.CODEC.fieldOf("moss_carpet").forGetter(Config::mossCarpet)
                ).apply(codec, Config::new));
    }
}
