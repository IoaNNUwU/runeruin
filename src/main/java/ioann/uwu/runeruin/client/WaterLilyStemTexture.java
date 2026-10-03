package ioann.uwu.runeruin.client;

import ioann.uwu.runeruin.RR;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

import static ioann.uwu.runeruin.blocks.WaterLilyLeafBlock.MAX_HEIGHT;
import static ioann.uwu.runeruin.blocks.WaterLilyLeafBlock.REACH;

/**
 * Sprite sheet with pixel-art stems for every leaf offset. Columns are the
 * horizontal distances a leaf can have from its root; rows are its height,
 * each with {@link #VARIANTS} differently bent stems.
 *
 * <p>Each cell is the vertical plane from the root to the leaf at 16 pixels
 * per block, so the stem's pixels stay level with the ground and its curve
 * steps like stairs. Datagen draws the sheet; the root model only maps cells.</p>
 */
public final class WaterLilyStemTexture {
    public static final Identifier SPRITE = RR.id("block/water_lily_stems");
    /** Where stems leave the root block, in blocks. */
    public static final float START_Y = 13.0F / 16.0F;
    public static final int VARIANTS = 4;
    public static final int CELL_WIDTH = 80;
    /** Pixels below the stem start, room for U-bends under the water. */
    public static final int BELOW = 16;
    public static final int CELL_HEIGHT = BELOW + 64;
    /** Pixels left of the root axis in every cell. */
    public static final int MARGIN = 4;

    private static final int[] DISTANCES_SQ = IntStream.rangeClosed(0, REACH)
            .flatMap(x -> IntStream.rangeClosed(0, REACH).map(z -> x * x + z * z))
            .distinct().sorted().toArray();
    public static final int WIDTH = CELL_WIDTH * DISTANCES_SQ.length;
    public static final int HEIGHT = CELL_HEIGHT * MAX_HEIGHT * VARIANTS;

    // Grey levels take the foliage tint; the accent's hue turns yellowish green.
    private static final int OUTLINE = 0xFF5A5A5A;
    private static final int RIM = 0xFF8A8A8A;
    private static final int BODY = 0xFFA4A4A4;
    private static final int ACCENT = 0xFFC8D6A0;
    private static final int ACCENT_RARITY = 5;

    private WaterLilyStemTexture() {
    }

    /** Left pixel of the cell for a leaf at horizontal offset (dx, dz). */
    public static int cellX(int dx, int dz) {
        return Arrays.binarySearch(DISTANCES_SQ, dx * dx + dz * dz) * CELL_WIDTH;
    }

    /** Top pixel of the cell for a leaf {@code dy} blocks above its root. */
    public static int cellY(int dy, int variant) {
        return ((MAX_HEIGHT - dy) * VARIANTS + variant) * CELL_HEIGHT;
    }

    public static byte[] png() throws IOException {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        for (int column = 0; column < DISTANCES_SQ.length; column++) {
            for (int dy = 1; dy <= MAX_HEIGHT; dy++) {
                for (int variant = 0; variant < VARIANTS; variant++) {
                    drawStem(image, column * CELL_WIDTH, cellY(dy, variant), (float) Math.sqrt(DISTANCES_SQ[column]) * 16,
                            (dy - START_Y) * 16, new SplittableRandom(((long) column * MAX_HEIGHT + dy) * VARIANTS + variant));
                }
            }
        }
        shade(image);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return bytes.toByteArray();
    }

    /**
     * Rasterizes a 2x2 pixel brush along a cubic Bezier that ends straight
     * below the leaf. An S-stem leaves the root straight up; a U-stem first
     * dips under the water and then climbs steeply. A leaf right above the
     * water has no room for an S, so it always gets a U.
     */
    private static void drawStem(BufferedImage image, int cellX, int cellY, float length, float height, RandomGenerator random) {
        boolean uBend = height < 16 || random.nextBoolean();
        // A U to a leaf right above the root swings out sideways and back.
        float x1 = uBend ? length * (0.2F + 0.5F * random.nextFloat()) + (length < 8 ? 6 + 6 * random.nextFloat() : 0) : 0;
        float y1 = uBend ? -(16 + 16 * random.nextFloat()) : height * (0.3F + 0.4F * random.nextFloat());
        float y2 = height * (uBend ? 0.2F + 0.4F * random.nextFloat() : 0.3F + 0.4F * random.nextFloat());
        float wiggle = (random.nextFloat() * 2 - 1) * 2.5F;
        int waves = 1 + random.nextInt(3);

        for (int step = 0; step <= 1024; step++) {
            float t = step / 1024.0F;
            float u = 1 - t;
            float x = MARGIN + x1 * 3 * u * u * t + length * (3 * u * t * t + t * t * t) + wiggle * (float) Math.sin(Math.PI * waves * t);
            float y = y1 * 3 * u * u * t + y2 * 3 * u * t * t + height * t * t * t;
            for (int px = (int) Math.floor(x - 0.5F); px <= (int) Math.floor(x + 0.5F); px++) {
                // Rows stop below the leaf, so the stem never pokes through it.
                for (int py = (int) Math.floor(y - 0.5F); py <= Math.min((int) Math.floor(y + 0.5F), (int) height - 1); py++) {
                    if (px >= 0 && py >= -BELOW) {
                        image.setRGB(cellX + px, cellY + CELL_HEIGHT - 1 - BELOW - py, BODY);
                    }
                }
            }
        }
    }

    /** Lit from above left: a dark lower right outline, a softer upper rim and pale green flecks. */
    private static void shade(BufferedImage image) {
        int[] colors = new int[WIDTH * HEIGHT];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (image.getRGB(x, y) == 0) {
                    continue;
                }
                if (isEmpty(image, x + 1, y) || isEmpty(image, x, y + 1)) {
                    colors[y * WIDTH + x] = OUTLINE;
                } else if (Math.floorMod(x * 73856093 ^ y * 19349663, ACCENT_RARITY) == 0) {
                    colors[y * WIDTH + x] = ACCENT;
                } else {
                    colors[y * WIDTH + x] = isEmpty(image, x, y - 1) ? RIM : BODY;
                }
            }
        }
        image.setRGB(0, 0, WIDTH, HEIGHT, colors, 0, WIDTH);
    }

    private static boolean isEmpty(BufferedImage image, int x, int y) {
        return x >= WIDTH || y < 0 || y >= HEIGHT || image.getRGB(x, y) == 0;
    }
}
