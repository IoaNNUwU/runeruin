package ioann.uwu.runeruin.dimension.structures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/** The shape of a dinosaur skeleton: a bent spine with a rib cage, from the neck (start) to the tail tip. */
public final class DinosaurSkeletonGenerator {

    private static final int SAMPLES_PER_BLOCK = 2;
    private static final int VERTEBRA_SPACING = 3;
    private static final double RIB_CAGE_START = 0.15;
    private static final double RIB_CAGE_MIDDLE = 0.4;
    private static final double RIB_CAGE_END = 0.65;
    /** How far around the body a rib goes, in radians from the spine; the rest is the open belly. */
    private static final double RIB_ARC = 2.3;

    private DinosaurSkeletonGenerator() {}

    /** Bone blocks by position relative to the middle of the spine. The same seed gives the same skeleton. */
    public static Map<BlockPos, BlockState> generate(long seed) {
        RandomSource random = RandomSource.create(seed);
        int length = random.nextIntBetweenInclusive(30, 40);
        double yaw = random.nextDouble() * Math.PI * 2.0;
        double bend = (random.nextDouble() - 0.5) * 1.6;
        double wave = (random.nextDouble() - 0.5) * 0.8;
        double slope = (random.nextDouble() - 0.5) * 0.2;
        // 0 lies on the belly, half a turn on the back with the ribs up.
        double roll = random.nextDouble() * Math.PI * 2.0;
        double twist = (random.nextDouble() - 0.5) * 0.8;
        double ribRadius = 5.5 + random.nextDouble() * 2.0;

        // The rib cage holds the spine up, the neck and the tail come down to the ground.
        double lift = ribRadius * (1.0 + Math.cos(roll));
        int samples = length * SAMPLES_PER_BLOCK + 1;
        Vec3[] spine = new Vec3[samples];
        double x = 0.0;
        double z = 0.0;
        for (int i = 0; i < samples; i++) {
            double t = i / (double) (samples - 1);
            double hump = t < RIB_CAGE_MIDDLE ? t / RIB_CAGE_MIDDLE : (1.0 - t) / (1.0 - RIB_CAGE_MIDDLE);
            spine[i] = new Vec3(x, lift * Mth.smoothstep(hump) + slope * length * (t - 0.5), z);
            double heading = yaw + bend * (t - 0.5) + wave * Math.sin(t * Math.PI * 2.0);
            x += Math.cos(heading) / SAMPLES_PER_BLOCK;
            z += Math.sin(heading) / SAMPLES_PER_BLOCK;
        }
        Vec3 middle = new Vec3(spine[samples / 2].x, 0.0, spine[samples / 2].z);

        Map<BlockPos, BlockState> bones = new HashMap<>();
        for (int i = 0; i < samples; i++) {
            double t = i / (double) (samples - 1);
            Vec3 center = spine[i].subtract(middle);
            Vec3 tangent = spine[Math.min(i + 1, samples - 1)].subtract(spine[Math.max(i - 1, 0)]).normalize();
            double thickness = 0.5 + 0.8 * Math.sin(Math.PI * Math.pow(t, 0.7));
            addBall(bones, center, thickness, bone(Direction.getApproximateNearest(tangent)));

            if (i % (VERTEBRA_SPACING * SAMPLES_PER_BLOCK) != 0) {
                continue;
            }
            Vec3 side = new Vec3(-tangent.z, 0.0, tangent.x).normalize();
            Vec3 up = side.cross(tangent);
            double angle = roll + twist * (t - 0.5);
            Vec3 back = up.scale(Math.cos(angle)).add(side.scale(Math.sin(angle)));
            Vec3 flank = side.scale(Math.cos(angle)).subtract(up.scale(Math.sin(angle)));

            BlockPos vertebra = BlockPos.containing(center);
            addLine(bones, vertebra, BlockPos.containing(center.add(back.scale(thickness * 2.0 + 0.5))));

            if (t < RIB_CAGE_START || t > RIB_CAGE_END) {
                continue;
            }
            double cage = (t - RIB_CAGE_START) / (RIB_CAGE_END - RIB_CAGE_START);
            double radius = ribRadius * (0.55 + 0.45 * Math.sin(cage * Math.PI));
            for (int flankSign = -1; flankSign <= 1; flankSign += 2) {
                BlockPos previous = vertebra;
                for (double arc = 0.0; arc <= RIB_ARC; arc += 0.5 / radius) {
                    Vec3 point = center
                            .add(flank.scale(flankSign * radius * Math.sin(arc)))
                            .subtract(back.scale(radius * (1.0 - Math.cos(arc))));
                    previous = addLine(bones, previous, BlockPos.containing(point));
                }
            }
        }
        return bones;
    }

    private static void addBall(Map<BlockPos, BlockState> bones, Vec3 center, double radius, BlockState bone) {
        bones.put(BlockPos.containing(center), bone);
        int reach = Mth.ceil(radius);
        for (BlockPos pos : BlockPos.betweenClosed(
                BlockPos.containing(center.subtract(reach)), BlockPos.containing(center.add(reach)))) {
            if (pos.distToCenterSqr(center) <= radius * radius) {
                bones.put(pos.immutable(), bone);
            }
        }
    }

    /** Blocks joined by their faces from {@code from} (not placed) to {@code to}; returns {@code to}. */
    private static BlockPos addLine(Map<BlockPos, BlockState> bones, BlockPos from, BlockPos to) {
        BlockPos.MutableBlockPos pos = from.mutable();
        while (!pos.equals(to)) {
            Direction step = Direction.getApproximateNearest(
                    to.getX() - pos.getX(), to.getY() - pos.getY(), to.getZ() - pos.getZ());
            bones.putIfAbsent(pos.move(step).immutable(), bone(step));
        }
        return to;
    }

    private static BlockState bone(Direction along) {
        return Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, along.getAxis());
    }
}
