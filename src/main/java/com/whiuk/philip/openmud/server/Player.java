package com.whiuk.philip.openmud.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;

import com.whiuk.philip.openmud.Messages;

class Player {
	private final Server server;
	private final World world;
	private final String username;

	Player(Server server, World world, String username) {
		this.server = server;
		this.world = world;
		this.username = username;
	}

	ConnectedClient client;
	private Location currentLocation;
	private Map<String, Item> currentItems = new HashMap<>();
	private Map<String, Integer> experience = new HashMap<>();
	private Map<Slot, Item> equipment = new HashMap<>();
	private Character playerCharacter;
	
	void setup() {
		currentLocation = this.world.startLocation;
		currentItems.putAll(this.world.startItems);
		experience.putAll(this.world.startExperience);
		equipment.putAll(this.world.startEquipment);
		
		playerCharacter = new Character();
		playerCharacter.health = 10;
		playerCharacter.alive = true;
		changeLocation(currentLocation);
	}

	boolean play(ObjectInputStream inputStream) throws IOException {
		performTurn(inputStream);
		if (!playerCharacter.alive) {
			sendOutput("Game Over");
			return false;
		}
		return true;
	}

	private void performTurn(ObjectInputStream inputStream) {
		String nextCommand = readCommand(inputStream);
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
		this.server.running = false;
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
		boolean character1AttacksFirst = this.server.random.nextBoolean();
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
		boolean hit = this.server.random.nextBoolean();
		if (hit) {
			int damage = this.server.random.nextInt(Server.MAX_DAMAGE);
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
			sendOutput(this.world.conversationNodes.get(character.openingRemark).remark);
		}
		
		ConversationState state = buildInitialState(character.openingRemark, character.shortDescription);
		while (!state.stateType.equals(ConversationStateType.FINISHED)) {
			state = performDialog(state);
		}
	}
	
	private ConversationState buildInitialState(String startNode, String characterName) {
		ConversationState state = new ConversationState();
		state.stateType = ConversationStateType.INITIAL;
		state.currentNode = this.world.conversationNodes.get(startNode);
		state.characterName = characterName;
		return state;
	}
	
	private ConversationState performDialog(ConversationState state) {
		sendOutput("What do you want to say?");
		printConversationOptions(state.currentNode.options);
		String optionName = readCommand(client.inputStream).toUpperCase();
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
		currentState.currentNode = this.world.conversationNodes.get(option.childNode);
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
		sendOutput("You gain "+amount+" exp in "+this.world.skills.get(skillId).name);
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
	
	private String readCommand(ObjectInputStream inputStream) {
		try {
			inputStream.readByte(); //TEXT
			String line = client.inputStream.readUTF();
			sendOutput("> "+ line);
			return line.trim();
		} catch (IOException e) {
			throw new RuntimeException("Client stream disconnection", e);
		}
	}
	
	private void sendOutput(String output) {
		try {
			client.outputStream.writeByte(Messages.FromServer.GAME);
			client.outputStream.writeByte(Messages.FromServer.Game.TEXT);
			client.outputStream.writeUTF(output);
			client.outputStream.flush();
		} catch (IOException e) {
			throw new RuntimeException("Client stream disconnection", e);
		}
	}
}