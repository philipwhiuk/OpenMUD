package com.whiuk.philip.openmud;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;

import org.apache.log4j.Logger;

import com.whiuk.philip.openmud.server.Server;
import com.whiuk.philip.openmud.client.Client;

/**
 * This is really just a convenient entry-point to spin 
 * up a local client and server with some fairly basic debugging stuff.
 */
public class OpenMUD {	
	public static Logger logger = Logger.getLogger(OpenMUD.class);
	
	public static void main(String[] args) throws Exception {
		setupExceptionLogging();
		startServer();
		startClients(1);
	}	
	
	private static void setupExceptionLogging() {
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				logger.fatal("Unhandled exception", e);
			}
		});
	}
	
	private static void startServer() {
		new Thread(new Runnable() {
			public void run() {
				try {
					new Server(1750);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	private static void startClients(int count) {
		for (int i = 0; i < count; i++) {
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
