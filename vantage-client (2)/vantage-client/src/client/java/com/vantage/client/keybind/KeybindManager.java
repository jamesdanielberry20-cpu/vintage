package com.vantage.client.keybind;

import com.vantage.client.config.ConfigManager;
import com.vantage.client.config.VantageConfig;
import com.vantage.client.feature.FlightManager;
import com.vantage.client.gui.ClickGuiScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Registers every keybind through Minecraft's normal {@link KeyBinding}
 * system (so they show up, and are rebindable, in
 * Options -> Controls -> Key Binds like any vanilla keybind) and polls them
 * once per client tick with {@code wasPressed()} for clean single-fire
 * toggling.
 */
public final class KeybindManager {

	private static final String CATEGORY = "key.categories.vantage-client";

	public final KeyBinding toggleEsp;
	public final KeyBinding toggleEntityInfo;
	public final KeyBinding toggleFlight;
	public final KeyBinding openClickGui;
	public final KeyBinding toggleReach;

	private final FlightManager flightManager;

	public KeybindManager(FlightManager flightManager) {
		this.flightManager = flightManager;

		toggleEsp = register("key.vantage-client.toggle_esp", GLFW.GLFW_KEY_RIGHT_BRACKET);
		toggleEntityInfo = register("key.vantage-client.toggle_entity_info", GLFW.GLFW_KEY_UNKNOWN);
		toggleFlight = register("key.vantage-client.toggle_flight", GLFW.GLFW_KEY_V);
		openClickGui = register("key.vantage-client.open_gui", GLFW.GLFW_KEY_1);
		toggleReach = register("key.vantage-client.toggle_reach", GLFW.GLFW_KEY_UNKNOWN);
	}

	private static KeyBinding register(String translationKey, int defaultGlfwKey) {
		return KeyBindingHelper.registerKeyBinding(new KeyBinding(
				translationKey,
				InputUtil.Type.KEYSYM,
				defaultGlfwKey,
				CATEGORY
		));
	}

	public void register() {
		ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
	}

	private void onTick(MinecraftClient client) {
		VantageConfig cfg = ConfigManager.get();

		while (toggleEsp.wasPressed()) {
			cfg.espEnabled = !cfg.espEnabled;
			ConfigManager.save();
		}
		while (toggleEntityInfo.wasPressed()) {
			cfg.entityInfoEnabled = !cfg.entityInfoEnabled;
			ConfigManager.save();
		}
		while (toggleFlight.wasPressed()) {
			flightManager.toggle();
		}
		while (toggleReach.wasPressed()) {
			cfg.reachEnabled = !cfg.reachEnabled;
			ConfigManager.save();
		}
		while (openClickGui.wasPressed()) {
			if (client.currentScreen == null) {
				client.setScreen(new ClickGuiScreen());
			}
		}
	}
}
