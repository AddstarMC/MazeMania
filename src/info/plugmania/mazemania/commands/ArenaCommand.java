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

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import info.plugmania.mazemania.MazeMania;
import info.plugmania.mazemania.Util;
import info.plugmania.mazemania.helpers.PlayerStore;

public class ArenaCommand {

	MazeMania plugin;

	public ArenaCommand(MazeMania instance) {
		plugin = instance;
	}

	public int scheduleId;
	public int scheduleMobId;
	public int scheduleGameTimer;
	public boolean scheduleActive;

	public boolean joinHandle(Player player) {
		if (plugin.arena.waiting.contains(player) || plugin.arena.playing.contains(player)) {
			return true;
		}
		
		if (!plugin.arena.arenaEnabled) {
			player.sendMessage(Util.formatMessage("Sorry, the game is currently disabled!"));
			return true;
		}
		
		if(plugin.mainConf.getInt("maximumPlayers", 0) != 0 && plugin.arena.waiting.size() >= plugin.mainConf.getInt("maximumPlayers", 0)){
			//player.sendMessage(Util.formatMessage("Only " + plugin.mainConf.getInt("maximumPlayers", 0) + " players allowed at a time."));
			player.sendMessage(Util.formatMessage("Sorry, the game is already full. Please try again later."));
			return true;
		}

		Util.broadcastInside(ChatColor.GOLD + player.getName() + ChatColor.BLUE + " has joined the game");
		if (plugin.arena.gameActive) {
			// Dont let players join while already playing
			if (plugin.arena.playing.contains(player)) return true;
			
			if (plugin.mainConf.getBoolean("joinWhenever")) {
				if(plugin.mainConf.getInt("maximumPlayers", 0) != 0 && plugin.arena.playing.size() >= plugin.mainConf.getInt("maximumPlayers", 0)){
					player.sendMessage(Util.formatMessage("Sorry, the game is already full. Please try again later."));
					return true;
				}
				joinMatch(player);
				player.sendMessage(Util.formatMessage("You have joined the game!"));
			} else {
				player.sendMessage(Util.formatMessage("The game is currently in play, please try again later."));
			}
			return true;
		}

		// Add player to waiting queue
		plugin.arena.store.remove(player);
		joinMatch(player);

		// Nothing more to do if the timer has already started
		if (scheduleActive) {
			player.sendMessage(Util.formatMessage("The game will begin shortly!"));
			return true;
		}
		
		// Check if we have enough players
		int minP = plugin.mainConf.getInt("minimumPlayers", 2);
		//if (minP < 2) minP = 2;
		//if (plugin.debug) minP = 1;
		final int minPlayers = minP;

		if (plugin.arena.waiting.size() >= minPlayers) {
			startMatch();
		} else {
			player.sendMessage(Util.formatMessage("You have been added to the waiting list, the game will begin when the required number of players join!"));
			//Util.broadcastInside(ChatColor.RED + "Not enough players to start game!");
			Util.broadcastInside(ChatColor.GOLD + "" + plugin.arena.waiting.size() + ChatColor.BLUE + " player(s) waiting, " + ChatColor.GOLD + minPlayers + ChatColor.BLUE + " required.");
			return true;
		}

		return true;
	}

	public boolean leaveHandle(Player player) {
		if (plugin.arena.playing.contains(player)) {
			plugin.arena.removePlayer(player);
			player.sendMessage(Util.formatMessage("You have left the game."));
			Util.broadcastInside(ChatColor.GOLD + player.getName() + ChatColor.BLUE + " has left the maze!");
			if (plugin.arena.playing.isEmpty()) {
				player.sendMessage(Util.formatMessage("The game was forfeited, all players left!"));
			}
			return true;
		} else if (plugin.arena.waiting.contains(player)) {
			player.sendMessage(Util.formatMessage("You are no longer on the waiting list."));
			plugin.arena.removePlayer(player);
			return true;
		} else {
			player.sendMessage(Util.formatMessage("You are not in the MazeMania game."));
			return true;
		}
	}

	public void leaveMatch(Player player) {
		if (plugin.arena.playing.contains(player) || plugin.arena.waiting.contains(player)) {
			Util.debug("Player " + player.getName() + " has left the match.");

			if (plugin.arena.playing.contains(player)) {
				plugin.arena.playing.remove(player);
			}
			
			if (plugin.arena.waiting.contains(player)) {
				plugin.arena.waiting.remove(player);
			}

			// Don't try to teleport dead players.. it causes a "no respawn" problem
			if (!player.isDead()) {
				player.getInventory().clear();

				Location back = null;
				if (plugin.arena.store.containsKey(player)) {
					PlayerStore ps = plugin.arena.store.get(player);
					plugin.arena.store.remove(player);
	
					player.getInventory().setContents(ps.inv.getContents());
					back = ps.previousLoc;
					player.setGameMode(ps.gm);
					player.setFoodLevel(ps.hunger);
					player.setHealth(ps.health);
					player.getInventory().setArmorContents(ps.armour);
				} else {
					player.setHealth(20);
				}
			
				for (PotionEffect effect : player.getActivePotionEffects()) {
					player.removePotionEffect(effect.getType());
				}
				if (player.getFireTicks() > 0) {
					player.setFireTicks(0);
				}

				if (back == null) {
					Util.log("Back was empty for " + player.getName() + "!");
					player.sendMessage(Util.formatMessage("Your previous location was not found."));
					player.teleport(player.getWorld().getSpawnLocation());
				} else {
					Util.log("Back: " + back.toString());
					player.teleport(back);
				}
			}
		} else {
			Util.log("Player NOT in match!!");
		}
	}

	public class GameTimer implements Runnable {
		int remaining;
		
		public GameTimer(int seconds) {
			remaining = seconds;
		}
		
		@Override
		public void run() {
			remaining--;
			if (remaining > 0) {
				// Timer still running
				if ((remaining < 5) || ((remaining % 5) == 0)) {
					Util.broadcastInside("Game will start in " + ChatColor.GOLD + remaining + ChatColor.BLUE + " second(s)!");
				}
				return;
			}

			// Cancel the timer
			Bukkit.getScheduler().cancelTask(scheduleId);
			scheduleActive = false;

			// Check if we have enough players
			int minP = plugin.mainConf.getInt("minimumPlayers", 2);
			//if (minP < 2) minP = 1;
			//if (plugin.debug) minP = 1;
			final int minPlayers = minP;

			// Check that we still have enough players (people can leave during countdown)
			if (plugin.arena.waiting.size() < minPlayers) {
				Util.broadcastInside(ChatColor.RED + "Not enough players to start game!");
				Util.broadcastInside(ChatColor.GOLD + "" + plugin.arena.waiting.size() + ChatColor.BLUE + " players waiting, " + ChatColor.GOLD + minPlayers + ChatColor.BLUE + " minimum.");
				return;
			}

			// Start the mob spawning timer
			int mOut = plugin.mainConf.getInt("mobDelay", 7);
			if (mOut < 1) mOut = 1;
			scheduleMobId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
				@Override
				public void run() {
					if (!plugin.arena.gameActive) {
						Util.log("Game no longer active. Stopping mob spawn timer.");
						Bukkit.getScheduler().cancelTask(scheduleMobId);
						return;
					}
					int arenaMobs = plugin.arena.countAllMobs(); 
					//Util.log("Arena mobs: " + arenaMobs);
					if (arenaMobs <= plugin.mainConf.getInt("maxMobs", 15)) {
						//Util.log("Spawning more mobs!");
						for (Player p : plugin.arena.playing) {
							Location l = plugin.arena.getRandomLocation(p.getLocation(), 10);
							int ran = (int) (Math.random() * 100);
							if (ran >= 95) {
								p.getWorld().spawnEntity(l, EntityType.SPIDER);
							}
							else if (ran >= 80) {
								LivingEntity ent = (LivingEntity) p.getWorld().spawnEntity(l, EntityType.SKELETON);
								ent.getEquipment().setItemInHand(new ItemStack(Material.BOW));
								ent.getEquipment().setBoots(new ItemStack(Material.GOLD_BOOTS));
							}
							else {
								LivingEntity ent = (LivingEntity) p.getWorld().spawnEntity(l, EntityType.ZOMBIE);
								ent.getEquipment().setBoots(new ItemStack(Material.GOLD_BOOTS));
							}
						}
					} else {
						//Util.log("Too many mobs!");
					}
				}
			}, mOut * 20L, mOut * 20L);

			// Start the mob spawning timer
			scheduleGameTimer = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
				int remaining = plugin.mainConf.getInt("maxGameTime", 900);
				
				@Override
				public void run() {
					if (!plugin.arena.gameActive) {
						Util.log("Game no longer active. Stopping game timer.");
						Bukkit.getScheduler().cancelTask(scheduleGameTimer);
						return;
					}

					remaining--;
					if (remaining > 0) {
						// Timer still running
						if ((remaining < 5) || (remaining == 10) || (remaining == 30) || ((remaining % 60) == 0)) {
							String timetext = "";
							if ((remaining % 60) == 0) {
								int mins = remaining / 60;
								timetext = ChatColor.GOLD + "" + mins + ChatColor.BLUE + " minutes remaining";
							} else {
								timetext = ChatColor.GOLD + "" + remaining + ChatColor.RED + " seconds remaining!";
							}
							Util.broadcastInside(timetext);
						}
					} else {
						Util.log("Sorry, you ran out of time and the game has been aborted.");
						Bukkit.getScheduler().cancelTask(scheduleGameTimer);
						abortGame();
					}
				}
			}, 20L, 20L);

			// Remove all zombie entities located in the maze arena (just to make it more fair)
			plugin.arena.removeAllMobs();
			
			// Clean up everything from previous game 
			plugin.arena.gameActive = true;
			
			// Bring in all queued players
			for (Player p : plugin.arena.waiting) {
				if ((p != null) && (p.isOnline())) {
					// Send player to spawn point
					plugin.arena.playing.add(p);
	
					if (plugin.mainConf.getBoolean("randomSpawn", true)) {
						Location loc = plugin.arena.getRandomSpawn();
						if (loc == null) {
							p.sendMessage(ChatColor.RED + "Sorry, an error has occurred! Please contact staff!");
							Util.log.warning("Null random spawn selected for " + p.getName() + "!");
							continue;
						}
						p.teleport(loc);
					} else {
						Location spawn = plugin.arena.getSpawn();
						if (spawn == null) return;
						p.teleport(spawn);
					}
					plugin.arena.RefreshLoadout(p);
				} else {
					if (p == null) {
						Util.log.warning("Null player in waiting list!!");
					} else {
						Util.log.warning("Player " + p.getName() + " is in waiting list was offline!");
					}
				}
			}

			plugin.arena.waiting.clear();
			Util.broadcastInside(ChatColor.YELLOW + "The game has begun!");
			Util.broadcastInside(ChatColor.GREEN + "Your objective is to find " +
									ChatColor.YELLOW + plugin.mainConf.getInt("itemAmountToCollect") + 
									ChatColor.GREEN + " x " + 
									ChatColor.YELLOW + plugin.mainConf.getString("itemToCollect"));
			Util.broadcastInside(ChatColor.GREEN + "Type " + ChatColor.AQUA + "/maze leave" + ChatColor.GREEN + " to leave the game.");
		}
	}
		
	private void startMatch() {
		Location spawn = plugin.arena.getSpawn();
		if (spawn == null) {
			Bukkit.broadcastMessage(Util.formatBroadcast("MazeMania spawn location not set!"));
			return;
		}

		int tOut = plugin.mainConf.getInt("waitingDelay", 20);
		if (tOut < 1) tOut = 1;

		Util.broadcastInside("Game will start in " + ChatColor.GOLD + tOut + ChatColor.BLUE +" second(s)!");

		// Start the game timer
		scheduleActive = true;
		GameTimer gametimer = new GameTimer(tOut);
		scheduleId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, gametimer, 20L, 20L);
	}

	private void joinMatch(Player p) {
		plugin.arena.store.put(p, new PlayerStore());

		Inventory inv = Bukkit.createInventory(null, p.getInventory().getSize());
		inv.setContents(p.getInventory().getContents());

		PlayerStore ps = plugin.arena.store.get(p);
		ps.inv = inv;
		ps.gm = p.getGameMode();
		ps.previousLoc = p.getLocation();
		ps.health = p.getHealth();
		ps.hunger = p.getFoodLevel();
		ps.armour = p.getInventory().getArmorContents();

		//Util.log("Set previousLoc: " + ps.previousLoc.toString());
		
		p.setGameMode(GameMode.SURVIVAL);
		p.getInventory().clear();
		p.getInventory().setArmorContents(null);
		p.setHealth(p.getMaxHealth());
		p.setFoodLevel(20);
		p.setFlying(false);
		p.setAllowFlight(false);
		p.setWalkSpeed(0.2F);
        p.setFlySpeed(0.2F);

		// Set the initial inventory loadout
		//plugin.arena.RefreshLoadout(p);

		if (plugin.mainConf.get("noDamageDelay") != null && plugin.mainConf.getInt("noDamageDelay", 0) != 0) {
			p.setNoDamageTicks(plugin.mainConf.getInt("noDamageDelay") * 20);
		}

		// If game in progress, send straight to spawn point, otherwise send to lobby
		if (plugin.arena.gameActive) {
			// Send player to spawn point
			plugin.arena.playing.add(p);

			if (plugin.mainConf.getBoolean("randomSpawn", true)) {
				Location l = plugin.arena.getRandomSpawn();
				if (l == null) {
					p.sendMessage(ChatColor.RED + "Sorry, an error has occurred! Please contact staff!");
					Util.log.warning("Null random spawn selected for " + p.getName() + "!");
					l = plugin.arena.getLobby();
				}
				p.teleport(l);
			} else {
				Location spawn = plugin.arena.getSpawn();
				if (spawn == null) {
					return;
				}
				p.teleport(spawn);
			}
			plugin.arena.RefreshLoadout(p);

			// Fix for client not showing players after they join
			for (Player otherplayer : plugin.arena.playing) {
			    if (otherplayer.canSee(p)) otherplayer.showPlayer(p);	// Make new player visible to others
			    if (p.canSee(otherplayer)) p.showPlayer(otherplayer);	// Make other players visible to new player
			}
		} else {
			// Send players to the waiting lobby
			plugin.arena.waiting.add(p);
			Location lobby = plugin.arena.getLobby();
			if (lobby == null) {
				return;
			}
			p.teleport(lobby);

			// Fix for client not showing players after they join
			for (Player otherplayer : plugin.arena.waiting) {
			    if (otherplayer.canSee(p)) otherplayer.showPlayer(p);	// Make new player visible to others
			    if (p.canSee(otherplayer)) p.showPlayer(otherplayer);	// Make other players visible to new player
			}
		}
	}
	
	public void disableArena(CommandSender sender) {
		sender.sendMessage(Util.formatBroadcast(ChatColor.RED + "Arena has been disabled!"));
		plugin.arena.arenaEnabled = false;
	}

	public void enableArena(CommandSender sender) {
		sender.sendMessage(Util.formatBroadcast(ChatColor.GREEN + "Arena has been enabled!"));
		plugin.arena.arenaEnabled = true;
	}
	
	public void abortGame() {
		Bukkit.getScheduler().cancelTask(scheduleMobId);
		Bukkit.getScheduler().cancelTask(scheduleGameTimer);

		// Prevent a CME by copying the list first
		List<Player> players = new ArrayList<Player>();
		for (Player p : plugin.arena.playing) {
			players.add(p);
		}

		for (Player p : players) {
			plugin.arena.removePlayer(p);
			p.sendMessage(Util.formatBroadcast(ChatColor.RED + "Game has ended. There was no winner!"));
			
			// Fix for client not showing players after they teleport
			for (Player otherplayer : players) {
			    if (otherplayer.canSee(p)) otherplayer.showPlayer(p);
			}
		}
	}
}
