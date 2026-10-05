package com.github.hrobasti.permaclicker.client.mixin;

import com.github.hrobasti.permaclicker.client.MiningTickGuard;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips vanilla's per-tick {@code continueAttack} once PermaClicker has already advanced mining in
 * the current tick (see {@link MiningTickGuard}). PermaClicker's own invoker call runs before the
 * guard is armed, so it is never skipped. Shared between Fabric and NeoForge via
 * {@code permaclicker.client.mixins.json}.
 */
@Mixin(Minecraft.class)
abstract class MinecraftContinueAttackGuardMixin {
    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void permaclicker$skipWhenMiningAlreadyAdvanced(boolean down, CallbackInfo ci) {
        if (MiningTickGuard.shouldSkipVanillaContinueAttack()) {
            ci.cancel();
        }
    }
}
