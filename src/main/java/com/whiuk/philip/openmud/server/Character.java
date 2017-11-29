package com.whiuk.philip.openmud.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

abstract class Character {	
	int health;
	MapArea mapArea;
	int x;
	int y;
		
	abstract public void tick(MapArea mapArea);

	abstract public String getName();

	public MapArea getMapArea() {
		return mapArea;
	}
}

class NonPlayerCharacter extends Character {
	String shortName;
	String shortDescription;
	String description;
	boolean canTalk;
	boolean startsTalking;
	String openingRemark;
	
	@Override
	public void tick(MapArea mapArea) {
		// TODO Auto-generated method stub
	}

	@Override
	public String getName() {
		return shortName;
	}
}

class PlayerCharacter extends Character {
	private Map<String, Item> currentItems = new HashMap<>();
	private Map<String, Integer> experience = new HashMap<>();
	private Map<Slot, Item> equipment = new HashMap<>();
	ConcurrentLinkedDeque<Action> actions;
	private String name;

	public PlayerCharacter(String name, World world) {
		this.name = name;
		currentItems.putAll(world.startItems);
		experience.putAll(world.startExperience);
		equipment.putAll(world.startEquipment);
		health = world.startHealth;
		mapArea = world.startMapArea;
		x = world.startX;
		y = world.startY;
		world.startMapArea.characters.add(this);
	}
	
	@Override
	public void tick(MapArea mapArea) {
		boolean tickConsumed = false;
		while (!tickConsumed) {
			Action action = actions.poll();
			if (action != null) {
				tickConsumed = action.execute();
			}
		}
		actions.poll().execute();
	}
	
	public void addLastAction(Action a) {
		actions.addLast(a);
	}
	
	public void addFirstAction(Action a) {
		actions.addFirst(a);
	}

	@Override
	public String getName() {
		return name;
	}
}

interface Action {
	public boolean execute();
}