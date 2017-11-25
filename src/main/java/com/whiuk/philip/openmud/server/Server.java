package com.whiuk.philip.openmud.server;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.jdom2.JDOMException;

public class Server {
	
	public static final int MAX_DAMAGE = 6;
	
	private ServerSocket serverSocket;
	private Map<Long, ConnectedClient> connectedClients = new HashMap<>();
	private World world;
	
	boolean running = true;
	
	private Map<String, Player> players = new HashMap<String, Player>();
	
	Random random = new Random();
	
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
				client.player = new Player(Server.this, world);
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
		System.out.println("Starting server");		
		buildWorld();
		createNetworkSocket(port);
		
		boolean listening = true;
		System.out.println("Listening for clients");
		while (listening) {
			acceptClient();
		}
	}
	
	private void buildWorld() throws Exception {
		System.out.println("Loading scenario");
		world = new XMLWorldFactory().create("gameData.xml");
	}
	
	private void createNetworkSocket(int port) throws IOException {
		serverSocket = new ServerSocket(port);
	}
	
	private void acceptClient() throws IOException {
		Socket clientSocket = serverSocket.accept();
		ConnectedClient client = new ConnectedClient();
		client.id = nextId();
		client.thread = new ClientThread(client);
		client.socket = clientSocket;
		client.thread.start();
		System.out.println("Accepted client from: "+clientSocket.getInetAddress()+":"+clientSocket.getPort()+" on "+clientSocket.getLocalPort());
		connectedClients.put(nextId(), client);
	}
	
	void disconnect(ConnectedClient client) {
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
}
