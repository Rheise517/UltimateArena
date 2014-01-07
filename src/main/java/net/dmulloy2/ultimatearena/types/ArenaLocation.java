/**
 * (c) 2013 dmulloy2
 */
package net.dmulloy2.ultimatearena.types;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

/**
 * This class provides a lazy-load Location, so that World doesn't need to be
 * initialized yet when an object of this class is created, only when the
 * {@link Location} is first accessed. This class is also serializable and
 * can easily be converted into a {@link Location} or {@link SimpleVector}
 * 
 * @author dmulloy2
 */

@Getter
public class ArenaLocation implements ConfigurationSerializable
{
	private transient World world;
	private transient Location location;
	private transient SimpleVector simpleVector;

	private String worldName;

	private int x;
	private int y;
	private int z;

	public ArenaLocation(String worldName, int x, int y, int z)
	{
		this.worldName = worldName;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public ArenaLocation(String worldName, int x, int z)
	{
		this(worldName, x, 0, z);
	}

	public ArenaLocation(World world, int x, int y, int z)
	{
		this(world.getName(), x, y, z);
	}

	public ArenaLocation(World world, int x, int z)
	{
		this(world.getName(), x, z);
	}

	public ArenaLocation(Location location)
	{
		this(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
	}

	public ArenaLocation(Player player)
	{
		this(player.getLocation());
	}

	public ArenaLocation(Map<String, Object> args)
	{
		this.worldName = (String) args.get("worldName");
		this.x = (int) args.get("x");
		this.y = (int) args.get("y");
		this.z = (int) args.get("z");
	}

	public World getWorld()
	{
		if (world == null)
			world = Bukkit.getWorld(worldName);

		return world;
	}

	/**
	 * Initializes the Location
	 */
	private void initLocation()
	{
		// if location is already initialized, simply return
		if (location != null)
			return;

		// get World; hopefully it's initialized at this point
		World world = getWorld();
		if (world == null)
			return;

		// store the Location for future calls, and pass it on
		location = new Location(world, x, y, z);
	}

	/**
	 * Initializes the SimpleVector
	 */
	private void initSimpleVector()
	{
		if (simpleVector != null)
			return;

		if (getLocation() == null)
			return;

		simpleVector = new SimpleVector(getLocation());
	}

	/**
	 * Returns the actual {@link Location}
	 */
	public Location getLocation()
	{
		initLocation();
		return location;
	}

	/**
	 * Returns a new {@link SimpleVector} based around this
	 */
	public SimpleVector getSimpleVector()
	{
		initSimpleVector();
		return simpleVector;
	}

	@Override
	public Map<String, Object> serialize()
	{
		Map<String, Object> result = new LinkedHashMap<String, Object>();

		result.put("worldName", worldName);
		result.put("x", x);
		result.put("y", y);
		result.put("z", z);

		return result;
	}
}