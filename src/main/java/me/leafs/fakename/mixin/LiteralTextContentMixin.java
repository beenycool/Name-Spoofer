package me.leafs.fakename.mixin;

import me.leafs.fakename.utils.NameUtils;
import net.minecraft.text.LiteralTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LiteralTextContent.class)
public abstract class LiteralTextContentMixin {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static String nameSpoofer$applySpoofing(String literal) {
        return NameUtils.apply(literal);
    }
}
