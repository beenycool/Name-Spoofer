package me.leafs.fakename.mixin;

import me.leafs.fakename.utils.NameUtils;
import net.minecraft.text.LiteralTextContent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LiteralTextContent.class)
public abstract class LiteralTextContentMixin {
    @Mutable
    @Shadow @Final private String literal;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void nameSpoofer$applySpoofing(String literal, CallbackInfo ci) {
        this.literal = NameUtils.apply(this.literal);
    }
}
