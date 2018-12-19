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

import info.plugmania.mazemania.MazeMania;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.logging.Level;


public class Trigger {
	public Material material;
	public String effect;
	public String arguments;

	public Trigger(Material ID, String e,String args){
		material=ID;
		effect=e;
		arguments=args;
	}
	
	public Trigger(ConfigurationSection csec){
		if(csec.contains("blockId")){
			MazeMania.instance.getLogger().log(Level.WARNING,"Trigger with blockID NOT SUPPORTED: "+csec.getCurrentPath());
			return;
		}
		material= Material.matchMaterial(csec.getString("material"));
		if(material == null)MazeMania.instance.getLogger().log(Level.WARNING,"Config has null material: "+csec.getCurrentPath() + csec.getString("material"));
		effect=csec.getString("effect");
		arguments=csec.getString("arguments");
	}
	
	public ConfigurationSection asConfigSection(ConfigurationSection csec){
		csec.set("material", material);
		csec.set("effect",effect);
		csec.set("arguments", arguments);
		return csec;
	}

public void apply(Player p,MazeMania instance){
	Effects e=new Effects(instance);
	e.apply(p, arguments, effect);
}
}
