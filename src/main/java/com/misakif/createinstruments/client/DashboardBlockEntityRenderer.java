package com.misakif.createinstruments.client;

import com.misakif.createinstruments.block.DashboardBlock;
import com.misakif.createinstruments.block.DashboardBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public class DashboardBlockEntityRenderer extends SafeBlockEntityRenderer<DashboardBlockEntity> {

    private static final float NR = 0.95f, NG = 0.93f, NB = 0.80f; // needle: warm cream
    private static final float PR = 0.18f, PG = 0.18f, PB = 0.18f; // pivot:  dark grey
    private static final float NEEDLE_HALF_WIDTH = 0.012f;
    private static final float NEEDLE_LEN_FRONT  = 0.085f;
    private static final float NEEDLE_LEN_BACK   = 0.022f;
    private static final float PIVOT_HALF        = 0.014f;

    public DashboardBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    private static float whiteU = Float.NaN, whiteV = Float.NaN;

    private static void ensureWhiteUV() {
        if (!Float.isNaN(whiteU)) return;
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getModelManager()
                .getAtlas(TextureAtlas.LOCATION_BLOCKS)
                .getSprite(ResourceLocation.parse("minecraft:block/white_concrete"));
        whiteU = (sprite.getU0() + sprite.getU1()) * 0.5f;
        whiteV = (sprite.getV0() + sprite.getV1()) * 0.5f;
    }

    @Override
    protected void renderSafe(DashboardBlockEntity be, float partialTicks, PoseStack ms,
                               MultiBufferSource buffer, int light, int overlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof DashboardBlock)) return;

        ensureWhiteUV();
        Direction facing = state.getValue(DashboardBlock.FACING);
        VertexConsumer vc = buffer.getBuffer(RenderType.solid());

        ms.pushPose();
        // Center → rotate to match FACING → restore center offset
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(new Quaternionf().rotationY((float) (-facing.toYRot() * Math.PI / 180.0)));
        ms.translate(-0.5, -0.5, -0.5);

        for (int i = 0; i < 4; i++) {
            float progress = Math.max(0f,
                    Mth.lerp(partialTicks, be.prevDialStates[i], be.dialStates[i]));

            // Gauge layout (SOUTH frame, panel face at z=1.0):
            // i=0 top-left, i=1 top-right, i=2 bottom-left, i=3 bottom-right
            float gx = (i % 2) * 0.5f + 0.25f;
            float gy = 0.75f - (i / 2) * 0.5f;
            float gz = 1.003f; // protrude slightly beyond panel face

            // 180° sweep: progress=0 → left (π/2), progress=1 → right (-π/2)
            float angle = (float) (Math.PI * 0.5 * (1.0 - 2.0 * progress));

            ms.pushPose();
            ms.translate(gx, gy, gz);
            ms.mulPose(new Quaternionf().rotationZ(angle));

            Matrix4f pose = ms.last().pose();
            PoseStack.Pose norm = ms.last();

            // Front part of needle (tip)
            quad(vc, pose, norm, light,
                    -NEEDLE_HALF_WIDTH, 0,  NEEDLE_HALF_WIDTH, 0,
                     NEEDLE_HALF_WIDTH, NEEDLE_LEN_FRONT, -NEEDLE_HALF_WIDTH, NEEDLE_LEN_FRONT,
                    NR, NG, NB);
            // Back part of needle (balance tail, darker)
            quad(vc, pose, norm, light,
                    -NEEDLE_HALF_WIDTH, 0,  NEEDLE_HALF_WIDTH, 0,
                     NEEDLE_HALF_WIDTH, -NEEDLE_LEN_BACK, -NEEDLE_HALF_WIDTH, -NEEDLE_LEN_BACK,
                    PR * 2, PR * 2, PR * 2);

            ms.popPose();

            // Pivot cap rendered above needle (higher z)
            ms.pushPose();
            ms.translate(gx, gy, gz + 0.002f);
            Matrix4f pp = ms.last().pose();
            PoseStack.Pose pn = ms.last();
            quad(vc, pp, pn, light,
                    -PIVOT_HALF, -PIVOT_HALF,  PIVOT_HALF, -PIVOT_HALF,
                     PIVOT_HALF,  PIVOT_HALF, -PIVOT_HALF,  PIVOT_HALF,
                    PR, PG, PB);
            ms.popPose();
        }

        ms.popPose();
    }

    private static void quad(VertexConsumer vc, Matrix4f pose, PoseStack.Pose norm, int light,
                              float x0, float y0, float x1, float y1,
                              float x2, float y2, float x3, float y3,
                              float r, float g, float b) {
        int ri = (int) (r * 255), gi = (int) (g * 255), bi = (int) (b * 255);
        // All 4 verts sample the same white-concrete pixel so vertex color drives the final color
        vc.addVertex(pose, x0, y0, 0).setColor(ri, gi, bi, 255).setUv(whiteU, whiteV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(norm, 0, 0, 1);
        vc.addVertex(pose, x1, y1, 0).setColor(ri, gi, bi, 255).setUv(whiteU, whiteV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(norm, 0, 0, 1);
        vc.addVertex(pose, x2, y2, 0).setColor(ri, gi, bi, 255).setUv(whiteU, whiteV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(norm, 0, 0, 1);
        vc.addVertex(pose, x3, y3, 0).setColor(ri, gi, bi, 255).setUv(whiteU, whiteV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(norm, 0, 0, 1);
    }
}
