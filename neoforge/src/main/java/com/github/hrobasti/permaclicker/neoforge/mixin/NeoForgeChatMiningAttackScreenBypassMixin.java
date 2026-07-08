package com.github.hrobasti.permaclicker.neoforge.mixin;

import com.github.hrobasti.permaclicker.neoforge.PermaClickNeoForgeEntrypoint;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Allows continued block mining while a chat screen is open.
 *
 * <p>In 26.2 the screen check moved out of {@code Minecraft.continueAttack} into the caller
 * ({@code handleKeybinds}), where the boolean argument is computed from {@code gui.screen() == null}.
 * We force that argument to {@code true} while the mod's chat-mining context is active, instead of
 * redirecting the (now caller-side) screen read.</p>
 */
@Mixin(Minecraft.class)
final class NeoForgeChatMiningAttackScreenBypassMixin {
    @ModifyArg(
        method = "handleKeybinds",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;continueAttack(Z)V"
        ),
        require = 0
    )
    private boolean permaclicker$allowChatMiningWithOpenChat(boolean down) {
        Minecraft instance = (Minecraft) (Object) this;
        if (PermaClickNeoForgeEntrypoint.isChatMiningContextActive(instance)) {
            return true;
        }

        return down;
    }
}
