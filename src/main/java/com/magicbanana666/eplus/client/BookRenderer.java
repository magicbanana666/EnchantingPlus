package com.magicbanana666.eplus.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.magicbanana666.eplus.EnchLogic;
import com.magicbanana666.eplus.EnchantingPlus;
import com.magicbanana666.eplus.block.entity.BookBlockEntity;
import com.magicbanana666.eplus.client.fx.EnchantFxRender;
import com.magicbanana666.eplus.client.fx.EnchantRitual;
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
    public static final SpriteId TABLE_BOOK = new SpriteId(TextureAtlas.LOCATION_BLOCKS, EnchantingPlus.id("entity/book/arcane"));

    private final SpriteGetter sprites;
    private final BookModel bookModel;
    private final ItemModelResolver itemModelResolver;

    public BookRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
        this.itemModelResolver = context.itemModelResolver();
        this.bookModel = new BookModel(ArcaneBookModel.createLayer().bakeRoot());
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
        // Snapshot the actual unlock predicates on the extraction thread.
        // Rendering only reads these flags; no separate moon/daylight rules.
        state.treasureAvailable = blockEntity.getLevel() != null
                && EnchLogic.isTreasureSetup(blockEntity.getLevel(), blockEntity.getBlockPos());
        state.cursesAvailable = blockEntity.getLevel() != null
                && EnchLogic.isWickedNight(blockEntity.getLevel(), blockEntity.getBlockPos());
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
        submitCoverJewels(bookState, poseStack, submitNodeCollector);
        poseStack.popPose();
        submitAura(state, poseStack, submitNodeCollector, camera);
        submitUnlockHints(state, poseStack, submitNodeCollector, camera);
        if (state.ritual != null) {
            submitRitual(state, poseStack, submitNodeCollector, camera);
        }
    }

    /** Jewels follow the actual cover joints as the book opens and turns. */
    private static void submitCoverJewels(BookModel.State bookState, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.pushPose();
        poseStack.scale(1F / 16, 1F / 16, 1F / 16);
        for (int side : new int[] {-1, 1}) {
            poseStack.pushPose();
            poseStack.translate(0, 0, side);
            poseStack.mulPose(Axis.YP.rotation(side < 0 ? (float) Math.PI + bookState.openness() : -bookState.openness()));
            final double x = side * 6.65;
            collector.submitCustomGeometry(poseStack, EnchantFxRender.JEWEL, (pose, vc) -> {
                EnchantFxRender.jewel(pose, vc, x, 0, -0.18, 1.48, 1.08, 0.28, true);
                EnchantFxRender.jewel(pose, vc, x, 0, -0.43, 1.18, 0.80, 0.64, false);
            });
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(90));
        collector.submitCustomGeometry(poseStack, EnchantFxRender.JEWEL, (pose, vc) -> {
            EnchantFxRender.jewel(pose, vc, 0, 5.35, 0.05, 1.1, 1.35, 0.22, true);
            EnchantFxRender.jewel(pose, vc, 0, 5.35, -0.20, 0.78, 1.0, 0.58, false);
        });
        poseStack.popPose();
        poseStack.popPose();
    }

    /** Ambient reference-style glow; no world particles or server tick work. */
    private static void submitAura(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        final double clock = state.time / 20.0;
        final float brightness = 0.24F + 0.28F * state.open;
        final Vector3f left = new Vector3f(-1, 0, 0).rotate(camera.orientation);
        final Vector3f up = new Vector3f(0, 1, 0).rotate(camera.orientation);
        collector.submitCustomGeometry(poseStack, EnchantFxRender.RIBBON, (pose, vc) -> {
            EnchantFxRender.orbit(pose, vc, clock, 0.64, 1.14, 0, brightness);
            EnchantFxRender.orbit(pose, vc, -clock * 0.75, 0.55, 1.05, Math.PI, brightness * 0.5F);
        });
        collector.submitCustomGeometry(poseStack, EnchantFxRender.CIRCLE, (pose, vc) ->
                EnchantFxRender.circle(pose, vc, 0.5, 0.008, 0.5, 0.70, clock * 0.10, 0.55F));
        collector.submitCustomGeometry(poseStack, EnchantFxRender.GLOW, (pose, vc) -> {
            // Small local halos, rather than one opaque purple sheet.
            EnchantFxRender.billboard(pose, vc, left, up, 0.5, 0.83, 0.5, 0.21, 0, 0.12F, 0.025F, 0.20F);
            for (int i = 0; i < 12; i++) {
                final double a = clock * 0.35 + i * Math.PI * 2 / 12;
                final double y = 0.27 + ((clock * 0.13 + i / 12.0) % 1.0) * 1.36;
                final float k = (float) (0.2 + 0.8 * Math.pow(Math.max(0, Math.sin(clock * 2.2 + i * 2.4)), 3));
                EnchantFxRender.billboard(pose, vc, left, up, 0.5 + Math.cos(a) * 0.62, y, 0.5 + Math.sin(a) * 0.62,
                        0.018 + 0.014 * k, clock, 0.40F * k, 0.13F * k, 0.65F * k);
            }
        });
    }

    /** Availability hints use the same ribbons and soft lights as the aura. */
    private static void submitUnlockHints(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.treasureAvailable && !state.cursesAvailable) {
            return;
        }
        final double clock = state.time / 20.0;
        final Vector3f left = new Vector3f(-1, 0, 0).rotate(camera.orientation);
        final Vector3f up = new Vector3f(0, 1, 0).rotate(camera.orientation);
        final float pulse = (float) (0.44 + 0.10 * Math.sin(clock * 1.8));
        if (state.treasureAvailable) {
            // Warm light winds around the rim; eight small gold/ivory sparks
            // rise gently. The existing violet book aura stays intact.
            collector.submitCustomGeometry(poseStack, EnchantFxRender.RIBBON, (pose, vc) ->
                    EnchantFxRender.orbit(pose, vc, clock * 0.60, 0.57, 0.82, 0, pulse, 1F, 0.72F, 0.28F, 0.055));
            collector.submitCustomGeometry(poseStack, EnchantFxRender.GLOW, (pose, vc) -> {
                for (int i = 0; i < 8; i++) {
                    final double a = clock * 0.24 + i * Math.PI / 4;
                    final double rise = (clock * 0.20 + i / 8.0) % 1.0;
                    final float k = (float) (Math.sin(Math.PI * rise) * 0.58);
                    EnchantFxRender.billboard(pose, vc, left, up,
                            0.5 + Math.cos(a) * 0.57, 0.78 + rise * 0.35, 0.5 + Math.sin(a) * 0.57,
                            0.024, 0, k, 0.80F * k, 0.43F * k);
                }
            });
        }
        if (state.cursesAvailable) {
            // A low counter-rotating lilac ribbon and four distinct floating
            // diamond runes identify the curse window, beyond generic purple.
            collector.submitCustomGeometry(poseStack, EnchantFxRender.RIBBON, (pose, vc) -> {
                EnchantFxRender.orbit(pose, vc, -clock * 0.55, 0.60, 0.80, Math.PI, pulse, 0.78F, 0.35F, 1F, 0.045);
                for (int i = 0; i < 4; i++) {
                    final double a = -clock * 0.18 + i * Math.PI / 2;
                    final double y = 0.92 + 0.035 * Math.sin(clock * 1.6 + i);
                    EnchantFxRender.runeDiamond(pose, vc, left, up,
                            0.5 + Math.cos(a) * 0.60, y, 0.5 + Math.sin(a) * 0.60,
                            0.035, 0.060, 0.65F * pulse, 0.34F * pulse, pulse);
                }
            });
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

        // One restrained violet ring, inside the crystal sockets. The sparse
        // mask and capped alpha keep the stone inlay visible during the surge.
        if (glow > 0.005F) {
            collector.submitCustomGeometry(poseStack, EnchantFxRender.CIRCLE, (pose, vc) -> {
                EnchantFxRender.circle(pose, vc, top.x, top.y + 0.014, top.z, 0.34, spin * 0.55, 0.48F * glow);
            });
        }

        final Vector3f left = new Vector3f(-1, 0, 0).rotate(camera.orientation);
        final Vector3f up = new Vector3f(0, 1, 0).rotate(camera.orientation);

        // A distinct finale after the enchanting spin completes. These are
        // render-only quads: no extra packets, particles or inventory changes.
        if (age >= EnchantRitual.RITUAL && age < EnchantRitual.END) {
            final double finish = clamp01((age - EnchantRitual.RITUAL) / (EnchantRitual.END - EnchantRitual.RITUAL));
            final double expand = outCubic(finish);
            final float strength = (float) (smoother(finish / 0.08) * Math.pow(1 - finish, 1.5));
            collector.submitCustomGeometry(poseStack, EnchantFxRender.CIRCLE, (pose, vc) -> {
                EnchantFxRender.itemSeal(pose, vc, left, up, itemPos.x, itemPos.y, itemPos.z,
                        0.10 + expand * 0.78, spin * 0.65, 0.72F * strength);
                EnchantFxRender.circle(pose, vc, 0.5, 0.012, 0.5,
                        0.25 + expand * 1.15, -spin * 0.35, 0.90F * strength);
            });
            collector.submitCustomGeometry(poseStack, EnchantFxRender.GLOW, (pose, vc) -> {
                EnchantFxRender.billboard(pose, vc, left, up, itemPos.x, itemPos.y, itemPos.z,
                        0.18 + expand * 0.30, 0, 0.50F * strength, 0.30F * strength, 0.70F * strength);
                for (int i = 0; i < 16; i++) {
                    final double a = i * Math.PI * 2 / 16 + spin * 0.15;
                    final double r = 0.08 + expand * 0.88;
                    EnchantFxRender.billboard(pose, vc, left, up,
                            itemPos.x + left.x * Math.cos(a) * r + up.x * Math.sin(a) * r,
                            itemPos.y + left.y * Math.cos(a) * r + up.y * Math.sin(a) * r,
                            itemPos.z + left.z * Math.cos(a) * r + up.z * Math.sin(a) * r,
                            0.025 + 0.018 * strength, a, 0.64F * strength, 0.40F * strength, 0.85F * strength);
                }
            });
        }

        // The item, with a soft glow behind it. It spins up during the ritual and pulses when it bursts.
        final double appear = outBack(age / 0.3, 1.6);
        if (!state.item.isEmpty() && fade > 0.01) {
            collector.submitCustomGeometry(poseStack, EnchantFxRender.GLOW, (pose, vc) ->
                    EnchantFxRender.billboard(pose, vc, left, up, itemPos.x, itemPos.y, itemPos.z, 0.20 * appear, clock * 0.4, 0.24F * glow, 0.12F * glow, 0.32F * glow));
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
                        EnchantFxRender.beam(pose, vc, itemPos.x, top.y, itemPos.z, 1.9, 0.5 + 0.3 * beam, clock * 0.9, 0.58F * b, 0.38F * b, 0.78F * b));
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
        // The idle orbit extends beyond the block and above the book.
        return new AABB(blockEntity.getBlockPos()).inflate(0.22, 0, 0.22).expandTowards(0, 0.8, 0);
    }

    public static class State extends EnchantTableRenderState {
        public boolean treasureAvailable;
        public boolean cursesAvailable;
        public @Nullable EnchantRitual ritual;
        public double clock;
        public List<EnchantRitual.Spark> sparks = List.of();
        public final ItemStackRenderState item = new ItemStackRenderState();
    }
}
