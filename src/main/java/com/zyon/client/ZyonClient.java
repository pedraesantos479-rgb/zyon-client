package com.zyon.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ZyonClient implements ClientModInitializer {
    public static final DiscoverySettings SETTINGS = new DiscoverySettings();
    public static final DiscoveryScanner SCANNER = new DiscoveryScanner();
    public static KeyBinding zoomKey;
    public static KeyBinding fcForward, fcBack, fcLeft, fcRight, fcUp, fcDown;
    private static KeyBinding openMenu;

    @Override
    public void onInitializeClient() {
        openMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zyon.open_gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyBinding.Category.MISC
        ));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zyon.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C,
                KeyBinding.Category.MISC
        ));
        fcForward = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zyon.fc_forward", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_W, KeyBinding.Category.MISC));
        fcBack = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zyon.fc_back", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_S, KeyBinding.Category.MISC));
        fcLeft = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zyon.fc_left", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_A, KeyBinding.Category.MISC));
        fcRight = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zyon.fc_right", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_D, KeyBinding.Category.MISC));
        fcUp = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zyon.fc_up", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_SPACE, KeyBinding.Category.MISC));
        fcDown = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.zyon.fc_down", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT, KeyBinding.Category.MISC));
        ClientTickEvents.START_CLIENT_TICK.register(client -> ZyonModules.startTick(client));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new ZyonScreen());
                else if (client.currentScreen instanceof ZyonScreen) client.setScreen(null);
            }
            if (client.world == null || client.player == null) return;
            if (SETTINGS.anyScannerEnabled()) SCANNER.tick(client);
            ZyonModules.tick(client);
        });
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.of("zyon", "hud"),
                (context, tickCounter) -> ZyonModules.renderHud(context, tickCounter.getTickProgress(false)));
    }
}
