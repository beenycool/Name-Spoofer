package me.leafs.fakename.mixin;

import me.leafs.fakename.utils.NameUtils;
import net.minecraft.client.font.TextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TextRenderer.class)
public abstract class TextRendererMixin {
    @ModifyVariable(method = "draw(Lnet/minecraft/client/util/math/MatrixStack;Ljava/lang/String;FFI)I", at = @At("HEAD"), argsOnly = true)
    private String nameSpoofer$modifyDrawString(String value) {
        return NameUtils.apply(value);
    }

    @ModifyVariable(method = "drawWithShadow(Lnet/minecraft/client/util/math/MatrixStack;Ljava/lang/String;FFI)I", at = @At("HEAD"), argsOnly = true)
    private String nameSpoofer$modifyDrawWithShadow(String value) {
        return NameUtils.apply(value);
    }

    @ModifyVariable(method = "drawWithShadow(Lnet/minecraft/client/util/math/MatrixStack;Ljava/lang/String;FFIZ)I", at = @At("HEAD"), argsOnly = true)
    private String nameSpoofer$modifyDrawWithShadowBoolean(String value) {
        return NameUtils.apply(value);
    }

    @ModifyVariable(method = "getWidth(Ljava/lang/String;)I", at = @At("HEAD"), argsOnly = true)
    private String nameSpoofer$modifyGetWidth(String value) {
        return NameUtils.apply(value);
    }
}
