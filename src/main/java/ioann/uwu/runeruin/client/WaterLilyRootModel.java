package ioann.uwu.runeruin.client;

import ioann.uwu.runeruin.blocks.WaterLilyRootBlock;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static ioann.uwu.runeruin.client.WaterLilyStemTexture.*;

/**
 * Root model plus one stem to every attached leaf.
 *
 * <p>The stems are part of the root's chunk mesh, so leaves stay plain blocks.
 * Each stem is a flat, double-sided cell of {@link WaterLilyStemTexture}
 * standing in the vertical plane through the root and the leaf.</p>
 */
public class WaterLilyRootModel implements DynamicBlockStateModel {
    private final BlockStateModel base;
    private final TextureAtlasSprite sprite;
    private final BakedQuad.MaterialInfo material;

    public WaterLilyRootModel(BlockStateModel base, TextureAtlasSprite sprite) {
        this.base = base;
        this.sprite = sprite;
        this.material = new BakedQuad.MaterialInfo(sprite, ChunkSectionLayer.CUTOUT, Sheets.cutoutBlockItemSheet(), 0, true, 0, false);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        this.base.collectParts(level, pos, state, random, parts);
        List<BakedQuad> quads = new ArrayList<>();
        for (BlockPos leaf : WaterLilyRootBlock.attachedLeaves(level, pos)) {
            this.addStem(quads, leaf.getX() - pos.getX(), leaf.getY() - pos.getY(), leaf.getZ() - pos.getZ());
        }
        if (!quads.isEmpty()) {
            parts.add(new StemPart(quads, this.base.particleMaterial()));
        }
    }

    private void addStem(List<BakedQuad> quads, int dx, int dy, int dz) {
        Vector3f along = dx == 0 && dz == 0 ? new Vector3f(1, 0, 0) : new Vector3f(dx, 0, dz).normalize();
        Vector3f bottomLeft = new Vector3f(0.5F, START_Y, 0.5F).fma(-MARGIN / 16.0F, along);
        Vector3f bottomRight = new Vector3f(bottomLeft).fma(CELL_WIDTH / 16.0F, along);
        Vector3f topRight = new Vector3f(bottomRight).add(0, CELL_HEIGHT / 16.0F, 0);
        Vector3f topLeft = new Vector3f(bottomLeft).add(0, CELL_HEIGHT / 16.0F, 0);

        float u0 = this.sprite.getU((float) cellX(dx, dz) / WIDTH);
        float u1 = this.sprite.getU((float) (cellX(dx, dz) + CELL_WIDTH) / WIDTH);
        float v0 = this.sprite.getV((float) cellY(dy) / HEIGHT);
        float v1 = this.sprite.getV((float) (cellY(dy) + CELL_HEIGHT) / HEIGHT);
        long bl = UVPair.pack(u0, v1);
        long br = UVPair.pack(u1, v1);
        long tr = UVPair.pack(u1, v0);
        long tl = UVPair.pack(u0, v0);

        Vector3f normal = along.cross(0, 1, 0, new Vector3f());
        Direction front = Direction.getApproximateNearest(normal.x, normal.y, normal.z);
        quads.add(new BakedQuad(bottomLeft, bottomRight, topRight, topLeft, bl, br, tr, tl, front, this.material));
        quads.add(new BakedQuad(bottomLeft, topLeft, topRight, bottomRight, bl, tl, tr, br, front.getOpposite(), this.material));
    }

    @Override
    public Material.Baked particleMaterial() {
        return this.base.particleMaterial();
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags() {
        return this.base.materialFlags();
    }

    private record StemPart(List<BakedQuad> quads, Material.Baked particleMaterial) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            return direction == null ? this.quads : List.of();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return false;
        }

        @Override
        public @BakedQuad.MaterialFlags int materialFlags() {
            return 0;
        }
    }
}
