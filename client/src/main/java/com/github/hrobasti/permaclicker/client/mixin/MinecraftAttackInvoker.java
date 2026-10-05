package com.github.hrobasti.permaclicker.client.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Compile-checked access to Minecraft's private attack entry points, replacing reflection by
 * method name. A rename in a future Minecraft version now fails at build/mixin-apply time
 * instead of silently falling through at runtime. Shared between Fabric and NeoForge via
 * {@code permaclicker.client.mixins.json}.
 */
@Mixin(Minecraft.class)
public interface MinecraftAttackInvoker {
    @Invoker("continueAttack")
    void permaclicker$invokeContinueAttack(boolean down);

    @Invoker("startAttack")
    boolean permaclicker$invokeStartAttack();
}
