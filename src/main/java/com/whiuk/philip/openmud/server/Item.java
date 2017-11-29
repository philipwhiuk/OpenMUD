package com.whiuk.philip.openmud.server;

import java.util.HashMap;
import java.util.Map;

class Item {
	public Item() {			
	}
	
	public Item(Item item) {
		this.shortName = item.shortName;
		this.shortDescription = item.shortDescription;
		this.description = item.description;
		this.actions = item.actions;
	}
	String shortName;
	String shortDescription;
	String description;
	String creationMessage;
	Map<String, ItemAction> actions = new HashMap<>();
	boolean canTake;
	
	//TODO: tick
	public void tick(MapArea mapArea) {
		return;
	}
}