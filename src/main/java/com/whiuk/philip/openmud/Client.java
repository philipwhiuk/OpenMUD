package com.whiuk.philip.openmud;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {

	private Socket socket;
	private ObjectInputStream inputStream;
	private ObjectOutputStream outputStream;
	private volatile boolean running = true; 
	
	public Client(String address, int port) throws UnknownHostException, IOException {
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

		new Thread(new Runnable() {
			public void run() {
				try {
					Scanner scanner = new Scanner(System.in);
					while(running) {
						String s = scanner.nextLine();
						outputStream.writeUTF(s);
						outputStream.flush();
					}
					scanner.close();
				} catch (Exception e) {
					setRunning(false);
				}
			}
		}).start();
		
		new Thread(new Runnable() {
			public void run() {
				try {
					PrintWriter writer = new PrintWriter(System.out);
					while(running) {
						writer.println(inputStream.readUTF());
						writer.flush();
					}
				} catch (Exception e) {
					setRunning(false);
				}
			}
		}).start();
	}

	private void setRunning(boolean isRunning) {
		running = false;
	}
	
}
