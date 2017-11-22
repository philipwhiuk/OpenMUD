package com.whiuk.philip.openmud;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

public class OpenMUD {
	
	public static final int MAX_DAMAGE = 6;

	InputStream in;
	OutputStream out;
	Scanner scanner;
	PrintWriter printWriter;
	Map<String, Location> locations;
	Map<String, Item> items;
	Map<String, Action> actions;
	Map<String, Character> characters;
	
	boolean running = true;
	
	Location currentLocation;
	Map<String, Item> currentItems;
	Map<String, Skill> skills;
	Map<Slot, Item> equipment;
	Character playerCharacter;
	Random random = new Random();
	
	public static void main(String[] args) throws Exception {
		new OpenMUD(System.in, System.out);
	}
	
	class Location {
		String shortDescription;
		String description;
		Map<Direction, Location> connections = new HashMap<>();
		Map<String, Drop> drops = new HashMap<>();
		Map<String, Item> structures = new HashMap<>();
		Map<String, Character> characters = new HashMap<>();
	}
	
	class Skill {
		String id;
		String name;
		int experience;
	}
	
	class Action {
		List<Effect> effects = new ArrayList<>();
	}
	
	class Effect {
		EffectType type;
		Item item;
		Item structure;
		String skill;
		int modifier;
	}
	
	class Character {
		String shortName;
		String shortDescription;
		String description;
		int health;
		boolean alive;
	}
	
	enum EffectType {
		PROVIDES, CREATES, REWARDS;
	}
	
	class Drop {
		Item item;
		int count;
	}
	
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
		Map<String, Action> actions = new HashMap<>();
		boolean canTake;
	}
	
	enum Direction {
		NORTH, EAST, SOUTH, WEST;
	}
	
	enum Slot {
		HEAD, BODY, MAIN_HAND, OFF_HAND, LEGS;
	}
	
	public OpenMUD(InputStream in, OutputStream out) throws Exception {
		this.in = in;
		this.out = out;
		scanner = new Scanner(in);
		printWriter = new PrintWriter(out);
		playGame();
	}
	
	private void playGame() throws Exception {
		setupGame();
		playerCharacter = new Character();
		playerCharacter.health = 10;
		playerCharacter.alive = true;
		changeLocation(currentLocation);
		do {
			performTurn();			
		} while(playerCharacter.alive && running);
		if (!playerCharacter.alive) {
			printOutput("Game Over");
		}
	}

	private void setupGame() throws JDOMException, IOException {
		locations = new HashMap<>();
		items = new HashMap<>();
		actions = new HashMap<>();
		skills = new HashMap<>();
		characters = new HashMap<>();
		
		Element root = new SAXBuilder().build(new File("gameData.xml")).getRootElement();
		populateSkills(root);
		populateItems(root);
		populateActions(root);
		populateItemActions(root);
		populateLocations(root);
		populateConnections(root);
		populateCharacters(root);

		currentLocation = locations.get(root.getChildText("startingLocation"));
		currentItems = new HashMap<>();
		equipment = new HashMap<>();
		
		for (Element itemXml : root.getChild("startingItems").getChildren("item")) {
			currentItems.put(items.get(itemXml.getText()).shortName, new Item(items.get(itemXml.getText())));
		}
		
		for (Element equipmentXml : root.getChild("startingEquipment").getChildren()) {
			equipment.put(Slot.valueOf(equipmentXml.getName()), new Item(items.get(equipmentXml.getText())));
		}
	}
	
	private void populateCharacters(Element root) {
		for (Element characterXml : root.getChild("characters").getChildren("character")) {
			Character character = new Character();
			character.shortName = characterXml.getChildText("shortName");
			character.shortDescription = characterXml.getChildText("shortDescription");
			character.description = characterXml.getChildText("description");
			character.health = Integer.parseInt(characterXml.getChildText("health"));
			characters.put(character.shortName, character);
			locations.get(characterXml.getChildText("startingLocation")).characters.put(character.shortName, character);
		}
	}

	private void populateSkills(Element root) {
		for (Element skillXml : root.getChild("skills").getChildren("skill")) {
			Skill skill = new Skill();
			skill.id = skillXml.getAttributeValue("id");
			skill.name = skillXml.getAttributeValue("name");
			skills.put(skill.id, skill);
		}
	}
	
	private void populateItems(Element root) {
		for (Element itemXml : root.getChild("items").getChildren("item")) {
			Item item = new Item();
			item.shortName = itemXml.getChildText("shortName");
			item.shortDescription = itemXml.getChildText("shortDescription");
			item.description = itemXml.getChildText("description");
			item.creationMessage = itemXml.getChildText("creationMessage");
			item.canTake = Boolean.parseBoolean(itemXml.getChildText("canTake"));
			items.put(item.shortName, item);
		}
	}
	
	private void populateActions(Element root) {
		for (Element actionXml : root.getChild("actions").getChildren("action")) {
			Action action = new Action();
			for (Element effectXml : actionXml.getChildren("effect")) {
				Effect effect = new Effect();
				effect.type = EffectType.valueOf(effectXml.getAttributeValue("type"));
				effect.item = items.get(effectXml.getAttributeValue("item"));
				effect.structure = items.get(effectXml.getAttributeValue("structure"));
				if (effectXml.getAttributeValue("skill") != null) {
					effect.skill = effectXml.getAttributeValue("skill");
				}
				if (effectXml.getAttributeValue("modifier") != null) {
					effect.modifier = Integer.parseInt(effectXml.getAttributeValue("modifier"));
				}
				action.effects.add(effect);
			}
			actions.put(actionXml.getAttributeValue("id"), action);
		}
	}
	
	private void populateItemActions(Element root) {
		for (Element itemXml : root.getChild("items").getChildren("item")) {
			Item item = items.get(itemXml.getChildText("shortName"));
			if (itemXml.getChild("actions") != null) {
				for (Element actionXml : itemXml.getChild("actions").getChildren("action")) {
					item.actions.put(actionXml.getAttributeValue("item"), 
							actions.get(actionXml.getAttributeValue("id")));
				}
			}
		}
	}
	
	private void populateLocations(Element root) {
		for (Element locationXml : root.getChild("locations").getChildren("location")) {
			Location location = new Location();
			location.description = locationXml.getChildText("description");
			location.shortDescription = locationXml.getChildText("shortDescription");
			if (locationXml.getChild("structures") != null) {
				for (Element structureXml : locationXml.getChild("structures").getChildren("structure")) {
					Item structure = new Item(items.get(structureXml.getAttributeValue("item")));
					location.structures.put(structure.shortName, structure);
				}
			}
			if (locationXml.getChild("drops") != null) {
				for (Element dropXml : locationXml.getChild("drops").getChildren("drop")) {
					Drop drop = new Drop();
					drop.count = Integer.parseInt(dropXml.getAttributeValue("count"));
					drop.item = new Item(items.get(dropXml.getAttributeValue("item")));
					location.drops.put(drop.item.shortName, drop);
				}
			}
			locations.put(locationXml.getAttributeValue("id"), location);
		}
	}
	
	private void populateConnections(Element root) {
		for (Element connectionXml : root.getChild("connections").getChildren("connection")) {
			locations.get(connectionXml.getAttributeValue("from")).connections.put(
					Direction.valueOf(connectionXml.getAttributeValue("direction")), 
					locations.get(connectionXml.getAttributeValue("to"))
					);
		}
	}
	
	private void performTurn() {
		final String fullCommand = readCommand().trim();
		final String[] command = fullCommand.toUpperCase().split(" ", 2);
		final String action = command[0];
		switch(action) {
		//System
		case "QUIT":
			printOutput("Thanks for playing");
			running = false;
			break;
		//Data View
		case "INVENTORY":
			printInventory();
			break;
		case "EQUIPMENT":
			printEquipment();
			break;
		//Combat
		case "ATTACK":
			if (command.length != 2) {
				printOutput("Need to provide a character to attack");
			} else {
				attack(command[1]);
			}
			break;
		case "KILL":
			if (command.length != 2) {
				printOutput("Need to provide a character to attack");
			} else {
				kill(command[1]);
			}
			break;
		//General	
		case "MOVE":
			if (command.length != 2) {
				printOutput("Need to provide a direction to move");
			} else {
				move(command[1]);
			}
			break;
		case "TAKE":
			if (command.length != 2) {
				printOutput("Need to provide an item to take");
			} else {
				take(command[1]);
			}
			break;
		case "DROP":
			if (command.length != 2) {
				printOutput("Need to provide an item to drop");
			} else {
				drop(command[1]);
			}
			break;
		case "USE":
			String[] commandData = command[1].split(" ");
			if (commandData.length != 2) {
				printOutput("Need to provide an item and a target");
			} else {
				use(commandData[0], command[1]);
			}
			break;
		default:
			printOutput("Unrecognised command: "+ fullCommand);
		}
	}
	
	private void printInventory() {
		printOutput("Inventory:");
		for (Map.Entry<String, Item> itemEntry: currentItems.entrySet()) {
			printOutput("* "+itemEntry.getKey()+" - "+itemEntry.getValue().shortDescription);
		}
	}
	
	private void printEquipment() {
		printOutput("Equipment:");
		if (equipment.isEmpty()) {
			printOutput("* Not wielding anything");
		} else {
			for (Map.Entry<Slot, Item> itemEntry: equipment.entrySet()) {
				printOutput("* "+itemEntry.getKey()+" - "+itemEntry.getValue().shortDescription);
			}
		}
	}

	private void kill(String character) {
		if (currentLocation.characters.containsKey(character)) {
			Character target = currentLocation.characters.get(character);
			printOutput("Fighting "+character+" to the death.");
			boolean finished = false;
			int i = 1;
			while (!finished) {
				printOutput("Round: "+i);
				finished = performCombatRound(playerCharacter, target);
				i++;
			}
		}
	}
	
	private void attack(String character) {
		if (currentLocation.characters.containsKey(character)) {
			Character target = currentLocation.characters.get(character);
			printOutput("Attacking "+character+".");
			performCombatRound(playerCharacter, target);
		}
	}
		
	private boolean performCombatRound(Character character1, Character character2) {
		boolean character1AttacksFirst = random.nextBoolean();
		boolean somebodyKilled = false;
		if (character1AttacksFirst) {
			somebodyKilled = performHitAttempt(character1, character2);
			if (!somebodyKilled) {
				somebodyKilled = performHitAttempt(character2, character1);
			}
		} else {
			somebodyKilled = performHitAttempt(character2, character1);
			if (!somebodyKilled) {
				somebodyKilled = performHitAttempt(character1, character2);
			}
		}
		return somebodyKilled;
	}
	
	private boolean performHitAttempt(Character attacker, Character defender) {
		boolean hit = random.nextBoolean();
		if (hit) {
			int damage = random.nextInt(MAX_DAMAGE);
			if (attacker == playerCharacter) {
				printOutput("Did "+damage+"HP of damage");
				if (defender.health <= damage) {
					printOutput("Killed "+defender.shortName+".");
					defender.alive = false;
					currentLocation.characters.remove(defender.shortName);
					return true;
				} else {
					defender.health -= damage;
					printOutput(defender.shortName+" has "+defender.health+"HP left.");
				}
			} else if (defender == playerCharacter){
				printOutput(attacker.shortName+" did "+damage+"HP of damage");
				if (defender.health <= damage) {
					printOutput("Killed by "+attacker.shortName+".");
					defender.alive = false;
					return true;
				} else {
					defender.health -= damage;
					printOutput("You have "+defender.health+"HP left.");
				}
			} else {
				throw new UnsupportedOperationException();
			}
		} else {
			if (attacker == playerCharacter) {
				printOutput("Missed "+defender.shortName);
			} else {
				printOutput(attacker.shortName+" missed");
			}
		}
		return false;
	}
	
	private void move(String directionString) {
		Direction direction;
		try {
			direction = Direction.valueOf(directionString);
		} catch (IllegalArgumentException e) {
			printOutput("Unknown direction: " + directionString);
			return;
		}
		
		if (currentLocation.connections.containsKey(direction))
			changeLocation(currentLocation.connections.get(direction));
		else
			printOutput("Unable to move in that direction");
	}
	
	private void changeLocation(Location newLocation) {
		currentLocation = newLocation;
		printDescription(currentLocation);
		printStuff(currentLocation);
		printDirections(currentLocation);
		
	}

	private void take(String itemName) {
		if (currentLocation.drops.containsKey(itemName)) {
			Drop drop = currentLocation.drops.get(itemName);
			if (drop.count < 1) {
				printOutput("Item has already gone");
				currentLocation.drops.remove(itemName);
			} else {
				removeItem(currentLocation, drop, itemName);
				currentItems.put(drop.item.shortName, drop.item);
				printOutput("You pick up the "+drop.item.shortName);
			}
		} else {
			printOutput("There is no "+itemName+" to pick up.");
		}
	}

	private void drop(String itemName) {
		if (currentItems.containsKey(itemName)) {
			Drop drop;
			if (currentLocation.drops.containsKey(itemName)) {
				drop = currentLocation.drops.get(itemName);
				drop.count++;
			} else {
				drop = new Drop();
				drop.count = 1;
				drop.item = currentItems.get(itemName);
				currentLocation.drops.put(itemName, drop);
			}
			currentItems.remove(itemName);
			printOutput("You drop the "+ itemName);
		} else {
			printOutput("You don't have that item to drop");
		}
	}
	
	private void removeItem(Location currentLocation2, Drop drop, String itemName) {
		if (drop.count == 1) {
			currentLocation.drops.remove(itemName);;
		} else {
			drop.count--;
		}
	}

	private void use(String itemName, String targetName) {
		if (currentItems.containsKey(itemName)) {
			if (currentLocation.structures.containsKey(targetName)) {
				useOnStructure(itemName, targetName);
			} else if (currentLocation.drops.containsKey(targetName)) {
				useOnDrop(itemName, targetName);
			} else {
				printOutput("Can't find "+targetName);
			}
		} else {
			printOutput("You don't have a "+itemName);
		}
	}
	
	private void useOnStructure(String itemName, String targetName) {
		Item target = currentLocation.structures.get(targetName);
		if (target.actions.containsKey(itemName)) {
			Action action = target.actions.get(itemName);
			for (Effect effect : action.effects) {
			switch (effect.type) {
				case PROVIDES:
					Item item = new Item(effect.item);
					currentItems.put(item.shortName, item);
					printOutput("You get some "+item.shortName);
					break;
				case REWARDS:
					skills.get(effect.skill).experience += effect.modifier;
					printOutput("You gain "+effect.modifier+" exp in "+skills.get(effect.skill).name);
					break;
				default:
					throw new UnsupportedOperationException();
				}
			}
		} else {
			printOutput("Can't use the "+itemName+" on the "+targetName);
		}
	}
	
	private void useOnDrop(String itemName, String targetName) {
		if(currentLocation.drops.get(targetName).item.actions.containsKey(itemName)) {
			Action action = currentLocation.drops.get(targetName).item.actions.get(itemName);
			for (Effect effect : action.effects) {
				switch (effect.type) {
					case CREATES:
						currentLocation.structures.put(effect.structure.shortName, new Item(effect.structure));
						removeItem(currentLocation, currentLocation.drops.get(targetName), targetName);
						printOutput(effect.structure.creationMessage);
						break;
					case REWARDS:
						skills.get(effect.skill).experience += effect.modifier;
						printOutput("You gain "+effect.modifier+" exp in "+skills.get(effect.skill).name);
						break;
					default:
						throw new UnsupportedOperationException();
				}
			}
		} else {
			printOutput("Can't use the "+itemName+" on the "+targetName);
		}
	}
	
	private void printDescription(Location location) {
		printOutput(location.description);
	}
	
	private void printStuff(Location location) {
		if (location.structures.size() > 0) {
			printOutput("Theres's the following structures:");
			for (Map.Entry<String, Item> structure: location.structures.entrySet()) {
				final String key = structure.getKey();
				final Item item = structure.getValue();
				printOutput("* "+key+" - "+item.shortDescription);
			}
		}
		if (location.drops.size() > 0) {
			printOutput("On the ground are the following items:");
			for (Map.Entry<String, Drop> dropEntry: location.drops.entrySet()) {
				final String key = dropEntry.getKey();
				final Drop drop = dropEntry.getValue();
				printOutput("* "+key+" ("+drop.count+") - "+drop.item.shortDescription);
			}
		}
		if (location.characters.size() > 0) {
			printOutput("There's the following characters:");
			for (Map.Entry<String, Character> characterEntry: location.characters.entrySet()) {
				final String key = characterEntry.getKey();
				final Character character = characterEntry.getValue();
				printOutput("* "+key+" - "+character.shortDescription);
			}
		}
	}
	
	private void printDirections(Location location) {
		if (location.connections.size() == 0) {
			printOutput("You can't move in any direction from here");
		}
		
		for (Map.Entry<Direction, Location> connection: location.connections.entrySet()) {
			printOutput("To the "+connection.getKey()+" is "+connection.getValue().shortDescription+". ");			
		}
	}
	
	private String readCommand() {
		String line = scanner.nextLine();
		printOutput("> "+ line);
		return line;
	}
	
	private void printOutput(String output) {
		printWriter.println(output);
		printWriter.flush();
	}
	
	
}
