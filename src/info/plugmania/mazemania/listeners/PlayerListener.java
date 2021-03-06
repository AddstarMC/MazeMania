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

package info.plugmania.mazemania.listeners;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import info.plugmania.mazemania.MazeMania;
import info.plugmania.mazemania.Util;
import info.plugmania.mazemania.helpers.PlayerStore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;


public class PlayerListener implements Listener {

	MazeMania plugin;

	public PlayerListener(MazeMania instance) {
		plugin = instance;
	}

	@EventHandler(ignoreCancelled=true)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (event.getMessage().startsWith("/maze") || event.getMessage().startsWith("/op")) return;
		if (event.getPlayer().isOp()) return;
		if (!plugin.arena.playing.contains(event.getPlayer()) && !plugin.arena.waiting.contains(event.getPlayer())) return;
		event.setCancelled(true);
	}

	/*
	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event) {
		if (event.isCancelled()) return;
		if (plugin.arena.isInArena(event.getEntity().getLocation()))
			event.setCancelled(true);
	}
	*/

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (!plugin.arena.gameActive) return;

		Player player = event.getEntity();
		if (!plugin.arena.playing.contains(player)) return;

		String DeathMsg = event.getDeathMessage();
		if (DeathMsg == null) {
			DeathMsg = ChatColor.GOLD + player.getName() + ChatColor.BLUE + " has died";
		} else {
			DeathMsg = DeathMsg.replaceFirst(player.getName(), ChatColor.GOLD + player.getName() + ChatColor.BLUE);
		}
		Util.broadcastInside(DeathMsg);
		
		Util.debug("Death Message: " + event.getDeathMessage());
		Util.debug("Player Name: " + player.getName());
		Util.debug("Player Location: " + player.getLocation().toString());

		event.setDeathMessage(null);
		event.getDrops().clear();
		event.setDroppedExp(0);
		//plugin.arena.removePlayer(player);
		
		/*
		if (plugin.arena.playing.size() == 1) {
			Player winner = plugin.arena.playing.get(0);
			Bukkit.broadcastMessage(Util.formatBroadcast(winner.getName() + " is the last man standing and won the maze!"));
			plugin.mazeCommand.arenaCommand.leaveMatch(winner);
			//WIN
			plugin.arena.store.remove(winner);
			player.sendMessage(Util.formatMessage("Thank you for playing MazeMania."));
			plugin.arena.playing.clear();
			plugin.arena.gameActive = false;

			plugin.reward.rewardPlayer(winner);
		} else if (plugin.arena.playing.isEmpty()) {
			plugin.arena.gameActive = false;
			event.getEntity().sendMessage(Util.formatMessage("The MazeMania game was forfeited, all players left!"));
		}
		*/

		// TODO: Look into this.. might be good
		//if (plugin.arena.playing.isEmpty()) {
		//	event.getEntity().sendMessage(Util.formatMessage("The game was forfeited, all players left!"));
		//}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();

		// Player respawned in the maze?
		if (plugin.arena.isInArena(player.getLocation())) {
			Util.debug(player.getName() + " respawned in maze");
			if (plugin.arena.playing.contains(player)) {
				// Game is still running
				PlayerStore ps = plugin.arena.store.get(player);
				ps.chests.clear();
				Util.debug(player.getName() + " is still part of the current game.. resume!");
				Location loc = plugin.arena.getRandomSpawn();
				event.setRespawnLocation(loc);
				plugin.arena.RefreshLoadout(player);
			} else {
				// Game has ended
				Util.debug(player.getName() + " is not part of a game.. handle exit");
				Location back = plugin.arena.restorePlayer(player);
				if (back == null) {
					player.sendMessage(Util.formatMessage("Your previous location was not found."));
					event.setRespawnLocation(player.getWorld().getSpawnLocation());
				} else {
					event.setRespawnLocation(back);
				}
				plugin.arena.store.remove(player);
			}
		}
	}

	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event) {
		if ((!plugin.arena.gameActive) && (plugin.arena.waiting.size() == 0)) return;

		Player player = event.getPlayer();
		if (plugin.arena.playing.contains(player)) {
			plugin.arena.removePlayer(player);
			Util.broadcastInside(ChatColor.GOLD + "" + player.getName() + ChatColor.BLUE + " has left the maze!");

			if (plugin.arena.playing.isEmpty()) {
				event.getPlayer().sendMessage(Util.formatMessage("The game was forfeited, all players left!"));
			}
		}
		
		if (plugin.arena.waiting.contains(player)) {
			plugin.arena.removePlayer(player);
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (plugin.arena.store.containsKey(player)) {
			Util.log("PlayerJoinEvent: Player inside arena");

			Location back = plugin.arena.restorePlayer(player);
			if (back == null) {
				player.sendMessage(Util.formatMessage("Your previous location was not found."));
			} else {
				player.teleport(back);
			}
			plugin.arena.store.remove(player);
		}
		
		if (plugin.arena.isInArena(player.getLocation())) {
			player.teleport(player.getWorld().getSpawnLocation());
		}
	}

	// Custom chest inventory - Cancel player opening a chest and open a fake inventory instead
	@EventHandler(ignoreCancelled=true, priority=EventPriority.LOWEST)
	public void onChestInteract(PlayerInteractEvent event) {
		if (!plugin.arena.gameActive) return;

		Player player = event.getPlayer();
		if (!plugin.arena.playing.contains(player)) return;

		if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			Block b = event.getClickedBlock();
			if (b.getType().equals(Material.CHEST)) {
				Util.debug("[onChestInteract] Attempting to open fake chest...");
				event.setCancelled(true);
				InventoryView iv = player.getOpenInventory();
				if (iv != null) {
					Util.debug("[onChestInteract] Inventory already open, closing it!");
					iv.close();
				}

				Inventory inv;
				PlayerStore ps = plugin.arena.store.get(player);
				if (ps.chests.containsKey(b.getLocation())) {
					// Chest has been open by this player before
					//Util.debug("  [chest] Previously opened player chest found...");
					inv = ps.chests.get(b.getLocation());
					//Util.debug("  Chest inv contents: " + plugin.util.compressInventory(inv.getContents()));
					player.openInventory(inv);
				} else {
					// Chest has not been open by this player yet
					//Util.debug("  [chest] New chest found - creating contents...");
					Chest chest = (Chest) b.getState();
					inv = Bukkit.createInventory(null, chest.getInventory().getSize(), ChatColor.BLUE + "Maze Chest");
					inv.setContents(chest.getInventory().getContents());
					//Util.debug("  Chest inv contents: " + plugin.util.compressInventory(inv.getContents()));
					ps.chests.put(b.getLocation(), inv);
					player.openInventory(inv);
				}

				// We need to deep clone the inventory into the "open chest" inventory
				ItemStack[] openChest = new ItemStack[inv.getContents().length];
				for (int x = 0; x < inv.getContents().length; x++) {
					if (inv.getContents()[x] == null) continue;
					openChest[x] = inv.getContents()[x].clone();
				}
				ps.openChest = openChest;
			}
		}
	}

	// Win condition - Handle when player clicks the gold block
	@EventHandler(ignoreCancelled=true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (!plugin.arena.gameActive) return;

		Player player = event.getPlayer();
		if (!plugin.arena.playing.contains(player)) return;

		if (event.getAction().equals(Action.RIGHT_CLICK_AIR)
				|| event.getAction().equals(Action.LEFT_CLICK_AIR)) {
			return;
		}
		Block block = event.getClickedBlock();

		Material matBlock = Material.getMaterial(plugin.mainConf.getString("blockMaterial", "GOLD_BLOCK"));
		if (matBlock == null) matBlock = Material.GOLD_BLOCK;

		if (!block.getType().equals(matBlock)) return;

		//WIN
		Bukkit.broadcastMessage(Util.formatBroadcast(ChatColor.GOLD + player.getName() + ChatColor.GREEN + " has won the maze!"));
		for (Player p : plugin.arena.playing) {
			Util.debug("  - Playing: " + p.getName());
		}
		
		// Prevent a CME by copying the list first
		List<Player> players = new ArrayList<Player>();
		for (Player p : plugin.arena.playing) {
			players.add(p);
		}

		for (Player p : players) {
			plugin.arena.removePlayer(p);
			p.sendMessage(Util.formatMessage("Thank you for playing the Maze."));

			// Fix for client not showing players after they teleport
			for (Player otherplayer : players) {
			    if (otherplayer.canSee(p)) otherplayer.showPlayer(p);
			}
		}
		plugin.arena.removeAllMobs();
		plugin.arena.playing.clear();
		plugin.arena.gameActive = false;
		plugin.reward.rewardPlayer(player);
		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled=true)
	public void onPlayerMove(PlayerMoveEvent event) {
		if (!plugin.arena.gameActive) return;

		if (!plugin.mainConf.getString("mode", "collectItems").equalsIgnoreCase("collectItems")) return;

		if (event.getFrom().getBlockX() != event.getTo().getBlockX()
				|| event.getFrom().getBlockY() != event.getTo().getBlockY()
				|| event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {

			if (!plugin.arena.playing.contains(event.getPlayer())) return;


			if (event.getTo().getBlockX() == plugin.arena.getExit().getBlockX()
					&& event.getTo().getBlockY() == plugin.arena.getExit().getBlockY()
					&& event.getTo().getBlockZ() == plugin.arena.getExit().getBlockZ()) {
				Player player = event.getPlayer();
				Inventory inv = player.getInventory();

				Material item = Material.getMaterial(plugin.mainConf.getString("itemToCollect", "GOLD_NUGGET"));
				if (item == null) item = Material.GOLD_NUGGET;
				int amount = plugin.mainConf.getInt("itemAmountToCollect", 10);
				if (amount < 1) amount = 1;
				if (inv.contains(item, amount)) {
					//WIN
					// Prevent a CME by copying the list first
					List<Player> players = new ArrayList<Player>();
					for (Player p : plugin.arena.playing) {
						players.add(p);
					}

					Bukkit.broadcastMessage(Util.formatBroadcast(ChatColor.GOLD + player.getName() + ChatColor.GREEN + " has won the maze!"));
					for (Player p : players) {
						plugin.arena.removePlayer(p);
						p.sendMessage(Util.formatMessage("Thank you for playing the Maze."));

						// Fix for client not showing players after they teleport
						for (Player otherplayer : players) {
						    if (otherplayer.canSee(p)) otherplayer.showPlayer(p);
						}
					}
					plugin.arena.playing.clear();
					plugin.arena.gameActive = false;
					Bukkit.getScheduler().cancelTasks(plugin);

					plugin.reward.rewardPlayer(player);

				} else {
					player.sendMessage(Util.formatMessage("You found the exit but have not collected enough items!"));
				}
			}
		}
	}

	//
	@EventHandler(ignoreCancelled=true)
	public void InventoryClose(InventoryCloseEvent event) {
		if (!plugin.arena.gameActive) return;
		if(!plugin.arena.playing.contains(event.getPlayer())) return;
		if(!plugin.getConfig().getBoolean("notifyLoot",false)) return;
		
		// We only care about chests
		if (event.getInventory().getType() != InventoryType.CHEST) { return; }
		
		PlayerStore ps=plugin.arena.store.get(event.getPlayer());
		ItemStack[] before=ps.openChest;
		ItemStack[] after=event.getInventory().getContents();

		// Don't bother checking if the chest/inv was empty to begin with
		if ((before == null) || (before.length == 0)) {
			Util.log("[InventoryClose] before is NULL or Zero length");
			return;
		}

		// We also have to check for AIR item stacks now
		Boolean empty = true;
		for (ItemStack is : before) {
			if ((is != null) && (is.getType() != Material.AIR)) {
				empty = false;
				break;
			}
		}
		if (empty) {
			Util.debug("[InventoryClose] before is AIR/empty");
			return;
		}
		
		HashMap<String, Integer> cBefore =  plugin.util.compressInventory(before);
		HashMap<String, Integer> cAfter =  plugin.util.compressInventory(after);

		String looted = plugin.util.createDifferenceString(cBefore, cAfter);
		Util.debug("[InventoryClose] before: " + cBefore);
		Util.debug("[InventoryClose] after: " + cAfter);
		Util.debug("[InventoryClose] looted: " + looted);
		
		// Find the material type and collect mode
		Material mat = Material.matchMaterial(plugin.mainConf.getString("itemToCollect", "GOLD_NUGGET"));
		Boolean collectMode = plugin.mainConf.getString("mode", "collectItems").equalsIgnoreCase("collectItems");

		// Check if the item has been collected from the chest
		Boolean inBefore = cBefore.containsKey(mat.toString());
		Boolean inAfter  = cAfter.containsKey(mat.toString());
		Boolean hasCollected = (inBefore && !inAfter);
		//Util.debug("inBefore: " + inBefore);
		//Util.debug("inAfter: " + inAfter);
		//Util.debug("hasCollected: " + hasCollected);
		
		// Check if the required item was collected
		if(collectMode && hasCollected) {
			ItemStack[] pinv = event.getPlayer().getInventory().getContents();
			int found = plugin.util.compressInventory(pinv).get(mat.toString());
			int needed = plugin.mainConf.getInt("itemAmountToCollect", 10);
			int remain = (needed - found);
			//Util.log("[InventoryClose] Found " + found + " of " + needed + " with " + remain + " left");
			Util.broadcastInside(ChatColor.GOLD + event.getPlayer().getName() + ChatColor.BLUE + " has found " + ChatColor.GOLD + found + ChatColor.BLUE + " of " + ChatColor.GOLD + needed + " " + ChatColor.YELLOW + mat.toString());
			if (remain == 0) {
				Player p = (Player) event.getPlayer();
				p.sendMessage(Util.formatBroadcast(ChatColor.YELLOW + "Congratulations! You have found all required items!"));
				p.sendMessage(Util.formatBroadcast(ChatColor.YELLOW + "Now you must find the exit to win!"));
			}
		}
		else if(looted.length()>=5) {
			Util.broadcastInside(ChatColor.GOLD + event.getPlayer().getName() + ChatColor.BLUE + " found " + looted + "!");
		}
	}
	
	String strCompress(HashMap<String,Integer> items){
		String ret="";
		for(Entry<String,Integer> itemst:items.entrySet()){
			ret+=itemst.getKey() + " " +  itemst.getValue() + ";";
		}
		return ret;
	}

	@EventHandler(ignoreCancelled=true)
	public void onTriggers(PlayerMoveEvent event) {
		if (!plugin.arena.gameActive) return;

		if (event.getFrom().getBlockX() != event.getTo().getBlockX()
				|| event.getFrom().getBlockY() != event.getTo().getBlockY()
				|| event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {

			if (!plugin.arena.playing.contains(event.getPlayer())) return;
			
			plugin.triggers.handle(event.getTo().getBlock().getLocation(), event.getPlayer());
			Block b= event.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN);
			if(plugin.TriggerManager.isTrigger(b.getType())) {
				//Bukkit.broadcastMessage("Trigger: " + event.getPlayer().getName() + " -> " + b.getType());
				plugin.TriggerManager.applyTrigger(b.getType(),plugin,event.getPlayer());
			}
		}
	}
}
