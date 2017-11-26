package com.whiuk.philip.openmud.server;

import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.net.Socket;

class ConnectedClient {
	int id;
	Socket socket;
	Thread thread;
	BufferedOutputStream outputStream;
	BufferedInputStream inputStream;
	Player player;
	
	public boolean isAuthenticated() {
		return player != null;
	}
}