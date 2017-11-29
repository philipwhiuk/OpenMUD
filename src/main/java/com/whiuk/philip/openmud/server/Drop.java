package com.whiuk.philip.openmud.server;

class Drop {
	Item item;
	int count;
	int ticksLeft;
	
	
	public void tick(MapArea m) {
		ticksLeft--;
	}
}