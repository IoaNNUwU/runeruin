package ioann.uwu.runeruin.preview;

import ioann.uwu.runeruin.region.RegionExport;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

/**
 * Sparse in-memory {@link WorldGenLevel} for headless structure/feature previews.
 * Writes go into a map; unread cells are air. Unused world APIs no-op or
 * delegate to an optional live {@link ServerLevel}.
 */
public final class PreviewWorld {
    public static final int MIN_Y = 0;
    public static final int HEIGHT = 512;

    private final Map<Long, BlockState> blocks = new HashMap<>();
    private final long seed;
    private final RandomSource random;
    private final WorldGenLevel view;
    private final @Nullable ServerLevel server;

    private PreviewWorld(long seed, @Nullable ServerLevel server) {
        this.seed = seed;
        this.random = RandomSource.create(seed);
        this.server = server;
        this.view = (WorldGenLevel) Proxy.newProxyInstance(
            WorldGenLevel.class.getClassLoader(),
            new Class<?>[] {WorldGenLevel.class},
            this::invoke
        );
    }

    public static PreviewWorld create(long seed) {
        return new PreviewWorld(seed, null);
    }

    public static PreviewWorld create(long seed, ServerLevel server) {
        return new PreviewWorld(seed, server);
    }

    public WorldGenLevel asLevel() {
        return view;
    }

    public RandomSource random() {
        return random;
    }

    public long seed() {
        return seed;
    }

    public BlockState get(BlockPos pos) {
        BlockState state = blocks.get(pos.asLong());
        return state != null ? state : Blocks.AIR.defaultBlockState();
    }

    public void set(BlockPos pos, BlockState state) {
        long key = pos.asLong();
        if (state.isAir()) {
            blocks.remove(key);
        } else {
            blocks.put(key, state);
        }
    }

    public int placedCount() {
        return blocks.size();
    }

    public BoundingBox occupiedBox() {
        if (blocks.isEmpty()) {
            return new BoundingBox(0, 0, 0, 0, 0, 0);
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (long key : blocks.keySet()) {
            cursor.set(key);
            minX = Math.min(minX, cursor.getX());
            minY = Math.min(minY, cursor.getY());
            minZ = Math.min(minZ, cursor.getZ());
            maxX = Math.max(maxX, cursor.getX());
            maxY = Math.max(maxY, cursor.getY());
            maxZ = Math.max(maxZ, cursor.getZ());
        }
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public RegionExport.Snapshot capture(String dimension, BoundingBox box) {
        return RegionExport.capture(dimension, box, this::get);
    }

    public void fillBox(BoundingBox box, BlockState state) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = box.minY(); y <= box.maxY(); y++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int x = box.minX(); x <= box.maxX(); x++) {
                    set(cursor.set(x, y, z), state);
                }
            }
        }
    }

    private Object invoke(Object proxy, Method method, Object @Nullable [] args) throws Throwable {
        return switch (method.getName()) {
            case "setBlock" -> {
                set((BlockPos) args[0], (BlockState) args[1]);
                yield true;
            }
            case "getBlockState" -> get((BlockPos) args[0]);
            case "getFluidState" -> get((BlockPos) args[0]).getFluidState();
            case "removeBlock", "destroyBlock" -> {
                set((BlockPos) args[0], Blocks.AIR.defaultBlockState());
                yield true;
            }
            case "scheduleTick" -> null;
            case "getBlockEntity" -> null;
            case "addFreshEntity" -> false;
            case "ensureCanWrite", "hasChunk", "hasChunkAt" -> true;
            case "getSeed" -> seed;
            case "getRandom" -> random;
            case "getMinY" -> MIN_Y;
            case "getHeight" -> HEIGHT;
            case "getMaxY" -> MIN_Y + HEIGHT - 1;
            case "nextSubTickCount", "getGameTime" -> 0L;
            case "getLevel" -> {
                if (server != null) {
                    yield server;
                }
                throw unsupported(method);
            }
            case "getServer" -> server != null ? server.getServer() : null;
            default -> {
                if (method.isDefault()) {
                    yield InvocationHandler.invokeDefault(proxy, method, args);
                }
                if (server != null && method.getDeclaringClass().isInstance(server)) {
                    yield method.invoke(server, args);
                }
                throw unsupported(method);
            }
        };
    }

    private static UnsupportedOperationException unsupported(Method method) {
        return new UnsupportedOperationException("PreviewWorld does not implement " + method);
    }
}
