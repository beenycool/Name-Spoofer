package me.leafs.fakename.mixin;

import com.mojang.authlib.GameProfile;
import me.leafs.fakename.utils.SkinSpoofStorage;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public class PlayerListEntryMixin {
    @Inject(method = "getSkinTextures", at = @At("HEAD"), cancellable = true)
    private void nameSpoofer$overrideSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        PlayerListEntry entry = (PlayerListEntry) (Object) this;
        GameProfile profile = entry.getProfile();
        SkinSpoofStorage.getTextures(profile).ifPresent(cir::setReturnValue);
    }
}
