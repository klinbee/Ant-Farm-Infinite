package com.quidvio.ant_farm_inf.mixin;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class WorldMixin {

    @Inject(method = "Lnet/minecraft/world/World;isValidHorizontally(Lnet/minecraft/util/math/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
    private static void changeBounds(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        //cir.cancel();
        // might not be needed
        System.out.println("checking bounds");
        boolean inNewBounds = pos.getX() >= -1 && pos.getZ() >= -30000000 && pos.getX() < 16 && pos.getZ() < 30000000;
        cir.setReturnValue(inNewBounds);
    }

}
