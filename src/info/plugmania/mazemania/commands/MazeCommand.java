/*
    MazeMania; a minecraft bukkit plugin for managing a maze as an arena.
    Copyright (C) 2012 Plugmania (Sorroko,korikisulda) and contributors.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package info.plugmania.mazemania.commands;

import info.plugmania.mazemania.MazeMania;
import info.plugmania.mazemania.Util;
import info.plugmania.mazemania.helpers.Effects;
import info.plugmania.mazemania.helpers.Trigger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class MazeCommand implements CommandExecutor {

	public SetCommand setCommand;
	public ArenaCommand arenaCommand;

	MazeMania plugin;

	public MazeCommand(MazeMania instance) {
		plugin = instance;

		setCommand = new SetCommand(plugin);
		arenaCommand = new ArenaCommand(plugin);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		if (command.getName().equalsIgnoreCase("maze")) {
			Player player = null;
			if (sender instanceof Player)
				player = (Player) sender;

			if (args.length == 0) {
				sender.sendMessage(Util.formatMessage("---------------- MazeMania Help -----------------"));
				sender.sendMessage(Util.formatMessage("Player Commands:"));
				sender.sendMessage(Util.formatMessage("/maze join  - Join the MazeMania game"));
				sender.sendMessage(Util.formatMessage("/maze leave - Leave the MazeMania game"));
				sender.sendMessage(Util.formatMessage("/maze about - Show MazeMania credits and info"));
			}

			if (args.length > 0) {
				if (args[0].equalsIgnoreCase("set")) {
					if (!(sender instanceof Player)) {
						Util.sendMessageNotPlayer(sender);
						return true;
					}
					if (!plugin.util.hasPermMsg(player, "admin")) return true;
					return setCommand.handle(sender, args);

				} else if (args[0].equalsIgnoreCase("join")) {
					if (!(sender instanceof Player)) {
						Util.sendMessageNotPlayer(sender);
						return true;
					}
					if (!plugin.util.hasPermMsg(player, "use")) return true;
					return arenaCommand.joinHandle(player);

				} else if (args[0].equalsIgnoreCase("disable")) {
					if (!plugin.util.hasPermMsg(player, "admin")) return true;
					arenaCommand.disableArena(sender);
					return true;

				} else if (args[0].equalsIgnoreCase("enable")) {
					if (!plugin.util.hasPermMsg(player, "admin")) return true;
					arenaCommand.enableArena(sender);
					return true;

				} else if (args[0].equalsIgnoreCase("leave")) {
					if (!(sender instanceof Player)) {
						Util.sendMessageNotPlayer(sender);
						return true;
					}
					if (!plugin.util.hasPermMsg(player, "use")) return true;
					return arenaCommand.leaveHandle(player);

				} else if (args[0].equalsIgnoreCase("trigger")) {
					if (!plugin.util.hasPermMsg(player, "admin")) return true;
					return setCommand.triggerHandle(sender, args);
					
				} else if (args[0].equalsIgnoreCase("block")) {
					if (!plugin.util.hasPermMsg(player, "admin")) return true;
					if(args.length==1){
						sender.sendMessage(Util.formatMessage("--------------- MazeMania Blocks ----------------"));
						sender.sendMessage(Util.formatMessage("Event Types:"));
						sender.sendMessage(Util.formatMessage(plugin.util.join((new Effects()).listEffects().toArray(),", ",0)));
						sender.sendMessage(Util.formatMessage("Setup Commands:"));
						sender.sendMessage(Util.formatMessage("/maze {block} [blockname] [event] [args]"));
						sender.sendMessage(Util.formatMessage("/maze {block} - shows this helppage"));
						sender.sendMessage(Util.formatMessage("/maze {block} [blockname] - remove all associated triggers"));
						sender.sendMessage(Util.formatMessage("/maze {block} [blockname] [event] - sets an event with default args"));
						sender.sendMessage(Util.formatMessage("/maze {block} [blockname] [event] [args] - sets an event with defined args"));
						sender.sendMessage(ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + "-------------------------------------------------");
						sender.sendMessage(Util.formatMessage("Currently defined events: [Block] [Event] [Arg]"));
						for (Trigger t:plugin.TriggerManager.getTriggers()){
							sender.sendMessage(Util.formatMessage(t.material.name() + " " + t.effect + " " + t.arguments));
						}
					}else if(args.length==2){
						Material mat = Material.matchMaterial(args[1]);
						if (mat == null) {
							sender.sendMessage("Invalid block type!");
							return true;
						}
						plugin.TriggerManager.removeTrigger(mat);
						sender.sendMessage("Triggers for '" + args[1] + "' removed.");
					}else if(args.length==3){
						Material mat = Material.matchMaterial(args[1]);
						if (mat == null) {
							sender.sendMessage("Invalid block type!");
							return true;
						}
						plugin.TriggerManager.addTrigger(new Trigger(mat, args[2], ""));
						sender.sendMessage("Added trigger for '" + args[1] + "'.");
					}else if(args.length>=4){
						Material mat = Material.matchMaterial(args[1]);
						if (mat == null) {
							sender.sendMessage("Invalid block type!");
							return true;
						}
						plugin.TriggerManager.addTrigger(new Trigger(mat, args[2], plugin.util.join(args, " ", 3)));
						sender.sendMessage("Added trigger for '" + args[1] + "'.");
					}else{
						
					}
				} else if (args[0].equalsIgnoreCase("list")) {
					if (!plugin.util.hasPermMsg(player, "list")) return true;
					sender.sendMessage(ChatColor.GREEN + "Waiting: " + ChatColor.YELLOW + Util.getPlayerNames(plugin.arena.waiting));
					sender.sendMessage(ChatColor.GREEN + "Playing: " + ChatColor.YELLOW + Util.getPlayerNames(plugin.arena.playing));
					sender.sendMessage(ChatColor.GREEN + "Stored: " + ChatColor.YELLOW + Util.getStorePlayerNames(plugin.arena.store));
				} else if (args[0].equalsIgnoreCase("about") || args[0].equalsIgnoreCase("info")) {
					sender.sendMessage(Util.formatMessage("---------------------- " + Util.pdfFile.getName() + " ----------------------"));
					sender.sendMessage(Util.formatMessage(plugin.getName() + " developed by " + Util.pdfFile.getAuthors().get(0)));
					sender.sendMessage(Util.formatMessage("To view more information visit http://plugmania.github.com/ (<-- You can click it!)"));
				}
			}

			return true;
		}
		return false;
	}

}
