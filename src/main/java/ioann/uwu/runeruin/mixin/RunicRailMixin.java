package ioann.uwu.runeruin.mixin;

import ioann.uwu.runeruin.blocks.RRBlocks;
import ioann.uwu.runeruin.entities.RunicMinecart;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartBehavior;
import net.minecraft.world.entity.vehicle.minecart.OldMinecartBehavior;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * On a runic rail a minecart keeps the speed it came with; only its rider changes it. A runic minecart does
 * the same on any rail while someone rides it, and stops soon once it is empty. Powered rails, switched on
 * or off, treat it like any other minecart.
 *
 * <p>A mixin, because NeoForge has no hook into minecart movement ({@code IBaseRailBlockExtension} only gives the
 * shape of a rail), and every minecart on a runic rail has to move this way, not only ours; besides, a minecart
 * creates its behaviour itself and keeps it in a private final field.</p>
 */
@Mixin(OldMinecartBehavior.class)
public abstract class RunicRailMixin extends MinecartBehavior {

    // A rider reaches the top speed in about three seconds of holding forward.
    @Unique
    private static final double RIDER_ACCELERATION = 0.008;
    // What an empty runic minecart keeps of its speed each tick: it stops within three blocks.
    @Unique
    private static final double EMPTY_BRAKE = 0.8;

    /** The speed at the start of this tick, or -1 when nothing keeps it. */
    @Unique
    private double runeruin$keptSpeed = -1;
    /** An empty runic minecart, away from runic and powered rails. */
    @Unique
    private boolean runeruin$brakes;

    protected RunicRailMixin(AbstractMinecart minecart) {
        super(minecart);
    }

    @Inject(method = "moveAlongTrack", at = @At("HEAD"))
    private void runeruin$rememberSpeed(ServerLevel level, CallbackInfo ci) {
        BlockState rail = level.getBlockState(this.minecart.getCurrentBlockPosOrRailBelow());
        boolean runicMinecart = this.minecart instanceof RunicMinecart
                && !(rail.getBlock() instanceof PoweredRailBlock powered && !powered.isActivatorRail());
        boolean keeps = rail.is(RRBlocks.RUNIC_RAIL) || runicMinecart && this.minecart.isVehicle();
        this.runeruin$keptSpeed = keeps ? this.getDeltaMovement().horizontalDistance() : -1;
        this.runeruin$brakes = runicMinecart && !keeps;
    }

    @ModifyConstant(method = "moveAlongTrack", constant = @Constant(doubleValue = 0.0078125))
    private double runeruin$doNotSlideOnSlopes(double slideSpeed) {
        return this.runeruin$keptSpeed < 0 && !this.runeruin$brakes ? slideSpeed : 0;
    }

    @Inject(method = "moveAlongTrack", at = @At("RETURN"))
    private void runeruin$keepSpeed(ServerLevel level, CallbackInfo ci) {
        Vec3 movement = this.getDeltaMovement();
        if (this.runeruin$brakes) {
            this.setDeltaMovement(movement.multiply(EMPTY_BRAKE, 1.0, EMPTY_BRAKE));
            return;
        }
        if (this.runeruin$keptSpeed < 0) {
            return;
        }
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
