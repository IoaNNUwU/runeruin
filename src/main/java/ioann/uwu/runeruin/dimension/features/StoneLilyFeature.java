package ioann.uwu.runeruin.dimension.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ioann.uwu.runeruin.dimension.RRTags;
import ioann.uwu.runeruin.dimension.structures.TemplatePiece;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.frog.FrogVariant;
import net.minecraft.world.entity.animal.frog.FrogVariants;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProtectedBlockProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.Vec3;

public class StoneLilyFeature extends Feature<StoneLilyFeature.Config> {

    /** The lilies that a person built: a template of this folder, picked at random, is the flower on the trunk. */
    private static final String STONE_LILY_FOLDER = "stone_lily";
    private static final Direction[] TRUNK_DIRECTIONS = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

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
            level.setBlock(rootPos, root, Block.UPDATE_CLIENTS);
        }

        BlockPos.MutableBlockPos cursor = origin.mutable().move(Direction.UP, 2);

        if (random.nextBoolean()) {
            level.setBlock(cursor, straightWall, Block.UPDATE_CLIENTS);
            cursor.move(Direction.UP);
        }

        int nSegments = random.nextIntBetweenInclusive(2, 7);

        // --- Trunk ---
        for (int i = 0; i < nSegments; i++) {
            Direction direction = Util.getRandom(TRUNK_DIRECTIONS, random);

            level.setBlock(cursor, straightWall.setValue(WallBlock.PROPERTY_BY_DIRECTION.get(direction), WallSide.LOW), Block.UPDATE_CLIENTS);
            cursor.move(direction);
            level.setBlock(cursor, straightWall.setValue(WallBlock.PROPERTY_BY_DIRECTION.get(direction.getOpposite()), WallSide.LOW), Block.UPDATE_CLIENTS);
            cursor.move(Direction.UP);
            level.setBlock(cursor, straightWall, Block.UPDATE_CLIENTS);
            cursor.move(Direction.UP);
        }

        // --- Lily: a template of the folder, with its middle on the trunk ---
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(Rotation.getRandom(random))
                .setMirror(Util.getRandom(Mirror.values(), random))
                .addProcessor(new ProtectedBlockProcessor(
                        level.registryAccess().lookupOrThrow(Registries.BLOCK).getOrThrow(RRTags.VEGETABLES_NON_REPLACEABLE)
                ))
                // The template is built with vanilla moss carpets.
                .addProcessor(new RuleProcessor(List.of(new ProcessorRule(
                        new BlockMatchTest(Blocks.MOSS_CARPET),
                        AlwaysTrueTest.INSTANCE,
                        config.mossCarpet.getState(level, random, origin)
                ))));
        StructureTemplateManager templates = level.getLevel().getStructureManager();
        StructureTemplate template = templates.getOrCreate(Util.getRandom(TemplatePiece.variants(templates, STONE_LILY_FOLDER), random));
        Vec3i size = template.getSize();
        BlockPos corner = cursor.subtract(StructureTemplate.calculateRelativePosition(settings, new BlockPos((size.getX() - 1) / 2, 0, (size.getZ() - 1) / 2)));
        template.placeInWorld(level, corner, corner, settings, random, Block.UPDATE_CLIENTS);

        // --- Frogs: one lily in ten has one, another one in ten has two ---
        cursor.move(Direction.UP);
        int frogs = Math.max(0, random.nextInt(10) - 7);
        Holder<FrogVariant> greenFrog = level.holderLookup(Registries.FROG_VARIANT).get(FrogVariants.COLD).orElseThrow();
        for (int i = 0; i < frogs; i++) {
            Frog frog = new Frog(EntityTypes.FROG, level.getLevel());
            frog.setComponent(DataComponents.FROG_VARIANT, greenFrog);
            frog.setPos(Vec3.atCenterOf(cursor));
            frog.setBaby(random.nextBoolean());
            level.addFreshEntity(frog);
            cursor.move(Direction.UP);
        }

        return true;
    }

    public record Config(
            BlockStateProvider rootBlock,
            BlockStateProvider trunkWall,
            BlockStateProvider mossCarpet
    ) implements FeatureConfiguration {

        public static final Codec<Config> CODEC = RecordCodecBuilder.create(codec ->
                codec.group(
                        BlockStateProvider.CODEC.fieldOf("root_block").forGetter(Config::rootBlock),
                        BlockStateProvider.CODEC.fieldOf("trunk_block").forGetter(Config::trunkWall),
                        BlockStateProvider.CODEC.fieldOf("moss_carpet").forGetter(Config::mossCarpet)
                ).apply(codec, Config::new));
    }
}
