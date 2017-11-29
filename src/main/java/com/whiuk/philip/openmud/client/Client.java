package com.whiuk.philip.openmud.client;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import com.whiuk.philip.openmud.messages.Messages;
import com.whiuk.philip.openmud.messages.Messages.AuthMessageToServer;
import com.whiuk.philip.openmud.messages.Messages.GameMessageToClient.LocationMessageToClient;
import com.whiuk.philip.openmud.messages.Messages.GameMessageToClient.MapAreaMessage;
import com.whiuk.philip.openmud.messages.Messages.GameMessageToServer;
import com.whiuk.philip.openmud.messages.Messages.GameMessageToServer.GameMessageType;
import com.whiuk.philip.openmud.messages.Messages.GameMessageToServer.TextMessageToServer;
import com.whiuk.philip.openmud.messages.Messages.MessageToClient;
import com.whiuk.philip.openmud.messages.Messages.MessageToServer;
import com.whiuk.philip.openmud.messages.Messages.MessageType;
import com.whiuk.philip.openmud.messages.Messages.AuthMessageToServer.AuthMessageToServerType;
import com.whiuk.philip.openmud.messages.Messages.AuthMessageToServer.LoginMessage;

import static com.whiuk.philip.openmud.Constants.MAP_AREA_SIZE;

@SuppressWarnings("serial")
public class Client extends JFrame {
	private static final Logger logger = Logger.getLogger(Client.class);
	
	private final ClientGameThread gameThread;
	private NetworkReceiverThread networkReceiverThread;

	private final String address;
	private final int port;
	
	private JPanel loadingPanel;
	
	private JPanel loginPanel;
	private JTextField usernameInput;
	private JLabel usernameLabel;
	private JPasswordField passwordInput;
	private JLabel passwordLabel;
	private JButton loginButton;

	private JTextField textInput;
	private JScrollPane textOutputAreaScroll;
	private JTextArea textOutputArea;
	private GameCanvas gameCanvas;
	private GameState gameState;

	private Socket socket;
	private BufferedInputStream inputStream;
	private BufferedOutputStream outputStream;
	private volatile boolean running = true;
	
	private class MapArea {
		private String name;
		private Tile[][] tiles;
	}
	
	private class Tile {
		Color color;	
		
		public Tile(com.whiuk.philip.openmud.messages.Messages.GameMessageToClient.Tile tile) {
			switch(tile.getType()) {
			case FLOOR: this.color = Color.BLACK; break;
			case PATH: this.color = Color.GRAY; break;
			case GRASS: this.color = new Color(0, 64, 0); break;
			case GROUND: this.color = new Color(139, 69, 19); break;
			case ROCK: this.color = Color.DARK_GRAY; break;
			case TREE: this.color = new Color(0, 128, 0); break;
			case WATER: this.color = Color.BLUE; break;
			default: this.color = Color.PINK; break;	
			}
		}	
	}
	
	class GameState {
		boolean loading;
		boolean loggedIn;
		boolean loginFailed;
		String username;
		MapArea mapArea;
		LocationMessageToClient location;
		
		public void setLoading() {
			Client.this.setLoadingView();
		}
		
		public void handleLoggedIn(String username) {
			loggedIn = true;
			loginFailed = false;
			this.username = username;
			Client.this.setLoggedInView();
			gameCanvas.repaint();
		}
		public void handleLoginFailure() {
			loggedIn = false;
			loginFailed = true;
		}
		public void handleMapAreaData(MapAreaMessage mapAreaMessage) {
			MapArea mapArea = new MapArea();
			mapArea.name = mapAreaMessage.getName();
			mapArea.tiles = buildTiles(mapAreaMessage.getTilesList());
			this.mapArea = mapArea;
			gameCanvas.repaint();
		}

		private Tile[][] buildTiles(
				List<com.whiuk.philip.openmud.messages.Messages.GameMessageToClient.Tile> tilesList) {
			Tile[][] tiles = new Tile[MAP_AREA_SIZE][];
			int i = 0;
			for (int y = 0; y < MAP_AREA_SIZE; y++) {
				tiles[y] = new Tile[MAP_AREA_SIZE];
				for (int x = 0; x < MAP_AREA_SIZE; x++) {
					logger.info("Tile("+y+","+x+") - "+tilesList.get(i).getType());
					tiles[y][x] = new Tile(tilesList.get(i));
					i++;
				}
			}
			return tiles;
		}

		public void handleLocationUpdate(LocationMessageToClient location) {
			this.location = location;
		}
	}
	
	static final int PADDING = 5;
	
	class GameCanvas extends Canvas  {
		public void paint(Graphics g) {
			int width = this.getWidth();
			int height = this.getHeight();
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, width, height);
			
			int gameAreaSize = 0;
			//Determine Game Area Size
			if (width > height) {
				gameAreaSize = height-(PADDING*2);
				g.translate((width-height)/2, 0);
			} else {
				gameAreaSize = width-(PADDING*2);
				g.translate(0, (height-width)/2);
			}
			gameAreaSize = (gameAreaSize/MAP_AREA_SIZE)*MAP_AREA_SIZE;
			
			//Render Border
			g.setColor(Color.WHITE);
			g.drawRect(PADDING-1, PADDING-1, gameAreaSize+1, gameAreaSize+1);
			g.translate(PADDING, PADDING);
			
			//Draw Tiles
			if (gameState.mapArea != null) {
				for (int y = 0; y < gameState.mapArea.tiles.length; y++) {
					for (int x = 0; x < gameState.mapArea.tiles[y].length; x++) {
						g.setColor(gameState.mapArea.tiles[y][x].color);
						g.fillRect(x*(gameAreaSize/MAP_AREA_SIZE), y*(gameAreaSize/MAP_AREA_SIZE), 
								(gameAreaSize/MAP_AREA_SIZE), (gameAreaSize/MAP_AREA_SIZE));
					}
				}
			}
			
			//Draw Player
			if (gameState.location != null) {
				g.setColor(Color.WHITE);
				g.drawOval(gameState.location.getX()*(gameAreaSize/MAP_AREA_SIZE), 
						gameState.location.getY()*(gameAreaSize/MAP_AREA_SIZE),
						(gameAreaSize/MAP_AREA_SIZE), (gameAreaSize/MAP_AREA_SIZE));
			}
		}
	}

	public Client(String address, int port) throws UnknownHostException, IOException {
		super("OpenMUD Game Client");
		this.address = address;
		this.port = port;
		Dimension size = new Dimension(1024, 768);
		setPreferredSize(size);
		buildLoadingComponents();
		gameState = new GameState();
		gameState.setLoading();
		
		//TODO: Move this outside the constructor
		buildLoginComponents();
		buildGameComponents();
		setLayout(new BorderLayout());
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setVisible(true);
		gameThread = new ClientGameThread();
		gameThread.start();
	}
	
	private void setLoadingView() {
		this.getContentPane().removeAll();
		this.getContentPane().add(loadingPanel, BorderLayout.CENTER);
		this.revalidate();
	}
	
	private void setLoginView() {		
		this.getContentPane().removeAll();
		this.getContentPane().add(loginPanel, BorderLayout.CENTER);
		this.pack();
		this.revalidate();
	}
	
	private void setLoggedInView() {
		this.getContentPane().removeAll();
		this.getContentPane().add(gameCanvas, BorderLayout.NORTH);
		this.getContentPane().add(textOutputAreaScroll, BorderLayout.CENTER);
		this.getContentPane().add(textInput, BorderLayout.SOUTH);
		this.revalidate();
	}
	
	private void buildLoadingComponents() {		
		loadingPanel = new JPanel();
		JLabel loadingText = new JLabel("Loading OpenMUD!");
		loadingPanel.add(loadingText);
	}
	
	private void buildGameComponents() {
		gameCanvas = new GameCanvas();
		gameCanvas.setSize(1024, 520);

		textOutputArea = new JTextArea();
		textOutputArea.setEditable(false);
		textOutputArea.setMinimumSize(new Dimension(1000, 100));

		textOutputAreaScroll = new JScrollPane(textOutputArea);
		textOutputAreaScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

		textInput = new JTextField();
		textInput.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final JTextField source = ((JTextField) e.getSource());
				final String message = source.getText();
				source.setText("");
				try {
					MessageToServer.newBuilder()
							.setMessageType(MessageType.GAME)
							.setGame(GameMessageToServer.newBuilder()
									.setGameMessageType(GameMessageType.TEXT)
									.setText(TextMessageToServer.newBuilder().setText(message)))
							.build().writeDelimitedTo(outputStream);
					outputStream.flush();
				} catch (IOException ioex) {
					ioex.printStackTrace();
				}
			}
		});
	}
	
	private void buildLoginComponents() {
		loginPanel = new JPanel();
		JPanel loginFormPanel = new JPanel();
		usernameLabel = new JLabel("Username");
		passwordLabel = new JLabel("Password");
		usernameInput = new JTextField("");
		passwordInput = new JPasswordField("");
		loginButton = new JButton("Login");
		loginButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String username = usernameInput.getText();
				String password = passwordInput.getText();
				usernameInput.setText("");
				passwordInput.setText("");
				try {
					MessageToServer.newBuilder()
							.setMessageType(MessageType.AUTH)
							.setAuth(AuthMessageToServer.newBuilder()
									.setMessageType(AuthMessageToServerType.LOGIN)
									.setLogin(LoginMessage.newBuilder().setUsername(username).setPassword(password)))
							.build().writeDelimitedTo(outputStream);
					outputStream.flush();
				} catch (IOException ioex) {
					logger.warn("Error writing auth message", ioex);
				}
			}
		});
		
		GridLayout layout =  new GridLayout(0,2);
		loginFormPanel.setLayout(layout);
		loginFormPanel.add(usernameLabel);
		loginFormPanel.add(usernameInput);
		loginFormPanel.add(passwordLabel);
		loginFormPanel.add(passwordInput);
		loginFormPanel.add(loginButton);
		loginFormPanel.setPreferredSize(new Dimension(500, 300));
		loginPanel.setLayout(new BorderLayout());
		loginPanel.add(loginFormPanel, BorderLayout.CENTER);
	}
	
	private void setRunning(boolean isRunning) {
		running = false;
	}

	class ClientGameThread extends Thread {

		ClientGameThread() {
			super("GameThread");
		}
		
		@Override
		public void run() {
			boolean connected = false;
			while (!connected) {
				connected = attemptConnection();
			}
			try {
				outputStream = new BufferedOutputStream(socket.getOutputStream());
				inputStream = new BufferedInputStream(socket.getInputStream());
			} catch (IOException e) {
				logger.error("IO error while creating object streams", e);
			}
			networkReceiverThread = new NetworkReceiverThread();
			networkReceiverThread.start();
			Client.this.setLoginView();
		}
		
		private boolean attemptConnection() {
			try {
				logger.info("Connecting to " + address + ":" + port);
				socket = new Socket(address, port);
				logger.info("Connected to server");
				return true;
			} catch (IOException e) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
				}
				return false;
			}
		}
	}

	class NetworkReceiverThread extends Thread {
		public NetworkReceiverThread() {
			super("NetworkReceieverThread");
		}
		
		@Override
		public void run() {
			try {
				while (running) {
					logger.info("Running");
					MessageToClient message = Messages.MessageToClient.parseDelimitedFrom(inputStream);
					logger.info("Handling message");
					logger.info("Message type: "+message.getMessageType().toString());
					switch (message.getMessageType()) {
					case AUTH:
						switch(message.getAuth().getMessageType()) {
						case LOGIN_SUCCESS: 
							gameState.handleLoggedIn(message.getAuth().getLoginSuccess().getUsername());							
							break;
						case LOGIN_FAILURE:
							gameState.handleLoginFailure();
							break;
						}
						break;
					case GAME:
						logger.info("Game message type: "+message.getGame().getGameMessageType().toString());
						switch(message.getGame().getGameMessageType()) {
						case REFRESH:
						case MAP_AREA:
							gameState.handleMapAreaData(message.getGame().getMapArea());
							break;
						case LOCATION:
							gameState.handleLocationUpdate(message.getGame().getLocation());
						case TEXT:
							String input = message.getGame().getText().getText();
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									textOutputArea.append(input + System.lineSeparator());
								}
							});
							break;
						default:
							logger.info("Unsupported message type: "+message.getGame().getGameMessageType().toString());
							throw new UnsupportedOperationException(message.getGame().getGameMessageType().toString());
						}
						break;
					default:
						logger.info("Unsupported message type: "+message.getMessageType().toString());
						throw new UnsupportedOperationException(message.getMessageType().toString());
					}
				}
			} catch (Exception e) {
				setRunning(false);
			}
		}
	}
}
