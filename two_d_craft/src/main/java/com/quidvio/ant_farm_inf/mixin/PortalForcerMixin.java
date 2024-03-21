package com.quidvio.ant_farm_inf.mixin;


import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PortalForcer;
import net.minecraft.world.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PortalForcer.class)
public class PortalForcerMixin {

    /*
    @Redirect(method = "Lnet/minecraft/world/PortalForcer;createPortal(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction$Axis;)Ljava/util/Optional;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorder;contains(Lnet/minecraft/util/math/BlockPos;)Z"))
    private boolean changePortalBounds(WorldBorder instance, BlockPos pos) {
        return instance.contains(pos) && (pos.getX() >= -1 && pos.getX() < 16);
    }
     */

    // Not 16 because width of portal.
    @Redirect(method = "Lnet/minecraft/world/PortalForcer;createPortal(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction$Axis;)Ljava/util/Optional;", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;getX()I"))
    private int clampPortalXRange(BlockPos instance) {
        return Math.max(0,Math.min(13,instance.getX()));
    }

}
