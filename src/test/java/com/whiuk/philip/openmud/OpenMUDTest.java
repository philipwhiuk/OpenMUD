package com.whiuk.philip.openmud;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.junit.Test;

public class OpenMUDTest {

	private InputStream sendCommands(String[] commands) throws UnsupportedEncodingException {
		StringBuilder builder = new StringBuilder();
		for (String command: commands) {
			builder.append(command);
			builder.append(System.lineSeparator());
		}
		return new ByteArrayInputStream(builder.toString().getBytes("UTF-8"));
	}
	
	@Test
	public void canQuit() throws Exception {
		InputStream in = sendCommands(new String[]{"QUIT"});
		new OpenMUD(in, System.out);
	}
	
	@Test
	public void canMove() throws Exception {
		InputStream in = sendCommands(new String[]{"MOVE NORTH", "QUIT"});
		new OpenMUD(in, System.out);
	}
	
	@Test
	public void canTake() throws Exception {
		InputStream in = sendCommands(new String[]{"MOVE EAST", "TAKE AXE", "QUIT"});
		new OpenMUD(in, System.out);
	}
	
	@Test
	public void canChopTree() throws Exception {
		InputStream in = sendCommands(new String[]{"MOVE EAST", "TAKE AXE", "MOVE WEST", "MOVE NORTH", "USE AXE TREE", "QUIT"});
		new OpenMUD(in, System.out);
	}
	
	@Test
	public void canLightFire() throws Exception {
		InputStream in = sendCommands(new String[]{"MOVE EAST", "TAKE AXE", "MOVE WEST", "MOVE NORTH", "USE AXE TREE", "MOVE SOUTH", "DROP LOGS", "USE FLINT LOGS", "QUIT"});
		new OpenMUD(in, System.out);
	}
	
	@Test
	public void axeGoneAndFirePresent() throws Exception {
		InputStream in = sendCommands(new String[]{"MOVE EAST", "TAKE AXE", "MOVE WEST", "MOVE NORTH", "USE AXE TREE", "MOVE SOUTH", "DROP LOGS", "USE FLINT LOGS", "MOVE EAST", "MOVE WEST", "QUIT"});
		new OpenMUD(in, System.out);
	}
}
