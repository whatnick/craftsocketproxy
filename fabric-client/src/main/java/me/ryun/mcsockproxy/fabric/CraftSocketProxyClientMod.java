package me.ryun.mcsockproxy.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class CraftSocketProxyClientMod implements ClientModInitializer {
    private static KeyBinding openScreenKey;

    @Override
    public void onInitializeClient() {
        openScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.craftsocketproxy.open",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "category.craftsocketproxy"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while(openScreenKey.wasPressed()) {
                openScreen(client);
            }
        });
    }

    public static void openScreen(MinecraftClient client) {
        client.setScreen(new CraftSocketProxyScreen(client.currentScreen));
    }

    public static void sendStatus(Text message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if(client.player != null) {
            client.player.sendMessage(message, false);
        }
    }
}
