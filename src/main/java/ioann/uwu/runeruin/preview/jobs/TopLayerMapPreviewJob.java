package ioann.uwu.runeruin.preview.jobs;

import ioann.uwu.runeruin.dimension.RRBiomeSource;
import ioann.uwu.runeruin.dimension.RRBiomes;
import ioann.uwu.runeruin.dimension.chunkgenerator.TopLayerAndBloomingCavesGen;
import ioann.uwu.runeruin.preview.HeadlessTerrainGenerator;
import ioann.uwu.runeruin.preview.PreviewArgs;
import ioann.uwu.runeruin.preview.PreviewJob;
import ioann.uwu.runeruin.preview.PreviewJobs;
import ioann.uwu.runeruin.preview.PreviewWorld;
import ioann.uwu.runeruin.region.RegionExport;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * A map of the top layer from above, without building chunks: each island coloured by its biome and shaded by
 * the height of its ground, water in blue. Shows where the biomes lie and the land they shape.
 */
public final class TopLayerMapPreviewJob implements PreviewJob {

    private static final Map<ResourceKey<Biome>, Integer> COLORS = Map.of(
            RRBiomes.ELDEN_GARDEN, 0xE59A6E,
            RRBiomes.SIMILAR_FOREST, 0x2E7D32,
            RRBiomes.PLAINS, 0x9CCC65,
            RRBiomes.HILLS, 0x7A8F4A,
            RRBiomes.SWAMP, 0x4E6E5D,
            RRBiomes.PLATEAU, 0xC08A3E,
            RRBiomes.DESERT, 0xE8D9A0
    );
    private static final int VOID = 0x101820;
    private static final int WATER = 0x3F76E4;

    @Override
    public String id() {
        return "top_layer_map";
    }

    @Override
    public String description() {
        return "Map of the top layer from above: biomes, height, water. params: x, z (centre), radius, step";
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args) {
        throw new IllegalStateException("top_layer_map requires a running server registry");
    }

    @Override
    public PreviewJobs.Result run(PreviewArgs args, MinecraftServer server) throws IOException {
        int centerX = args.getInt("x", 0);
        int centerZ = args.getInt("z", 0);
        int radius = args.getInt("radius", 512);
        int step = args.getInt("step", 2);
        if (radius < 16 || step < 1 || radius / step > 2048) {
            throw new IllegalArgumentException("top_layer_map needs radius >= 16, step >= 1 and at most 4096 pixels a side");
        }
        RRBiomeSource biomes = (RRBiomeSource) HeadlessTerrainGenerator.generator(server).getBiomeSource();
        RandomState randomState = HeadlessTerrainGenerator.randomState(server, args.seed());

        int size = 2 * radius / step;
        int[] heights = new int[size * size];
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        Map<String, Integer> counts = new TreeMap<>();
        for (int px = 0; px < size; px++) {
            for (int pz = 0; pz < size; pz++) {
                int x = centerX - radius + px * step;
                int z = centerZ - radius + pz * step;
                int y = TopLayerAndBloomingCavesGen.topLayerSurfaceY(x, z, randomState).orElse(Integer.MIN_VALUE);
                heights[px * size + pz] = y;
                if (y != Integer.MIN_VALUE) {
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        for (int px = 0; px < size; px++) {
            for (int pz = 0; pz < size; pz++) {
                int y = heights[px * size + pz];
                if (y == Integer.MIN_VALUE) {
                    image.setRGB(px, pz, VOID);
                    continue;
                }
                int x = centerX - radius + px * step;
                int z = centerZ - radius + pz * step;
                Holder<Biome> biome = biomes.topLayerBiome(x, z, randomState.sampler());
                boolean water = TopLayerAndBloomingCavesGen.topLayerWaterY(x, z, randomState).isPresent();
                counts.merge(water ? "water" : biome.getRegisteredName(), 1, Integer::sum);
                int color = water ? WATER : biome.unwrapKey().map(COLORS::get).orElse(0x808080);
                // Lit from the north-west, brighter the higher the ground.
                int northWest = px > 0 && pz > 0 && heights[(px - 1) * size + pz - 1] != Integer.MIN_VALUE ? heights[(px - 1) * size + pz - 1] : y;
                float light = 0.45f + 0.85f * (y - minY) / Math.max(1, maxY - minY) + 0.15f * (y - northWest) / step;
                image.setRGB(px, pz, shade(color, Mth.clamp(light, 0.35f, 1.4f)));
            }
        }

        String name = RegionExport.sanitizeName(args.name("top_layer_map"));
        Files.createDirectories(args.exportDir());
        Path png = args.exportDir().resolve(name + ".png");
        ImageIO.write(image, "png", png.toFile());
        Path info = args.exportDir().resolve(name + "_info.txt");
        List<String> lines = new ArrayList<>(List.of(
                "# job: " + id(),
                "# seed: " + args.seed(),
                "# centre: " + centerX + " " + centerZ + ", radius " + radius + ", step " + step + " (x to the right, z down)",
                "# ground Y: " + minY + " .. " + maxY,
                "# colours:"
        ));
        COLORS.forEach((biome, color) -> lines.add(String.format("#   %06X %s", color, biome.identifier())));
        lines.add(String.format("#   %06X water, %06X no island", WATER, VOID));
        int land = counts.values().stream().mapToInt(Integer::intValue).sum();
        lines.add("# share of the islands:");
        counts.forEach((what, count) -> lines.add(String.format(Locale.ROOT, "#   %5.1f%% %s", 100f * count / land, what)));
        Files.write(info, lines);
        return new PreviewJobs.Result(PreviewWorld.create(args.seed()), null, png, List.of(png, info));
    }

    private static int shade(int color, float light) {
        int r = Math.min(255, (int) ((color >> 16 & 0xFF) * light));
        int g = Math.min(255, (int) ((color >> 8 & 0xFF) * light));
        int b = Math.min(255, (int) ((color & 0xFF) * light));
        return r << 16 | g << 8 | b;
    }
}
