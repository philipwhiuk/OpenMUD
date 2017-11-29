package com.whiuk.philip.openmud.server;

import static org.junit.Assume.assumeNoException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.whiuk.philip.openmud.messages.Messages.GameMessageToClient;
import com.whiuk.philip.openmud.messages.Messages.GameMessageToClient.GameMessageType;
import com.whiuk.philip.openmud.messages.Messages.GameMessageToClient.LocationMessageToClient;
import com.whiuk.philip.openmud.messages.Messages.GameMessageToServer;
import com.whiuk.philip.openmud.messages.Messages.GameMessageToServer.Direction;

import static com.whiuk.philip.openmud.Constants.MAP_AREA_SIZE;

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

	public void moveUpdated() {}
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
	ConcurrentLinkedDeque<Action> actions = new ConcurrentLinkedDeque<>();
	private String name;
	private Player player;

	public PlayerCharacter(Player player, String name, World world) {
		this.player = player;
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
				System.out.println("Executing Action");
				tickConsumed = action.execute(mapArea, this);
			}
		}
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

	@Override
	public void moveUpdated() {
		player.queueGameMessage(GameMessageToClient
				.newBuilder()
				.setGameMessageType(GameMessageType.LOCATION)
				.setLocation(LocationMessageToClient.newBuilder().setX(x).setY(y)).build());
	}
}

interface Action {
	public boolean execute(MapArea m, Character c);
}

class MoveAction implements Action {
	Direction direction;
	
	MoveAction(Direction d) {
		this.direction = d;
	}

	@Override
	public boolean execute(MapArea m, Character c) {
		switch(direction) {
			case NORTH:
				if (c.y > 0) {
					c.y--;
					c.moveUpdated();
				}
				break;
			case SOUTH:
				if (c.y < MAP_AREA_SIZE-1) {
					c.y++;
					c.moveUpdated();
				}
				break;
			case WEST:
				if (c.x > 0) {
					c.x--;
					c.moveUpdated();
				}
				break;
			case EAST:
				if (c.x < MAP_AREA_SIZE-1) {
					c.x++;
					c.moveUpdated();
				}
				break;
		}
		return true;
	}
}