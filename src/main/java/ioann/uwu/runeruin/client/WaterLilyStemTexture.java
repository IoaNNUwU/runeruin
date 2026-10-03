package ioann.uwu.runeruin.client;

import ioann.uwu.runeruin.RR;
import net.minecraft.resources.Identifier;

import java.util.Arrays;
import java.util.stream.IntStream;

import static ioann.uwu.runeruin.blocks.WaterLilyLeafBlock.MAX_HEIGHT;
import static ioann.uwu.runeruin.blocks.WaterLilyLeafBlock.REACH;

/**
 * Layout of the hand-drawn stem sprite sheet {@code water_lily_stems.png}.
 *
 * <p>Columns of {@link #CELL_WIDTH} pixels are the horizontal distances a leaf
 * can have from its root, ascending: 0, 1, sqrt2, 2, sqrt5, sqrt8, 3, sqrt10,
 * sqrt13 and sqrt18 blocks. Rows of {@link #CELL_HEIGHT} pixels go from the
 * highest leaf (4 blocks above the root) at the top to the lowest at the
 * bottom, {@link #VARIANTS} rows per height.</p>
 *
 * <p>Each cell is the vertical plane from the root towards the leaf at 16
 * pixels per block. The root axis is {@link #MARGIN} pixels from the cell's
 * left edge; the stem leaves the root {@link #BELOW} pixels above the cell's
 * bottom edge and must end at the leaf centre, {@code distance * 16} pixels to
 * the right and {@code (height - 13/16) * 16} pixels up. The tint of the
 * biome's foliage multiplies the colors.</p>
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
}
