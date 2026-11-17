package com.magicbanana666.eplus.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.magicbanana666.eplus.EnchantingPlus;
import com.magicbanana666.eplus.block.entity.BookBlockEntity;
import com.magicbanana666.eplus.client.fx.EnchantFxRender;
import com.magicbanana666.eplus.client.fx.EnchantRitual;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.EnchantTableRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/** Renders the floating book of the advanced table (same animation as the enchanting table), and the enchanting animation. */
public class BookRenderer<T extends BookBlockEntity> implements BlockEntityRenderer<T, BookRenderer.State> {

    /** The book texture is stitched onto the block atlas (assets/minecraft/atlases/blocks.json). */
    public static final SpriteId TABLE_BOOK = new SpriteId(TextureAtlas.LOCATION_BLOCKS, EnchantingPlus.id("entity/book/eplus"));

    private final SpriteGetter sprites;
    private final BookModel bookModel;
    private final ItemModelResolver itemModelResolver;

    public BookRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
        this.itemModelResolver = context.itemModelResolver();
        this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T blockEntity, State state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.flip = Mth.lerp(partialTicks, blockEntity.oFlip, blockEntity.flip);
        state.open = Mth.lerp(partialTicks, blockEntity.oOpen, blockEntity.open);
        state.time = blockEntity.time + partialTicks;
        float or = blockEntity.rot - blockEntity.oRot;
        while (or >= (float) Math.PI) {
            or -= (float) (Math.PI * 2);
        }
        while (or < (float) -Math.PI) {
            or += (float) (Math.PI * 2);
        }
        state.yRot = blockEntity.oRot + or * partialTicks;

        state.ritual = EnchantRitual.get(blockEntity.getBlockPos());
        if (state.ritual != null) {
            state.clock = EnchantRitual.clock();
            state.sparks = state.ritual.sparks();
            this.itemModelResolver.updateForTopItem(state.item, state.ritual.item, ItemDisplayContext.FIXED, blockEntity.getLevel(), null, (int) blockEntity.getBlockPos().asLong());
        }
        else {
            state.sparks = List.of();
            state.item.clear();
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.75F, 0.5F);
        poseStack.translate(0.0F, 0.1F + Mth.sin(state.time * 0.1F) * 0.01F, 0.0F);
        poseStack.mulPose(Axis.YP.rotation(-state.yRot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(80.0F));
        final float ff1 = Mth.frac(state.flip + 0.25F) * 1.6F - 0.3F;
        final float ff2 = Mth.frac(state.flip + 0.75F) * 1.6F - 0.3F;
        final BookModel.State bookState = BookModel.State.forAnimation(state.time, Mth.clamp(ff1, 0.0F, 1.0F), Mth.clamp(ff2, 0.0F, 1.0F), state.open);
        submitNodeCollector.submitModel(this.bookModel, bookState, poseStack, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, TABLE_BOOK, this.sprites, 0, state.breakProgress);
        poseStack.popPose();
        if (state.ritual != null) {
            submitRitual(state, poseStack, submitNodeCollector, camera);
        }
    }

    /** The enchanting animation, see {@link EnchantRitual}. */
    private static void submitRitual(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        final EnchantRitual ritual = state.ritual;
        final double clock = state.clock;
        final double age = ritual.age(clock);
        final double t = EnchantRitual.ritualProgress(age);
        final BlockPos origin = ritual.pos;
        // Everything is drawn relative to the block.
        final Vec3 top = ritual.top().subtract(Vec3.atLowerCornerOf(origin));
        final Vec3 itemPos = ritual.itemPos(age).subtract(Vec3.atLowerCornerOf(origin));

        // Fades in at the start, fades out at the end.
        final double fade = 1 - smoother((age - (EnchantRitual.END - EnchantRitual.FADE)) / EnchantRitual.FADE);
        final double presence = smoother(age / 0.25) * fade;
        final double surge = t >= 0 ? 1.4 * Math.sin(Math.PI * clamp01(t / 0.75)) : 0;
        final float glow = (float) (presence * (0.65 + surge));
        final double spin = clock * 0.25 + 4 * smoother(age / EnchantRitual.RITUAL);

        // Two rune circles turning in opposite directions on the table top.
        if (glow > 0.005F) {
            collector.submitCustomGeometry(poseStack, EnchantFxRender.CIRCLE, (pose, vc) -> {
                EnchantFxRender.circle(pose, vc, top.x, top.y + 0.012, top.z, 0.47, spin, 0.85F * glow);
                EnchantFxRender.circle(pose, vc, top.x, top.y + 0.02, top.z, 0.25, -spin * 1.7, 0.55F * glow);
            });
        }

        final Vector3f left = new Vector3f(-1, 0, 0).rotate(camera.orientation);
        final Vector3f up = new Vector3f(0, 1, 0).rotate(camera.orientation);

        // The item, with a soft glow behind it. It spins up during the ritual and pulses when it bursts.
        final double appear = outBack(age / 0.3, 1.6);
        if (!state.item.isEmpty() && fade > 0.01) {
            collector.submitCustomGeometry(poseStack, EnchantFxRender.GLOW, (pose, vc) ->
                    EnchantFxRender.billboard(pose, vc, left, up, itemPos.x, itemPos.y, itemPos.z, 0.24 * appear, clock * 0.4, 0.3F * glow, 0.18F * glow, 0.5F * glow));
            final double extraSpin = 900 * outCubic(age / EnchantRitual.RITUAL);
            final double pulse = t >= 0 && age > EnchantRitual.BURST ? 1 + 0.18 * Math.sin(Math.PI * clamp01((t - 1.0 / 3) / (0.8 - 1.0 / 3))) : 1;
            final float scale = (float) (0.5 * fade * appear * pulse);
            poseStack.pushPose();
            poseStack.translate(itemPos.x, itemPos.y, itemPos.z);
            poseStack.mulPose(Axis.YP.rotationDegrees((float) ((clock * 40 + extraSpin) % 360)));
            poseStack.scale(scale, scale, scale);
            state.item.submit(poseStack, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }

        // A beam of light going up when the item bursts.
        if (t >= 0 && age > EnchantRitual.BURST) {
            final double beam = 1 - smoother((t - 1.0 / 3) / (2.0 / 3));
            if (beam > 0.01) {
                final float b = (float) (beam * 1.3);
                collector.submitCustomGeometry(poseStack, EnchantFxRender.BEAM, (pose, vc) ->
                        EnchantFxRender.beam(pose, vc, itemPos.x, top.y, itemPos.z, 1.9, 0.5 + 0.3 * beam, clock * 0.9, 0.7F * b, 0.5F * b, 1.0F * b));
            }
        }

        // Sparks.
        if (!state.sparks.isEmpty()) {
            final List<EnchantRitual.Spark> sparks = state.sparks;
            collector.submitCustomGeometry(poseStack, EnchantFxRender.GLOW, (pose, vc) -> {
                for (final EnchantRitual.Spark spark : sparks) {
                    final double sparkAge = clock - spark.born();
                    if (sparkAge < 0 || sparkAge >= spark.life()) {
                        continue;
                    }
                    final Vec3 at = spark.position(sparkAge);
                    final float k = spark.brightness(sparkAge);
                    EnchantFxRender.billboard(pose, vc, left, up, at.x - origin.getX(), at.y - origin.getY(), at.z - origin.getZ(),
                            spark.radius(sparkAge), 0, spark.r() * k, spark.g() * k, spark.b() * k);
                }
            });
        }
    }

    private static double clamp01(double t) {
        return Math.clamp(t, 0.0, 1.0);
    }

    private static double smoother(double t) {
        t = clamp01(t);
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double outCubic(double t) {
        t = clamp01(t);
        return 1 - Math.pow(1 - t, 3);
    }

    private static double outBack(double t, double overshoot) {
        t = clamp01(t);
        return 1 + (overshoot + 1) * Math.pow(t - 1, 3) + overshoot * Math.pow(t - 1, 2);
    }

    @Override
    public AABB getRenderBoundingBox(T blockEntity) {
        if (EnchantRitual.get(blockEntity.getBlockPos()) != null) {
            // Room for the beam and the sparks of the enchanting animation.
            return new AABB(blockEntity.getBlockPos()).inflate(1.5).expandTowards(0, 2, 0);
        }
        return new AABB(blockEntity.getBlockPos()).expandTowards(0, 0.5, 0);
    }

    public static class State extends EnchantTableRenderState {
        public @Nullable EnchantRitual ritual;
        public double clock;
        public List<EnchantRitual.Spark> sparks = List.of();
        public final ItemStackRenderState item = new ItemStackRenderState();
    }
}
