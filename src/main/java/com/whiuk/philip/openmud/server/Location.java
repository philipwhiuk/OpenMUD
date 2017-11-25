package com.whiuk.philip.openmud.server;

import java.util.HashMap;
import java.util.Map;

class Location {
	String shortDescription;
	String description;
	Map<Direction, Location> connections = new HashMap<>();
	Map<String, Drop> drops = new HashMap<>();
	Map<String, Item> structures = new HashMap<>();
	Map<String, Character> characters = new HashMap<>();
}