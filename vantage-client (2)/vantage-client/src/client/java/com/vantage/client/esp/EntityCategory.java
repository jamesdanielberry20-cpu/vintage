package com.vantage.client.esp;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.vehicle.VehicleEntity;

/**
 * Coarse buckets used to let the player enable/disable ESP per group rather
 * than per entity type. Order here also defines the order categories appear
 * in the ClickGUI.
 */
public enum EntityCategory {
	PLAYERS("Players", 0x55FFFF),
	HOSTILE_MOBS("Hostile Mobs", 0xFF5555),
	PASSIVE_MOBS("Passive Mobs", 0xFFFF55),
	ANIMALS("Animals", 0x55FF55),
	VILLAGERS("Villagers", 0xFFAA00),
	ITEMS("Items", 0xFFFFFF),
	PROJECTILES("Projectiles", 0xAA00AA),
	VEHICLES("Vehicles", 0x5555FF),
	OTHER("Other", 0xAAAAAA);

	public final String displayName;
	/** Default outline colour, 0xRRGGBB. */
	public final int color;

	EntityCategory(String displayName, int color) {
		this.displayName = displayName;
		this.color = color;
	}

	/**
	 * Classifies an entity into exactly one category. Order of checks matters
	 * (e.g. villagers are also PassiveEntity, so they're checked first).
	 */
	public static EntityCategory classify(Entity entity) {
		if (entity instanceof PlayerEntity) {
			return PLAYERS;
		}
		if (entity instanceof VillagerEntity || entity instanceof WanderingTraderEntity) {
			return VILLAGERS;
		}
		if (entity instanceof Monster || entity instanceof HostileEntity) {
			return HOSTILE_MOBS;
		}
		if (entity instanceof AnimalEntity) {
			return ANIMALS;
		}
		if (entity instanceof PassiveEntity) {
			return PASSIVE_MOBS;
		}
		if (entity instanceof ItemEntity) {
			return ITEMS;
		}
		if (entity instanceof ProjectileEntity) {
			return PROJECTILES;
		}
		if (entity instanceof VehicleEntity) {
			return VEHICLES;
		}
		return OTHER;
	}

	public static boolean isLiving(Entity entity) {
		return entity instanceof LivingEntity;
	}
}
