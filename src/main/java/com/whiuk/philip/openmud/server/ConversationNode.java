package com.whiuk.philip.openmud.server;

import java.util.HashMap;
import java.util.Map;

class ConversationNode {
	String id;
	String remark;
	Map<String, ConversationOption> options = new HashMap<>();
}