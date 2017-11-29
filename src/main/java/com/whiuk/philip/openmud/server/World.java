package com.whiuk.philip.openmud.server;

import java.util.Map;

class World {
	public static final int MAX_DAMAGE = 6;
	
	Map<String, MapArea> locations;
	Map<String, Item> items;
	Map<String, ItemAction> actions;
	Map<String, Character> characters;
	Map<String, ConversationNode> conversationNodes;
	Map<String, Skill> skills;
	MapArea startMapArea;
	Map<String, Item> startItems;
	Map<String, Integer> startExperience;
	Map<Slot, Item> startEquipment;
	int startHealth = 10;
	int startX; 
	int startY;
	
	/**
	 * A tick is an evolution of the game world. 
	 * In each tick an action can occur. Ticks produce events that are dispatched to listeners.
	 */
	public void tick() {
		try {
			locations.values().forEach(area -> area.tick());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}