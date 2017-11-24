package com.whiuk.philip.openmud;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

public class Client extends JFrame {

	private final ClientGameThread gameThread;
	private final JTextArea textArea; 
	
	private final String address;
	private final int port;
	
	private Socket socket;
	private ObjectInputStream inputStream;
	private ObjectOutputStream outputStream;
	private volatile boolean running = true;
	private JTextField input;
	private JScrollPane textAreaScroll;

	
	public Client(String address, int port) throws UnknownHostException, IOException {
		super("OpenMUD Game Client");
		this.address = address;
		this.port = port;
		
		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setMinimumSize(new Dimension(640,480));
		
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
		this.getContentPane().add(textAreaScroll, BorderLayout.CENTER);
		this.getContentPane().add(input, BorderLayout.SOUTH);

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setVisible(true);


		gameThread = new ClientGameThread();
		gameThread.start();
	}
	
	class ClientGameThread extends Thread {
		
		ClientGameThread() throws IOException {
			super("GameThread");
			boolean connected = false;
			while (!connected) {
				try {
					System.out.println("Connecting to "+address+":"+port);
					socket = new Socket(address, port);
					System.out.println("Connected to server");
					connected = true;
				} catch(IOException e) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
					}
				}
			}

			outputStream = new ObjectOutputStream(socket.getOutputStream());
			inputStream = new ObjectInputStream(socket.getInputStream());
			
			startPipes();
		}
		
		public void startPipes() {			
			new Thread(new Runnable() {
				public void run() {
					try {
						while(running) {
							final String input = inputStream.readUTF();
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									textArea.append(input+System.lineSeparator());
								}
							});
						}
					} catch (Exception e) {
						setRunning(false);
					}
				}
			}).start();
		}
	}

	private void setRunning(boolean isRunning) {
		running = false;
	}
	
}
