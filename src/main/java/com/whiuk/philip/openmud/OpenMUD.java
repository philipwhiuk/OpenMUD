package com.whiuk.philip.openmud;

import java.io.IOException;

import com.whiuk.philip.openmud.server.Server;

public class OpenMUD {	
	public static void main(String[] args) throws Exception {
		new Thread(new Runnable() {
			public void run() {
				try {
					new Server(1750);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
		for (int i = 0; i < 3; i++) {
			new Thread(new Runnable() {
				public void run() {
					try {
						new Client("127.0.0.1", 1750);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}	
}
