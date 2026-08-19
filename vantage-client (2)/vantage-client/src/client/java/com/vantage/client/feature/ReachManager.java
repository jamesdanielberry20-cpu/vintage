package com.vantage.client.feature;

import com.vantage.client.config.ConfigManager;
import com.vantage.client.config.VantageConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Adjusts block/entity interaction range.
 *
 * <p><b>Why this only works in single-player, by construction:</b> reach is
 * enforced server-side via the {@code minecraft:block_interaction_range} and
 * {@code minecraft:entity_interaction_range} attributes on the
 * server-authoritative player entity. This manager only ever reaches for
 * {@link MinecraftClient#isIntegratedServerRunning()} — i.e. the "server" is
 * the integrated singleplayer server running in this same JVM — and edits
 * that {@link ServerPlayerEntity}'s attribute instance directly. On a real
 * multiplayer connection there is no local {@code ServerPlayerEntity} to
 * reach, {@code isIntegratedServerRunning()} is false, and this class is a
 * deliberate no-op. It never touches network packets and adds no client-side
 * spoofing of reach against a remote server.
 */
public final class ReachManager {

	private double vanillaBlockRange = 4.5;
	private double vanillaEntityRange = 3.0;
	private boolean capturedDefaults = false;

	public void register() {
		ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
	}

	private void onTick(MinecraftClient client) {
		VantageConfig cfg = ConfigManager.get();

		ServerPlayerEntity serverPlayer = getIntegratedServerPlayer(client);
		if (serverPlayer == null) {
			capturedDefaults = false;
			return;
		}

		EntityAttributeInstance blockRange = serverPlayer.getAttributeInstance(EntityAttributes.BLOCK_INTERACTION_RANGE);
		EntityAttributeInstance entityRange = serverPlayer.getAttributeInstance(EntityAttributes.ENTITY_INTERACTION_RANGE);
		if (blockRange == null || entityRange == null) {
			return;
		}

		if (!capturedDefaults) {
			vanillaBlockRange = blockRange.getBaseValue();
			vanillaEntityRange = entityRange.getBaseValue();
			capturedDefaults = true;
		}

		if (cfg.reachEnabled) {
			blockRange.setBaseValue(clamp(cfg.reachBlockRange, cfg.reachMax));
			entityRange.setBaseValue(clamp(cfg.reachEntityRange, cfg.reachMax));
		} else {
			blockRange.setBaseValue(vanillaBlockRange);
			entityRange.setBaseValue(vanillaEntityRange);
		}
	}

	/** Resets both values in the config back to vanilla defaults. */
	public void resetToVanilla() {
		VantageConfig cfg = ConfigManager.get();
		cfg.reachBlockRange = vanillaBlockRange;
		cfg.reachEntityRange = vanillaEntityRange;
		ConfigManager.save();
	}

	private static double clamp(double value, double max) {
		return Math.max(0.5, Math.min(value, max));
	}

	/**
	 * Returns the integrated server's copy of the local player, or null if
	 * we're not in a running singleplayer world (including LAN-hosted worlds
	 * — those still run an integrated server the host owns, so this is
	 * intentionally still allowed for the host only; remote LAN guests will
	 * not have {@code isIntegratedServerRunning() == true} and are excluded).
	 */
	private static ServerPlayerEntity getIntegratedServerPlayer(MinecraftClient client) {
		if (!client.isIntegratedServerRunning() || client.getServer() == null) {
			return null;
		}
		PlayerEntity clientPlayer = client.player;
		if (clientPlayer == null) {
			return null;
		}
		return client.getServer().getPlayerManager().getPlayer(clientPlayer.getUuid());
	}
}
