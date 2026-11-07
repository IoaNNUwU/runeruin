package ioann.uwu.runeruin.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import ioann.uwu.runeruin.entities.Colossus;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * What a moving entity collides with includes the turned cubes of a Colossus, as small boxes along each cube.
 *
 * <p>A mixin, because the game takes the box of every other entity as it is ({@code EntityGetter.getEntityCollisions}),
 * square to the blocks, and NeoForge has no hook there: around a turned cube that box would stand out of it like a
 * wall of air. The other way, a pool of small part entities laid along the colossus near whoever stands on it, was
 * turned down for this one.</p>
 */
@Mixin(Entity.class)
public abstract class ColossusCollisionMixin {

    @WrapOperation(method = "collide", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getEntityCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<VoxelShape> runeruin$colossusCubes(Level level, Entity entity, AABB area, Operation<List<VoxelShape>> original) {
        return Colossus.withCubes(level, entity, area, original.call(level, entity, area));
    }
}
