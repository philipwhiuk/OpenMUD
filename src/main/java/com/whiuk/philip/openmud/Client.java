package com.whiuk.philip.openmud;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class Client extends JFrame {

	private final ClientGameThread gameThread;
	private NetworkReceiverThread networkReceiverThread;
	private final JTextArea textArea;

	private final String address;
	private final int port;

	private Socket socket;
	private ObjectInputStream inputStream;
	private ObjectOutputStream outputStream;
	private volatile boolean running = true;
	private JTextField input;
	private JScrollPane textAreaScroll;
	private Canvas canvas;

	public Client(String address, int port) throws UnknownHostException, IOException {
		super("OpenMUD Game Client");
		this.address = address;
		this.port = port;
		Dimension size = new Dimension(1024, 768);
		setPreferredSize(size);

		canvas = new Canvas() {
			public void paint(Graphics g) {
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, this.getWidth(), this.getHeight());
			}
		};
		canvas.setSize(1024, 520);

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
					outputStream.writeUTF(message);
					outputStream.flush();
				} catch (IOException ioex) {
					ioex.printStackTrace();
				}
			}
		});

		setLayout(new BorderLayout());
		this.getContentPane().add(canvas, BorderLayout.NORTH);
		this.getContentPane().add(textAreaScroll, BorderLayout.CENTER);
		this.getContentPane().add(input, BorderLayout.SOUTH);

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setVisible(true);

		gameThread = new ClientGameThread();
		gameThread.start();
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
				outputStream = new ObjectOutputStream(socket.getOutputStream());
				inputStream = new ObjectInputStream(socket.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
			networkReceiverThread = new NetworkReceiverThread();
			startPipes();
		}

		private void startPipes() {
			Client.this.networkReceiverThread.start();
		}
		
		private boolean attemptConnection() {
			try {
				System.out.println("Connecting to " + address + ":" + port);
				socket = new Socket(address, port);
				System.out.println("Connected to server");
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
					final String input = inputStream.readUTF();
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							textArea.append(input + System.lineSeparator());
							canvas.repaint();
						}
					});
				}
			} catch (Exception e) {
				setRunning(false);
			}
		}
	}
}
