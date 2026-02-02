package de.coldfang.voidpressure.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.coldfang.voidpressure.blockentity.PressureAltarBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class PressureAltarBlockEntityRenderer implements BlockEntityRenderer<PressureAltarBlockEntity> {

    public PressureAltarBlockEntityRenderer(
            @NotNull BlockEntityRendererProvider.Context ctx
    ) {
    }

    @Override
    public void render(
            @NotNull PressureAltarBlockEntity altar,
            float partialTick,
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        ItemStack stack = altar.getOffered();
        if (stack.isEmpty()) return;

        poseStack.pushPose();

        poseStack.translate(0.5D, 1.25D, 0.5D);

        double t = System.nanoTime() * 1.0e-9;
        float hover = (float) (Math.sin(t * 1.5) * 0.06);
        poseStack.translate(0.0D, hover, 0.0D);

        float rot = (float) (t * 40.0 % 360.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(rot));

        poseStack.scale(0.6f, 0.6f, 0.6f);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.GROUND,
                packedLight,
                packedOverlay,
                poseStack,
                buffer,
                altar.getLevel(),
                0
        );

        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(@NotNull PressureAltarBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
