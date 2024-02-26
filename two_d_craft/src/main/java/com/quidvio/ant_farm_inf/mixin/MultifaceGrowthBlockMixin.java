package com.quidvio.ant_farm_inf.mixin;

import com.quidvio.ant_farm_inf.Ant_farm_inf_main;
import net.minecraft.block.BlockState;
import net.minecraft.block.MultifaceGrowthBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultifaceGrowthBlock.class)
public class MultifaceGrowthBlockMixin {

    @Inject(method = "Lnet/minecraft/block/MultifaceGrowthBlock;canGrowOn(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/Direction;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z", at = @At("RETURN"), cancellable = true)
    private static void ant_farm_inf_stopGrowthOnBorders_MGB(BlockView world, Direction direction, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(!state.equals(Ant_farm_inf_main.BORDER_BLOCK.getDefaultState()) && cir.getReturnValue());
    }

}
