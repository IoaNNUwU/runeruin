package ioann.uwu.runeruin.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ioann.uwu.runeruin.RR;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * The land a biome of the top layer asks for, in blocks above the underside of its island. The terrain blends the
 * profiles of the biomes around a column, and near the edge of an island it keeps the island's own shape whatever
 * the biome asks for.
 *
 * @param height  the ground without relief
 * @param hills   how high the big hills rise above it
 * @param bumps   how high the fine relief rises above it
 * @param plateau 0 for round hills, 1 for hills cut into steps
 * @param calm    the share of the biome that is calm land, nearly flat and without cliffs: 0 for none, 0.5 for half of it
 * @param cliffs  the cliffs that stand on the ground
 */
public record TerrainProfile(float height, float hills, float bumps, float plateau, float calm, Cliffs cliffs) {

    /** The land of a biome without a profile: the top layer as it was before its biomes shaped it. */
    public static final TerrainProfile DEFAULT = new TerrainProfile(14, 0, 0, 0, 0, new Cliffs(13, 1, 0.5f));

    /**
     * @param height    how high their walls rise at most; their tops rise a little more further from the wall
     * @param winding   1 for the winding relief of the islands, 0 for wide calm shapes
     * @param threshold the value of their noise from which they stand: 0.5 is about half of the land, more is rarer
     */
    public record Cliffs(float height, float winding, float threshold) {

        public static final Cliffs NONE = new Cliffs(0, 0, 1);

        static final Codec<Cliffs> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("height").forGetter(Cliffs::height),
                Codec.floatRange(0, 1).fieldOf("winding").forGetter(Cliffs::winding),
                Codec.floatRange(0, 1).fieldOf("threshold").forGetter(Cliffs::threshold)
        ).apply(instance, Cliffs::new));
    }

    public static final Codec<TerrainProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("height").forGetter(TerrainProfile::height),
            Codec.FLOAT.optionalFieldOf("hills", 0f).forGetter(TerrainProfile::hills),
            Codec.FLOAT.optionalFieldOf("bumps", 0f).forGetter(TerrainProfile::bumps),
            Codec.floatRange(0, 1).optionalFieldOf("plateau", 0f).forGetter(TerrainProfile::plateau),
            Codec.floatRange(0, 1).optionalFieldOf("calm", 0f).forGetter(TerrainProfile::calm),
            Cliffs.CODEC.optionalFieldOf("cliffs", Cliffs.NONE).forGetter(TerrainProfile::cliffs)
    ).apply(instance, TerrainProfile::new));

    public static final DataMapType<Biome, TerrainProfile> DATA_MAP =
            DataMapType.builder(RR.id("terrain_profile"), Registries.BIOME, CODEC).build();

    public static void registerDataMap(RegisterDataMapTypesEvent event) {
        event.register(DATA_MAP);
    }

    public static TerrainProfile of(Holder<Biome> biome) {
        TerrainProfile profile = biome.getData(DATA_MAP);
        return profile == null ? DEFAULT : profile;
    }
}
