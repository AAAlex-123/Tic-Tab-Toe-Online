package ttt_online;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Server-side application to handle chat between players.
 */
public class ChatServer {
	private static final int CHAT_PORT = 10002;

	private final int playerCount;
	private final boolean printStackTrace;

	private ServerSocket chatServer;
	private final ObjectOutputStream[] outputs;

	/**
	 * Keeps track of the running threads to know when another connection can be
	 * accepted.<br>
	 * When one is created, give it a slot. When one is stopped, free up its slot.
	 * <p>
	 * No it's not spaghetti, yes we need it to keep track of which <code>outputs</code> indexes
	 * are empty and can have an Output Stream
	 */
	private final boolean[] available;

	/**
	 * Constructs the Chat Server object.
	 * 
	 * @param playerCount     int, the maximum number of connections allowed.
	 * @param printStackTrace boolean, whether or not to print stack trace when
	 *                        Exceptions occur.
	 */
	public ChatServer(int playerCount, boolean printStackTrace) {
		this.printStackTrace = printStackTrace;
		this.playerCount = playerCount;
		this.available = new boolean[playerCount];
		outputs = new ObjectOutputStream[playerCount];
		for (int i = 0; i < playerCount; i++)
			available[i] = true;
	}

	/**
	 * Runs the Chat Server
	 */
	private void run() {
		initialiseServer();
		getConnections();
	}

	/**
	 * Initialises the Chat Server on port <code>CHAT_PORT</code> with
	 * <code>playerCount</code> total possible connections.
	 */
	private void initialiseServer() {
		try {
			chatServer = new ServerSocket(CHAT_PORT);
			log("\n\nChat Server ready, listening for up to %d players", playerCount);
		} catch (IOException e) {
			logerr("IOException in initialiseServer()");
			if (printStackTrace)
				e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Every 2 seconds check if there is an <code>available</code> slot to get a
	 * connection. If there is, accept a connection and assign it this slot then
	 * start a new <code>ChatServerThread</code> handing it the accepted Socket.
	 * 
	 * @see ChatServerThread
	 * @see ChatServer#getAvailable() getAvailable()
	 * @see ChatServer#available available
	 */
	private void getConnections() {
		try {
			int index = -1;
			Socket chatConnection = null;
			while (true) {
				// wait until there is an available slot to accept a connection
				index = getAvailable();
				if (index != -1) {
					chatConnection = chatServer.accept();
				} else {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						;
					}
					continue;
				}

				log("accepted #%d", index);

				outputs[index] = new ObjectOutputStream(chatConnection.getOutputStream());

				// exchange send ack message
				outputs[index].writeObject(new Packet<String>(Server.TEXT,
						String.format("Hi player #%d, you're now connected.\nYou can start chatting.", index)));

				log("\nChat Connection #%d established", index);

				new ChatServerThread(chatConnection, index).start();
				index++;
			}
		} catch (IOException e) {
			logerr("IOException in getConnections()\nidkwhatishappeningplshelp...");
			if (printStackTrace)
				e.printStackTrace();
		}
	}

	/**
	 * Returns the first slot of the <code>available</code> that is available to get
	 * a connection and sets it to <code>false</code>.
	 * 
	 * @return int, the available slot, -1 when no slot is available.
	 * @see ChatServer#available available
	 */
	private int getAvailable() {
		for (int i = 0; i < playerCount; i++)
			if (available[i]) {
				available[i] = false;
				return i;
			}
		return -1;
	}

	/**
	 * Private inner class that listens to a specific client's Output Stream<br>
	 * and upon receiving a message, broadcasts it to every client connected to the
	 * Chat Server.
	 * <p>
	 * When an Exception occurs, this Thread terminates execution,<br>
	 * the client's Output Stream is closed and the <code>available</code> slot<br>
	 * specified by <code>index</code> is freed up.
	 *
	 * @see ChatServer#broadcast(String, Object[]) broadcast()
	 * @see ChatServer#closeOutputStream(int) closeOutputStream()
	 * @see ChatServer#available available
	 */
	private class ChatServerThread extends Thread {

		private ObjectInputStream input;
		private final int index;

		/**
		 * Constructs the Thread to listen to <code>socket</code>.
		 * 
		 * @param socket Socket, the socket this Threads listens to
		 * @param count  int, used to keep track the slot this thread occupies in the
		 *               <code>available</code> table
		 */
		public ChatServerThread(Socket socket, int count) {
			this.index = count;
			try {
				this.input = new ObjectInputStream(socket.getInputStream());
			} catch (IOException e) {
				logerr("IOException in ChatServerThread()\nidkwhatishappeningplshelp...");
				if (printStackTrace)
					e.printStackTrace();
			}
		}

		/**
		 * Runs the thread
		 */
		@SuppressWarnings("rawtypes")
		public void run() {
			log("Thread #%d started", index);
			// also remove the following line
			Packet p;
			while (true) {
				try {
					// replace lines 149-155 with this when removing packets (haven't tested it (: )
					/*
					 * String msg = (String) input.readObject() log("Thread #%d received: '%s'",
					 * index, msg); broadcast(msg);
					 */
					p = (Packet) input.readObject();
					log("Thread #%d received: '%s'", index, p.value);
					if (p.attribute == Server.CHAT) {
						broadcast((String) p.value);
					} else {
						logerr("WhatTheFuckIsThisMessageException in ChatServerThread.run()");
					}
				} catch (SocketException e) {
					logerr("SocketException in ChatServerThread.run(); connection #%d closed by user\n", index);
					if (printStackTrace)
						e.printStackTrace();
					closeOutputStream(index);
					break;
				} catch (IOException e) {
					logerr("IOException in ChatServerThread.run()\nidkwhatishappeningplshelp...");
					if (printStackTrace)
						e.printStackTrace();
					closeOutputStream(index);
					break;
				} catch (ClassNotFoundException e) {
					logerr("ClassNotFoundException in ChatServerThread.run()\nidkwhatishappeningplshelp...");
					if (printStackTrace)
						e.printStackTrace();
					closeOutputStream(index);
					break;
				}
			}
		}
	}

	/**
	 * Closes the Output Stream bound to client #<code>index</code><br>
	 * and frees up the <code>available</code> slot this Thread occupied (specified
	 * by <code>index</code>).
	 * 
	 * @param index int, the index of the client
	 * @see ChatServer#available available
	 */
	private void closeOutputStream(int index) {
		log("Closing thread #%d", index);
		try {
			outputs[index].close();
			outputs[index] = null;
			available[index] = true;
		} catch (IOException e) {
			logerr("IOException in closeOutputStream()");
			if (printStackTrace)
				e.printStackTrace();
		}
	}

	/**
	 * Sends <code>message</code> to every client connected.<br>
	 * Uses <code>Sting.format(format, args)</code> to format <code>message</code>
	 * with <code>args</code>.
	 * 
	 * @param message String, message to send
	 * @param args    Object[], arguments
	 */
	private void broadcast(String message, Object... args) {
		for (int i = 0; i < playerCount; i++) {
			try {
				outputs[i].writeObject(new Packet<String>(Server.CHAT, String.format(message, args)));
			} catch (IOException e) {
				logerr("IOException in broadcast()");
				if (printStackTrace)
					e.printStackTrace();
			} catch (NullPointerException e) {
				;
			}
		}
	}

	/**
	 * Same as <code>System.out.printf(text, args)</code>
	 * 
	 * @param text String, text to send
	 * @param args Object[], arguments
	 */
	private static void log(String text, Object... args) {
		System.out.printf(text + "\n", args);
	}

	/**
	 * Same as <code>System.err.printf(text, args)</code>
	 * 
	 * @param text String, text to send
	 * @param args Object[], arguments
	 */
	private static void logerr(String text, Object... args) {
		System.err.printf(text + "\n", args);
	}

	/**
	 * Main method. Run to create and run a chat server
	 * 
	 * @param args args[0] is used for the maximum number of players allowed to
	 *             connect
	 * @param args args[1] is used to determine <code>printStackTrace</code> field,
	 *             '1' for true, other for false
	 */
	public static void main(String[] args) {
		int playerCount;
		boolean printStackTrace;

		try {
			playerCount = Integer.parseInt(args[0]);
		} catch (ArrayIndexOutOfBoundsException e) {
			logerr("Warning: No <player_count> argument provided;\nInitialised to 2");
			playerCount = 2;
		} catch (NumberFormatException e) {
			logerr("Warning: <player_count> argument has illegal format;\nInitialised to 2");
			playerCount = 2;
		}

		try {
			printStackTrace = args[1].equals("1") ? true : false;
		} catch (ArrayIndexOutOfBoundsException e) {
			logerr("Warning: No <printStackTrace> argument provided;\nInitialised to false;");
			printStackTrace = false;
		}

		ChatServer server = new ChatServer(playerCount, printStackTrace);
		server.run();
	}
}
