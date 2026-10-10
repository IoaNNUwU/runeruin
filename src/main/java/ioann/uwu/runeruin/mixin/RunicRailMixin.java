package ioann.uwu.runeruin.mixin;

import ioann.uwu.runeruin.blocks.RRBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** On a runic rail a minecart keeps the speed it came with; only its rider changes it. */
@Mixin(OldMinecartBehavior.class)
public abstract class RunicRailMixin extends MinecartBehavior {

    // A rider reaches the top speed in about three seconds of holding forward.
    @Unique
    private static final double RIDER_ACCELERATION = 0.008;

    /** The speed at the start of this tick, or -1 when the rail under the cart is not runic. */
    @Unique
    private double runeruin$keptSpeed = -1;

    protected RunicRailMixin(AbstractMinecart minecart) {
        super(minecart);
    }

    @Inject(method = "moveAlongTrack", at = @At("HEAD"))
    private void runeruin$rememberSpeed(ServerLevel level, CallbackInfo ci) {
        boolean runic = level.getBlockState(this.minecart.getCurrentBlockPosOrRailBelow()).is(RRBlocks.RUNIC_RAIL);
        this.runeruin$keptSpeed = runic ? this.getDeltaMovement().horizontalDistance() : -1;
    }

    @ModifyConstant(method = "moveAlongTrack", constant = @Constant(doubleValue = 0.0078125))
    private double runeruin$doNotSlideOnSlopes(double slideSpeed) {
        return this.runeruin$keptSpeed < 0 ? slideSpeed : 0;
    }

    @Inject(method = "moveAlongTrack", at = @At("RETURN"))
    private void runeruin$keepSpeed(ServerLevel level, CallbackInfo ci) {
        if (this.runeruin$keptSpeed < 0) {
            return;
        }
        Vec3 movement = this.getDeltaMovement();
        double speed = movement.horizontalDistance();
        // Standing still, or stopped by an obstacle this tick.
        if (speed < 1.0E-5) {
            return;
        }

        double target = this.runeruin$keptSpeed;
        if (this.minecart.getFirstPassenger() instanceof ServerPlayer rider) {
            Vec3 intent = rider.getLastClientMoveIntent();
            target += (intent.x * movement.x + intent.z * movement.z) / speed * RIDER_ACCELERATION;
        }
        // A cart with a rider moves by 0.75 of its speed, so it needs more of it for the same top speed.
        double topSpeed = this.getMaxSpeed(level) / (this.minecart.isVehicle() ? 0.75 : 1.0);
        double scale = Mth.clamp(target, 0.0, topSpeed) / speed;
        this.setDeltaMovement(movement.x * scale, movement.y, movement.z * scale);
    }
}
