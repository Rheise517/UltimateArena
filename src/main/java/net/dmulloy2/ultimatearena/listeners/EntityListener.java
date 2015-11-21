/**
 * UltimateArena - fully customizable PvP arenas
 * Copyright (C) 2012 - 2015 MineSworn
 * Copyright (C) 2013 - 2015 dmulloy2
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.dmulloy2.ultimatearena.listeners;

import java.lang.reflect.Method;

import lombok.RequiredArgsConstructor;
import net.dmulloy2.ultimatearena.UltimateArena;
import net.dmulloy2.ultimatearena.arenas.Arena;
import net.dmulloy2.ultimatearena.arenas.spleef.SpleefArena;
import net.dmulloy2.ultimatearena.types.ArenaClass;
import net.dmulloy2.ultimatearena.types.ArenaPlayer;
import net.dmulloy2.util.FormatUtil;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.inventory.ItemStack;

/**
 * @author dmulloy2
 */

@RequiredArgsConstructor
public class EntityListener implements Listener
{
	private final UltimateArena plugin;

	// Stop block damage in arenas
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event)
	{
		if (plugin.isInArena(event.getLocation()))
		{
			event.blockList().clear();
		}
	}

	// Prevent food level change in the lobby
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onFoodLevelChange(FoodLevelChangeEvent event)
	{
		if (event.getEntity() instanceof Player)
		{
			Player player = (Player) event.getEntity();
			ArenaPlayer ap = plugin.getArenaPlayer(player);
			if (ap != null)
			{
				Arena a = ap.getArena();
				if (a.isInLobby())
				{
					player.setFoodLevel(20);
					event.setCancelled(true);
				}
			}
		}
	}

	// Stop combustion in the lobby
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityCombust(EntityCombustEvent event)
	{
		Entity entity = event.getEntity();
		if (entity instanceof Player)
		{
			Player player = (Player) entity;
			ArenaPlayer ap = plugin.getArenaPlayer(player);
			if (ap != null)
			{
				Arena arena = ap.getArena();
				if (arena.isInLobby())
				{
					player.setFireTicks(0);
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityDamageByEntityHighest(EntityDamageByEntityEvent event)
	{
		Player attacker = getPlayer(event.getDamager());
		if (attacker == null)
			return;

		// Prevent attacking your attack dogs
		if (event.getEntity() instanceof Wolf)
		{
			if (event.getEntity().hasMetadata("ua_attack_dog_" + attacker.getName()))
			{
				event.setCancelled(true);
				return;
			}
		}

		Player defender = getPlayer(event.getEntity());
		if (defender == null)
			return;

		ArenaPlayer ap = plugin.getArenaPlayer(attacker);
		if (ap != null)
		{
			ArenaPlayer dp = plugin.getArenaPlayer(defender);
			if (dp != null)
			{
				Arena arena = ap.getArena();
				if (arena.isInLobby())
				{
					// Prevent lobby PvP
					ap.sendMessage(plugin.getMessage("lobbyPvp"));
					event.setCancelled(true);
					return;
				}

				// Prevent team killing
				if (! arena.isAllowTeamKilling())
				{
					if (dp.getTeam() == ap.getTeam())
					{
						ap.sendMessage(plugin.getMessage("friendlyFire"));
						event.setCancelled(true);
						return;
					}
				}
			}
			else
			{
				ap.sendMessage(plugin.getMessage("hurtOutside"));
				event.setCancelled(true);
				return;
			}
		}
		else
		{
			if (plugin.isInArena(defender))
			{
				attacker.sendMessage(plugin.getPrefix() + FormatUtil.format(plugin.getMessage("hurtInside")));
				event.setCancelled(true);
				return;
			}
		}
	}

	// Repair armor and in-hand items
	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityDamageByEntityMonitor(EntityDamageByEntityEvent event)
	{
		Player player = getPlayer(event.getDamager());
		if (player == null)
			return;

		ArenaPlayer ap = plugin.getArenaPlayer(player);
		if (ap != null)
		{
			// Repair in-hand item
			ItemStack inHand = player.getItemInHand();
			if (inHand != null && inHand.getType() != Material.AIR)
			{
				if (inHand.getType().getMaxDurability() != 0)
				{
					inHand.setDurability((short) 0);
				}
			}

			// Repair armor
			for (ItemStack armor : player.getInventory().getArmorContents())
			{
				if (armor != null && armor.getType() != Material.AIR)
				{
					armor.setDurability((short) 0);
				}
			}

			// Healer class
			if (inHand != null && inHand.getType() == Material.GOLD_AXE)
			{
				Player damaged = getPlayer(event.getEntity());
				if (damaged != null)
				{
					ArenaPlayer dp = plugin.getArenaPlayer(damaged);
					if (dp != null)
					{
						if (ap.getTeam() == dp.getTeam())
						{
							ArenaClass ac = ap.getArenaClass();
							if (ac != null && ac.getName().equalsIgnoreCase("healer"))
							{
								Player heal = dp.getPlayer();
								double health = heal.getHealth();
								double maxHealth = heal.getMaxHealth();
								if (health > 0.0D && health < maxHealth)
								{
									heal.setHealth(Math.min(health + 2.0D, maxHealth));
									ap.sendMessage(plugin.getMessage("healer"), dp.getName());
								}
							}
						}
					}
				}
			}
		}
	}

	// Cancel damage in the lobby
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event)
	{
		if (event.getEntity() instanceof Player)
		{
			Player player = (Player) event.getEntity();
			ArenaPlayer ap = plugin.getArenaPlayer(player);
			if (ap != null)
			{
				Arena arena = ap.getArena();
				if (arena.isInLobby())
				{
					event.setCancelled(true);
				}
			}
		}
	}

	// Clear drops in arenas
	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityDeathMonitor(EntityDeathEvent event)
	{
		LivingEntity entity = event.getEntity();
		if (plugin.isInArena(entity.getLocation()))
		{
			event.getDrops().clear();
			event.setDroppedExp(0);
		}
	}

	// Handle deaths in arenas
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDeath(EntityDeathEvent event)
	{
		LivingEntity died = event.getEntity();
		if (died == null)
			return;

		if (died instanceof Player)
		{
			Player pdied = (Player) died;
			ArenaPlayer dp = plugin.getArenaPlayer(pdied);
			if (dp != null)
			{
				// Prevent duplicate deaths
				if (dp.isDead()) return;
				dp.onDeath();

				Arena ar = dp.getArena();

				if (pdied.getKiller() instanceof Player)
				{
					Player killer = pdied.getKiller();
					if (killer.getName().equals(pdied.getName())) // Suicide
					{
						ar.tellPlayers(plugin.getMessage("suicide"), pdied.getName());
						dp.displayStats();
					}
					else
					{
						// PvP
						ar.tellPlayers(plugin.getMessage("pvpKill"), killer.getName(), pdied.getName(), getWeapon(killer));
						dp.displayStats();

						// Handle killer
						ArenaPlayer kp = plugin.getArenaPlayer(killer);
						if (kp != null)
						{
							kp.setKills(kp.getKills() + 1);
							kp.setKillStreak(kp.getKillStreak() + 1);
							kp.getArena().handleKillStreak(kp);
							kp.addXP(100);

							kp.displayStats();
						}
					}
				}
				else
				{
					// From this point on, we will return when there is a valid match
					if (pdied.getKiller() instanceof LivingEntity)
					{
						LivingEntity lentity = pdied.getKiller();
						String name = FormatUtil.getFriendlyName(lentity.getType());

						ar.tellPlayers(plugin.getMessage("pvpKill"), pdied.getName(), FormatUtil.getArticle(name), name);
						dp.displayStats();
						return;
					}
					else if (pdied.getKiller() instanceof Projectile)
					{
						Projectile proj = (Projectile) pdied.getKiller();
						if (proj.getShooter() instanceof Player)
						{
							Player killer = (Player) proj.getShooter();
							ar.tellPlayers(plugin.getMessage("pvpKill"), killer.getName(), pdied.getName(), getWeapon(killer));
							dp.displayStats();

							// Handle killer
							ArenaPlayer kp = plugin.getArenaPlayer(killer);
							kp.setKills(kp.getKills() + 1);
							kp.setKillStreak(kp.getKillStreak() + 1);
							kp.getArena().handleKillStreak(kp);
							kp.addXP(100);

							kp.displayStats();
							return;
						}
						else if (proj.getShooter() instanceof LivingEntity)
						{
							LivingEntity lentity = pdied.getKiller();
							String name = FormatUtil.getFriendlyName(lentity.getType());

							ar.tellPlayers(plugin.getMessage("pveDeath"), pdied.getName(), FormatUtil.getArticle(name), name);
							dp.displayStats();
							return;
						}
					}

					// Attempt to grab from their last damage cause
					EntityDamageEvent damageEvent = pdied.getLastDamageCause();
					DamageCause cause = damageEvent != null ? damageEvent.getCause() : null;

					if (cause == DamageCause.ENTITY_ATTACK)
					{
						if (damageEvent instanceof EntityDamageByEntityEvent)
						{
							EntityDamageByEntityEvent damageByEntity = (EntityDamageByEntityEvent) damageEvent;
							Entity damager = damageByEntity.getDamager();
							if (damager != null)
							{
								if (damager instanceof Player)
								{
									Player killer = (Player) damager;
									ar.tellPlayers(plugin.getMessage("pvpKill"), killer.getName(), pdied.getName(), getWeapon(killer));
									dp.displayStats();

									// Handle killer
									ArenaPlayer kp = plugin.getArenaPlayer(killer);
									if (kp != null)
									{
										kp.setKills(kp.getKills() + 1);
										kp.setKillStreak(kp.getKillStreak() + 1);
										kp.getArena().handleKillStreak(kp);
										kp.addXP(100);

										kp.displayStats();
									}
								}
								else
								{
									String name = FormatUtil.getFriendlyName(damager.getType());
									ar.tellPlayers(plugin.getMessage("pveDeath"), pdied.getName(), FormatUtil.getArticle(name), name);
									dp.displayStats();
								}

								return;
							}
						}
					}
					else if (cause == DamageCause.PROJECTILE)
					{
						if (damageEvent instanceof EntityDamageByEntityEvent)
						{
							EntityDamageByEntityEvent damageByEntity = (EntityDamageByEntityEvent) damageEvent;
							Entity damager = damageByEntity.getDamager();
							if (damager != null)
							{
								if (damager instanceof Projectile)
								{
									Projectile proj = (Projectile) damager;
									if (proj.getShooter() != null)
									{
										if (proj.getShooter() instanceof Player)
										{
											Player killer = (Player) proj.getShooter();
											ar.tellPlayers(plugin.getMessage("pvpKill"), killer.getName(), pdied.getName(), getWeapon(killer));
											dp.displayStats();

											// Handle killer
											ArenaPlayer kp = plugin.getArenaPlayer(killer);
											if (kp != null)
											{
												kp.setKills(kp.getKills() + 1);
												kp.setKillStreak(kp.getKillStreak() + 1);
												kp.getArena().handleKillStreak(kp);
												kp.addXP(100);

												kp.displayStats();
											}
										}
										else
										{
											String name = proj.getShooter() instanceof LivingEntity ? FormatUtil.getFriendlyName(((LivingEntity) proj.getShooter()).getType()) : "";
											ar.tellPlayers(plugin.getMessage("pveDeath"), pdied.getName(), FormatUtil.getArticle(name), name);
											dp.displayStats();
										}

										return;
									}
								}
							}
						}
					}
					else if (cause != null)
					{
						// There's probably nothing else we can do here, so just turn it into a string
						ar.tellPlayers(plugin.getMessage("genericDeath"), pdied.getName(), FormatUtil.getFriendlyName(cause));
						dp.displayStats();
					}
					else if (ar instanceof SpleefArena)
					{
						// If they were in spleef, they probably fell through the floor
						ar.tellPlayers(plugin.getMessage("spleefDeath"), pdied.getName());
						dp.displayStats();
					}
					else
					{
						// No idea
						ar.tellPlayers(plugin.getMessage("unknownDeath"), pdied.getName());
						dp.displayStats();
					}
				}
			}
		}
		else
		{
			if (died instanceof LivingEntity)
			{
				LivingEntity lentity = died;
				if (lentity.getKiller() instanceof Player)
				{
					Player killer = lentity.getKiller();
					if (plugin.isInArena(killer))
					{
						ArenaPlayer ap = plugin.getArenaPlayer(killer);

						// Selectively count mob kills
						if (ap.getArena().isCountMobKills())
						{
							ap.addXP(25);
							ap.setKills(ap.getKills() + 1);
							ap.setKillStreak(ap.getKillStreak() + 1);
							ap.getArena().handleKillStreak(ap);

							String name = FormatUtil.getFriendlyName(lentity.getType());
							ap.sendMessage(plugin.getMessage("pveKill"), killer.getName(), FormatUtil.getArticle(name), name);
							ap.displayStats();
						}
					}
				}
			}
		}
	}

	// Line count for onEntityDeath = 238

	private String getWeapon(Player player)
	{
		ItemStack inHand = player.getItemInHand();
		if (inHand == null || inHand.getType() == Material.AIR)
		{
			return "their fists";
		}
		else
		{
			String name = FormatUtil.getFriendlyName(inHand.getType());
			String article = FormatUtil.getArticle(name);
			return "&3" + article + " &e" + name;
		}
	}

	private Player getPlayer(Entity entity)
	{
		if (entity instanceof Player)
		{
			return (Player) entity;
		}

		if (entity instanceof Projectile)
		{
			Projectile proj = (Projectile) entity;
			Object shooter = getShooter(proj);
			if (shooter instanceof Player)
				return (Player) shooter;
		}

		return null;
	}

	private Method getShooter;

	private Object getShooter(Projectile proj)
	{
		try
		{
			if (getShooter == null)
				getShooter = Projectile.class.getMethod("getShooter");
			if (getShooter.getReturnType() == LivingEntity.class)
				return getShooter.invoke(proj);
		} catch (Throwable ex) { }
		return proj.getShooter();
	}
}
