package com.vantage.client.gui;

import com.vantage.client.config.ConfigManager;
import com.vantage.client.config.VantageConfig;
import com.vantage.client.esp.EntityCategory;
import com.vantage.client.gui.widget.SettingsList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The mod's configuration GUI, opened with the "Open ClickGUI" keybind
 * (Right Shift by default). Organized as a row of category tabs above a
 * scrollable list of the matching settings; a search box at the top filters
 * the current tab's entries by label.
 */
public class ClickGuiScreen extends Screen {

	private enum Tab {
		ESP, ENTITY_INFO, REACH, FLIGHT, GENERAL, KEYBINDS
	}

	private Tab currentTab = Tab.ESP;
	private SettingsList list;
	private TextFieldWidget searchBox;
	private final List<ButtonWidget> tabButtons = new ArrayList<>();

	public ClickGuiScreen() {
		super(Text.literal("Vantage Client"));
	}

	@Override
	protected void init() {
		int tabWidth = 90;
		int startX = this.width / 2 - (tabWidth * Tab.values().length) / 2;
		int y = 24;

		tabButtons.clear();
		for (int i = 0; i < Tab.values().length; i++) {
			Tab tab = Tab.values()[i];
			ButtonWidget button = ButtonWidget.builder(Text.literal(prettyTabName(tab)), b -> switchTab(tab))
					.dimensions(startX + i * tabWidth, y, tabWidth - 2, 20)
					.build();
			addDrawableChild(button);
			tabButtons.add(button);
		}

		searchBox = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, y + 26, 200, 18,
				Text.literal("Search"));
		searchBox.setPlaceholder(Text.literal("Search settings..."));
		searchBox.setChangedListener(query -> rebuildList());
		addDrawableChild(searchBox);

		list = new SettingsList(this.client, this.width, this.height, y + 50, 24);
		addDrawableChild(list);

		rebuildList();
	}

	private void switchTab(Tab tab) {
		this.currentTab = tab;
		if (searchBox != null) {
			searchBox.setText("");
		}
		rebuildList();
	}

	private void rebuildList() {
		if (list == null) {
			return;
		}
		list.clearEntries();
		String query = searchBox == null ? "" : searchBox.getText().toLowerCase(Locale.ROOT);
		VantageConfig cfg = ConfigManager.get();

		switch (currentTab) {
			case ESP -> buildEspTab(cfg, query);
			case ENTITY_INFO -> buildEntityInfoTab(cfg, query);
			case REACH -> buildReachTab(cfg, query);
			case FLIGHT -> buildFlightTab(cfg, query);
			case GENERAL -> buildGeneralTab(cfg, query);
			case KEYBINDS -> buildKeybindsTab(query);
		}
	}

	private void buildEspTab(VantageConfig cfg, String query) {
		addToggle("ESP Enabled", cfg.espEnabled, v -> cfg.espEnabled = v, query);
		addSlider("Max Distance", cfg.espMaxDistance, 4, 128,
				v -> cfg.espMaxDistance = v, () -> cfg.espMaxDistance = 64.0, query);
		addToggle("Filled Boxes", cfg.espFilledBoxes, v -> cfg.espFilledBoxes = v, query);
		addSlider("Box Opacity", cfg.espBoxAlpha, 0.05, 1.0,
				v -> cfg.espBoxAlpha = (float) v, () -> cfg.espBoxAlpha = 0.35f, query);

		list.addSectionLabel("Categories", query.isEmpty());
		for (EntityCategory category : EntityCategory.values()) {
			boolean enabled = cfg.espCategoryEnabled.getOrDefault(category.name(), false);
			addToggle(category.displayName, enabled,
					v -> cfg.espCategoryEnabled.put(category.name(), v), query);
		}
	}

	private void buildEntityInfoTab(VantageConfig cfg, String query) {
		addToggle("Entity Info Enabled", cfg.entityInfoEnabled, v -> cfg.entityInfoEnabled = v, query);
		addToggle("Show Name", cfg.showName, v -> cfg.showName = v, query);
		addToggle("Show Distance", cfg.showDistance, v -> cfg.showDistance = v, query);
		addToggle("Show Health (numbers)", cfg.showHealthNumeric, v -> cfg.showHealthNumeric = v, query);
		addToggle("Show Health Bar", cfg.showHealthBar, v -> cfg.showHealthBar = v, query);
        addToggle("Show Entity Type", cfg.showEntityType, v -> cfg.showEntityType = v, query);
	}

	private void buildReachTab(VantageConfig cfg, String query) {
		addToggle("Reach Modifier Enabled", cfg.reachEnabled, v -> cfg.reachEnabled = v, query);
		addSlider("Block Reach", cfg.reachBlockRange, 1.0, cfg.reachMax,
				v -> cfg.reachBlockRange = v, () -> cfg.reachBlockRange = 4.5, query);
		addSlider("Entity Reach", cfg.reachEntityRange, 1.0, cfg.reachMax,
				v -> cfg.reachEntityRange = v, () -> cfg.reachEntityRange = 3.0, query);
		if (matches("Reset Reach To Vanilla", query)) {
			list.addButton("Reset Reach To Vanilla", () -> {
				cfg.reachBlockRange = 4.5;
				cfg.reachEntityRange = 3.0;
				ConfigManager.save();
				rebuildList();
			});
		}
	}

	private void buildFlightTab(VantageConfig cfg, String query) {
		addToggle("Flight Enabled", cfg.flightEnabled, v -> cfg.flightEnabled = v, query);
		addSlider("Horizontal Speed", cfg.flightHorizontalSpeed, 0.1, 5.0,
				v -> cfg.flightHorizontalSpeed = v, () -> cfg.flightHorizontalSpeed = 1.0, query);
		addSlider("Vertical Speed", cfg.flightVerticalSpeed, 0.1, 5.0,
				v -> cfg.flightVerticalSpeed = v, () -> cfg.flightVerticalSpeed = 1.0, query);
	}

	private void buildGeneralTab(VantageConfig cfg, String query) {
		addToggle("Enable ESP On World Join", cfg.espEnabledOnJoin, v -> cfg.espEnabledOnJoin = v, query);
		if (matches("Reset All Settings", query)) {
			list.addButton("Reset All Settings", () -> {
				VantageConfig fresh = new VantageConfig();
				ConfigManager.save(fresh);
				this.client.setScreen(null);
				this.client.setScreen(new ClickGuiScreen());
			});
		}
	}

	private void buildKeybindsTab(String query) {
		if (matches("Keybinds are managed in vanilla Controls", query)) {
			list.addInfoLabel("Rebind Vantage keys under");
			list.addInfoLabel("Options -> Controls -> Key Binds -> Vantage Client");
			list.addButton("Open Controls Menu", () ->
					this.client.setScreen(new net.minecraft.client.gui.screen.option.ControlsOptionsScreen(
							this, this.client.options)));
		}
	}

	private boolean matches(String label, String query) {
		return query.isEmpty() || label.toLowerCase(Locale.ROOT).contains(query);
	}

	private void addToggle(String label, boolean current, java.util.function.Consumer<Boolean> setter, String query) {
		if (!matches(label, query)) {
			return;
		}
		list.addToggle(label, current, value -> {
			setter.accept(value);
			ConfigManager.save();
		});
	}

	private void addSlider(String label, double current, double min, double max,
			java.util.function.Consumer<Double> setter, Runnable resetToDefault, String query) {
		if (!matches(label, query)) {
			return;
		}
		list.addSlider(label, current, min, max, value -> {
			setter.accept(value);
			ConfigManager.save();
		}, () -> {
			resetToDefault.run();
			ConfigManager.save();
			rebuildList();
		});
	}

	private static String prettyTabName(Tab tab) {
		return switch (tab) {
			case ESP -> "ESP";
			case ENTITY_INFO -> "Info";
			case REACH -> "Reach";
			case FLIGHT -> "Flight";
			case GENERAL -> "General";
			case KEYBINDS -> "Keybinds";
		};
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context, mouseX, mouseY, delta);
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 6, 0xFFFFFF);
	}

	@Override
	public boolean shouldPause() {
		// This is a single-player utility GUI; pausing the game while it's
		// open matches vanilla expectations and avoids the world advancing
		// while the player is fiddling with sliders.
		return true;
	}

	@Override
	public void close() {
		ConfigManager.save();
		super.close();
	}
}
