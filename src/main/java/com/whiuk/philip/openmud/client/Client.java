package com.whiuk.philip.openmud.client;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import com.whiuk.philip.openmud.messages.Messages;
import com.whiuk.philip.openmud.messages.Messages.GameMessageToClient.RoomMessage;
import com.whiuk.philip.openmud.messages.Messages.GameMessageToServer;
import com.whiuk.philip.openmud.messages.Messages.GameMessageToServer.GameMessageType;
import com.whiuk.philip.openmud.messages.Messages.GameMessageToServer.TextMessageToServer;
import com.whiuk.philip.openmud.messages.Messages.MessageToClient;
import com.whiuk.philip.openmud.messages.Messages.MessageToServer;
import com.whiuk.philip.openmud.messages.Messages.MessageType;

@SuppressWarnings("serial")
public class Client extends JFrame {
	private static final Logger logger = Logger.getLogger(Client.class);
	
	private final ClientGameThread gameThread;
	private NetworkReceiverThread networkReceiverThread;
	private JTextArea textArea;

	private final String address;
	private final int port;

	private Socket socket;
	private BufferedInputStream inputStream;
	private BufferedOutputStream outputStream;
	private volatile boolean running = true;
	private JTextField input;
	private JScrollPane textAreaScroll;
	private GameCanvas gameCanvas;
	private GameState gameState;
	
	private class MapArea {
		private String name;
		private Tile[][] tiles;
	}
	
	private class Tile {
		Color color;		
	}
	
	class GameState {
		boolean loggedIn;
		boolean loginFailed;
		String username;
		MapArea room;
		
		public void handleLoggedIn(String username) {
			loggedIn = true;
			loginFailed = false;
			this.username = username;
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
		buildComponents();
		setLayout(new BorderLayout());
		this.getContentPane().add(gameCanvas, BorderLayout.NORTH);
		this.getContentPane().add(textAreaScroll, BorderLayout.CENTER);
		this.getContentPane().add(input, BorderLayout.SOUTH);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setVisible(true);
		gameThread = new ClientGameThread();
		gameThread.start();
	}
	
	private void buildComponents() {
		gameCanvas = new GameCanvas();
		gameCanvas.setSize(1024, 520);

		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setMinimumSize(new Dimension(1000, 100));

		textAreaScroll = new JScrollPane(textArea);
		textAreaScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

		input = new JTextField();
		input.addActionListener(new ActionListener() {
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
			startPipes();
		}

		private void startPipes() {
			Client.this.networkReceiverThread.start();
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
					MessageToClient message = Messages.MessageToClient.parseDelimitedFrom(inputStream);
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
						switch(message.getGame().getGameMessageType()) {
						case ROOM:
							gameState.handleRoomData(message.getGame().getRoom());
							break;
						case TEXT:
							String input = message.getGame().getText().getText();
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									textArea.append(input + System.lineSeparator());
								}
							});
							break;
						default:
							throw new UnsupportedOperationException(message.getGame().getGameMessageType().toString());
						}
						break;
					default:
						throw new UnsupportedOperationException(message.getMessageType().toString());
					}
				}
			} catch (Exception e) {
				setRunning(false);
			}
		}
	}
}
