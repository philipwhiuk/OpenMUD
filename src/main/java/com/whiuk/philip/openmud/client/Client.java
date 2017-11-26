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
import com.whiuk.philip.openmud.messages.Messages.GameMessageToClient.RoomMessage;
import com.whiuk.philip.openmud.messages.Messages.GameMessageToServer;
import com.whiuk.philip.openmud.messages.Messages.GameMessageToServer.GameMessageType;
import com.whiuk.philip.openmud.messages.Messages.GameMessageToServer.TextMessageToServer;
import com.whiuk.philip.openmud.messages.Messages.MessageToClient;
import com.whiuk.philip.openmud.messages.Messages.MessageToServer;
import com.whiuk.philip.openmud.messages.Messages.MessageType;
import com.whiuk.philip.openmud.messages.Messages.AuthMessageToServer.AuthMessageToServerType;
import com.whiuk.philip.openmud.messages.Messages.AuthMessageToServer.LoginMessage;

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
	}
	
	class GameState {
		boolean loading;
		boolean loggedIn;
		boolean loginFailed;
		String username;
		MapArea room;
		
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
		public void handleRoomData(RoomMessage room2) {
			MapArea room = new MapArea();
			room.name = room2.getName();
			this.room = room;
			gameCanvas.repaint();
		}
	}
	
	class GameCanvas extends Canvas  {
		public void paint(Graphics g) {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
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
					ioex.printStackTrace();
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
						case ROOM:
							gameState.handleRoomData(message.getGame().getRoom());
							break;
						case TEXT:
							logger.info("Text: "+message.getGame().getText().getText());
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
