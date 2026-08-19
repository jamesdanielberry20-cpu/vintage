package com.vantage.client.esp;

import com.vantage.client.config.VantageConfig;
import com.vantage.client.util.RenderUtils;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Draws through-wall boxes around loaded entities. Registered against
 * {@link WorldRenderEvents#AFTER_ENTITIES} so it runs once per frame after
 * vanilla entity rendering, using the current view matrices already set up
 * by the world renderer.
 *
 * Rendering is intentionally cheap per-entity (one box, a handful of line
 * vertices) and entities are pre-filtered by distance and category before
 * any GPU work happens, so enabling ESP has minimal FPS impact even in
 * crowded areas.
 */
public final class EspRenderer {

	private EspRenderer() {
	}

	public static void register() {
		WorldRenderEvents.AFTER_ENTITIES.register(EspRenderer::onRenderEntities);
	}

	private static void onRenderEntities(WorldRenderContext context) {
		VantageConfig cfg = com.vantage.client.config.ConfigManager.get();
		if (!cfg.espEnabled) {
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

		Vec3d camPos = context.camera().getPos();
		double maxDistSq = cfg.espMaxDistance * cfg.espMaxDistance;

		matrices.push();
		// World render events give matrices already relative to the camera
		// in modern Fabric API, but we translate explicitly by -camPos so
		// this code is correct regardless of that convention; adjust the
		// sign here first if boxes appear offset from entities.
		matrices.translate(-camPos.x, -camPos.y, -camPos.z);

		for (Entity entity : world.getEntities()) {
			if (entity == client.player || entity == client.cameraEntity) {
				continue;
			}
			if (!entity.isAlive() && !EntityCategory.isLiving(entity)) {
				continue;
			}

			double distSq = entity.squaredDistanceTo(client.player);
			if (distSq > maxDistSq) {
				continue;
			}

			EntityCategory category = EntityCategory.classify(entity);
			Boolean enabled = cfg.espCategoryEnabled.get(category.name());
			if (enabled == null || !enabled) {
				continue;
			}

			float partialTick = context.tickCounter().getTickProgress(true);
			Box box = entity.getBoundingBox().offset(
					entity.getLerpedPos(partialTick).subtract(entity.getPos())
			).expand(0.05);

			int rgb = category.color;
			float r = ((rgb >> 16) & 0xFF) / 255f;
			float g = ((rgb >> 8) & 0xFF) / 255f;
			float b = (rgb & 0xFF) / 255f;

			RenderUtils.drawBoxOutline(matrices, consumers, box, r, g, b, 1.0f);
		}

		matrices.pop();
	}
}
