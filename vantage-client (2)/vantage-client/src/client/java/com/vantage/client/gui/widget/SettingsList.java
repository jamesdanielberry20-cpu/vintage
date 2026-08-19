package com.vantage.client.gui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/**
 * <b>Compatibility note:</b> {@code ElementListWidget.Entry}'s abstract
 * method signatures (render/children/selectableChildren) and helper methods
 * like {@code getRowWidth()}/{@code getScrollbarX()} have shifted more than
 * once across recent Minecraft versions. If this file doesn't compile as-is
 * against your exact Yarn build, check {@code ElementListWidget} in your
 * decompiled sources for the current method signatures — the settings/logic
 * in this file (toggle/slider/button behaviour) won't need to change, only
 * the override signatures.
 *
 * A vanilla-style scrollable list ({@link ElementListWidget}) whose rows are
 * built from a handful of reusable entry types (toggle, slider, button,
 * section/info labels). Used as the body of {@link com.vantage.client.gui.ClickGuiScreen}
 * for every tab, and rebuilt (via {@link #clearEntries()} + adds) each time
 * the tab or search query changes.
 */
public class SettingsList extends ElementListWidget<SettingsList.Row> {

	public SettingsList(MinecraftClient client, int width, int height, int top, int itemHeight) {
		super(client, width, height, top, itemHeight);
		this.setRenderHeader(false, 0);
	}

	@Override
	public int getRowWidth() {
		return Math.min(360, this.width - 20);
	}

	@Override
	protected int getScrollbarX() {
		return this.getRowRight() + 4;
	}

	public void addSectionLabel(String text, boolean skipIfFirst) {
		this.addEntry(Row.sectionLabel(text));
	}

	public void addInfoLabel(String text) {
		this.addEntry(Row.infoLabel(text));
	}

	public void addToggle(String label, boolean initial, Consumer<Boolean> onChange) {
		this.addEntry(Row.toggle(label, initial, onChange));
	}

	public void addSlider(String label, double initial, double min, double max,
			DoubleConsumer onChange, Runnable onReset) {
		this.addEntry(Row.slider(label, initial, min, max, onChange, onReset));
	}

	public void addButton(String label, Runnable onClick) {
		this.addEntry(Row.button(label, onClick));
	}

	public void clearEntries() {
		this.replaceEntries(List.of());
	}

	/** A single row in the settings list. Exactly one of the *Widget fields is non-null besides the label. */
	public static final class Row extends ElementListWidget.Entry<Row> {

		private final String label;
		private final ButtonWidget toggleWidget;
		private final LabeledSlider sliderWidget;
		private final ButtonWidget actionButton;
		private final boolean isSectionHeader;

		private Row(String label, ButtonWidget toggleWidget, LabeledSlider sliderWidget,
				ButtonWidget actionButton, boolean isSectionHeader) {
			this.label = label;
			this.toggleWidget = toggleWidget;
			this.sliderWidget = sliderWidget;
			this.actionButton = actionButton;
			this.isSectionHeader = isSectionHeader;
		}

		static Row sectionLabel(String text) {
			return new Row(text, null, null, null, true);
		}

		static Row infoLabel(String text) {
			return new Row(text, null, null, null, false);
		}

		static Row toggle(String label, boolean initial, Consumer<Boolean> onChange) {
			boolean[] state = { initial };
			ButtonWidget button = ButtonWidget.builder(toggleText(label, state[0]), b -> {
				state[0] = !state[0];
				b.setMessage(toggleText(label, state[0]));
				onChange.accept(state[0]);
			}).dimensions(0, 0, 340, 20).build();
			return new Row(label, button, null, null, false);
		}

		static Row slider(String label, double initial, double min, double max,
				DoubleConsumer onChange, Runnable onReset) {
			LabeledSlider slider = new LabeledSlider(0, 0, 280, 20, label, initial, min, max, onChange);
			ButtonWidget reset = ButtonWidget.builder(Text.literal("Reset"), b -> onReset.run())
					.dimensions(0, 0, 50, 20).build();
			Row row = new Row(label, null, slider, reset, false);
			return row;
		}

		static Row button(String label, Runnable onClick) {
			ButtonWidget button = ButtonWidget.builder(Text.literal(label), b -> onClick.run())
					.dimensions(0, 0, 340, 20).build();
			return new Row(label, null, null, button, false);
		}

		private static Text toggleText(String label, boolean value) {
			return Text.literal(label + ": " + (value ? "ON" : "OFF"));
		}

		@Override
		public List<? extends net.minecraft.client.gui.Element> children() {
			if (toggleWidget != null) return List.of(toggleWidget);
			if (sliderWidget != null && actionButton != null) return List.of(sliderWidget, actionButton);
			if (actionButton != null) return List.of(actionButton);
			return List.of();
		}

		@Override
		public List<? extends net.minecraft.client.gui.ParentElement> selectableChildren() {
			return List.of();
		}

		@Override
		public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
				int mouseX, int mouseY, boolean hovered, float tickDelta) {
			if (isSectionHeader) {
				context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
						Text.literal(label).formatted(net.minecraft.util.Formatting.YELLOW, net.minecraft.util.Formatting.BOLD),
						x, y + entryHeight / 2 - 4, 0xFFFFFF);
				return;
			}
			if (toggleWidget != null) {
				toggleWidget.setX(x);
				toggleWidget.setY(y);
				toggleWidget.setWidth(entryWidth);
				toggleWidget.render(context, mouseX, mouseY, tickDelta);
				return;
			}
			if (sliderWidget != null) {
				sliderWidget.setX(x);
				sliderWidget.setY(y);
				sliderWidget.setWidth(entryWidth - 54);
				sliderWidget.render(context, mouseX, mouseY, tickDelta);
				if (actionButton != null) {
					actionButton.setX(x + entryWidth - 50);
					actionButton.setY(y);
					actionButton.render(context, mouseX, mouseY, tickDelta);
				}
				return;
			}
			if (actionButton != null) {
				actionButton.setX(x);
				actionButton.setY(y);
				actionButton.setWidth(entryWidth);
				actionButton.render(context, mouseX, mouseY, tickDelta);
				return;
			}
			// Plain info label.
			context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, Text.literal(label),
					x, y + entryHeight / 2 - 4, 0xAAAAAA);
		}

		@Override
		public boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (toggleWidget != null) return toggleWidget.mouseClicked(mouseX, mouseY, button);
			if (sliderWidget != null) {
				boolean handled = sliderWidget.mouseClicked(mouseX, mouseY, button);
				if (actionButton != null) handled |= actionButton.mouseClicked(mouseX, mouseY, button);
				return handled;
			}
			if (actionButton != null) return actionButton.mouseClicked(mouseX, mouseY, button);
			return false;
		}

		@Override
		public boolean mouseReleased(double mouseX, double mouseY, int button) {
			if (sliderWidget != null) return sliderWidget.mouseReleased(mouseX, mouseY, button);
			return false;
		}

		@Override
		public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
			if (sliderWidget != null) return sliderWidget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
			return false;
		}
	}

	/** A vanilla {@link SliderWidget} whose label shows the live numeric value. */
	private static final class LabeledSlider extends SliderWidget {
		private final String baseLabel;
		private final double min;
		private final double max;
		private final DoubleConsumer onChange;

		LabeledSlider(int x, int y, int width, int height, String baseLabel,
				double initial, double min, double max, DoubleConsumer onChange) {
			super(x, y, width, height, Text.empty(), (initial - min) / (max - min));
			this.baseLabel = baseLabel;
			this.min = min;
			this.max = max;
			this.onChange = onChange;
			updateMessage();
		}

		private double currentValue() {
			return min + (max - min) * this.value;
		}

		@Override
		protected void updateMessage() {
			this.setMessage(Text.literal(String.format(Locale.ROOT, "%s: %.2f", baseLabel, currentValue())));
		}

		@Override
		protected void applyValue() {
			onChange.accept(currentValue());
		}
	}
}
