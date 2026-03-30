package com.github.hrobasti.permaclicker.fabric.mixin;

import com.github.hrobasti.permaclicker.fabric.PermaClickFabricEntrypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
final class FabricChatMiningAttackScreenBypassMixin {
    @Redirect(
        method = { "continueAttack", "startAttack" },
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;",
            opcode = Opcodes.GETFIELD
        ),
        require = 0
    )
    private Screen permaclicker$allowChatMiningWithOpenChat(Minecraft instance) {
        if (PermaClickFabricEntrypoint.isChatMiningContextActive(instance)) {
            return null;
        }

        return instance.screen;
    }
}
