package com.rotbound.dailychaos.mixin;

import com.rotbound.dailychaos.RotboundDailyChaos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Changes the scheduled interval used by Java fire itself, not the world random-tick rate. */
@Mixin(FireBlock.class)
public abstract class FireBlockMixin {
    @Inject(method = "getFireTickDelay", at = @At("RETURN"), cancellable = true)
    private static void rotbound$adjustFireDelay(RandomSource random, CallbackInfoReturnable<Integer> callback) {
        int mode = RotboundDailyChaos.fireTickDelayMode();
        if (mode == 1) {
            callback.setReturnValue(Math.max(1, callback.getReturnValue() / 2));
        } else if (mode == 2) {
            callback.setReturnValue(1);
        }
    }
}
