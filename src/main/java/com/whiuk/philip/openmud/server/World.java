package com.whiuk.philip.openmud.server;

import java.util.Map;

class World {
	Map<String, Location> locations;
	Map<String, Item> items;
	Map<String, Action> actions;
	Map<String, Character> characters;
	Map<String, ConversationNode> conversationNodes;
	Map<String, Skill> skills;
	Location startLocation;
	Map<String, Item> startItems;
	Map<String, Integer> startExperience;
	Map<Slot, Item> startEquipment;
}