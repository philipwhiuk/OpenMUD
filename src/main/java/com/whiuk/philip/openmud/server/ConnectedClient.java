package com.whiuk.philip.openmud.server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

class ConnectedClient {
	int id;
	Socket socket;
	Thread thread;
	ObjectOutputStream outputStream;
	ObjectInputStream inputStream;
	Player player;
	
	public boolean isAuthenticated() {
		return player != null;
	}
}