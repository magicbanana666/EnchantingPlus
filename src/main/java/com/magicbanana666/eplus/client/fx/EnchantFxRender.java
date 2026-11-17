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
import com.mojang.blaze3d.PrimitiveTopology;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
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

    public static final Identifier CIRCLE_TEXTURE = EnchantingPlus.id("textures/fx/arcane_circle.png");
    public static final Identifier GLOW_TEXTURE = EnchantingPlus.id("textures/fx/glow.png");
    public static final Identifier BEAM_TEXTURE = EnchantingPlus.id("textures/fx/beam.png");
    public static final Identifier RIBBON_TEXTURE = EnchantingPlus.id("textures/fx/arcane_ribbon.png");
    public static final Identifier JEWEL_TEXTURE = EnchantingPlus.id("textures/fx/arcane_jewel.png");
    public static final RenderType JEWEL = RenderTypes.entitySolid(JEWEL_TEXTURE);

    /** Full bright, no depth write, alpha blended: for the rune circles. */
    private static final RenderPipeline CIRCLE_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(EnchantingPlus.id("pipeline/enchant_circle"))
            .withVertexShader("core/entity")
            .withFragmentShader("core/entity")
            .withShaderDefine("EMISSIVE")
            .withShaderDefine("NO_OVERLAY")
            .withShaderDefine("NO_CARDINAL_LIGHTING")
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
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
            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();

    public static final RenderType CIRCLE = RenderType.create("eplus_enchant_circle",
            RenderSetup.builder(CIRCLE_PIPELINE).withTexture("Sampler0", CIRCLE_TEXTURE).sortOnUpload().createRenderSetup());
    public static final RenderType GLOW = RenderType.create("eplus_enchant_glow",
            RenderSetup.builder(GLOW_PIPELINE).withTexture("Sampler0", GLOW_TEXTURE).sortOnUpload().createRenderSetup());
    public static final RenderType BEAM = RenderType.create("eplus_enchant_beam",
            RenderSetup.builder(GLOW_PIPELINE).withTexture("Sampler0", BEAM_TEXTURE).sortOnUpload().createRenderSetup());
    public static final RenderType RIBBON = RenderType.create("eplus_enchant_ribbon",
            RenderSetup.builder(GLOW_PIPELINE).withTexture("Sampler0", RIBBON_TEXTURE).sortOnUpload().createRenderSetup());

    /** Muted violet, matching the reference's inlaid circle and crystals. */
    private static final int CIRCLE_TOP = 0xA26CB3;
    private static final int CIRCLE_BOTTOM = 0xC89BDA;
    private static final float[][] UV = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};

    private EnchantFxRender() {
    }

    /** A solid octahedral jewel with actual triangular facets and depth writes. */
    public static void jewel(PoseStack.Pose pose, VertexConsumer vc, double x, double y, double z,
            double rx, double ry, double rz, boolean gold) {
        double[][] ring = {{x-rx,y,z}, {x,y+ry,z}, {x+rx,y,z}, {x,y-ry,z}};
        for (int side : new int[] {-1, 1}) {
            for (int i = 0; i < 4; i++) {
                double[] a = ring[i];
                double[] b = ring[(i+1)%4];
                double[] c = {x,y,z+side*rz};
                if (side > 0) { double[] swap=a; a=b; b=swap; }
                Vector3f normal = new Vector3f((float)(b[0]-a[0]), (float)(b[1]-a[1]), (float)(b[2]-a[2]))
                        .cross(new Vector3f((float)(c[0]-a[0]), (float)(c[1]-a[1]), (float)(c[2]-a[2]))).normalize();
                float shade = 0.70F + 0.10F * i;
                for (double[] p : new double[][] {a,b,c,c}) {
                    vc.addVertex(pose, (float)p[0], (float)p[1], (float)p[2])
                            .setColor(channel((gold ? 1F : 0.83F)*shade), channel((gold ? 0.70F : 0.57F)*shade), channel((gold ? 0.28F : 1F)*shade), 255)
                            .setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT)
                            .setNormal(pose, normal.x, normal.y, normal.z);
                }
            }
        }
    }

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(CIRCLE_PIPELINE);
        event.registerPipeline(GLOW_PIPELINE);
    }

    /** A tapered, tilted violet trail circling the floating book. */
    public static void orbit(PoseStack.Pose pose, VertexConsumer vc, double clock, double radius, double height, double phase, float brightness) {
        orbit(pose, vc, clock, radius, height, phase, brightness, 0.62F, 0.20F, 1F, 0.30);
    }

    /** Tinted trail for unlock hints, sharing the existing aura's soft profile. */
    public static void orbit(PoseStack.Pose pose, VertexConsumer vc, double clock, double radius, double height, double phase,
            float brightness, float red, float green, float blue, double tilt) {
        final int segments = 48;
        final double spin = clock * 0.65 + phase;
        for (int i = 0; i < segments; i++) {
            final double a = spin + i * Math.PI * 1.8 / segments;
            final double b = spin + (i + 1) * Math.PI * 1.8 / segments;
            final double ay = height + tilt * Math.sin(a + clock * 0.12);
            final double by = height + tilt * Math.sin(b + clock * 0.12);
            final double width = 0.012;
            final float k = brightness * (0.12F + 0.88F * i / segments);
            glowVertex(pose, vc, 0.5 + Math.cos(a) * (radius - width), ay, 0.5 + Math.sin(a) * (radius - width), 0, red*k, green*k, blue*k);
            glowVertex(pose, vc, 0.5 + Math.cos(b) * (radius - width), by, 0.5 + Math.sin(b) * (radius - width), 1, red*k, green*k, blue*k);
            glowVertex(pose, vc, 0.5 + Math.cos(b) * (radius + width), by, 0.5 + Math.sin(b) * (radius + width), 2, red*k, green*k, blue*k);
            glowVertex(pose, vc, 0.5 + Math.cos(a) * (radius + width), ay, 0.5 + Math.sin(a) * (radius + width), 3, red*k, green*k, blue*k);
        }
    }

    /** Four softly glowing strokes form a readable camera-facing rune. */
    public static void runeDiamond(PoseStack.Pose pose, VertexConsumer vc, Vector3f left, Vector3f up,
            double x, double y, double z, double width, double height, float r, float g, float b) {
        final double[][] points = {{0, -height}, {width, 0}, {0, height}, {-width, 0}};
        for (int edge = 0; edge < 4; edge++) {
            final double[] a = points[edge];
            final double[] c = points[(edge + 1) % 4];
            final double dx = c[0] - a[0], dy = c[1] - a[1];
            final double length = Math.sqrt(dx * dx + dy * dy);
            final double px = -dy / length * 0.006, py = dx / length * 0.006;
            final double[][] corners = {{a[0]-px,a[1]-py}, {c[0]-px,c[1]-py}, {c[0]+px,c[1]+py}, {a[0]+px,a[1]+py}};
            for (int i = 0; i < 4; i++) {
                final double u = corners[i][0], v = corners[i][1];
                glowVertex(pose, vc, x + left.x*u + up.x*v, y + left.y*u + up.y*v, z + left.z*u + up.z*v, i, r, g, b);
            }
        }
    }

    /** A flat rune circle lying on the table, centered on (x, y, z) relative to the pose. */
    public static void circle(PoseStack.Pose pose, VertexConsumer vc, double x, double y, double z, double radius, double angle, float brightness) {
        final float presence = Math.clamp(brightness, 0F, 1F);
        final int alpha = channel(0.42F * presence * presence * (3F - 2F * presence));
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
    public static void itemSeal(PoseStack.Pose pose, VertexConsumer vc, Vector3f left, Vector3f up, double x, double y, double z, double radius, double angle, float opacity) {
        final int alpha = channel(opacity);
        final double d = radius * Math.sqrt(2.0);
        for (int i = 0; i < 4; i++) {
            final double a = angle + Math.PI / 4 + i * Math.PI / 2;
            final double u = Math.cos(a) * d;
            final double v = Math.sin(a) * d;
            vc.addVertex(pose, (float) (x + left.x * u + up.x * v), (float) (y + left.y * u + up.y * v), (float) (z + left.z * u + up.z * v))
                    .setColor(225, 175, 255, alpha)
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
