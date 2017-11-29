package com.whiuk.philip.openmud.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.whiuk.philip.openmud.messages.Messages.GameMessageToClient.Tile;
import com.whiuk.philip.openmud.messages.Messages.GameMessageToServer.Direction;

class MapArea {
	String name;
	String shortDescription;
	String description;
	Tile[][] tiles;
	Map<Direction, MapArea> connections = new HashMap<>();
	List<Drop> drops = new ArrayList<>();
	List<Item> structures = new ArrayList<>();
	List<Character> characters = new ArrayList<>();
	public Iterable<? extends Tile> getTiles() {
		List<Tile> tileList = new ArrayList<>();
		for (int y = 0; y < tiles.length; y++) {			
			for (int x = 0; x < tiles[y].length; x++) {
				tileList.add(tiles[y][x]);
			}
		}
		return tileList;
	}
	
	public void tick() {
		drops.forEach(drops -> drops.tick(this));
		Iterator<Drop> dropI = drops.iterator();
		while(dropI.hasNext()) {
			Drop drop = dropI.next();
			if (drop.ticksLeft == 0) {
				dropI.remove();
			}
		}
		
		structures.forEach(structure -> structure.tick(this));
		// Shuffle necessary so that an early arriver doesn't consistently beat late-comers in getting executed first.
		// TODO: Combat
		Collections.shuffle(characters);
		characters.forEach(character -> character.tick(this));
		return;
	}
	public void remove(Drop drop) {
		drops.remove(drop);
	}
}