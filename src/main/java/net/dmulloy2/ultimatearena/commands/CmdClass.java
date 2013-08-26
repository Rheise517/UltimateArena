package net.dmulloy2.ultimatearena.commands;

import net.dmulloy2.ultimatearena.UltimateArena;
import net.dmulloy2.ultimatearena.arenas.objects.ArenaClass;
import net.dmulloy2.ultimatearena.arenas.objects.ArenaPlayer;
import net.dmulloy2.ultimatearena.permissions.Permission;
import net.dmulloy2.ultimatearena.util.FormatUtil;

public class CmdClass extends UltimateArenaCommand
{
	public CmdClass(UltimateArena plugin)
	{
		super(plugin);
		this.name = "class";
		this.aliases.add("cl");
		this.optionalArgs.add("class");
		this.description = "Switch UltimateArena classes";
		this.permission = Permission.CLASS;
		
		this.mustBePlayer =  true;
	}
	
	@Override
	public void perform()
	{
		if (! plugin.isInArena(player))
		{
			err("You are not in an arena!");
			return;
		}
		
		ArenaPlayer ap = plugin.getArenaPlayer(player);
		
		if (args.length == 0)
		{
			if (ap.getArenaClass() == null)
			{
				err("You do not have a class!");
				return;
			}
			
			sendpMessage("&3Your current class is: &e{0}", ap.getArenaClass().getName());
			return;
		}
		else if (args.length == 1)
		{
			for (ArenaClass cl : plugin.classes)
			{
				if (cl.getName().equalsIgnoreCase(args[0]))
				{
					ap.setClass(cl);
					
					String name = cl.getName();
					String article = FormatUtil.getArticle(name);
					
					if (ap.getArena().isInLobby())
					{
						sendpMessage("&3You will spawn as {0}: &e{1}", article, name);
					}
					else
					{
						sendpMessage("&3You will respawn as {0}: &e{1}", article, name);
					}
					
					return;
				}
			}
			
			err("Invalid class \"{0}\"!", args[0]);
			return;
		}
		else
		{
			err("Invalid input! Try /ua class <class>");
			return;
		}
	}
}