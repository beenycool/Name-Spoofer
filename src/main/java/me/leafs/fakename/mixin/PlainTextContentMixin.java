package me.leafs.fakename.mixin;

import me.leafs.fakename.utils.NameUtils;
import net.minecraft.text.PlainTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlainTextContent.Literal.class)
public abstract class PlainTextContentMixin {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static String nameSpoofer$applySpoofing(String literal) {
        return NameUtils.apply(literal);
    }
}
