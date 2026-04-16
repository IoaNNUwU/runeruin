package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.dimension.RRTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.frog.FrogVariant;
import net.minecraft.world.entity.animal.frog.FrogVariants;
import net.minecraft.world.entity.variant.VariantUtils;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.FossilFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.List;

public class StoneLilyFeature extends Feature<StoneLilyFeature.Config> {

    private static final Identifier STONE_LILY_TEMPLATE = RR.id("stone_lily");

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

        // BlockState slab = config.flowerSlab.getState(level, random, origin);

        BlockState wall = config.trunkWall.getState(level, random, origin);
        BlockState straightWall = wall.setValue(WallBlock.UP, true);

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

        StructureTemplate structureTemplate = manager.get(STONE_LILY_TEMPLATE).orElseThrow();

        Rotation rotation = Rotation.getRandom(random);
        int rand = random.nextIntBetweenInclusive(0, Mirror.values().length - 1);
        Mirror mirror = Mirror.values()[rand];

        // TODO: Look into so called BlockProcessors. Maybe they allow to not replace existing blocks.

        var blockProcessor = new ProtectedBlockProcessor(RRTags.VEGETABLES_NON_REPLACEABLE);

        structureTemplate.placeInWorld(
                level,
                currentBlockState,
                currentBlockState,
                new StructurePlaceSettings()
                        .setRotation(rotation)
                        .setMirror(mirror)
                        .addProcessor(blockProcessor)
                ,
                random,
                1
                );

        // --- Frog xD ---

        currentBlockState.move(Direction.UP);

        int frogRand = random.nextIntBetweenInclusive(0, 9);
        int nFrogs;
        if (frogRand == 9) {
            nFrogs = 2;
        } else if (frogRand == 8) {
            nFrogs = 1;
        } else {
            nFrogs = 0;
        }

        var frogVariants = ctx.level().holderLookup(Registries.FROG_VARIANT);

        Holder<FrogVariant> greenFrogVariant = frogVariants.get(FrogVariants.COLD).orElseThrow();

        for (int i = 0; i < nFrogs; i++) {
            Frog frog = new Frog(EntityType.FROG, level.getLevel());
            frog.setComponent(DataComponents.FROG_VARIANT, greenFrogVariant);
            frog.setPos(Vec3.atCenterOf(currentBlockState));

            frog.setBaby(random.nextBoolean());

            level.addFreshEntity(frog);
            currentBlockState.move(Direction.UP);
        }

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
