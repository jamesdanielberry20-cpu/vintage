package com.vantage.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;

/**
 * Small collection of immediate-mode-style helpers for drawing wireframe /
 * translucent boxes that render through terrain (ESP). Everything here is
 * pure rendering utility with no gameplay side effects.
 *
 * <p><b>Compatibility note:</b> Mojang's ongoing Blaze3D/RenderPipeline
 * rewrite (accelerated by the 26.2 Vulkan-backend work) is the single most
 * likely place for this project to need a small update after generation —
 * {@link RenderLayer.MultiPhaseParameters} / {@link RenderPhase} construction
 * has been in flux across recent versions, with some layers migrating to
 * {@code RenderPipeline.builder()}. If this file fails to compile against
 * your exact Yarn build, check "Rendering" under docs.fabricmc.net for the
 * current recommended way to build a depth-test-disabled custom RenderLayer
 * — the drawing logic below (vertex positions/colors) will not need to
 * change, only the layer construction boilerplate.
 */
public final class RenderUtils {

	private RenderUtils() {
	}

	/**
	 * A custom render layer that disables depth testing so lines/quads drawn
	 * with it show through blocks and other entities. This is what makes ESP
	 * "see-through" rather than a normal outline that terrain would occlude.
	 */
	public static final RenderLayer ESP_LINES = RenderLayer.of(
			"vantage_esp_lines",
			1536,
			RenderLayer.DrawMode.LINES,
			RenderLayer.VertexFormats.POSITION_COLOR,
			RenderLayer.MultiPhaseParameters.builder()
					.program(RenderLayer.LINES_PROGRAM)
					.lineWidth(new RenderPhase.LineWidth(java.util.OptionalDouble.of(2.0)))
					.layering(RenderLayer.VIEW_OFFSET_Z_LAYERING)
					.transparency(RenderLayer.TRANSLUCENT_TRANSPARENCY)
					.depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
					.writeMaskState(RenderPhase.COLOR_MASK)
					.cull(RenderLayer.DISABLE_CULLING)
					.build(false)
	);

	public static final RenderLayer ESP_QUADS = RenderLayer.of(
			"vantage_esp_quads",
			1536,
			RenderLayer.DrawMode.TRIANGLE_STRIP,
			RenderLayer.VertexFormats.POSITION_COLOR,
			RenderLayer.MultiPhaseParameters.builder()
					.program(RenderLayer.POSITION_COLOR_PROGRAM)
					.transparency(RenderLayer.TRANSLUCENT_TRANSPARENCY)
					.depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
					.writeMaskState(RenderPhase.COLOR_MASK)
					.cull(RenderLayer.DISABLE_CULLING)
					.build(false)
	);

	/** Draws a wireframe box using {@link #ESP_LINES}. */
	public static void drawBoxOutline(MatrixStack matrices, VertexConsumerProvider consumers, Box box,
			float r, float g, float b, float a) {
		VertexConsumer buffer = consumers.getBuffer(ESP_LINES);
		Matrix4f matrix = matrices.peek().getPositionMatrix();

		float minX = (float) box.minX, minY = (float) box.minY, minZ = (float) box.minZ;
		float maxX = (float) box.maxX, maxY = (float) box.maxY, maxZ = (float) box.maxZ;

		// Bottom face
		line(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
		line(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
		line(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
		line(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);
		// Top face
		line(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
		line(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
		line(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
		line(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
		// Verticals
		line(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
		line(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
		line(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
		line(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
	}

	private static void line(VertexConsumer buffer, Matrix4f matrix,
			float x1, float y1, float z1, float x2, float y2, float z2,
			float r, float g, float b, float a) {
		buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a);
		buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
	}

	public static int argb(int rgb, float alpha) {
		int a = MathHelper.clamp((int) (alpha * 255), 0, 255);
		return (a << 24) | (rgb & 0xFFFFFF);
	}
}
