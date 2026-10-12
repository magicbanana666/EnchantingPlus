package com.magicbanana666.eplus.client;

import java.util.EnumMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.magicbanana666.eplus.EnchantingPlus;
import com.magicbanana666.eplus.block.BookType;
import com.magicbanana666.eplus.block.DecorativeBookBlock;
import com.magicbanana666.eplus.block.entity.BookBlockEntity;
import com.magicbanana666.eplus.block.entity.DecorativeBookBlockEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.blockentity.state.EnchantTableRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Renders the floating book of the advanced table and the decorative books (same animation as the enchanting table). */
public class BookRenderer<T extends BookBlockEntity> implements BlockEntityRenderer<T, BookRenderer.State> {

    /** The advanced table's book. Book textures are stitched onto the block atlas (assets/minecraft/atlases/blocks.json). */
    public static final SpriteId TABLE_BOOK = sprite(BookType.EPLUS);
    private static final Map<BookType, SpriteId> BOOKS = new EnumMap<>(BookType.class);

    static {
        for (final BookType type : BookType.values()) {
            BOOKS.put(type, type == BookType.VANILLA ? EnchantTableRenderer.BOOK_TEXTURE : sprite(type));
        }
    }

    private final SpriteGetter sprites;
    private final BookModel bookModel;

    public BookRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
        this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    private static SpriteId sprite(BookType type) {
        return new SpriteId(TextureAtlas.LOCATION_BLOCKS, EnchantingPlus.id("entity/book/" + type.getSerializedName()));
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
        if (blockEntity instanceof DecorativeBookBlockEntity book && blockEntity.getBlockState().getBlock() instanceof DecorativeBookBlock block) {
            state.sprite = BOOKS.get(block.getBookType());
            state.height = 0.75f + book.getHeight();
        }
        else {
            state.sprite = TABLE_BOOK;
            state.height = 0.75f;
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, state.height, 0.5F);
        poseStack.translate(0.0F, 0.1F + Mth.sin(state.time * 0.1F) * 0.01F, 0.0F);
        poseStack.mulPose(Axis.YP.rotation(-state.yRot));
        poseStack.mulPose(Axis.ZP.rotationDegrees(80.0F));
        final float ff1 = Mth.frac(state.flip + 0.25F) * 1.6F - 0.3F;
        final float ff2 = Mth.frac(state.flip + 0.75F) * 1.6F - 0.3F;
        final BookModel.State bookState = BookModel.State.forAnimation(state.time, Mth.clamp(ff1, 0.0F, 1.0F), Mth.clamp(ff2, 0.0F, 1.0F), state.open);
        submitNodeCollector.submitModel(this.bookModel, bookState, poseStack, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, state.sprite, this.sprites, 0, state.breakProgress);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(T blockEntity) {
        return new AABB(blockEntity.getBlockPos()).expandTowards(0, 0.5, 0);
    }

    public static class State extends EnchantTableRenderState {
        public SpriteId sprite = TABLE_BOOK;
        public float height = 0.75f;
    }
}
