package com.whiuk.philip.openmud;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;

import org.apache.log4j.Logger;

import com.whiuk.philip.openmud.server.Server;
import com.whiuk.philip.openmud.client.Client;

public class OpenMUD {	
	public static Logger logger = Logger.getLogger(OpenMUD.class);
	
	public static void main(String[] args) throws Exception {
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread t, Throwable e) {
				logger.fatal("Unhandled exception", e);
			}
			
		});
		new Thread(new Runnable() {
			public void run() {
				try {
					new Server(1750);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
		for (int i = 0; i < 1; i++) {
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
