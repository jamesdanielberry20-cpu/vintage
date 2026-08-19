package com.vantage.client;

import com.vantage.client.config.ConfigManager;
import com.vantage.client.config.VantageConfig;
import com.vantage.client.esp.EntityInfoRenderer;
import com.vantage.client.esp.EspRenderer;
import com.vantage.client.feature.FlightManager;
import com.vantage.client.feature.ReachManager;
import com.vantage.client.keybind.KeybindManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/**
 * Entry point declared in fabric.mod.json's "client" entrypoint list. Since
 * this mod is {@code "environment": "client"} in its metadata, it will
 * refuse to load on a dedicated server entirely — there is no server-side
 * component to this mod by design.
 */
public class VantageClient implements ClientModInitializer {

	private static VantageClient instance;

	private final FlightManager flightManager = new FlightManager();
	private final ReachManager reachManager = new ReachManager();
	private KeybindManager keybindManager;

	@Override
	public void onInitializeClient() {
		instance = this;

		// Load config once at startup; individual managers read the shared
		// singleton via ConfigManager.get() rather than holding their own copy.
		ConfigManager.get();

		EspRenderer.register();
		EntityInfoRenderer.register();
		reachManager.register();
		flightManager.register();

		keybindManager = new KeybindManager(flightManager);
		keybindManager.register();

		// Apply the "ESP on join" preference each time a world/server
		// connection is (re)established, so it doesn't have to be toggled
		// manually every session if the player wants it always-on.
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			VantageConfig cfg = ConfigManager.get();
			cfg.espEnabled = cfg.espEnabledOnJoin;
		});

		System.out.println("[Vantage] Client-side singleplayer utility mod initialized.");
	}

	public static VantageClient getInstance() {
		return instance;
	}

	public FlightManager getFlightManager() {
		return flightManager;
	}

	public ReachManager getReachManager() {
		return reachManager;
	}
}
