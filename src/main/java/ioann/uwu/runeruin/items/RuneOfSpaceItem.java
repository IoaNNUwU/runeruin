package ioann.uwu.runeruin.items;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.Const;
import ioann.uwu.runeruin.dimension.RRDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Teleports between the Overworld and Rune Ruin, to the closest portal or else onto the ground above the player. */
public class RuneOfSpaceItem extends Item {

    public RuneOfSpaceItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var server = level.getServer();
        if (server == null) {
            return InteractionResult.PASS;
        }

        BlockPos playerPos = player.blockPosition();
        if (level.dimension() == RRDimension.LEVEL) {
            return teleport(player, server.getLevel(Level.OVERWORLD), playerPos);
        }
        if (level.dimension() == Level.OVERWORLD) {
            return teleport(player, server.getLevel(RRDimension.LEVEL), playerPos.atY(Const.TOP_LAYER_PORTAL_Y));
        }
        return InteractionResult.PASS;
    }

    private static InteractionResult teleport(Player player, ServerLevel target, BlockPos search) {
        if (target == null) {
            RR.LOGGER.warn("The dimension to teleport to does not exist");
            return InteractionResult.PASS;
        }

        Vec3 destination = target.getPortalForcer()
                .findClosestPortalPosition(search, false, target.getWorldBorder())
                .map(Vec3::atCenterOf)
                .orElseGet(() -> Vec3.atBottomCenterOf(target.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, search)));

        player.teleport(new TeleportTransition(target, destination, Vec3.ZERO, player.getYHeadRot(), player.getXRot(), _ -> {}));
        return InteractionResult.SUCCESS;
    }
}
