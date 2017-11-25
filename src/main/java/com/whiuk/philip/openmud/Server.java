package com.whiuk.philip.openmud;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

public class Server {
	
	public static final int MAX_DAMAGE = 6;
	
	private ServerSocket serverSocket;
	private Map<Long, ConnectedClient> connectedClients = new HashMap<>();
	
	private Map<String, Location> locations;
	private Map<String, Item> items;
	private Map<String, Action> actions;
	private Map<String, Character> characters;
	private Map<String, ConversationNode> conversationNodes;
	private Map<String, Skill> skills;
	private Location startLocation;
	private Map<String, Item> startItems;
	private Map<String, Integer> startExperience;
	private Map<Slot, Item> startEquipment;
	
	private boolean running = true;
	
	private Map<String, Player> players = new HashMap<String, Player>();
	
	private Random random = new Random();
	

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
		boolean canTalk;
		boolean startsTalking;
		String openingRemark;
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
	
	class ConversationState {
		ConversationNode currentNode;
		ConversationStateType stateType;
		String characterName;
	}
	
	class ConversationNode {
		private String id;
		private String remark;
		private Map<String, ConversationOption> options = new HashMap<>();
	}
	
	class ConversationOption {
		private String id;
		private String response;
		private String childNode; 
	}
	
	enum ConversationStateType {
		INITIAL, MORE_OPTIONS, FINISHED;
	}
	
	class ConnectedClient {
		long id;
		Socket socket;
		Thread thread;
		ObjectOutputStream outputStream;
		ObjectInputStream inputStream;
		Player player;
	}
	
	class ClientThread extends Thread {
		private ConnectedClient client;
		
		public ClientThread(ConnectedClient client) {
			super("ClientThread - Client: "+client.id);
			this.client = client;
		}
		
		public void run() {
			try {
				client.outputStream =
						new ObjectOutputStream(client.socket.getOutputStream());
				client.inputStream = 
						new ObjectInputStream(client.socket.getInputStream());
				client.player = new Player();
				client.player.client = client;
				client.player.setup();
				client.player.play();
			} catch (IOException e) {
				e.printStackTrace();
				disconnect(client);
				return;
			}
		}
	}
	
	private long nextId() {
		long value = random.nextLong();
		while(connectedClients.containsKey(value)) {
			System.out.println("collision");
			value = random.nextLong();
		}
		return value;
	}
	
	public Server(int port) throws Exception {
		System.out.println("Setting up game");
		setupGame();
		serverSocket = new ServerSocket(port);
		boolean listening = true;
		System.out.println("Listening for client");
		while (listening) {
			Socket clientSocket = serverSocket.accept();
			ConnectedClient client = new ConnectedClient();
			client.id = nextId();
			client.thread = new ClientThread(client);
			client.socket = clientSocket;
			client.thread.start();
			System.out.println("Accepted client from: "+clientSocket.getInetAddress()+":"+clientSocket.getPort()+" on "+clientSocket.getLocalPort());
			connectedClients.put(nextId(), client);
		}

	}
	
	private void disconnect(ConnectedClient client) {
		connectedClients.remove(client.id);
		closeQuietly(client.outputStream);
		closeQuietly(client.inputStream);
		closeQuietly(client.socket);
	}
	
	private void closeQuietly(Closeable c) {
		try {
			c.close();
		} catch (Exception ignored) {
		}
	}

	private void setupGame() throws JDOMException, IOException {
		locations = new HashMap<>();
		items = new HashMap<>();
		actions = new HashMap<>();
		skills = new HashMap<>();
		characters = new HashMap<>();
		conversationNodes = new HashMap<>();
		startExperience = new HashMap<>();
		
		Element root = new SAXBuilder().build(new File("gameData.xml")).getRootElement();
		populateSkills(root);
		populateItems(root);
		populateActions(root);
		populateItemActions(root);
		populateLocations(root);
		populateConnections(root);
		populateCharacters(root);
		populateConversationNodes(root);
		populateStartData(root);
	}

	private void populateStartData(Element root) {
		startLocation = locations.get(root.getChildText("startingLocation"));
		startItems = new HashMap<>();
		startEquipment = new HashMap<>();
		
		for (Element itemXml : root.getChild("startingItems").getChildren("item")) {
			startItems.put(items.get(itemXml.getText()).shortName, new Item(items.get(itemXml.getText())));
		}
		
		for (Element equipmentXml : root.getChild("startingEquipment").getChildren()) {
			startEquipment.put(Slot.valueOf(equipmentXml.getName()), new Item(items.get(equipmentXml.getText())));
		}
	}
	
	private void populateCharacters(Element root) {
		for (Element characterXml : root.getChild("characters").getChildren("character")) {
			Character character = new Character();
			character.shortName = characterXml.getChildText("shortName");
			character.shortDescription = characterXml.getChildText("shortDescription");
			character.description = characterXml.getChildText("description");
			character.health = Integer.parseInt(characterXml.getChildText("health"));
			character.canTalk = Boolean.parseBoolean(characterXml.getChildText("canTalk"));
			character.startsTalking = Boolean.parseBoolean(characterXml.getChildText("startsTalking"));
			character.openingRemark = characterXml.getChildText("openingRemark");
			characters.put(character.shortName, character);
			locations.get(characterXml.getChildText("startingLocation")).characters.put(character.shortName, character);
		}
	}
	
	private void populateConversationNodes(Element root) {
		for (Element conversationNodeXml : root.getChild("conversations").getChildren("conversation")) {
			ConversationNode node = new ConversationNode();
			node.id = conversationNodeXml.getAttributeValue("id");
			node.remark = conversationNodeXml.getChildText("remark");
			if (conversationNodeXml.getChild("options") != null) {
				for (Element optionXml: conversationNodeXml.getChild("options").getChildren("option")) {
					ConversationOption option = new ConversationOption();
					option.id = optionXml.getAttributeValue("id");
					option.response = optionXml.getChildText("response");
					option.childNode = optionXml.getChildText("childNode");
					node.options.put(option.id, option);
				}
			}
			conversationNodes.put(node.id, node);
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
	
	class Player {
		private ConnectedClient client;
		private Location currentLocation;
		private Map<String, Item> currentItems = new HashMap<>();
		private Map<String, Integer> experience = new HashMap<>();
		private Map<Slot, Item> equipment = new HashMap<>();
		private Character playerCharacter;
		
		private void setup() {
			currentLocation = startLocation;
			currentItems.putAll(startItems);
			experience.putAll(startExperience);
			equipment.putAll(startEquipment);
		}

		private void play() throws IOException {
			playerCharacter = new Character();
			playerCharacter.health = 10;
			playerCharacter.alive = true;
			changeLocation(currentLocation);
			do {
				performTurn();			
			} while(playerCharacter.alive && running);
			if (!playerCharacter.alive) {
				sendOutput("Game Over");
				disconnect(client);
			}
		}

		private void performTurn() {
			String nextCommand = readCommand();
			processCommand(nextCommand);
		}
		
		private void processCommand(final String fullCommand) {
			final String[] command = fullCommand.toUpperCase().split(" ", 2);
			final String action = command[0];
			switch(action) {
			//System
			case "QUIT": handleQuitCommand(); break;
			case "COMMANDS": printCommands(); break;
			//Data View
			case "INVENTORY": printInventory(); break;
			case "EQUIPMENT": printEquipment(); break;
			//Conversation
			case "TALK": handleTalkCommand(command); break;
			//Combat
			case "ATTACK": handleAttackCommand(command); break;
			case "KILL": handleKillCommand(command); break;
			//General	
			case "GO":
			case "MOVE": handleMoveCommand(command);  break;
			case "TAKE": handleTakeCommand(command); break;
			case "DROP": handleDropCommand(command); break;
			case "USE": handleUseCommand(command); break;
			default: handleUnrecognisedCommand(fullCommand); break;
			}
		}
		
		private void handleQuitCommand() {
			sendOutput("Thanks for playing");
			running = false;
		}
		
		private void handleAttackCommand(String[] command) {
			if (command.length != 2) {
				sendOutput("Need to provide a character to attack");
			} else {
				attack(command[1]);
			}
		}
		
		private void handleKillCommand(String[] command) {
			if (command.length != 2) {
				sendOutput("Need to provide a character to attack");
			} else {
				kill(command[1]);
			}
		}
		
		private void handleTalkCommand(String[] command) {
			if (command.length != 2) {
				sendOutput("Need to provide a character to talk to");
			} else {
				talk(command[1]);
			}
		}
		
		private void handleMoveCommand(String[] command) {
			if (command.length != 2) {
				sendOutput("Need to provide a direction to move");
			} else {
				move(command[1]);
			}
		}
		
		private void handleTakeCommand(String[] command) {
			if (command.length != 2) {
				sendOutput("Need to provide an item to take");
			} else {
				take(command[1]);
			}
		}
		
		private void handleDropCommand(String[] command) {
			if (command.length != 2) {
				sendOutput("Need to provide an item to drop");
			} else {
				drop(command[1]);
			}
		}
		
		private void handleUseCommand(String[] command) {
			String[] commandData = command[1].split(" ");
			if (commandData.length != 2) {
				sendOutput("Need to provide an item and a target");
			} else {
				use(commandData[0], commandData[1]);
			}
		}
		
		private void handleUnrecognisedCommand(String fullCommand) {
			sendOutput("Unrecognised command: "+ fullCommand);
		}
		
		private void printCommands() {
			sendOutput("COMMANDS - List commands");
			sendOutput("INVENTORY - Show current inventory");
			sendOutput("EQUIPMENT - Show current equipment");
			sendOutput("TALK - Talk to an NPC");
			sendOutput("ATTACK - Attack a character");
			sendOutput("KILL - Kill a character (fight until death)");
			sendOutput("MOVE/GO - Move in a direction");
			sendOutput("TAKE - Take an item");
			sendOutput("DROP - Drop an item");
			sendOutput("USE - Use an item");
			sendOutput("QUIT - Quit the game");
		}
		
		private void printInventory() {
			sendOutput("Inventory:");
			for (Map.Entry<String, Item> itemEntry: currentItems.entrySet()) {
				sendOutput("* "+itemEntry.getKey()+" - "+itemEntry.getValue().shortDescription);
			}
		}
		
		private void printEquipment() {
			sendOutput("Equipment:");
			if (equipment.isEmpty()) {
				sendOutput("* Not wielding anything");
			} else {
				for (Map.Entry<Slot, Item> itemEntry: equipment.entrySet()) {
					sendOutput("* "+itemEntry.getKey()+" - "+itemEntry.getValue().shortDescription);
				}
			}
		}
	
		private void kill(String character) {
			if (currentLocation.characters.containsKey(character)) {
				Character target = currentLocation.characters.get(character);
				sendOutput("Fighting "+character+" to the death.");
				boolean finished = false;
				int i = 1;
				while (!finished) {
					sendOutput("Round: "+i);
					finished = performCombatRound(playerCharacter, target);
					i++;
				}
			}
		}
		
		private void attack(String character) {
			if (currentLocation.characters.containsKey(character)) {
				Character target = currentLocation.characters.get(character);
				sendOutput("Attacking "+character+".");
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
					sendOutput("Did "+damage+"HP of damage");
					if (defender.health <= damage) {
						sendOutput("Killed "+defender.shortName+".");
						defender.alive = false;
						currentLocation.characters.remove(defender.shortName);
						return true;
					} else {
						defender.health -= damage;
						sendOutput(defender.shortName+" has "+defender.health+"HP left.");
					}
				} else if (defender == playerCharacter){
					sendOutput(attacker.shortName+" did "+damage+"HP of damage");
					if (defender.health <= damage) {
						sendOutput("Killed by "+attacker.shortName+".");
						defender.alive = false;
						return true;
					} else {
						defender.health -= damage;
						sendOutput("You have "+defender.health+"HP left.");
					}
				} else {
					throw new UnsupportedOperationException();
				}
			} else {
				if (attacker == playerCharacter) {
					sendOutput("Missed "+defender.shortName);
				} else {
					sendOutput(attacker.shortName+" missed");
				}
			}
			return false;
		}
		
		private void talk(String characterName) {
			if (currentLocation.characters.containsKey(characterName)) {
				if (currentLocation.characters.get(characterName).canTalk) {
					Character character = currentLocation.characters.get(characterName);
					performConversation(character);
				} else {
					sendOutput("This character can't talk");
				}
			} else {
				sendOutput("That character doesn't exist");
			}
		}
		
		private void performConversation(Character character) {
			if (character.openingRemark == null) {
				sendOutput(character.shortName + " has nothing to say to you.");
				return;
			}
			
			if (character.startsTalking) {
				sendOutput(conversationNodes.get(character.openingRemark).remark);
			}
			
			ConversationState state = buildInitialState(character.openingRemark, character.shortDescription);
			while (!state.stateType.equals(ConversationStateType.FINISHED)) {
				state = performDialog(state);
			}
		}
		
		private ConversationState buildInitialState(String startNode, String characterName) {
			ConversationState state = new ConversationState();
			state.stateType = ConversationStateType.INITIAL;
			state.currentNode = conversationNodes.get(startNode);
			state.characterName = characterName;
			return state;
		}
		
		private ConversationState performDialog(ConversationState state) {
			sendOutput("What do you want to say?");
			printConversationOptions(state.currentNode.options);
			String optionName = readCommand().toUpperCase();
			if (state.currentNode.options.containsKey(optionName)) {
				ConversationOption option = state.currentNode.options.get(optionName);
				sayOption(option);
				state = processResponse(state, option);
			}
			return state;
		}
		
		private void printConversationOptions(Map<String, ConversationOption> options) {
			for (Map.Entry<String, ConversationOption> option: options.entrySet()) {
				sendOutput("* "+option.getKey()+": "+option.getValue().response);
			}
		}
		
		private void sayOption(ConversationOption option) {
			sendOutput("\""+option.response+"\"");
		}
		
		private ConversationState processResponse(ConversationState currentState, ConversationOption option) {
			if (option.childNode == null) {
				currentState.stateType = ConversationStateType.FINISHED;
				currentState.currentNode = null;
				return currentState;
			}
			currentState.currentNode = conversationNodes.get(option.childNode);
			printResponse(currentState.characterName, currentState.currentNode);
			
			if (currentState.currentNode.options != null && currentState.currentNode.options.isEmpty()) {		
				currentState.stateType = ConversationStateType.FINISHED;
			} else {
				currentState.stateType = ConversationStateType.MORE_OPTIONS;
			}
			return currentState;
		}
		
		private void printResponse(String characterName, ConversationNode node) {
			sendOutput(characterName+": \""+node.remark+"\"");
		}
		
		private void move(String directionString) {
			Direction direction;
			try {
				direction = Direction.valueOf(directionString);
			} catch (IllegalArgumentException e) {
				sendOutput("Unknown direction: " + directionString);
				return;
			}
			
			if (currentLocation.connections.containsKey(direction))
				changeLocation(currentLocation.connections.get(direction));
			else
				sendOutput("Unable to move in that direction");
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
					sendOutput("Item has already gone");
					currentLocation.drops.remove(itemName);
				} else {
					removeItem(currentLocation, drop, itemName);
					currentItems.put(drop.item.shortName, drop.item);
					sendOutput("You pick up the "+drop.item.shortName);
				}
			} else {
				sendOutput("There is no "+itemName+" to pick up.");
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
				sendOutput("You drop the "+ itemName);
			} else {
				sendOutput("You don't have that item to drop");
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
					sendOutput("Can't find "+targetName);
				}
			} else {
				sendOutput("You don't have a "+itemName);
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
						sendOutput("You get some "+item.shortName);
						break;
					case REWARDS:
						gainExperience(effect.skill, effect.modifier);
						break;
					default:
						throw new UnsupportedOperationException();
					}
				}
			} else {
				sendOutput("Can't use the "+itemName+" on the "+targetName);
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
							sendOutput(effect.structure.creationMessage);
							break;
						case REWARDS:
							gainExperience(effect.skill, effect.modifier);
							break;
						default:
							throw new UnsupportedOperationException();
					}
				}
			} else {
				sendOutput("Can't use the "+itemName+" on the "+targetName);
			}
		}
		
		private void gainExperience(String skillId, int amount) {
			sendOutput("You gain "+amount+" exp in "+skills.get(skillId).name);
			experience.put(skillId, experience.get(skillId) + amount);
		}
		
		private void printDescription(Location location) {
			sendOutput(location.description);
		}
		
		private void printStuff(Location location) {
			if (location.structures.size() > 0) {
				sendOutput("Theres's the following structures:");
				for (Map.Entry<String, Item> structure: location.structures.entrySet()) {
					final String key = structure.getKey();
					final Item item = structure.getValue();
					sendOutput("* "+key+" - "+item.shortDescription);
				}
			}
			if (location.drops.size() > 0) {
				sendOutput("On the ground are the following items:");
				for (Map.Entry<String, Drop> dropEntry: location.drops.entrySet()) {
					final String key = dropEntry.getKey();
					final Drop drop = dropEntry.getValue();
					sendOutput("* "+key+" ("+drop.count+") - "+drop.item.shortDescription);
				}
			}
			if (location.characters.size() > 0) {
				sendOutput("There's the following characters:");
				for (Map.Entry<String, Character> characterEntry: location.characters.entrySet()) {
					final String key = characterEntry.getKey();
					final Character character = characterEntry.getValue();
					sendOutput("* "+key+" - "+character.shortDescription);
				}
			}
		}
		
		private void printDirections(Location location) {
			if (location.connections.size() == 0) {
				sendOutput("You can't move in any direction from here");
			}
			
			for (Map.Entry<Direction, Location> connection: location.connections.entrySet()) {
				sendOutput("To the "+connection.getKey()+" is "+connection.getValue().shortDescription+". ");			
			}
		}
		
		private String readCommand() {
			try {
				String line = client.inputStream.readUTF();
				sendOutput("> "+ line);
				return line.trim();
			} catch (IOException e) {
				throw new RuntimeException("Client stream disconnection", e);
			}
		}
		
		private void sendOutput(String output) {
			try {
				client.outputStream.writeUTF(output);
				client.outputStream.flush();
			} catch (IOException e) {
				throw new RuntimeException("Client stream disconnection", e);
			}
		}
	}
}
