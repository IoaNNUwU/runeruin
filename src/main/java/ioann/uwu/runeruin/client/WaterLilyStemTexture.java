package ioann.uwu.runeruin.client;

import ioann.uwu.runeruin.RR;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.IntStream;

import static ioann.uwu.runeruin.blocks.WaterLilyLeafBlock.MAX_HEIGHT;
import static ioann.uwu.runeruin.blocks.WaterLilyLeafBlock.REACH;

/**
 * Sprite sheet with one pixel-art stem per leaf offset. Columns are the
 * horizontal distances a leaf can have from its root, rows its height.
 *
 * <p>Each cell is the vertical plane from the root to the leaf at 16 pixels
 * per block, so the stem's pixels stay level with the ground and its curve
 * steps like stairs. Datagen draws the sheet; the root model only maps cells.</p>
 */
public final class WaterLilyStemTexture {
    public static final Identifier SPRITE = RR.id("block/water_lily_stems");
    /** Where stems leave the root block, in blocks. */
    public static final float START_Y = 13.0F / 16.0F;
    public static final int CELL_WIDTH = 80;
    public static final int CELL_HEIGHT = 64;
    /** Pixels left of the root axis in every cell. */
    public static final int MARGIN = 4;

    private static final int[] DISTANCES_SQ = IntStream.rangeClosed(0, REACH)
            .flatMap(x -> IntStream.rangeClosed(0, REACH).map(z -> x * x + z * z))
            .distinct().sorted().toArray();
    public static final int WIDTH = CELL_WIDTH * DISTANCES_SQ.length;
    public static final int HEIGHT = CELL_HEIGHT * MAX_HEIGHT;

    private static final int LIGHT = 0xFFA4A4A4;
    private static final int DARK = 0xFF6C6C6C;

    private WaterLilyStemTexture() {
    }

    /** Left pixel of the cell for a leaf at horizontal offset (dx, dz). */
    public static int cellX(int dx, int dz) {
        return Arrays.binarySearch(DISTANCES_SQ, dx * dx + dz * dz) * CELL_WIDTH;
    }

    /** Top pixel of the cell for a leaf {@code dy} blocks above its root. */
    public static int cellY(int dy) {
        return (MAX_HEIGHT - dy) * CELL_HEIGHT;
    }

    public static byte[] png() throws IOException {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        for (int column = 0; column < DISTANCES_SQ.length; column++) {
            for (int dy = 1; dy <= MAX_HEIGHT; dy++) {
                drawStem(image, column * CELL_WIDTH, cellY(dy), (float) Math.sqrt(DISTANCES_SQ[column]) * 16, (dy - START_Y) * 16);
            }
        }
        shade(image);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return bytes.toByteArray();
    }

    /**
     * Rasterizes a 2x2 pixel brush along a cubic Bezier that leaves the root
     * straight up and meets the leaf straight from below.
     */
    private static void drawStem(BufferedImage image, int cellX, int cellY, float length, float height) {
        float lift = height * 0.5F;
        for (int step = 0; step <= 1024; step++) {
            float t = step / 1024.0F;
            float u = 1 - t;
            float x = MARGIN + length * (3 * u * t * t + t * t * t);
            float y = lift * 3 * u * u * t + (height - lift) * 3 * u * t * t + height * t * t * t;
            for (int px = (int) Math.floor(x - 0.5F); px <= (int) Math.floor(x + 0.5F); px++) {
                // Rows stop below the leaf, so the stem never pokes through it.
                for (int py = (int) Math.floor(y - 0.5F); py <= Math.min((int) Math.floor(y + 0.5F), (int) height - 1); py++) {
                    if (py >= 0) {
                        image.setRGB(cellX + px, cellY + CELL_HEIGHT - 1 - py, LIGHT);
                    }
                }
            }
        }
    }

    /** Darkens the lower right edge of the stem, as if lit from above left. */
    private static void shade(BufferedImage image) {
        boolean[] dark = new boolean[WIDTH * HEIGHT];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                dark[y * WIDTH + x] = image.getRGB(x, y) != 0 && (isEmpty(image, x + 1, y) || isEmpty(image, x, y + 1));
            }
        }
        for (int i = 0; i < dark.length; i++) {
            if (dark[i]) {
                image.setRGB(i % WIDTH, i / WIDTH, DARK);
            }
        }
    }

    private static boolean isEmpty(BufferedImage image, int x, int y) {
        return x >= WIDTH || y >= HEIGHT || image.getRGB(x, y) == 0;
    }
}
