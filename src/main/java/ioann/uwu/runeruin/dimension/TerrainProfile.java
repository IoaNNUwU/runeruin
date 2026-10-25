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
 * @param plateau 0 for round hills, 1 for hills with flat tops and steep sides
 * @param water   the height up to which water fills the hollows, 0 for none
 */
public record TerrainProfile(float height, float hills, float bumps, float plateau, float water) {

    /** The land of a biome without a profile: the top layer as it was before its biomes shaped it. */
    public static final TerrainProfile DEFAULT = new TerrainProfile(14, 0, 20, 0, 0);

    public static final Codec<TerrainProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("height").forGetter(TerrainProfile::height),
            Codec.FLOAT.optionalFieldOf("hills", 0f).forGetter(TerrainProfile::hills),
            Codec.FLOAT.optionalFieldOf("bumps", 0f).forGetter(TerrainProfile::bumps),
            Codec.floatRange(0, 1).optionalFieldOf("plateau", 0f).forGetter(TerrainProfile::plateau),
            Codec.FLOAT.optionalFieldOf("water", 0f).forGetter(TerrainProfile::water)
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
