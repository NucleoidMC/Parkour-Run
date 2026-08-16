package io.github.haykam821.parkourrun.game.mixin;

import io.github.haykam821.parkourrun.Main;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStand.class)
public class ArmorStandMixin {
    @Inject(method="hurtServer", at = @At(value="INVOKE", target="Lnet/minecraft/world/entity/decoration/ArmorStand;brokenByPlayer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V"), cancellable = true)
    public void whetherToBreak(final ServerLevel level, final DamageSource source, final float damage, CallbackInfoReturnable<Boolean> info) {
        if (Main.isActiveGame(level)) {
            info.setReturnValue(false);
        }
    }
}
