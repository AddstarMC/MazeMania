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

package info.plugmania.mazemania.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import info.plugmania.mazemania.ConfigUtil;
import info.plugmania.mazemania.MazeMania;
import info.plugmania.mazemania.Util;

public class Arena {
	public List<Player> playing = new ArrayList<Player>();
	public List<Player> waiting = new ArrayList<Player>();

	public boolean gameActive = false;
	public boolean arenaEnabled = true;

	public HashMap<Player, PlayerStore> store = new HashMap<Player, PlayerStore>();

	private Location higherPos;
	private Location lowerPos;
	private List<Location> spawns = new ArrayList<Location>();

	public YamlConfiguration dbConf;

	MazeMania plugin;

	public Arena(MazeMania instance) {
		plugin = instance;

		ConfigUtil.loadConfig("db");
		dbConf = ConfigUtil.getConfig("db");

		updatePosLocs();
		loadSpawns();
		
		if(dbConf.isSet("triggers")) plugin.TriggerManager.loadTriggers(dbConf.getConfigurationSection("triggers"));
	}

	private void updatePosLocs() {
		String pos1 = dbConf.getString("arena.pos1");
		if (pos1 == null) return;
		String[] pos1Ar = pos1.split(":");
		World pos1w = Bukkit.getWorld(pos1Ar[0]);
		Location pos1Loc;

		if (pos1Ar.length != 4) return;
		if (pos1w == null) return;
		pos1Loc = new Location(pos1w, Integer.parseInt(pos1Ar[1]), Integer.parseInt(pos1Ar[2]), Integer.parseInt(pos1Ar[3]));

		String pos2 = dbConf.getString("arena.pos2");
		if (pos2 == null) return;
		String[] pos2Ar = pos2.split(":");
		World pos2w = Bukkit.getWorld(pos1Ar[0]);
		Location pos2Loc;

		if (pos2Ar.length != 4) return;
		if (pos2w == null) return;
		pos2Loc = new Location(pos2w, Integer.parseInt(pos2Ar[1]), Integer.parseInt(pos2Ar[2]), Integer.parseInt(pos2Ar[3]));

		int plx = 0, ply = 0, plz = 0, phx = 0, phy = 0, phz = 0;
		if (pos1Loc.getBlockX() > pos2Loc.getBlockX()) {
			phx = pos1Loc.getBlockX();
			plx = pos2Loc.getBlockX();
		} else {
			phx = pos2Loc.getBlockX();
			plx = pos1Loc.getBlockX();
		}
		if (pos1Loc.getBlockY() > pos2Loc.getBlockY()) {
			phy = pos1Loc.getBlockY();
			ply = pos2Loc.getBlockY();
		} else {
			phy = pos2Loc.getBlockY();
			ply = pos1Loc.getBlockY();
		}
		if (pos1Loc.getBlockZ() > pos2Loc.getBlockZ()) {
			phz = pos1Loc.getBlockZ();
			plz = pos2Loc.getBlockZ();
		} else {
			phz = pos2Loc.getBlockZ();
			plz = pos1Loc.getBlockZ();
		}

		Location higher = new Location(pos1Loc.getWorld(), phx, phy, phz);
		Location lower = new Location(pos1Loc.getWorld(), plx, ply, plz);
		higherPos = higher;
		lowerPos = lower;
	}
	
	public void loadSpawns() {
		List<String> myspawns = dbConf.getStringList("arena.spawns");
		spawns.clear();
		if (myspawns != null) {
			for (String s : myspawns) {
				if ((s != null) && (s.length() > 0)) {
					Location loc = Util.Str2Loc(s);
					if (loc != null) {
						spawns.add(loc);
					}
				}
			}
		}
		Util.log("Loaded " + spawns.size() + " spawns.");
	}
	
	public void addSpawn(Location loc) {
		spawns.add(loc);
		List <String> loclist = new ArrayList<String>();
		for (Location l : spawns) {
			String s = Util.Loc2Str(l);
			if ((s != null) && (s.length() > 0)) {
				loclist.add(s);
			}
		}

		dbConf.set("arena.spawns", loclist);
		ConfigUtil.saveConfig(dbConf, "db");
	}

	public void setPos1(Location loc) {
		int blockX = loc.getBlockX();
		int blockY = loc.getBlockY();
		int blockZ = loc.getBlockZ();
		dbConf.set("arena.pos1", loc.getWorld().getName() + ":" + blockX + ":" + blockY + ":" + blockZ);
		ConfigUtil.saveConfig(dbConf, "db");
		updatePosLocs();
	}

	public void setPos2(Location loc) {
		int blockX = loc.getBlockX();
		int blockY = loc.getBlockY();
		int blockZ = loc.getBlockZ();
		dbConf.set("arena.pos2", loc.getWorld().getName() + ":" + blockX + ":" + blockY + ":" + blockZ);
		ConfigUtil.saveConfig(dbConf, "db");
		updatePosLocs();
	}

	public void setLobby(Location loc) {
		int blockX = loc.getBlockX();
		int blockY = loc.getBlockY();
		int blockZ = loc.getBlockZ();
		dbConf.set("arena.lobby", loc.getWorld().getName() + ":" + blockX + ":" + blockY + ":" + blockZ);
		ConfigUtil.saveConfig(dbConf, "db");
	}

	public void setSpawn(Location loc) {
		int blockX = loc.getBlockX();
		int blockY = loc.getBlockY();
		int blockZ = loc.getBlockZ();
		dbConf.set("arena.spawn", loc.getWorld().getName() + ":" + blockX + ":" + blockY + ":" + blockZ);
		ConfigUtil.saveConfig(dbConf, "db");
	}

	public void setExit(Location loc) {
		int blockX = loc.getBlockX();
		int blockY = loc.getBlockY();
		int blockZ = loc.getBlockZ();
		dbConf.set("arena.exit", loc.getWorld().getName() + ":" + blockX + ":" + blockY + ":" + blockZ);
		ConfigUtil.saveConfig(dbConf, "db");
	}

	public boolean isInArena(Location loc) {
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();
		if (lowerPos == null || higherPos == null) return false;
		if (x >= lowerPos.getBlockX() && x <= higherPos.getBlockX()
				&& y >= lowerPos.getBlockY() && y <= higherPos.getBlockY()
				&& z >= lowerPos.getBlockZ() && z <= higherPos.getBlockZ()) {
			return true;
		}
		return false;
	}

	public Location getLowerPos() {
		return lowerPos;
	}

	public Location getHigherPos() {
		return higherPos;
	}
	
	public Location getRandomSpawn() {
		if (spawns.size() == 0) {
			Util.log.warning("ERROR: Spawn list is empty!");
			return null;
		}
		int ran = (int) (Math.random() * spawns.size());
		Location loc = spawns.get(ran);
		if (loc == null) {
			Util.log.warning("ERROR: Random spawn is null! (" + ran + " of " + spawns.size() + ")");
		}
		return loc;
	}
	
	/*
	public Location getRandomSpawn() {
		Location s = getSpawn();
		int d = 10;
		double dist;
		long deg;
		double x, z;
		Block b;
		boolean finish = false;
		;
		do {
			dist = Math.random() * d;
			deg = Math.round(Math.random() * 360);
			x = s.getBlockX() + (dist - d) * Math.cos(deg);
			z = s.getBlockZ() + (dist - d) * Math.cos(deg);
			b = s.getWorld().getBlockAt((int) x, s.getBlockY(), (int) z);
			if (b.getType().equals(Material.AIR)
					&& b.getRelative(BlockFace.UP).getType().equals(Material.AIR)
					&& isInArena(b.getLocation())) {
				finish = true;
			}
		} while (!finish);

		return b.getLocation().add(0.5, 0, 0.5);
	}
	*/

	public Location getRandomLocation(Location s, int d) {
		double dist;
		long deg;
		double x, z;
		Block b;
		boolean finish = false;
		int count = 0;

		//Util.log.info("Starting getRandomLocation() loop: dist(" + d + "), location("+ s.toString() + ")");
		do {
			count++;
			dist = Math.random() * d;
			deg = Math.round(Math.random() * 360);
			x = s.getBlockX() + (dist - d) * Math.cos(deg);
			z = s.getBlockZ() + (dist - d) * Math.cos(deg);
			b = s.getWorld().getBlockAt((int) x, s.getBlockY(), (int) z);
			//Util.log.info("  loop: dist(" + dist + "), deg(" + deg + "), location(" + b.getLocation().toString() + ")");
			if (b.getType().equals(Material.AIR)
					&& b.getRelative(BlockFace.UP).getType().equals(Material.AIR)
					&& isInArena(b.getLocation())) {
				finish = true;
			}
		} while ((!finish) && (count < 30));

		if (!finish) {
			// Did not find a good place, so just spawn on the player
			return s.add(0.5, 0, 0.5);
		}

		// Return the location
		return b.getLocation().add(0.5, 0, 0.5);
	}

	public Location getLobby() {
		String lobby = dbConf.getString("arena.lobby");
		if (lobby.isEmpty()) { return null; }
			
		String[] lobbyAr = lobby.split(":");
		World lobbyW = Bukkit.getWorld(lobbyAr[0]);

		if (lobbyAr.length != 4) return null;
		return new Location(lobbyW, Integer.parseInt(lobbyAr[1]), Integer.parseInt(lobbyAr[2]), Integer.parseInt(lobbyAr[3]));
	}

	public Location getSpawn() {
		String spawn = dbConf.getString("arena.spawn");
		if (spawn.isEmpty()) { return null; }
			
		String[] spawnAr = spawn.split(":");
		World spawnW = Bukkit.getWorld(spawnAr[0]);

		if (spawnAr.length != 4) return null;
		return new Location(spawnW, Integer.parseInt(spawnAr[1]), Integer.parseInt(spawnAr[2]), Integer.parseInt(spawnAr[3]));
	}

	public Location getExit() {
		try {
			String exit = dbConf.getString("arena.exit");
			String[] exitAr = exit.split(":");
			if (exitAr.length != 4) return new Location(plugin.getServer().getWorlds().get(0),0,0,0);
			World exitW = Bukkit.getWorld(exitAr[0]);
			return new Location(exitW, Integer.parseInt(exitAr[1]), Integer.parseInt(exitAr[2]), Integer.parseInt(exitAr[3]));
		}
		catch(Exception ex) {
			return new Location(plugin.getServer().getWorlds().get(0),0,0,0);
		}
		
	}

	public void removeAllMobs() {
		// Remove all zombie entities located in the maze arena (just to make it more fair)
		Util.log.info("Removing all Maze mobs...");
		
		int count = 0;
		Location spawn = plugin.arena.getSpawn();
		for (Entity e : spawn.getWorld().getEntities()) {
			if (!plugin.arena.isInArena(e.getLocation())) continue;
			if (e.getType().equals(EntityType.ZOMBIE)) { e.remove(); count++; }
			if (e.getType().equals(EntityType.SPIDER)) { e.remove(); count++; }
			if (e.getType().equals(EntityType.CAVE_SPIDER)) { e.remove(); count++; }
			if (e.getType().equals(EntityType.SKELETON)) { e.remove(); count++; }
			if (e.getType().equals(EntityType.CREEPER)) { e.remove(); count++; }
		}
		Util.log.info("Removed " + count + " mobs from maze arena");
	}

	public int countAllMobs() {
		int count = 0;
		Location spawn = plugin.arena.getSpawn();
		for (Entity e : spawn.getWorld().getEntities()) {
			if (!plugin.arena.isInArena(e.getLocation())) continue;
			if (e.getType().equals(EntityType.ZOMBIE)) { count++; continue; }
			if (e.getType().equals(EntityType.SPIDER)) { count++; continue; }
			if (e.getType().equals(EntityType.CAVE_SPIDER)) { count++; continue; }
			if (e.getType().equals(EntityType.SKELETON)) { count++; continue; }
			if (e.getType().equals(EntityType.CREEPER)) { count++; continue; }
		}
		return count;
	}
	
	public void RefreshLoadout(Player p) {
		PlayerInventory inv = p.getInventory();
		Material mat;
		
		for (Short i:plugin.mainConf.getShortList("startingItems")) {
			mat = Material.getMaterial(i);
			if (mat != null) {
				// Refresh the loadout by first removing each starting item if it exists
				if (inv.contains(mat)) {
					int slot = inv.first(mat);
					inv.remove(slot);
					inv.setItem(slot, new ItemStack(i));
				} else {
					// Auto equip armour (if necessary)
					if (mat.name().contains("HELMET")) {
						p.getInventory().setHelmet(new ItemStack(i));
					} else if (mat.name().contains("CHESTPLATE")) {
						p.getInventory().setChestplate(new ItemStack(i));
					} else if (mat.name().contains("LEGGINGS")) {
						p.getInventory().setLeggings(new ItemStack(i));
					} else if (mat.name().contains("BOOTS")) {
						p.getInventory().setBoots(new ItemStack(i));
					} else {
						// Add item to inventory
						p.getInventory().addItem(new ItemStack(i));
					}
				}
			}
		}
	}
	
	public Location restorePlayer(Player player) {
		player.getInventory().clear();

		Location back = null;
		PlayerStore ps = plugin.arena.store.get(player);
		if (ps != null) {
			player.getInventory().setContents(ps.inv.getContents());
			back = ps.previousLoc;
			player.setFoodLevel(ps.hunger);
			player.setHealth(ps.health);
			player.getInventory().setArmorContents(ps.armour);
		} else {
			player.getInventory().clear();
			player.setFoodLevel(20);
			player.setHealth(20);
		}
		player.setGameMode(GameMode.SURVIVAL);
		player.setSneaking(false);

		return back;
	}

	public void removePlayer(final Player player) {
		Util.debug("Removing: " + player.getName());

		// Ignore if the player is not playing/waiting
		if (!plugin.arena.playing.contains(player) && !plugin.arena.waiting.contains(player)) {
			return;
		}

		plugin.mazeCommand.arenaCommand.leaveMatch(player);

		// End the game if that was the last player
		if (plugin.arena.playing.isEmpty()) {
			if (plugin.arena.gameActive) {
				plugin.arena.gameActive = false;
				Bukkit.getScheduler().cancelTask(plugin.mazeCommand.arenaCommand.scheduleGameTimer);

				// Remove all mobs in the maze
				plugin.arena.removeAllMobs();
			}
		}
		
		// If start of game countdown was running...
		if (plugin.mazeCommand.arenaCommand.scheduleActive) {
			// If this was the last player, end the game
			if (plugin.arena.waiting.isEmpty()) {
				Bukkit.getScheduler().cancelTask(plugin.mazeCommand.arenaCommand.scheduleId);
				plugin.mazeCommand.arenaCommand.scheduleActive = false;
				Util.log.info(Util.formatBroadcast("Game cancelled, all waiting players left!"));
			}
			
			// Check that we still have enough players (people can leave during countdown)
			int minP = plugin.mainConf.getInt("minimumPlayers", 2);
			//if (minP < 2) minP = 1;
			final int minPlayers = minP;
			if (plugin.arena.waiting.size() < minP) {
				Bukkit.getScheduler().cancelTask(plugin.mazeCommand.arenaCommand.scheduleId);
				plugin.mazeCommand.arenaCommand.scheduleActive = false;
				Util.broadcastInside(ChatColor.RED + "Not enough players to start game!");
				Util.broadcastInside(ChatColor.GOLD + "" + plugin.arena.waiting.size() + ChatColor.BLUE + " players waiting, " + ChatColor.GOLD + minPlayers + ChatColor.BLUE + " minimum.");
				return;
			}

		}

		Util.debug("Player removed: " + player.getName());
	}
}
