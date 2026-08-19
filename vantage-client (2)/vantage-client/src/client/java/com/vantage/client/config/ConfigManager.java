package com.vantage.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles reading and writing {@link VantageConfig} to
 * {@code .minecraft/config/vantage-client.json}.
 *
 * Deliberately defensive: any failure to read/parse the file (missing,
 * empty, corrupted JSON, wrong types, IO error) falls back to a fresh
 * default config instead of crashing the game, and immediately re-writes
 * a clean file so the corruption doesn't persist.
 */
public final class ConfigManager {

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();

	private static final Path CONFIG_PATH = FabricLoader.getInstance()
			.getConfigDir()
			.resolve("vantage-client.json");

	private static VantageConfig instance;

	private ConfigManager() {
	}

	public static VantageConfig get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	public static VantageConfig load() {
		if (!Files.exists(CONFIG_PATH)) {
			VantageConfig fresh = new VantageConfig();
			save(fresh);
			return fresh;
		}

		try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
			VantageConfig loaded = GSON.fromJson(reader, VantageConfig.class);
			if (loaded == null) {
				// Empty or literally "null" file.
				throw new IOException("Config file was empty or invalid");
			}
			// Guard against a corrupted file that dropped the category map,
			// which would otherwise leave every ESP category unusable.
			if (loaded.espCategoryEnabled == null || loaded.espCategoryEnabled.isEmpty()) {
				loaded.espCategoryEnabled = new VantageConfig().espCategoryEnabled;
			}
			if (loaded.keybinds == null) {
				loaded.keybinds = new VantageConfig().keybinds;
			}
			return loaded;
		} catch (Exception e) {
			// Covers JsonSyntaxException, IOException, etc. Back up the bad
			// file so the user doesn't silently lose it, then start fresh.
			backupCorruptFile();
			VantageConfig fresh = new VantageConfig();
			save(fresh);
			return fresh;
		}
	}

	public static void save(VantageConfig config) {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException e) {
			// Non-fatal: settings just won't persist this session. Avoid
			// throwing out of a render/tick call site.
			System.err.println("[Vantage] Failed to save config: " + e.getMessage());
		}
	}

	public static void save() {
		if (instance != null) {
			save(instance);
		}
	}

	private static void backupCorruptFile() {
		try {
			if (Files.exists(CONFIG_PATH)) {
				Path backup = CONFIG_PATH.resolveSibling("vantage-client.json.bak");
				Files.copy(CONFIG_PATH, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException ignored) {
			// Best-effort only.
		}
	}
}
