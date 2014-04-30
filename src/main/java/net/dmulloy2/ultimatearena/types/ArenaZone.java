package net.dmulloy2.ultimatearena.types;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import lombok.Getter;
import lombok.Setter;
import net.dmulloy2.ultimatearena.UltimateArena;
import net.dmulloy2.ultimatearena.io.FileSerialization;
import net.dmulloy2.ultimatearena.util.FormatUtil;
import net.dmulloy2.ultimatearena.util.Util;
import net.milkbowl.vault.economy.EconomyResponse;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.google.common.io.Files;

/**
 * @author dmulloy2
 */

@Getter @Setter
public class ArenaZone implements Reloadable, ConfigurationSerializable
{
	private static transient final int CURRENT_VERSION = 3;

	protected int maxPlayers = 24;

	protected int liked;
	protected int disliked;
	protected int timesPlayed;

	protected transient Material specialType; // Spleef
	protected String specialTypeString;

	protected boolean disabled;
	protected transient boolean loaded;

	protected String worldName;
	protected String defaultClass;

	protected transient FieldType type;
	protected String typeString;

	protected ArenaLocation lobby1;
	protected ArenaLocation lobby2;
	protected ArenaLocation arena1;
	protected ArenaLocation arena2;
	protected ArenaLocation team1spawn;
	protected ArenaLocation team2spawn;
	protected ArenaLocation lobbyREDspawn;
	protected ArenaLocation lobbyBLUspawn;

	protected transient Field lobby;
	protected transient Field arena;

	protected transient List<String> voted = new ArrayList<String>();

	protected List<ArenaLocation> spawns = new ArrayList<ArenaLocation>();
	protected List<ArenaLocation> flags = new ArrayList<ArenaLocation>();

	protected transient File file;
	protected transient World world;
	protected transient String name;
	protected transient ArenaConfig config;

	protected transient final UltimateArena plugin;

	public ArenaZone(UltimateArena plugin)
	{
		this.plugin = plugin;
	}

	public ArenaZone(UltimateArena plugin, File file)
	{
		this.plugin = plugin;
		this.file = file;
		this.name = FormatUtil.trimFileExtension(file, ".dat");
		this.initialize();
	}

	public final boolean initialize()
	{
		// Fields
		this.arena = new Field();
		this.lobby = new Field();

		// Load the arena
		loadFromDisk();

		if (loaded)
		{
			// Load configuration settings
			loadConfiguration();

			// Default class
			if (defaultClass == null || defaultClass.isEmpty())
			{
				if (! plugin.getClasses().isEmpty())
				{
					ArenaClass ac = plugin.getClasses().get(0);
					if (ac != null)
					{
						this.defaultClass = ac.getName();
					}
				}
			}

			// Set lobby parameters
			lobby.setParam(lobby1, lobby2);
			arena.setParam(arena1, arena2);

			// Add to the loaded arenas list
			plugin.getLoadedArenas().add(this);
		}

		return loaded;
	}

	// ---- Getters and Setters

	public final World getWorld()
	{
		if (world == null)
			world = plugin.getServer().getWorld(worldName);

		return world;
	}

	public final void setWorld(World world)
	{
		this.world = world;
		this.worldName = world.getName();
	}

	public final FieldType getType()
	{
		if (type == null)
			type = FieldType.getByName(typeString);

		return type;
	}

	public final void setType(FieldType type)
	{
		this.type = type;
		this.typeString = type.getName();
	}

	public final Material getSpecialType()
	{
		if (specialType == null)
			specialType = Material.matchMaterial(specialTypeString);

		return specialType;
	}

	public final void setSpecialType(Material specialType)
	{
		this.specialType = specialType;
		this.specialTypeString = specialType.toString();
	}

	public final List<String> getStats()
	{
		List<String> lines = new ArrayList<String>();

		StringBuilder line = new StringBuilder();
		line.append(FormatUtil.format("&3====[ &e{0} &3]====", WordUtils.capitalize(name)));
		lines.add(line.toString());

		// Calculate percentage
		int total = plugin.getTotalArenasPlayed();
		int plays = timesPlayed;

		double percentage = (double) plays / (double) total * 100;

		line = new StringBuilder();
		line.append(FormatUtil.format("&3Plays: &e{0}&3/&e{1} &3(&e{2}%&3)", plays, total, percentage));
		lines.add(line.toString());

		// Calculate popularity
		if (voted.size() == 0)
		{
			percentage = 0.0D;
		}
		else
		{
			percentage = (double) liked / (double) voted.size() * 100;
		}

		line = new StringBuilder();
		line.append(FormatUtil.format("&3Popularity: &e{0}&3/&e{1} &3(&e{2}%&3)", liked, voted.size(), percentage));
		lines.add(line.toString());

		return lines;
	}

	// ---- Utility Methods

	public final boolean checkLocation(Location loc)
	{
		try
		{
			return lobby.isInside(loc) || arena.isInside(loc);
		}
		catch (Exception e)
		{
			plugin.outConsole(Level.WARNING, "Could not perform location check for arena {0}!", name);
			plugin.outConsole(Level.WARNING, "This is often caused by a null world!");
			plugin.outConsole(Level.WARNING, "worldName = {0}, world = {1}", worldName, getWorld() == null ? "null" : getWorld().getName());
			return false;
		}
	}

	public final boolean canLike(Player player)
	{
		return ! voted.contains(player.getName());
	}

	public void giveRewards(ArenaPlayer ap)
	{
		if (! config.isGiveRewards())
			return;

		plugin.debug("Rewarding player {0}", ap.getName());

		for (ItemStack stack : config.getRewards())
		{
			if (stack == null)
				continue;

			int amt = stack.getAmount();

			// Gradient based, if applicable
			if (config.isRewardBasedOnXp())
				amt = (int) Math.round(ap.getGameXP() / 200.0D);

			if (amt > 0)
			{
				stack.setAmount(amt);

				// Give the item
				ap.giveItem(stack);
			}
		}

		if (plugin.getConfig().getBoolean("moneyrewards", true))
		{
			if (plugin.getEconomy() != null)
			{
				double money = config.getCashReward();
				if (config.isRewardBasedOnXp())
					money = money * (ap.getGameXP() / 250.0D);

				if (money > 0.0D)
				{
					EconomyResponse er = plugin.getEconomy().depositPlayer(ap.getName(), money);
					if (er.transactionSuccess())
					{
						String format = plugin.getEconomy().format(money);
						ap.sendMessage(plugin.getPrefix() + FormatUtil.format("&a{0} has been added to your account!", format));
					}
					else
					{
						ap.sendMessage(plugin.getPrefix() + FormatUtil.format("&cCould not give cash reward: {0}", er.errorMessage));
					}
				}
			}
		}
	}

	// ---- I/O

	public final void loadFromDisk()
	{
		checkFile();

		FileConfiguration fc = YamlConfiguration.loadConfiguration(file);
		Map<String, Object> values = fc.getValues(false);

		// Versioning
		int version = 0;
		if (values.containsKey("version"))
			version = (int) values.get("version");

		// Conversion
		if (checkConversion(version))
			return;

		// Load
		for (Entry<String, Object> entry : values.entrySet())
		{
			try
			{
				for (java.lang.reflect.Field field : getClass().getDeclaredFields())
				{
					if (field.getName().equalsIgnoreCase(entry.getKey()))
					{
						boolean accessible = field.isAccessible();
						field.setAccessible(true);
						field.set(this, entry.getValue());
						field.setAccessible(accessible);
					}
				}
			} catch (Exception e) { }
		}

		this.specialType = Material.matchMaterial(specialTypeString);
		this.type = FieldType.getByName(typeString);
		this.loaded = true;

		loadConfiguration();
	}

	public final void saveToDisk()
	{
		checkFile();

		try
		{
			FileSerialization.save(this, file);
		}
		catch (IOException e)
		{
			plugin.outConsole(Level.SEVERE, Util.getUsefulStack(e, "saving arena " + name));
		}

		saveConfiguration();
	}

	private final void checkFile()
	{
		if (file == null)
		{
			File folder = new File(plugin.getDataFolder(), "arenas");
			if (! folder.exists())
			{
				folder.mkdirs();
			}

			file = new File(folder, name + ".dat");
		}
	}

	// ---- Conversion

	public final boolean checkConversion(int version)
	{
		if (version != CURRENT_VERSION)
		{
			convert();
			return true;
		}

		return false;
	}

	@Deprecated
	public final void convert()
	{
		try
		{
			// Make backup
			File backup = new File(file.getAbsolutePath() + "_old");
			Files.copy(file, backup);

			// Load legacy arena zone
			loadConfiguration();
			plugin.getFileHandler().load(this);

			// Delete
			file.delete();

			// Save
			saveToDisk();
			saveConfiguration();

			// Load
			loadFromDisk();
		}
		catch (Exception e)
		{
			plugin.outConsole(Level.SEVERE, Util.getUsefulStack(e, "converting " + name));
		}
	}

	// ---- Configuration

	public final void loadConfiguration()
	{
		config = new ArenaConfig(this);
		config.load(file, getDefaultConfig());
	}

	public final void saveConfiguration()
	{
		if (config == null) // Config not initialized yet
			return;

		Map<String, Object> def = getDefaultConfig().serialize();
		Map<String, Object> data = Util.filterDuplicateEntries(config.serialize(), def);
		if (data == null || data.isEmpty())
			return;

		YamlConfiguration fc = YamlConfiguration.loadConfiguration(file);
		for (Entry<String, Object> entry : data.entrySet())
		{
			fc.set(entry.getKey(), entry.getValue());
		}

		try
		{
			fc.save(file);
		} catch (Exception e) { }
	}

	private ArenaConfig getDefaultConfig()
	{
		return plugin.getConfig(getType());
	}

	@Override
	public Map<String, Object> serialize()
	{
		Map<String, Object> data = new HashMap<String, Object>();

		for (java.lang.reflect.Field field : getClass().getDeclaredFields())
		{
			if (Modifier.isTransient(field.getModifiers()))
				continue;

			try
			{
				boolean accessible = field.isAccessible();

				field.setAccessible(true);

				if (field.getType().equals(Integer.TYPE))
				{
					if (field.getInt(this) != 0)
						data.put(field.getName(), field.getInt(this));
				}
				else if (field.getType().equals(Long.TYPE))
				{
					if (field.getLong(this) != 0)
						data.put(field.getName(), field.getLong(this));
				}
				else if (field.getType().equals(Boolean.TYPE))
				{
					if (field.getBoolean(this))
						data.put(field.getName(), field.getBoolean(this));
				}
				else if (field.getType().isAssignableFrom(Collection.class))
				{
					if (! ((Collection<?>) field.get(this)).isEmpty())
						data.put(field.getName(), field.get(this));
				}
				else if (field.getType().isAssignableFrom(String.class))
				{
					if ((String) field.get(this) != null)
						data.put(field.getName(), field.get(this));
				}
				else if (field.getType().isAssignableFrom(Map.class))
				{
					if (! ((Map<?, ?>) field.get(this)).isEmpty())
						data.put(field.getName(), field.get(this));
				}
				else
				{
					if (field.get(this) != null)
						data.put(field.getName(), field.get(this));
				}

				field.setAccessible(accessible);
			} catch (Exception e) { }
		}

		data.put("version", CURRENT_VERSION);
		return data;
	}

	@Override
	public void reload()
	{
		if (! file.exists())
		{
			if (plugin.getArena(name) != null)
			{
				plugin.getArena(name).stop();
			}

			plugin.getLoadedArenas().remove(this);
			return;
		}

		// Re-initialize
		initialize();
	}

	// ---- Generic Methods

	@Override
	public String toString()
	{
		return name;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o instanceof ArenaZone)
		{
			ArenaZone that = (ArenaZone) o;
			return this.name.equals(that.name);
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return 71 * name.hashCode();
	}
}