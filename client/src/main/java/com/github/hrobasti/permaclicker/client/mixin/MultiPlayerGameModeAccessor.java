package com.github.hrobasti.permaclicker.client.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Compile-checked read access to the block-destroy state (progress and post-break delay), used to
 * detect whether a mining tick actually advanced. Shared between Fabric and NeoForge via
 * {@code permaclicker.client.mixins.json}.
 */
@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {
    @Accessor("destroyProgress")
    float permaclicker$getDestroyProgress();

    @Accessor("destroyDelay")
    int permaclicker$getDestroyDelay();
}
