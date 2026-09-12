package ioann.uwu.runeruin.region;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Writes a cuboid as {@code runeruin.region/1}: palette + Y-layers of X-strings.
 * A 2D projection {@code .txt} is added when any axis is 1 block thick.
 */
public final class RegionExport {
    public static final String FORMAT = "runeruin.region/1";
    public static final int MAX_VOLUME = 500_000;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private RegionExport() {}

    public static Path resolveExportDir(MinecraftServer server) {
        Path gameDir = server.getServerDirectory().toAbsolutePath().normalize();
        Path parent = gameDir.getParent();
        if (parent != null && Files.isDirectory(parent.resolve("src"))) {
            return parent.resolve("exports");
        }
        return gameDir.resolve("runeruin-exports");
    }

    public static String sanitizeName(String raw) {
        String cleaned = raw.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (cleaned.length() > 64) {
            cleaned = cleaned.substring(0, 64);
        }
        return cleaned.isEmpty() ? "region_" + FILE_TIME.format(LocalDateTime.now()) : cleaned;
    }

    public static String defaultName() {
        return "region_" + FILE_TIME.format(LocalDateTime.now());
    }

    public static Result write(ServerLevel level, BoundingBox box, String name) throws IOException, UnloadedChunkException {
        return write(scan(level, box), resolveExportDir(level.getServer()), name);
    }

    /**
     * Builds a snapshot from any block lookup. Missing positions become air.
     * Used by in-game {@code /rrexport} (via {@link #scan}) and headless previews.
     */
    public static Snapshot capture(String dimension, BoundingBox box, java.util.function.Function<BlockPos, BlockState> blocks) {
        int sizeX = box.getXSpan();
        int sizeY = box.getYSpan();
        int sizeZ = box.getZSpan();
        Palette palette = new Palette();
        char[][][] grid = new char[sizeY][sizeZ][sizeX];
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    cursor.set(box.minX() + x, box.minY() + y, box.minZ() + z);
                    BlockState state = blocks.apply(cursor);
                    if (state == null) {
                        state = Blocks.AIR.defaultBlockState();
                    }
                    grid[y][z][x] = palette.tokenFor(BlockStateParser.serialize(state), state);
                }
            }
        }

        return new Snapshot(dimension, box, sizeX, sizeY, sizeZ, palette, grid);
    }

    public static Result write(Snapshot snapshot, Path dir, String name) throws IOException {
        Files.createDirectories(dir);
        Path jsonPath = dir.resolve(name + ".json");
        Files.writeString(jsonPath, toJson(snapshot), StandardCharsets.UTF_8);

        Path txtPath = null;
        if (snapshot.sizeX == 1 || snapshot.sizeY == 1 || snapshot.sizeZ == 1) {
            txtPath = dir.resolve(name + ".txt");
            Files.writeString(txtPath, toProjectionText(snapshot), StandardCharsets.UTF_8);
        }
        return new Result(jsonPath, txtPath, snapshot);
    }

    static Snapshot scan(ServerLevel level, BoundingBox box) throws UnloadedChunkException {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = 0; y < box.getYSpan(); y++) {
            for (int z = 0; z < box.getZSpan(); z++) {
                for (int x = 0; x < box.getXSpan(); x++) {
                    cursor.set(box.minX() + x, box.minY() + y, box.minZ() + z);
                    if (!level.hasChunkAt(cursor)) {
                        throw new UnloadedChunkException(cursor.immutable());
                    }
                }
            }
        }
        return capture(level.dimension().identifier().toString(), box, level::getBlockState);
    }

    static String toJson(Snapshot snap) {
        JsonObject root = new JsonObject();
        root.addProperty("format", FORMAT);
        root.addProperty("dimension", snap.dimension);

        JsonArray origin = new JsonArray();
        origin.add(snap.box.minX());
        origin.add(snap.box.minY());
        origin.add(snap.box.minZ());
        root.add("origin", origin);

        JsonArray size = new JsonArray();
        size.add(snap.sizeX);
        size.add(snap.sizeY);
        size.add(snap.sizeZ);
        root.add("size", size);

        root.addProperty("volume", snap.volume());
        root.addProperty("projection", snap.projection());
        root.addProperty(
            "axes",
            "local (0,0,0) = origin; +X east (right in rows); +Z south (down the page); +Y up (layers bottom to top)"
        );

        JsonObject palette = new JsonObject();
        JsonObject counts = new JsonObject();
        for (Map.Entry<Character, String> entry : snap.palette.tokenToState.entrySet()) {
            String token = String.valueOf(entry.getKey());
            palette.addProperty(token, entry.getValue());
            counts.addProperty(token, snap.palette.counts.getOrDefault(entry.getKey(), 0));
        }
        root.add("palette", palette);
        root.add("counts", counts);

        JsonArray layers = new JsonArray();
        for (int y = 0; y < snap.sizeY; y++) {
            JsonArray layer = new JsonArray();
            for (int z = 0; z < snap.sizeZ; z++) {
                layer.add(new String(snap.grid[y][z]));
            }
            layers.add(layer);
        }
        root.add("layers", layers);
        return GSON.toJson(root) + "\n";
    }

    static String toProjectionText(Snapshot snap) {
        StringBuilder out = new StringBuilder();
        header(out, snap);
        legend(out, snap);

        if (snap.sizeY == 1) {
            out.append("# projection: XZ  (top-down, looking -Y; +X right, +Z down)\n");
            out.append("# world Y = ").append(snap.box.minY()).append('\n');
            out.append("#\n");
            appendGrid(out, snap.grid[0], snap.sizeX, snap.sizeZ, "X", "Z", snap.box.minX(), snap.box.minZ(), false);
        } else if (snap.sizeZ == 1) {
            out.append("# projection: XY  (elevation looking +Z / south; +X right, +Y up)\n");
            out.append("# world Z = ").append(snap.box.minZ()).append('\n');
            out.append("#\n");
            char[][] xy = new char[snap.sizeY][snap.sizeX];
            for (int y = 0; y < snap.sizeY; y++) {
                System.arraycopy(snap.grid[y][0], 0, xy[y], 0, snap.sizeX);
            }
            appendGrid(out, xy, snap.sizeX, snap.sizeY, "X", "Y", snap.box.minX(), snap.box.minY(), true);
        } else {
            out.append("# projection: YZ  (elevation looking +X / east; +Z right, +Y up)\n");
            out.append("# world X = ").append(snap.box.minX()).append('\n');
            out.append("#\n");
            char[][] yz = new char[snap.sizeY][snap.sizeZ];
            for (int y = 0; y < snap.sizeY; y++) {
                for (int z = 0; z < snap.sizeZ; z++) {
                    yz[y][z] = snap.grid[y][z][0];
                }
            }
            appendGrid(out, yz, snap.sizeZ, snap.sizeY, "Z", "Y", snap.box.minZ(), snap.box.minY(), true);
        }
        return out.toString();
    }

    private static void header(StringBuilder out, Snapshot snap) {
        out.append("# ").append(FORMAT).append('\n');
        out.append("# dimension: ").append(snap.dimension).append('\n');
        out.append("# origin (inclusive min): ")
            .append(snap.box.minX()).append(' ')
            .append(snap.box.minY()).append(' ')
            .append(snap.box.minZ()).append('\n');
        out.append("# size: ").append(snap.sizeX).append(" x ").append(snap.sizeY).append(" x ").append(snap.sizeZ)
            .append("  (X Y Z)\n");
        out.append("# volume: ").append(snap.volume()).append('\n');
        out.append("# local (0,0,0) = origin; world = origin + local\n");
        out.append("#\n");
    }

    private static void legend(StringBuilder out, Snapshot snap) {
        out.append("# legend:\n");
        List<Map.Entry<Character, String>> entries = new ArrayList<>(snap.palette.tokenToState.entrySet());
        entries.sort(Comparator.comparingInt((Map.Entry<Character, String> e) -> snap.palette.counts.getOrDefault(e.getKey(), 0)).reversed());
        int maxState = 0;
        for (Map.Entry<Character, String> entry : entries) {
            maxState = Math.max(maxState, entry.getValue().length());
        }
        for (Map.Entry<Character, String> entry : entries) {
            int count = snap.palette.counts.getOrDefault(entry.getKey(), 0);
            out.append("#   ").append(entry.getKey()).append("  ");
            out.append(String.format(Locale.ROOT, "%-" + maxState + "s", entry.getValue()));
            out.append("  ").append(count).append('\n');
        }
        out.append("#\n");
    }

    /**
     * @param flipVertical if true, first row is the high end of the vertical axis (Y up on the page)
     */
    private static void appendGrid(
        StringBuilder out,
        char[][] rows,
        int width,
        int height,
        String hName,
        String vName,
        int hOrigin,
        int vOrigin,
        boolean flipVertical
    ) {
        int indexWidth = Math.max(String.valueOf(Math.max(height - 1, 0)).length(), vName.length());
        out.append("# ").append(" ".repeat(indexWidth + 1)).append(hName).append(" →  (world ").append(hName)
            .append(" = ").append(hOrigin).append(" + local)\n");
        appendRuler(out, width, indexWidth);
        for (int row = 0; row < height; row++) {
            int localV = flipVertical ? height - 1 - row : row;
            char[] line = rows[localV];
            if (row == 0) {
                out.append("# ").append(String.format(Locale.ROOT, "%" + indexWidth + "s", vName)).append(' ');
            } else if (row == 1) {
                out.append("# ").append(String.format(Locale.ROOT, "%" + indexWidth + "s", flipVertical ? "↑" : "↓")).append(' ');
            } else {
                out.append("# ").append(" ".repeat(indexWidth)).append(' ');
            }
            out.append(String.format(Locale.ROOT, "%" + indexWidth + "d", localV)).append("  ");
            out.append(line).append('\n');
        }
        out.append("# world ").append(vName).append(" = ").append(vOrigin).append(" + local\n");
    }

    private static void appendRuler(StringBuilder out, int width, int indexWidth) {
        String indent = "# " + " ".repeat(indexWidth + 1 + indexWidth + 2);
        if (width >= 10) {
            out.append(indent);
            for (int x = 0; x < width; x++) {
                out.append(x % 10 == 0 ? (char) ('0' + ((x / 10) % 10)) : ' ');
            }
            out.append('\n');
        }
        out.append(indent);
        for (int x = 0; x < width; x++) {
            out.append(x % 10);
        }
        out.append('\n');
    }

    public record Result(Path jsonPath, @org.jspecify.annotations.Nullable Path txtPath, Snapshot snapshot) {
        public boolean hasProjection() {
            return txtPath != null;
        }
    }

    public static final class Snapshot {
        final String dimension;
        final BoundingBox box;
        final int sizeX;
        final int sizeY;
        final int sizeZ;
        final Palette palette;
        final char[][][] grid;

        Snapshot(String dimension, BoundingBox box, int sizeX, int sizeY, int sizeZ, Palette palette, char[][][] grid) {
            this.dimension = dimension;
            this.box = box;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.palette = palette;
            this.grid = grid;
        }

        int volume() {
            return sizeX * sizeY * sizeZ;
        }

        String projection() {
            if (sizeY == 1) {
                return "xz";
            }
            if (sizeZ == 1) {
                return "xy";
            }
            if (sizeX == 1) {
                return "yz";
            }
            return "xyz";
        }

        public int countMatching(java.util.function.Predicate<String> stateId) {
            int total = 0;
            for (Map.Entry<Character, String> entry : palette.tokenToState.entrySet()) {
                if (stateId.test(entry.getValue())) {
                    total += palette.counts.getOrDefault(entry.getKey(), 0);
                }
            }
            return total;
        }

        public Map<String, Integer> stateCounts() {
            Map<String, Integer> out = new LinkedHashMap<>();
            for (Map.Entry<Character, String> entry : palette.tokenToState.entrySet()) {
                out.put(entry.getValue(), palette.counts.getOrDefault(entry.getKey(), 0));
            }
            return out;
        }
    }

    static final class Palette {
        private static final String POOL = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789#*+=~@%&?:;<>^";
        private static final Map<String, Character> PREFERRED = preferred();

        final Map<String, Character> stateToToken = new LinkedHashMap<>();
        final Map<Character, String> tokenToState = new LinkedHashMap<>();
        final Map<Character, Integer> counts = new LinkedHashMap<>();
        private int nextPool = 0;

        char tokenFor(String state, BlockState blockState) {
            Character existing = stateToToken.get(state);
            if (existing != null) {
                counts.merge(existing, 1, Integer::sum);
                return existing;
            }
            char token = allocate(state, blockState);
            stateToToken.put(state, token);
            tokenToState.put(token, state);
            counts.put(token, 1);
            return token;
        }

        private char allocate(String state, BlockState blockState) {
            if (blockState.getBlock() == Blocks.AIR) {
                return claim('.', state);
            }
            if (blockState.getBlock() == Blocks.CAVE_AIR) {
                return claimOrNext(',', state);
            }
            if (blockState.getBlock() == Blocks.VOID_AIR) {
                return claimOrNext('\'', state);
            }
            String id = state;
            int bracket = id.indexOf('[');
            if (bracket >= 0) {
                id = id.substring(0, bracket);
            }
            Character preferred = PREFERRED.get(id);
            if (preferred != null && !tokenToState.containsKey(preferred)) {
                return preferred;
            }
            String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
            String last = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
            for (int i = 0; i < last.length(); i++) {
                char c = Character.toUpperCase(last.charAt(i));
                if (c >= 'A' && c <= 'Z' && !tokenToState.containsKey(c)) {
                    return c;
                }
            }
            for (int i = 0; i < last.length(); i++) {
                char c = Character.toLowerCase(last.charAt(i));
                if (c >= 'a' && c <= 'z' && !tokenToState.containsKey(c)) {
                    return c;
                }
            }
            return nextFree();
        }

        private char claim(char token, String state) {
            tokenToState.put(token, state);
            return token;
        }

        private char claimOrNext(char token, String state) {
            if (!tokenToState.containsKey(token)) {
                return claim(token, state);
            }
            return nextFree();
        }

        private char nextFree() {
            while (nextPool < POOL.length()) {
                char c = POOL.charAt(nextPool++);
                if (!tokenToState.containsKey(c)) {
                    return c;
                }
            }
            throw new IllegalStateException("Too many unique block states in selection (max " + POOL.length() + ")");
        }

        private static Map<String, Character> preferred() {
            Map<String, Character> map = new LinkedHashMap<>();
            map.put("minecraft:air", '.');
            map.put("minecraft:cave_air", ',');
            map.put("minecraft:void_air", '\'');
            map.put("minecraft:water", 'W');
            map.put("minecraft:lava", 'L');
            map.put("minecraft:stone", 'S');
            map.put("minecraft:deepslate", 'D');
            map.put("minecraft:dirt", 'd');
            map.put("minecraft:grass_block", 'G');
            map.put("minecraft:moss_block", 'm');
            map.put("minecraft:oak_log", 'O');
            map.put("runeruin:arcane_stone", 'A');
            map.put("runeruin:arcane_stone_bricks", 'B');
            map.put("runeruin:polished_arcane_stone", 'P');
            map.put("runeruin:arcane_stone_pillar", 'I');
            map.put("runeruin:arcane_stone_column", 'C');
            map.put("runeruin:diamond_arcane_stone", 'V');
            map.put("runeruin:glowing_moss", 'M');
            map.put("runeruin:glowing_moss_carpet", 'c');
            map.put("runeruin:elden_log", 'E');
            map.put("runeruin:elden_wood", 'w');
            map.put("runeruin:elden_leaves", 'F');
            map.put("runeruin:elden_planks", 'e');
            map.put("runeruin:moss_light", 'T');
            map.put("runeruin:lapis_light", 'U');
            return map;
        }
    }

    public static final class UnloadedChunkException extends Exception {
        public final BlockPos pos;

        UnloadedChunkException(BlockPos pos) {
            super("Unloaded chunk at " + pos);
            this.pos = pos;
        }
    }
}
