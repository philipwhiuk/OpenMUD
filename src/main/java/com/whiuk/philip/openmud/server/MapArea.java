package com.whiuk.philip.openmud.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.whiuk.philip.openmud.messages.Messages.GameMessageToClient.Tile;

class MapArea {
	String name;
	String shortDescription;
	String description;
	Tile[][] tiles;
	Map<Direction, MapArea> connections = new HashMap<>();
	Map<String, Drop> drops = new HashMap<>();
	Map<String, Item> structures = new HashMap<>();
	Map<String, Character> characters = new HashMap<>();
	public Iterable<? extends Tile> getTiles() {
		List<Tile> tileList = new ArrayList<>();
		for (int y = 0; y < tiles.length; y++) {			
			for (int x = 0; x < tiles[y].length; x++) {
				tileList.add(tiles[y][x]);
			}
		}
		return tileList;
	}
}