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

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

public class Server {
	public static final Logger logger = Logger.getLogger(Server.class);
	
	public static final int MAX_DAMAGE = 6;
	
	private ServerSocket serverSocket;
	private Map<Integer, ConnectedClient> connectedClients = new HashMap<>();
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
	
	private int nextId() {
		int value = random.nextInt();
		while(connectedClients.containsKey(value)) {
			logger.warn("Collision while fetching unused client ID");
			value = random.nextInt();
		}
		return value;
	}
	
	public Server(int port) throws Exception {
		BasicConfigurator.configure();
		logger.info("Starting server");		
		buildWorld();
		createNetworkSocket(port);
		
		boolean listening = true;
		logger.info("Listening for clients");
		while (listening) {
			acceptClient();
		}
	}
	
	private void buildWorld() throws Exception {
		logger.info("Loading scenario");
		world = new XMLWorldFactory().create("gameData.xml");
	}
	
	private void createNetworkSocket(int port) throws IOException {
		logger.info("Creating TCP socket on port:" + port);
		serverSocket = new ServerSocket(port);
	}
	
	private void acceptClient() throws IOException {
		Socket clientSocket = serverSocket.accept();
		ConnectedClient client = new ConnectedClient();
		client.id = nextId();
		client.thread = new ClientThread(client);
		client.socket = clientSocket;
		client.thread.start();
		if (logger.isInfoEnabled()) {
			String logClientDetails = String.format("Accepted client [%1d] from: %2s:%3d on %4d", 
					client.id, clientSocket.getInetAddress(), clientSocket.getPort(), clientSocket.getLocalPort());
			logger.info(logClientDetails);
		}
		connectedClients.put(nextId(), client);
	}
	
	void disconnect(ConnectedClient client) {
		logger.info("Disconnecting client ["+client.id+"]");
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
