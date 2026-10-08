package net.kroet.permaclicker.client.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Compile-checked access to Minecraft's private attack entry points: a rename
 * in a Minecraft update fails the build or mixin application instead of
 * silently doing nothing at runtime.
 */
@Mixin(Minecraft.class)
public interface MinecraftAttackInvoker {
    @Invoker("continueAttack")
    void permaclicker$invokeContinueAttack(boolean down);

    @Invoker("startAttack")
    boolean permaclicker$invokeStartAttack();
}
