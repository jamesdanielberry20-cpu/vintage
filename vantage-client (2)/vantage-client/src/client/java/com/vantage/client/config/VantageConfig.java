package com.vantage.client.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Plain data holder for every persisted setting. Kept as a simple POJO so it
 * can be serialized/deserialized with Gson without any custom adapters.
 * Every field has a sane default so a missing or partially-corrupted config
 * file still produces a usable object (see ConfigManager for the load logic).
 */
public class VantageConfig {

	// ---- ESP ----
	public boolean espEnabled = false;
	public double espMaxDistance = 64.0;
	public boolean espFilledBoxes = false;
	public float espBoxAlpha = 0.35f;

	/** Per-category enabled state. Keys match EntityCategory#name(). */
	public Map<String, Boolean> espCategoryEnabled = defaultCategoryMap();

	// ---- Entity Info ----
	public boolean entityInfoEnabled = false;
	public boolean showName = true;
	public boolean showDistance = true;
	public boolean showHealthNumeric = true;
	public boolean showHealthBar = true;
	public boolean showEntityType = false;

	// ---- Reach ----
	public boolean reachEnabled = false;
	public double reachBlockRange = 4.5;   // vanilla default block interaction range
	public double reachEntityRange = 3.0;  // vanilla default entity interaction range
	public double reachMax = 32.0;         // GUI slider cap

	// ---- Flight ----
	public boolean flightEnabled = false;
	public double flightHorizontalSpeed = 1.0; // multiplier over vanilla creative-fly speed
	public double flightVerticalSpeed = 1.0;

	// ---- General / GUI ----
	public boolean espEnabledOnJoin = false;
	public float guiScale = 1.0f;

	// ---- Keybinds (GLFW key codes; -1 = unbound). Stored purely for our own
	// records/import-export — Minecraft's KeyBinding system is the source of
	// truth at runtime and already persists to options.txt on its own. ----
	public Map<String, Integer> keybinds = new LinkedHashMap<>();

	private static Map<String, Boolean> defaultCategoryMap() {
		Map<String, Boolean> map = new LinkedHashMap<>();
		map.put("PLAYERS", true);
		map.put("HOSTILE_MOBS", true);
		map.put("PASSIVE_MOBS", true);
		map.put("ANIMALS", true);
		map.put("VILLAGERS", true);
		map.put("ITEMS", false);
		map.put("PROJECTILES", false);
		map.put("VEHICLES", false);
		map.put("OTHER", false);
		return map;
	}
}
