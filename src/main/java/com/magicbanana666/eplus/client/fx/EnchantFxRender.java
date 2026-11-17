package com.magicbanana666.eplus.client.fx;

import com.magicbanana666.eplus.EnchantingPlus;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.joml.Vector3f;

/**
 * Render types and quad helpers for the enchanting animation.
 * <p>
 * Based on the enchanting effects of <a href="https://modrinth.com/mod/cinematic-interactions">Cinematic Interactions</a>
 * by Southware (MIT), which also made the textures in {@code textures/fx}.
 */
public final class EnchantFxRender {

    public static final Identifier CIRCLE_TEXTURE = EnchantingPlus.id("textures/fx/enchant_circle_pixel.png");
    public static final Identifier GLOW_TEXTURE = EnchantingPlus.id("textures/fx/glow.png");
    public static final Identifier BEAM_TEXTURE = EnchantingPlus.id("textures/fx/beam.png");

    /** Full bright, no depth write, alpha blended: for the rune circles. */
    private static final RenderPipeline CIRCLE_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(EnchantingPlus.id("pipeline/enchant_circle"))
            .withVertexShader("core/entity")
            .withFragmentShader("core/entity")
            .withShaderDefine("EMISSIVE")
            .withShaderDefine("NO_OVERLAY")
            .withShaderDefine("NO_CARDINAL_LIGHTING")
            .withSampler("Sampler0")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();

    /** Full bright, no depth write, additive: for light (glow, beam, sparks). */
    private static final RenderPipeline GLOW_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(EnchantingPlus.id("pipeline/enchant_glow"))
            .withVertexShader("core/entity")
            .withFragmentShader("core/entity")
            .withShaderDefine("EMISSIVE")
            .withShaderDefine("NO_OVERLAY")
            .withShaderDefine("NO_CARDINAL_LIGHTING")
            .withSampler("Sampler0")
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();

    public static final RenderType CIRCLE = RenderType.create("eplus_enchant_circle",
            RenderSetup.builder(CIRCLE_PIPELINE).withTexture("Sampler0", CIRCLE_TEXTURE).sortOnUpload().createRenderSetup());
    public static final RenderType GLOW = RenderType.create("eplus_enchant_glow",
            RenderSetup.builder(GLOW_PIPELINE).withTexture("Sampler0", GLOW_TEXTURE).sortOnUpload().createRenderSetup());
    public static final RenderType BEAM = RenderType.create("eplus_enchant_beam",
            RenderSetup.builder(GLOW_PIPELINE).withTexture("Sampler0", BEAM_TEXTURE).sortOnUpload().createRenderSetup());

    /** Violet circle colors: top and bottom of the texture. */
    private static final int CIRCLE_TOP = 0xAB00F9;
    private static final int CIRCLE_BOTTOM = 0xC566FF;
    private static final float[][] UV = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};

    private EnchantFxRender() {
    }

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(CIRCLE_PIPELINE);
        event.registerPipeline(GLOW_PIPELINE);
    }

    /** A flat rune circle lying on the table, centered on (x, y, z) relative to the pose. */
    public static void circle(PoseStack.Pose pose, VertexConsumer vc, double x, double y, double z, double radius, double angle, float brightness) {
        final float presence = Math.clamp(brightness / 0.4F, 0F, 1F);
        final int alpha = channel(presence * presence * (3F - 2F * presence));
        if (alpha == 0) {
            return;
        }
        final double d = radius * Math.sqrt(2.0);
        for (int i = 0; i < 4; i++) {
            final double a = angle + Math.PI / 4 + i * Math.PI / 2;
            final int rgb = i < 2 ? CIRCLE_TOP : CIRCLE_BOTTOM;
            vc.addVertex(pose, (float) (x + Math.cos(a) * d), (float) y, (float) (z + Math.sin(a) * d))
                    .setColor(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, alpha)
                    .setUv(UV[i][0], UV[i][1])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightCoordsUtil.FULL_BRIGHT)
                    .setNormal(pose, 0F, 1F, 0F);
        }
    }

    /** A camera facing quad (glow, sparks). {@code left} and {@code up} are the camera axes. */
    public static void billboard(PoseStack.Pose pose, VertexConsumer vc, Vector3f left, Vector3f up, double x, double y, double z, double radius, double angle, float r, float g, float b) {
        final double d = radius * Math.sqrt(2.0);
        for (int i = 0; i < 4; i++) {
            final double a = angle + Math.PI / 4 + i * Math.PI / 2;
            final double u = Math.cos(a) * d;
            final double v = Math.sin(a) * d;
            glowVertex(pose, vc, x + left.x * u + up.x * v, y + left.y * u + up.y * v, z + left.z * u + up.z * v, i, r, g, b);
        }
    }

    /** Two crossed vertical planes of light going up from (x, y, z). */
    public static void beam(PoseStack.Pose pose, VertexConsumer vc, double x, double y, double z, double height, double width, double spin, float r, float g, float b) {
        for (int plane = 0; plane < 2; plane++) {
            final double a = spin + plane * Math.PI / 2;
            final double dx = Math.cos(a) * width / 2;
            final double dz = Math.sin(a) * width / 2;
            glowVertex(pose, vc, x - dx, y + height, z - dz, 0, r, g, b);
            glowVertex(pose, vc, x + dx, y + height, z + dz, 1, r, g, b);
            glowVertex(pose, vc, x + dx, y, z + dz, 2, r, g, b);
            glowVertex(pose, vc, x - dx, y, z - dz, 3, r, g, b);
        }
    }

    private static void glowVertex(PoseStack.Pose pose, VertexConsumer vc, double x, double y, double z, int corner, float r, float g, float b) {
        vc.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(channel(r), channel(g), channel(b), 255)
                .setUv(UV[corner][0], UV[corner][1])
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(pose, 0F, 1F, 0F);
    }

    private static int channel(float value) {
        return Math.clamp(Math.round(value * 255F), 0, 255);
    }
}
