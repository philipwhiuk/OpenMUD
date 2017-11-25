package com.whiuk.philip.openmud.server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

class ConnectedClient {
	long id;
	Socket socket;
	Thread thread;
	ObjectOutputStream outputStream;
	ObjectInputStream inputStream;
	Player player;
}