package ioann.uwu.runeruin;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.slf4j.Logger;

public class RR {
    public static final String MODID = "runeruin";
    public static final Identifier MOD_ID = id(MODID);

    public static final Logger LOGGER = LogUtils.getLogger();

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public static <T> ResourceKey<T> resourceKey(ResourceKey<Registry<T>> registry, String path) {
        return ResourceKey.create(registry, RR.id(path));
    }
}
