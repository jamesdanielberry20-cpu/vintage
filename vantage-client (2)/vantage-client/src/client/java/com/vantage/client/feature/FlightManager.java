package com.vantage.client.feature;

import com.vantage.client.config.ConfigManager;
import com.vantage.client.config.VantageConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Toggleable flight for single-player.
 *
 * <p>Implementation approach: rather than hand-rolling custom movement (which
 * would fight vanilla's own flight physics and anti-cheat-style server
 * checks even in singleplayer), this flips {@link PlayerAbilities#allowFlying}
 * / {@code flying} on both the client's local abilities and — because this
 * only ever runs against the integrated singleplayer server — the mirrored
 * abilities on the {@link ServerPlayerEntity}, then rides vanilla's existing
 * creative-style flight movement. Horizontal/vertical speed multipliers are
 * layered on top by scaling {@link PlayerAbilities#getFlySpeed()}.
 *
 * <p>Like {@link ReachManager}, this is a deliberate no-op unless
 * {@code MinecraftClient#isIntegratedServerRunning()} is true, so it has no
 * effect when connected to someone else's server.
 */
public final class FlightManager {

	private static final float VANILLA_FLY_SPEED = 0.05f;

	private boolean active = false;

	public void register() {
		ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
	}

	public boolean isActive() {
		return active;
	}

	public void toggle() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (!client.isIntegratedServerRunning()) {
			return; // Flight is intentionally singleplayer-only.
		}
		active = !active;
		ConfigManager.get().flightEnabled = active;
		ConfigManager.save();
		applyImmediate(client);
	}

	private void onTick(MinecraftClient client) {
		if (!client.isIntegratedServerRunning()) {
			// Left singleplayer (e.g. disconnected to title screen, or the
			// world unloaded) — make sure flight doesn't linger.
			if (active) {
				active = false;
			}
			return;
		}
		if (active) {
			applyImmediate(client);
		}
	}

	private void applyImmediate(MinecraftClient client) {
		ClientPlayerEntity clientPlayer = client.player;
		if (clientPlayer == null || client.getServer() == null) {
			return;
		}
		ServerPlayerEntity serverPlayer = client.getServer().getPlayerManager().getPlayer(clientPlayer.getUuid());
		if (serverPlayer == null) {
			return;
		}

		VantageConfig cfg = ConfigManager.get();
		boolean creativeOrSpectator = clientPlayer.getAbilities().creativeMode
				|| clientPlayer.isSpectator();

		for (PlayerAbilities abilities : new PlayerAbilities[] {
				clientPlayer.getAbilities(), serverPlayer.getAbilities() }) {
			if (!creativeOrSpectator) {
				// Survival/adventure players don't normally have
				// allowFlying; grant it only while our toggle is active.
				abilities.allowFlying = active;
			}
			abilities.flying = active && abilities.allowFlying;
			if (active) {
				float horizontal = VANILLA_FLY_SPEED * (float) cfg.flightHorizontalSpeed;
				abilities.setFlySpeed(horizontal);
			} else if (!creativeOrSpectator) {
				abilities.setFlySpeed(VANILLA_FLY_SPEED);
				abilities.allowFlying = false;
				abilities.flying = false;
			}
		}
		clientPlayer.sendAbilitiesUpdate();
	}

	/**
	 * Called from a per-tick hook when active, to apply the separate
	 * vertical speed multiplier vanilla abilities don't expose directly.
	 * Vanilla ties ascend/descend speed to the same flySpeed value, so a
	 * distinct vertical multiplier is applied here as extra Y-velocity on
	 * top of vanilla's own flight movement when the player is actively
	 * holding jump/sneak to ascend/descend.
	 */
	public void applyVerticalBoost(ClientPlayerEntity player) {
		if (!active) {
			return;
		}
		VantageConfig cfg = ConfigManager.get();
		double verticalMultiplier = cfg.flightVerticalSpeed;
		if (verticalMultiplier == 1.0) {
			return; // Vanilla speed already applied via flySpeed.
		}
		if (player.getAbilities().flying) {
			double vy = player.getVelocity().y;
			// Only scale the portion of vertical motion coming from active
			// ascend/descend input, not gravity/knockback.
			if (player.input.jumping || player.input.sneaking) {
				player.setVelocity(player.getVelocity().x, vy * verticalMultiplier, player.getVelocity().z);
			}
		}
	}
}
