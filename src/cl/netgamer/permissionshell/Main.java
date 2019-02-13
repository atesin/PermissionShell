package cl.netgamer.permissionshell;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;

// sorry for no comments, i lost all the sources in a silly accident
// had to decompile from a recent binary and reformat and reindent everything by hand :(

public final class Main extends JavaPlugin
{
	private Suggester suggester;
	
	public void onEnable()
	{
		suggester = new Suggester(this);
	}
	
	
	public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args)
	{
		if (!command.getName().equalsIgnoreCase("permissionshell"))
			return (List<String>)super.onTabComplete(sender, command, alias, args);
		
		return (List<String>) suggester.suggest(sender, args);
	}
	
	
	public boolean onCommand(final CommandSender sender, final Command command, final String alias, final String[] params)
	{
		if (!command.getName().equalsIgnoreCase("permissionshell"))
			return true;
		
		Args args = new Args(params);
		switch (args.shift())
		{
		case "remove":
			return remove(sender, args);
		case "":
			sender.sendMessage("\u00A7eNo subcommand specified, displaying general help...");
			return help(sender, "?");
		case "set":
			return set(sender, args);
		case "help":
			return help(sender, args.shift());
		case "list":
			return list(sender, args);
		}
		sender.sendMessage("\u00A7dUnrecognized subcommand, redirecting to general help...");
		return help(sender, "?");
	}
	
	
	private boolean remove(final CommandSender sender, final Args args)
	{
		final Permissible player = getPlayer(sender, args.shift());
		if (player == null)
			return true;
		
		final String filter = args.shift();
		if (!validatePermission(sender, filter))
			return true;
		
		final PermissionAttachment attachment = getOverrride(player, filter);
		if (attachment == null)
			return die(sender, "Overriden permission not found for this player");
		
		if (hasPermission(sender, String.valueOf((sender == player) ? "self." : "others.") + "modify"))
		{
			if (player.isOp())
				return die(sender, "Can't change operator's permissions");
			
			player.removeAttachment(attachment);
		}
		return true;
	}
	
	
	private boolean set(final CommandSender sender, final Args args)
	{
		final Permissible player = getPlayer(sender, args.shift());
		if (player == null)
			return true;
		
		final String permission = args.shift();
		if (!validatePermission(sender, permission))
			return true;
		
		final String valueArg = args.shift();
		if (valueArg.isEmpty())
			return die(sender, "Parameter count don't match with expected");
		
		final boolean value = valueArg.matches("(?i)(true|1)");
		if (!value && !valueArg.matches("(?i)(false|0)"))
			return die(sender, "Can't parse boolean representation");
		
		if (hasPermission(sender, String.valueOf((sender == player) ? "self." : "others.") + "modify"))
		{
			if (player.isOp())
				return die(sender, "Can't change operator's permissions");
			
			PermissionAttachment override = getOverrride(player, permission);
			if (override == null)
				override = player.addAttachment(this);
			override.setPermission(permission, value);
			player.recalculatePermissions();
		}
		return true;
	}
	
	
	private boolean list(final CommandSender sender, final Args args)
	{
		final Permissible player = getPlayer(sender, args.shift());
		if (player == null)
			return true;
		
		final String filter = args.shift();
		if (!filter.matches("[a-z0-1.*_-]*?"))
			return die(sender, "Invalid permission specification format");
		
		if (!hasPermission(sender, String.valueOf((sender == player) ? "self." : "others.") + "view"))
			return true;
		
		final Map<String, boolean[]> table = getPermissions(player, filter);
		String list = "\u00A7e" + table.size() + " permissions for '" + ((CommandSender)player).getName() + (player.isOp() ? "' (operator)" : "'") + ", matching with:\n'" + filter + "'";
		for (final Map.Entry<String, boolean[]> entry : table.entrySet())
		{
			final String format0 = entry.getValue()[1] ? "\u00A7o" : "";
			final String value = entry.getValue()[0] ? ("\u00A7b" + format0 + "true ") : ("\u00A7d" + format0 + "false");
			final String format2 = entry.getValue()[1] ? ("\u00A7f" + format0) : "\u00A77";
			list = String.valueOf(list) + "\n\u00A7r" + value + format2 + " = " + entry.getKey();
		}
		sender.sendMessage(list);
		return true;
	}
	
	
	private boolean validatePermission(final CommandSender sender, final String filter)
	{
		if (filter.isEmpty())
			return !die(sender, "Parameter count don't match with expected");
		
		return filter.matches("^[a-z0-1_-]+?(\\.[a-z0-1_-]+?)*?$") || !die(sender, "Invalid permission specification format");
	}
	
	
	private Map<String, boolean[]> getPermissions(final Permissible player, String filter)
	{
		filter = "(?i)^" + filter.replace(".", "\\.").replace("*", ".*?") + ".*";
		final Map<String, boolean[]> permissions = new TreeMap<String, boolean[]>();
		for (final PermissionAttachmentInfo info : player.getEffectivePermissions())
			if (info.getPermission().matches(filter))
			{
				final PermissionAttachment override = info.getAttachment();
				if (override != null)
					for (final Map.Entry<String, Boolean> permission : override.getPermissions().entrySet())
						permissions.put(permission.getKey(), new boolean[] { permission.getValue(), true });
				if (permissions.containsKey(info.getPermission()))
					continue;
				permissions.put(info.getPermission(), new boolean[] { info.getValue(), false });
			}
		return permissions;
	}
	
	
	Permissible getPlayerQuiet(final CommandSender sender, final String name)
	{
		return getPlayer(sender, name, false);
	}
	
	
	private Permissible getPlayer(final CommandSender sender, final String name)
	{
		return getPlayer(sender, name, true);
	}
	
	
	private Permissible getPlayer(final CommandSender sender, final String name, final boolean warn)
	{
		if (!name.isEmpty())
		{
			if (name.equalsIgnoreCase("@s"))
				return (Permissible)sender;
			
			final Player onlinePlayer = getServer().getPlayerExact(name);
			if (onlinePlayer != null)
				return (Permissible)onlinePlayer;
			
			if (warn)
				die(sender, "Specified player not found online");
		}
		else if (warn)
			die(sender, "Parameter count don't match with expected");
		return null;
	}
	
	
	private PermissionAttachment getOverrride(final Permissible player, final String permission)
	{
		if (!player.isPermissionSet(permission))
			return null;
		
		for (final PermissionAttachmentInfo info : player.getEffectivePermissions())
			if (info.getPermission().equals(permission))
				return info.getAttachment();
		
		return null;
	}
	
	
	private boolean die(final CommandSender sender, final String status)
	{
		sender.sendMessage("\u00A7d"+status);
		return true;
	}
	
	
	private boolean help(final CommandSender sender, final String page)
	{
		switch (page)
		{
		case "remove":
			return die(sender,
				"\u00A7e/perm remove: forget a permission override from player\n"+
				"\u00A7dwarning: \u00A7rmodifying runtime permissions could mess the server\n"+
				"\u00A7bsyntax: \u00A7r/perm remove <player> <permission>\n"+
				"\u00A7b- player: \u00A7rcurrent permission owner, shortcut '@s' to yourself\n"+
				"\u00A7b- permission: \u00A7rpermission to forget, format a[.b][.c]...\n"+
				"\u00A7bexample: \u00A7r/perm remove Steve void.fall");
		case "":
			sender.sendMessage("\u00A7eNo help page specified, displaying general help...");
			return help(sender, "?");
		case "?":
			return die(sender,
				"\u00A7e/permissionshell: manage player permissions in runtime\n"+
				"\u00A7bdefault alias: \u00A7r/perm\n\u00A7bsyntax: \u00A7r/perm <subcommand> [args...]\n"+
				"\u00A7bcurrent available subcommands: \u00A7rhelp, list, set, remove\n"+
				"\u00A7b- for specific subcommand info type: \u00A7r'/perm help [subcommand]'\n"+
				"\u00A7bpermissions: \u00A7rpermissionshell.<self | others>.<view | modify>\n"+
				"\u00A7b- default true: \u00A7rfor 'permissionshell.self.view' and for Ops");
		case "set":
			return die(sender,
				"\u00A7e/perm set: override a permission value for a player\n"+
				"\u00A7dwarning: \u00A7rmodifying runtime permissions could mess the server\n"+
				"\u00A7bsyntax: \u00A7r/perm set <player> <permission> <value>\n"+
				"\u00A7b- player: \u00A7rnew permission owner, shortcut '@s' to yourself\n"+
				"\u00A7b- permission: \u00A7rpermission to set, format a[.b][.c]...\n"+
				"\u00A7b- value: \u00A7rnew value for the permission, can be 'true' or 'false'\n"+
				"\u00A7bexample: \u00A7r/perm set Steve void.fall true");
		case "help":
			return die(sender,
				"\u00A7e/perm help: display PermissionShell plugin help pages\n"+
				"u00A7bsyntax: \u00A7r/perm help [subcommand]\n"+
				"\u00A7b- subcommand: \u00A7rdisplay subcommand help page\n"+
				"   with no or unrecognized subcommand display general help\n"+
				"   find available subcommands in general help\n"+
				"\u00A7bexamples: \u00A7r/perm help, /perm help something");
		case "list":
			return die(sender,
				"\u00A7e/perm list: display player permissions\n"+
				"\u00A7bsyntax: \u00A7r/perm list <player> [filter]\n"+
				"\u00A7b- player: \u00A7rpermissions owner, shortcut '@s' to yourself\n"+
				"\u00A7b- filter: \u00A7rpermissions starting with, or all if omitted\n"+
				"   format a[.b][.c]..., can use '*' as wildcards\n"+
				"\u00A7boutput: \u00A7r<value> = <permission>\n"+
				"\u00A7b- value: \u00A7rcan be either 'true' or 'false' for allowed or denied\n"+
				"\u00A7b- permission: \u00A7rpermission name, highlighted if overriden\n"+
				"\u00A7bexamples: \u00A7r/perm list @s, \u00A7r/perm list Steve v*.f");
		}
		sender.sendMessage("\u00A7dHelp page not found, redirecting to general help...");
		return help(sender, "?");
	}
	
	boolean hasPermission(final CommandSender sender, String permission)
	{
		if (sender.isOp())
			return true;
		
		permission = "permissionshell." + permission;
		final boolean isSet = sender.isPermissionSet(permission);
		final boolean isGrant = sender.hasPermission(permission);
		boolean effective;
		if (permission.equals("permissionshell.self.view"))
			effective = (!isSet || isGrant);
		else 
			effective = (isSet && isGrant);
		if (!effective)
			sender.sendMessage("\u00A7ePermissionShell: \u00A7dYou don't have '" + permission + "'");
		return effective;
	}
	
}
