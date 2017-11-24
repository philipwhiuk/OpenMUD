package com.whiuk.philip.openmud;

import java.io.IOException;

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
