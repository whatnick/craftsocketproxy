package me.ryun.mcsockproxy.fabric.mixin;

import me.ryun.mcsockproxy.fabric.CraftSocketProxyClientMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public abstract class MultiplayerScreenMixin extends Screen {
    protected MultiplayerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void craftsocketproxy$addLoginButton(CallbackInfo callbackInfo) {
        int buttonWidth = 158;
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.craftsocketproxy.multiplayer"), button -> {
            CraftSocketProxyClientMod.openScreen(MinecraftClient.getInstance());
        }).dimensions(width - buttonWidth - 8, 8, buttonWidth, 20).build());
    }
}
