package cl.netgamer.permissionshell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;

public class Suggester
{
	private Main plugin;
	
	public Suggester(final Main plugin)
	{
		this.plugin = plugin;
	}
	
	
	List<String> suggest(final CommandSender sender, final String[] args)
	{
		if (args.length == 1)
			return selectSuggestions(listActions(sender), args[0]);
		
		if (args.length == 2)
		{
			if (args[0].equalsIgnoreCase("help"))
				return selectSuggestions(listActions(sender), args[1]);
			
			if (args[0].matches("(?i)(list|set|remove)"))
				return selectSuggestions(listPlayers(sender), args[1]);
			
			return new ArrayList<String>();
		}
		else if (args.length == 3)
		{
			if (args[0].matches("(?i)(list|set|remove)"))
				return selectPermissions(sender, args[1], args[2], args[0].equalsIgnoreCase("remove"));
			
			return new ArrayList<String>();
		}
		else
		{
			if (args.length != 4 || !args[0].equalsIgnoreCase("set"))
				return new ArrayList<String>();
			
			return selectSuggestions(Arrays.asList("true", "false"), args[3]);
		}
	}
	
	
	private List<String> selectSuggestions(final List<String> suggestions, final String keyword)
	{
		final List<String> selected = new ArrayList<String>();
		for (final String suggestion : suggestions)
			if (suggestion.matches("(?i)" + keyword + ".*"))
				selected.add(suggestion);
		return selected;
	}
	
	
	private List<String> listActions(final CommandSender sender)
	{
		final List<String> selected = new ArrayList<String>();
		selected.add("help");
		if (plugin.hasPermission(sender, "self.view") || plugin.hasPermission(sender, "others.view"))
			selected.add("list");
		if (plugin.hasPermission(sender, "self.modify") || plugin.hasPermission(sender, "others.modify"))
		{
			selected.add("set");
			selected.add("remove");
		}
		return selected;
	}
	
	
	private List<String> listPlayers(final CommandSender sender)
	{
		final List<String> players = new ArrayList<String>();
		if (plugin.hasPermission(sender, "self.view") || plugin.hasPermission(sender, "self.modify"))
			players.add("@s");
		if (plugin.hasPermission(sender, "others.view") || plugin.hasPermission(sender, "others.modify"))
			for (final Player player : plugin.getServer().getOnlinePlayers())
				players.add(player.getName());
		return players;
	}
	
	
	private List<String> selectPermissions(final CommandSender sender, final String playerName, String filter, final boolean excludeDefaults)
	{
		final Permissible player = plugin.getPlayerQuiet(sender, playerName);
		if (player == null)
			return new ArrayList<String>();
		
		filter = "(?i)^(" + filter.replace(".", "\\.").replace("*", ".*?") + "[^.]*)\\.?.*";
		final Set<String> selected = new HashSet<String>();
		for (final PermissionAttachmentInfo info : player.getEffectivePermissions())
			if (info.getPermission().matches(filter))
			{
				if (!excludeDefaults)
					selected.add(info.getPermission().replaceFirst(filter, "$1"));
				final PermissionAttachment override = info.getAttachment();
				if (override == null)
					continue;
				for (final String permission : override.getPermissions().keySet())
					selected.add(permission.replaceFirst(filter, "$1"));
			}
		return new ArrayList<String>(selected);
	}
	
}
