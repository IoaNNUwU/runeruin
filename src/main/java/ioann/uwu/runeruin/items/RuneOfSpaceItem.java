package ioann.uwu.runeruin.items;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.RRDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalForcer;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class RuneOfSpaceItem extends Item {

    public RuneOfSpaceItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var server = level.getServer();

        if (server != null) {

            if (level.dimension() == RRDimension.LEVEL) {

                ServerLevel overworld = server.getLevel(Level.OVERWORLD);
                if (overworld == null) {
                    RR.LOGGER.warn("Overworld dimension does not exist");
                    return InteractionResult.PASS;
                }

                PortalForcer portalForcer = overworld.getPortalForcer();

                Vec3 playerPos = player.getPosition(0f);
                BlockPos exitBlockPos = new BlockPos((int) playerPos.x, (int) playerPos.y, (int) playerPos.z);

                Optional<BlockPos> optPortalPos = portalForcer.findClosestPortalPosition(exitBlockPos, false, overworld.getWorldBorder());

                BlockPos portalPos;
                if (optPortalPos.isEmpty()) {
                    RR.LOGGER.warn("Unable to find exit portal position in overworld");
                    portalPos = exitBlockPos;
                } else {
                    portalPos = optPortalPos.get();
                }

                TeleportTransition transition = new TeleportTransition(
                        overworld,
                        portalPos.getCenter(),
                        Vec3.ZERO,
                        player.getYHeadRot(),
                        player.getXRot(),
                        _ -> {}
                );

                player.teleport(transition);
                return InteractionResult.SUCCESS;

            } else if (level.dimension() == Level.OVERWORLD) {

                ServerLevel runeRuin = server.getLevel(RRDimension.LEVEL);
                if (runeRuin == null) {
                    RR.LOGGER.warn("Rune Ruin dimension does not exist");
                    return InteractionResult.PASS;
                }

                PortalForcer portalForcer = runeRuin.getPortalForcer();

                Vec3 playerPos = player.getPosition(0f);
                BlockPos exitBlockPos = new BlockPos((int) playerPos.x, 330, (int) playerPos.z);

                Optional<BlockPos> optPortalPos = portalForcer.findClosestPortalPosition(exitBlockPos, false, runeRuin.getWorldBorder());

                BlockPos portalPos;
                if (optPortalPos.isEmpty()) {
                    RR.LOGGER.warn("Unable to find exit portal position in Rune Ruin");
                    portalPos = exitBlockPos;
                } else {
                    portalPos = optPortalPos.get();
                }

                TeleportTransition transition = new TeleportTransition(
                        runeRuin,
                        portalPos.getCenter(),
                        Vec3.ZERO,
                        player.getYHeadRot(),
                        player.getXRot(),
                        _ -> {}
                );

                player.teleport(transition);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }
}
