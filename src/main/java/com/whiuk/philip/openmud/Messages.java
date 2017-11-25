package com.whiuk.philip.openmud;

public final class Messages {
	public static final class ToServer {
		public static final byte LOGIN = 1;
		public static final byte LOGOUT = 2;
		public static final byte MOVE = 3;
		public static final byte TEXT = 4;
	}
	public static final class FromServer {
		public static final byte LOGIN_SUCCESS = 1;
		public static final byte LOGIN_FAILURE = 2;
		public static final byte LOGGED_OUT = 3;
		public static final byte ROOM = 3;
		public static final byte TEXT = 4;
	}
}
