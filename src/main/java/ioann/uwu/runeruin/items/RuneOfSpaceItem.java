package ioann.uwu.runeruin.items;

import ioann.uwu.runeruin.RR;
import ioann.uwu.runeruin.dimension.RRDimension;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

public class RuneOfSpaceItem extends Item {

    public RuneOfSpaceItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var server = level.getServer();
        if (server != null && level.dimension() == Level.OVERWORLD) {

            TeleportTransition transition = new TeleportTransition(
                    server.getLevel(RRDimension.LEVEL),
                    player.getPosition(0f),
                    Vec3.ZERO,
                    player.getYHeadRot(),
                    player.getXRot(),
                    _ -> {}
            );

            player.teleport(transition);
        }
        return InteractionResult.SUCCESS;
    }
}
