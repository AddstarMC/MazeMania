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

package info.plugmania.mazemania;

import info.plugmania.mazemania.helpers.PlayerStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;

public class Util {
	static MazeMania plugin = MazeMania.instance;

	public static Logger log = Logger.getLogger("Minecraft");
	public static PluginDescriptionFile pdfFile;

	public Util(MazeMania instance) {
		plugin = instance;
	}

	public static String formatBroadcast(String msg) {
		String s = ChatColor.BLUE.toString() + "[" + ChatColor.AQUA.toString() + "Maze" + ChatColor.BLUE.toString() + "] " + msg;
		return s;
	}	

	public static String formatMessage(String msg) {
		String s = ChatColor.GOLD.toString() + ChatColor.ITALIC.toString() + msg;
		return s;
	}
	
	public static String formatHelp(String msg) {
		String s = ChatColor.GOLD.toString() + msg;
		return s;
	}	

	public static String formatHelpCmd(String msg) {
		String s = ChatColor.GOLD.toString() + msg + ChatColor.DARK_PURPLE.toString();
		return s;
	}
	
	public static void log(String msg) {
		log.info("[" + pdfFile.getName() + "] " + msg);
	}

	public static void debug(String msg) {
		if (plugin.debug) {
			log.info("[" + pdfFile.getName() + "] [DEBUG]: " + msg);
		}
	}

	public static void sendMessageNotPlayer(CommandSender sender) {
		sender.sendMessage(ChatColor.RED + "You must be a player to do this!");
	}

	public static void sendMessageNoPerms(CommandSender sender) {
		sender.sendMessage(ChatColor.RED + "You do not have permission to do that!");
	}

	public static void sendMessagePlayerNotOnline(CommandSender sender) {
		sender.sendMessage(ChatColor.RED + "That player is not online!");
	}
	
	public static void broadcastInside(String msg) {
		Util.log("broadcastInside(" + plugin.arena.playing.size() + "/" + plugin.arena.waiting.size() + "): " + msg);
		for(Player p:plugin.arena.playing) {
			p.sendMessage(ChatColor.BLUE + "[" + ChatColor.AQUA + "Maze" + ChatColor.BLUE + "] " + msg);
		}
		for(Player p:plugin.arena.waiting) {
			p.sendMessage(ChatColor.BLUE + "[" + ChatColor.AQUA + "Maze" + ChatColor.BLUE + "] " + msg);
		}
	}
	
	public static void chatInside(String msg) {
		for(Player p:plugin.arena.playing) {
			p.sendMessage(ChatColor.BLUE + "[MM] " + msg);
		}
	}
	
	public static String getPlayerNames(List<Player> players) {
		List<String> names = new ArrayList<String>();
		
		for (Player p: players) {
			names.add(p.getName());
		}
		return StringUtils.join(names, ", ");
	}
	
	public static String getStorePlayerNames(HashMap<Player, PlayerStore> ps) {
		List<String> names = new ArrayList<String>();
		
		for (Player p: ps.keySet()) {
			names.add(p.getName());
		}
		return StringUtils.join(names, ", ");
	}
	
	public static String Loc2Str(Location loc) {
		if (loc == null) { return null; }
		String result = loc.getWorld().getName() + "," +
						loc.getX() + "," + 
						loc.getY() + "," +
						loc.getZ() + "," +
						loc.getYaw() + "," +
						loc.getPitch();
		
		return result; 
	}
	
	public static Location Str2Loc(String input) {
		if (input == null) { return null; }

		Location loc;
		String[] parts = input.split(",");

		World world = plugin.getServer().getWorld(parts[0]);
		if (world == null) {
			Util.debug("WARNING: Str2Loc() world was invalid: " + input);
			return null;
		}
		
		loc = new Location(world,
				Double.parseDouble(parts[1]),
				Double.parseDouble(parts[2]),
				Double.parseDouble(parts[3]),
				Float.parseFloat(parts[4]),
				Float.parseFloat(parts[5]));
		return loc;
	}
	
	public boolean hasPermMsg(CommandSender sender, String perm) {
		if (sender instanceof Player) {
			if ((sender.hasPermission(plugin.basePerm + "." + perm)) || (sender.hasPermission(plugin.basePerm + ".*"))) {
				//Util.debug("Has permission for player: " + sender.getName() + " and perm: " + plugin.basePerm + "." + perm);
				return true;
			}
			sender.sendMessage(ChatColor.RED + "You do not have permission to do that!");
			return false;
		} else {
			// Console always has permissions
			return true;
		}
	}
	
	public String join(String[] a, String delimiter, Integer startIndex) {
		try {
			Collection<String> s = Arrays.asList(a);
			StringBuffer buffer = new StringBuffer();
			Iterator<String> iter = s.iterator();

			while (iter.hasNext()) {
				if (startIndex == 0) {
					buffer.append(iter.next());
					if (iter.hasNext()) {
						buffer.append(delimiter);
					}
				} else {
					startIndex--;
					iter.next();
				}
			}

			return buffer.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public HashMap<String,String> getFlags(String string) {
		HashMap<String,String> ret=new HashMap<String,String>();
		boolean inQuotes=false;
		boolean inFlag=true;
		String currentflag="";
		String flagvalue="";
		for(char c:string.toCharArray()) {
			if (c=='\''){
				inQuotes=!inQuotes;
			} else if (c==' '&&!inQuotes) {
				ret.put(currentflag, flagvalue);
                currentflag = "";
                flagvalue = "";
				inFlag=true;
			} else if (c==':'&&!inQuotes) {
				inFlag=false;
			} else if (inFlag) {
				currentflag+=c;
			} else if (inQuotes) {
				flagvalue+=c;
			} else if (!inFlag) {
				flagvalue+=c;
			}
			
		}
		ret.put(currentflag, flagvalue);
		return ret;
	}
	
	
	public boolean compare(HashMap<String,String> event,HashMap<String,String> entry){
		for(String s:entry.keySet()){
			if(!event.containsKey(s)) return false;
			if((event.get(s)!=entry.get(s))&&(entry.get(s)!="*")) return false;
		}
		return true;
	}

	public String join(Object[] array, String delimiter, int startIndex) {
		try {
			Collection<Object> s = Arrays.asList(array);
			StringBuffer buffer = new StringBuffer();
			Iterator<Object> iter = s.iterator();

			while (iter.hasNext()) {
				if (startIndex == 0) {
					buffer.append(String.valueOf(iter.next()));
					if (iter.hasNext()) {
						buffer.append(String.valueOf(delimiter));
					}
				} else {
					startIndex--;
					iter.next();
				}
			}

			return buffer.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	

	/*
	 * Utils originating in hawkeye;
	 */

	/**
	 * Compress an ItemStack[] into a HashMap of the item string and the total amount of that item
	 * Uses {@BlockUtil} to get the item string
	 * @param inventory ItemStack[] to compress
	 * @return HashMap<String,Integer>
	 */
	public HashMap<String,Integer> compressInventory(ItemStack[] inventory) {
		HashMap<String,Integer> items = new HashMap<String,Integer>();
		if (inventory == null) { return null; }
		for (ItemStack item : inventory) {
			if (item == null) continue;
			String iString = item.getType().toString();
			if (items.containsKey(iString)) items.put(iString, items.get(iString) + item.getAmount());
			else items.put(iString, item.getAmount());
		}
		return items;
	}


	/**
	 * Takes two compressed inventories and returns a string representation of the difference
	 * @param before HashMap<String,Integer> of inventory before changes
	 * @param after HashMap<String,Integer> of inventory after changes
	 * @return String in the form item:data,amount&item:data,amount@item:data,amount&item:data,amount where the first part is additions and second is subtractions
	 */
	public String createDifferenceString(HashMap<String,Integer> before, HashMap<String,Integer> after) {
		List<String> add = new ArrayList<String>();
		List<String> sub = new ArrayList<String>();

		if ((before == null) || (after == null)) return "";
		for (Entry<String, Integer> item : before.entrySet()) {
			//If the item does not appear after changes
		    if (!after.containsKey(item.getKey())) {
		    	String loot = item.getKey().replace("_", " ");
		    	sub.add(ChatColor.GREEN + "1 " + loot + " " + ChatColor.BLUE);
		    }
		    //If the item is smaller after changes
		    //else if (item.getValue() > after.get(item.getKey())) sub.add(item.getKey() + "," + (item.getValue() - after.get(item.getKey())));
		    //If the item is larger after changes
		    //else if (item.getValue() < after.get(item.getKey())) add.add(item.getKey() + "," + (after.get(item.getKey()) - item.getValue()));
		}
		//for (Entry<String, Integer> item : after.entrySet()) {
			//If the item does not appear before changes
			//if (!before.containsKey(item.getKey())) add.add(item.getKey() + "," + item.getValue());
		//}
		//return join(add.toArray(), "&",0) + "" + join(sub.toArray(), "&",0);
		
		return join(sub.toArray(), " & ",0);
	}
	
}
