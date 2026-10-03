package ioann.uwu.runeruin.datagen;

import com.google.common.hash.Hashing;
import ioann.uwu.runeruin.client.WaterLilyStemTexture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/** Writes the water lily stem sprite sheet drawn by {@link WaterLilyStemTexture}. */
public class DatagenWaterLilyStemProvider implements DataProvider {
    private final Path path;

    public DatagenWaterLilyStemProvider(PackOutput output) {
        this.path = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "textures")
                .file(WaterLilyStemTexture.SPRITE, "png");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        return CompletableFuture.runAsync(() -> {
            try {
                byte[] png = WaterLilyStemTexture.png();
                output.writeIfNeeded(this.path, png, Hashing.sha1().hashBytes(png));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Override
    public String getName() {
        return "Water lily stem texture";
    }
}
