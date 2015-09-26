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
package net.dmulloy2.ultimatearena.integration;

import java.util.Map;

import net.dmulloy2.integration.DependencyProvider;
import net.dmulloy2.ultimatearena.UltimateArena;
import net.dmulloy2.ultimatearena.types.ArenaClass;
import net.dmulloy2.ultimatearena.types.ArenaPlayer;
import net.dmulloy2.util.Util;

import org.bukkit.entity.Player;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.Kit;
import com.earth2me.essentials.User;

/**
 * Handles integration with Essentials.
 * <p>
 * All Essentials integration should go through this handler. Everything is
 * wrapped in a catch-all, since Essentials integration is somewhat buggy and
 * isn't necessary for functioning
 *
 * @author dmulloy2
 */

public class EssentialsHandler extends DependencyProvider<Essentials>
{
	public EssentialsHandler(UltimateArena plugin)
	{
		super(plugin, "Essentials");
	}

	/**
	 * Disables Essentials god mode.
	 *
	 * @param player {@link Player} to disable god mode for
	 */
	public final void disableGodMode(Player player)
	{
		if (! isEnabled())
			return;

		try
		{
			User user = getEssentialsUser(player);
			if (user != null)
				user.setGodModeEnabled(false);
		}
		catch (Throwable ex)
		{
			handler.getLogHandler().debug(Util.getUsefulStack(ex, "disableGodMode(" + player.getName() + ")"));
		}
	}

	/**
	 * Attempts to get a player's Essentials user.
	 *
	 * @param player {@link Player} to get Essentials user for
	 */
	private final User getEssentialsUser(Player player)
	{
		if (! isEnabled())
			return null;

		try
		{
			return getDependency().getUser(player);
		}
		catch (Throwable ex)
		{
			handler.getLogHandler().debug(Util.getUsefulStack(ex, "getEssentialsUser(" + player.getName() + ")"));
			return null;
		}
	}

	/**
	 * Attempts to give a player their class's Essentials kit items.
	 *
	 * @param player {@link ArenaPlayer} to give kit items to
	 */
	public final void giveKitItems(ArenaPlayer player)
	{
		if (! isEnabled())
			return;

		try
		{
			User user = getEssentialsUser(player.getPlayer());
			if (user == null)
				return;

			ArenaClass ac = player.getArenaClass();
			Kit kit = new Kit(ac.getEssKitName(), getDependency());
			kit.expandItems(user);
		}
		catch (Throwable ex)
		{
			player.sendMessage(player.getPlugin().getMessage("essentialsKitError"), ex.toString());
			handler.getLogHandler().debug(Util.getUsefulStack(ex, "giveKitItems(" + player.getName() + ")"));
		}
	}

	/**
	 * Attempts to read an Essentials kit from configuration.
	 *
	 * @param name Name of the Essentials kit
	 */
	public final Map<String, Object> readEssentialsKit(String name)
	{
		if (! isEnabled())
			return null;

		try
		{
			return getDependency().getSettings().getKit(name);
		}
		catch (Throwable ex)
		{
			handler.getLogHandler().debug(Util.getUsefulStack(ex, "readEssentialsKit(" + name + ")"));
			return null;
		}
	}
}
