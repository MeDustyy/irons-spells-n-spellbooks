package io.redspace.ironsspellbooks.block.alchemist_cauldron;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import io.redspace.ironsspellbooks.util.MinecraftInstanceHelper;
import joptsimple.internal.Strings;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderNameTagEvent;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;


public class AlchemistCauldronRenderer implements BlockEntityRenderer<AlchemistCauldronTile> {
    ItemRenderer itemRenderer;

    public AlchemistCauldronRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    private static final Vec3 ITEM_POS = new Vec3(.5, 1.5, .5);
    @Override
    public void render(AlchemistCauldronTile cauldron, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        float waterOffset = getWaterOffest(cauldron.getBlockState());

        int waterLevel = cauldron.getBlockState().getValue(AlchemistCauldronBlock.LEVEL);
        if (waterLevel > 0) {
            renderWater(cauldron, poseStack, bufferSource, packedLight, waterOffset);
        }

        var floatingItems = cauldron.inputItems;
        for (int i = 0; i < floatingItems.size(); i++) {
            var itemStack = floatingItems.get(i);
            if (!itemStack.isEmpty()) {
                float f = waterLevel > 0 ? cauldron.getLevel().getGameTime() + partialTick : 15;
                Vec2 floatOffset = getFloatingItemOffset(f, i * 587);
                float yRot = (f + i * 213) / (i + 1) * 1.5f;
                renderItem(itemStack,
                        new Vec3(
                                floatOffset.x,
                                waterOffset + i * .01f,
                                floatOffset.y),
                        yRot, cauldron, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            }
        }
        MinecraftInstanceHelper.ifPlayerPresent(player -> {
            int d = 3;
            if (player.isCrouching() && Math.abs(player.getX() - cauldron.getBlockPos().getX()) < d && Math.abs(player.getY() - cauldron.getBlockPos().getY()) < d && Math.abs(player.getZ() - cauldron.getBlockPos().getZ()) < d) {
                renderNameTag(cauldron.resultItems.stream().filter(item -> !item.isEmpty()).collect(Collectors.toList()), poseStack, bufferSource, packedLight);
            }
        });
    }

    protected void renderNameTag(List<ItemStack> items, PoseStack poseStack, MultiBufferSource pBuffer, int pPackedLight) {
        if (items.size() == 0) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5D, 1.5f, 0.5D);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f matrix4f = poseStack.last().pose();
        float f1 = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
        int j = (int) (f1 * 255.0F) << 24;
        Font font = Minecraft.getInstance().font;
        Component text = Component.literal(Strings.repeat(' ', items.size() * 4));
        float f2 = (float) (-font.width(text) / 2);
        font.drawInBatch(text, f2, 0, 553648127, false, matrix4f, pBuffer, false, j, pPackedLight);
        for (int k = 0; k < items.size(); k++) {
            var item = items.get(k);
            poseStack.pushPose();
            poseStack.scale(-0.25f / 0.025F, -0.25f / 0.025F, 0.25f / 0.025F);
            poseStack.translate((k - (items.size() - 1) / 2f) * 1.5, -0.4, 0);
            itemRenderer.renderStatic(item, ItemTransforms.TransformType.FIXED, pPackedLight, OverlayTexture.NO_OVERLAY, poseStack, pBuffer, 0);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    public Vec2 getFloatingItemOffset(float time, int offset) {
        //for our case, offset never changes
        float xspeed = offset % 2 == 0 ? .0075f : .025f * (1 + (offset % 88) * .001f);
        float yspeed = offset % 2 == 0 ? .025f : .0075f * (1 + (offset % 88) * .001f);
        float x = (time + offset) * xspeed;
        x = (Math.abs((x % 2) - 1) + 1) / 2;
        float y = (time + offset + 4356) * yspeed;
        y = (Math.abs((y % 2) - 1) + 1) / 2;

        //these values are "bouncing" between 0-1. however, this needs to be bounded to inside the limits of the cauldron, taking into account the item size
        x = Mth.lerp(x, -.2f, .75f);
        y = Mth.lerp(y, -.2f, .75f);
        return new Vec2(x, y);

    }

    public static float getWaterOffest(BlockState blockState) {
        return Mth.lerp(AlchemistCauldronBlock.getLevel(blockState) / (float) AlchemistCauldronBlock.MAX_LEVELS, .25f, .9f);
    }

    private void renderWater(AlchemistCauldronTile cauldron, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float waterOffset) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.beaconBeam(new ResourceLocation("textures/block/water_still.png"), true));
        long color = cauldron.getAverageWaterColor();
        var rgb = colorFromLong(color);

        Matrix4f pose = poseStack.last().pose();
        int frames = 32;
        float frameSize = 1f / frames;
        long frame = (cauldron.getLevel().getGameTime() / 3) % frames;
        float min_u = 0;
        float max_u = 1;
        float min_v = (frameSize * frame);
        float max_v = (frameSize * (frame + 1));


        consumer.vertex(pose, 1, waterOffset, 0).color(rgb.x(), rgb.y(), rgb.z(), 1f).uv(max_u, min_v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
        consumer.vertex(pose, 0, waterOffset, 0).color(rgb.x(), rgb.y(), rgb.z(), 1f).uv(min_u, min_v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
        consumer.vertex(pose, 0, waterOffset, 1).color(rgb.x(), rgb.y(), rgb.z(), 1f).uv(min_u, max_v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
        consumer.vertex(pose, 1, waterOffset, 1).color(rgb.x(), rgb.y(), rgb.z(), 1f).uv(max_u, max_v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(0, 1, 0).endVertex();
    }

    private Vector3f colorFromLong(long color) {
        //Copied from potion utils
        return new Vector3f(
                ((color >> 16) & 0xFF) / 255.0f,
                ((color >> 8) & 0xFF) / 255.0f,
                (color & 0xFF) / 255.0f
        );
    }

    private void renderItem(ItemStack itemStack, Vec3 offset, float yRot, AlchemistCauldronTile tile, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        //renderId seems to be some kind of uuid/salt
        int renderId = (int) tile.getBlockPos().asLong();
        //BakedModel model = itemRenderer.getModel(itemStack, null, null, renderId);
        poseStack.translate(offset.x, offset.y, offset.z);
        poseStack.mulPose(Vector3f.YP.rotationDegrees(yRot));
        poseStack.mulPose(Vector3f.XP.rotationDegrees(90));
        poseStack.scale(0.4f, 0.4f, 0.4f);

        itemRenderer.renderStatic(itemStack, ItemTransforms.TransformType.FIXED, LevelRenderer.getLightColor(tile.getLevel(), tile.getBlockPos()), packedOverlay, poseStack, bufferSource, renderId);
        poseStack.popPose();
    }

}
