package com.whiuk.philip.openmud.server;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

class XMLWorldFactory {

	XMLWorldFactory() {
	}

	World create(String filename) throws JDOMException, IOException {
		World world = new World();
		world.locations = new HashMap<>();
		world.items = new HashMap<>();
		world.actions = new HashMap<>();
		world.skills = new HashMap<>();
		world.characters = new HashMap<>();
		world.conversationNodes = new HashMap<>();
		world.startExperience = new HashMap<>();

		Element root = new SAXBuilder().build(new File(filename)).getRootElement();
		populateSkills(root, world);
		populateItems(root, world);
		populateActions(root, world);
		populateItemActions(root, world);
		populateLocations(root, world);
		populateConnections(root, world);
		populateCharacters(root, world);
		populateConversationNodes(root, world);
		populateStartData(root, world);
		return world;
	}

	private void populateStartData(Element root, World world) {
		world.startLocation = world.locations.get(root.getChildText("startingLocation"));
		world.startItems = new HashMap<>();
		world.startEquipment = new HashMap<>();

		for (Element itemXml : root.getChild("startingItems").getChildren("item")) {
			world.startItems.put(world.items.get(itemXml.getText()).shortName,
					new Item(world.items.get(itemXml.getText())));
		}

		for (Element equipmentXml : root.getChild("startingEquipment").getChildren()) {
			world.startEquipment.put(Slot.valueOf(equipmentXml.getName()),
					new Item(world.items.get(equipmentXml.getText())));
		}
	}

	private void populateCharacters(Element root, World world) {
		for (Element characterXml : root.getChild("characters").getChildren("character")) {
			Character character = new Character();
			character.shortName = characterXml.getChildText("shortName");
			character.shortDescription = characterXml.getChildText("shortDescription");
			character.description = characterXml.getChildText("description");
			character.health = Integer.parseInt(characterXml.getChildText("health"));
			character.canTalk = Boolean.parseBoolean(characterXml.getChildText("canTalk"));
			character.startsTalking = Boolean.parseBoolean(characterXml.getChildText("startsTalking"));
			character.openingRemark = characterXml.getChildText("openingRemark");
			world.characters.put(character.shortName, character);
			world.locations.get(characterXml.getChildText("startingLocation")).characters.put(character.shortName,
					character);
		}
	}

	private void populateConversationNodes(Element root, World world) {
		for (Element conversationNodeXml : root.getChild("conversations").getChildren("conversation")) {
			ConversationNode node = new ConversationNode();
			node.id = conversationNodeXml.getAttributeValue("id");
			node.remark = conversationNodeXml.getChildText("remark");
			if (conversationNodeXml.getChild("options") != null) {
				for (Element optionXml : conversationNodeXml.getChild("options").getChildren("option")) {
					ConversationOption option = new ConversationOption();
					option.id = optionXml.getAttributeValue("id");
					option.response = optionXml.getChildText("response");
					option.childNode = optionXml.getChildText("childNode");
					node.options.put(option.id, option);
				}
			}
			world.conversationNodes.put(node.id, node);
		}
	}

	private void populateSkills(Element root, World world) {
		for (Element skillXml : root.getChild("skills").getChildren("skill")) {
			Skill skill = new Skill();
			skill.id = skillXml.getAttributeValue("id");
			skill.name = skillXml.getAttributeValue("name");
			world.skills.put(skill.id, skill);
		}
	}

	private void populateItems(Element root, World world) {
		for (Element itemXml : root.getChild("items").getChildren("item")) {
			Item item = new Item();
			item.shortName = itemXml.getChildText("shortName");
			item.shortDescription = itemXml.getChildText("shortDescription");
			item.description = itemXml.getChildText("description");
			item.creationMessage = itemXml.getChildText("creationMessage");
			item.canTake = Boolean.parseBoolean(itemXml.getChildText("canTake"));
			world.items.put(item.shortName, item);
		}
	}

	private void populateActions(Element root, World world) {
		for (Element actionXml : root.getChild("actions").getChildren("action")) {
			Action action = new Action();
			for (Element effectXml : actionXml.getChildren("effect")) {
				Effect effect = new Effect();
				effect.type = EffectType.valueOf(effectXml.getAttributeValue("type"));
				effect.item = world.items.get(effectXml.getAttributeValue("item"));
				effect.structure = world.items.get(effectXml.getAttributeValue("structure"));
				if (effectXml.getAttributeValue("skill") != null) {
					effect.skill = effectXml.getAttributeValue("skill");
				}
				if (effectXml.getAttributeValue("modifier") != null) {
					effect.modifier = Integer.parseInt(effectXml.getAttributeValue("modifier"));
				}
				action.effects.add(effect);
			}
			world.actions.put(actionXml.getAttributeValue("id"), action);
		}
	}

	private void populateItemActions(Element root, World world) {
		for (Element itemXml : root.getChild("items").getChildren("item")) {
			Item item = world.items.get(itemXml.getChildText("shortName"));
			if (itemXml.getChild("actions") != null) {
				for (Element actionXml : itemXml.getChild("actions").getChildren("action")) {
					item.actions.put(actionXml.getAttributeValue("item"),
							world.actions.get(actionXml.getAttributeValue("id")));
				}
			}
		}
	}

	private void populateLocations(Element root, World world) {
		for (Element locationXml : root.getChild("locations").getChildren("location")) {
			Location location = new Location();
			location.description = locationXml.getChildText("description");
			location.shortDescription = locationXml.getChildText("shortDescription");
			if (locationXml.getChild("structures") != null) {
				for (Element structureXml : locationXml.getChild("structures").getChildren("structure")) {
					Item structure = new Item(world.items.get(structureXml.getAttributeValue("item")));
					location.structures.put(structure.shortName, structure);
				}
			}
			if (locationXml.getChild("drops") != null) {
				for (Element dropXml : locationXml.getChild("drops").getChildren("drop")) {
					Drop drop = new Drop();
					drop.count = Integer.parseInt(dropXml.getAttributeValue("count"));
					drop.item = new Item(world.items.get(dropXml.getAttributeValue("item")));
					location.drops.put(drop.item.shortName, drop);
				}
			}
			world.locations.put(locationXml.getAttributeValue("id"), location);
		}
	}

	private void populateConnections(Element root, World world) {
		for (Element connectionXml : root.getChild("connections").getChildren("connection")) {
			world.locations.get(connectionXml.getAttributeValue("from")).connections.put(
					Direction.valueOf(connectionXml.getAttributeValue("direction")),
					world.locations.get(connectionXml.getAttributeValue("to")));
		}
	}
}