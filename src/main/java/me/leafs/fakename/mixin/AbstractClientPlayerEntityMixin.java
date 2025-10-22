package me.leafs.fakename.mixin;

import com.mojang.authlib.GameProfile;
import me.leafs.fakename.utils.SkinSpoofStorage;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerEntityMixin {
    @Inject(method = "getSkinTextures", at = @At("HEAD"), cancellable = true)
    private void nameSpoofer$overrideSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity) (Object) this;
        GameProfile profile = player.getGameProfile();
        SkinSpoofStorage.getTextures(profile).ifPresent(cir::setReturnValue);
    }
}
