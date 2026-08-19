package com.vantage.client.esp;

import com.vantage.client.config.ConfigManager;
import com.vantage.client.config.VantageConfig;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Quaternionf;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Locale;

/**
 * Draws a small floating label above each ESP-eligible entity: name,
 * distance in blocks, HP text, and a simple two-tone health bar. Text is
 * billboarded to face the camera the same way vanilla nameplates are, so it
 * always reads correctly regardless of view angle.
 *
 * This intentionally reuses the same category/distance/enabled filtering as
 * {@link EspRenderer} but is toggled by a separate config flag, so info text
 * can be shown/hidden independently of the boxes themselves.
 */
public final class EntityInfoRenderer {

	private static final double LABEL_Y_OFFSET = 0.35;
	private static final float TEXT_SCALE = 0.025f;
	private static final int BAR_WIDTH = 40;
	private static final int BAR_HEIGHT = 4;

	private EntityInfoRenderer() {
	}

	public static void register() {
		WorldRenderEvents.AFTER_ENTITIES.register(EntityInfoRenderer::onRender);
	}

	private static void onRender(WorldRenderContext context) {
		VantageConfig cfg = ConfigManager.get();
		if (!cfg.espEnabled || !cfg.entityInfoEnabled) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		World world = client.world;
		if (world == null || client.player == null) {
			return;
		}

		MatrixStack matrices = context.matrixStack();
		VertexConsumerProvider consumers = context.consumers();
		if (matrices == null || consumers == null) {
			return;
		}

		TextRenderer textRenderer = client.textRenderer;
		Vec3d camPos = context.camera().getPos();
		Quaternionf cameraRotation = context.camera().getRotation();
		double maxDistSq = cfg.espMaxDistance * cfg.espMaxDistance;
		float partialTick = context.tickCounter().getTickProgress(true);

		for (Entity entity : world.getEntities()) {
			if (entity == client.player || entity == client.cameraEntity) {
				continue;
			}
			double distSq = entity.squaredDistanceTo(client.player);
			if (distSq > maxDistSq) {
				continue;
			}
			EntityCategory category = EntityCategory.classify(entity);
			Boolean catEnabled = cfg.espCategoryEnabled.get(category.name());
			if (catEnabled == null || !catEnabled) {
				continue;
			}

			double distance = Math.sqrt(distSq);
			Vec3d pos = entity.getLerpedPos(partialTick)
					.add(0, entity.getHeight() + LABEL_Y_OFFSET, 0);

			matrices.push();
			matrices.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z);
			matrices.multiply(cameraRotation);
			matrices.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

			int line = 0;
			if (cfg.showName) {
				String name = entity.getName().getString();
				drawCenteredText(matrices, consumers, textRenderer, name, line++);
			}
			if (cfg.showEntityType) {
				String type = category.displayName;
				drawCenteredText(matrices, consumers, textRenderer, type, line++);
			}
			if (cfg.showDistance) {
				String dist = String.format(Locale.ROOT, "%.1fm", distance);
				drawCenteredText(matrices, consumers, textRenderer, dist, line++);
			}
			if (entity instanceof LivingEntity living) {
				float hp = Math.max(living.getHealth(), 0f);
				float maxHp = Math.max(living.getMaxHealth(), 1f);
				if (cfg.showHealthNumeric) {
					String hpText = String.format(Locale.ROOT, "%.0f / %.0f HP", hp, maxHp);
					drawCenteredText(matrices, consumers, textRenderer, hpText, line++);
				}
				if (cfg.showHealthBar) {
					drawHealthBar(matrices, consumers, hp / maxHp, line);
				}
			}

			matrices.pop();
		}
	}

	private static void drawCenteredText(MatrixStack matrices, VertexConsumerProvider consumers,
			TextRenderer textRenderer, String text, int line) {
		float width = textRenderer.getWidth(text);
		float y = -line * 10f;
		textRenderer.draw(text, -width / 2f, y, 0xFFFFFF, false,
				matrices.peek().getPositionMatrix(), consumers,
				TextRenderer.TextLayerType.SEE_THROUGH, 0x40000000, 0xF000F0);
	}

	private static void drawHealthBar(MatrixStack matrices, VertexConsumerProvider consumers,
			float healthFraction, int line) {
		// Drawn as two thin colored quads (background + fill) using the ESP
		// quad layer so it's visible through terrain like the boxes.
		float y = -line * 10f - 2f;
		float x0 = -BAR_WIDTH / 2f;
		float x1 = x0 + BAR_WIDTH * Math.max(0f, Math.min(1f, healthFraction));
		float xEnd = x0 + BAR_WIDTH;

		var buffer = consumers.getBuffer(com.vantage.client.util.RenderUtils.ESP_QUADS);
		var matrix = matrices.peek().getPositionMatrix();

		// Background (dark red / gray)
		quad(buffer, matrix, x0, y, xEnd, y + BAR_HEIGHT, 0.15f, 0.15f, 0.15f, 0.8f);
		// Fill (green -> red based on fraction)
		float r = 1f - healthFraction;
		float g = healthFraction;
		quad(buffer, matrix, x0, y, x1, y + BAR_HEIGHT, r, g, 0.1f, 0.9f);
	}

	private static void quad(net.minecraft.client.render.VertexConsumer buffer,
			net.minecraft.util.math.Matrix4f matrix,
			float x0, float y0, float x1, float y1, float r, float g, float b, float a) {
		buffer.vertex(matrix, x0, y0, 0).color(r, g, b, a);
		buffer.vertex(matrix, x0, y1, 0).color(r, g, b, a);
		buffer.vertex(matrix, x1, y0, 0).color(r, g, b, a);
		buffer.vertex(matrix, x1, y1, 0).color(r, g, b, a);
	}
}
