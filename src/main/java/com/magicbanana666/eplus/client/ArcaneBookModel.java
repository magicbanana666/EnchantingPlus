package com.magicbanana666.eplus.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Sculpted covers using the vanilla book's named joints and page UV layout. */
final class ArcaneBookModel {
    private ArcaneBookModel() {}

    static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("left_lid", cover(true), PartPose.offset(0, 0, -1));
        root.addOrReplaceChild("right_lid", cover(false), PartPose.offset(0, 0, 1));
        root.addOrReplaceChild("seam", CubeListBuilder.create().texOffs(12, 0)
                .addBox(-1, -5, 0, 2, 10, 0.18F)
                .texOffs(36, 0).addBox(-1.1F, -5.3F, -0.08F, 2.2F, 0.45F, 0.45F)
                .texOffs(36, 0).addBox(-1.1F, 4.85F, -0.08F, 2.2F, 0.45F, 0.45F),
                PartPose.rotation(0, (float) Math.PI / 2, 0));
        // Keep the existing pages, thickness, flipping sheets and animation.
        root.addOrReplaceChild("left_pages", CubeListBuilder.create().texOffs(0, 10)
                .addBox(0, -4, -0.99F, 5, 8, 1), PartPose.ZERO);
        root.addOrReplaceChild("right_pages", CubeListBuilder.create().texOffs(12, 10)
                .addBox(0, -4, -0.01F, 5, 8, 1), PartPose.ZERO);
        root.addOrReplaceChild("flip_page1", CubeListBuilder.create().texOffs(24, 10)
                .addBox(0, -4, 0, 5, 8, 0.005F), PartPose.ZERO);
        root.addOrReplaceChild("flip_page2", CubeListBuilder.create().texOffs(24, 10)
                .addBox(0, -4, 0, 5, 8, 0.005F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static CubeListBuilder cover(boolean left) {
        float x = left ? -6 : 0;
        float outer = left ? -6.45F : 6;
        return CubeListBuilder.create().texOffs(left ? 0 : 16, 0)
                .addBox(x, -5, -0.18F, 6, 10, 0.18F)
                // Raised gold rails, not flat yellow lines painted on leather.
                .texOffs(36, 0).addBox(x - 0.2F, -5.3F, -0.3F, 6.4F, 0.45F, 0.55F)
                .texOffs(36, 0).addBox(x - 0.2F, 4.85F, -0.3F, 6.4F, 0.45F, 0.55F)
                .texOffs(36, 0).addBox(outer, -5.3F, -0.3F, 0.45F, 10.6F, 0.55F)
                .texOffs(36, 0).addBox(outer - 0.18F, -5.45F, -0.45F, 0.8F, 0.85F, 0.85F)
                .texOffs(36, 0).addBox(outer - 0.18F, 4.6F, -0.45F, 0.8F, 0.85F, 0.85F);
    }
}
