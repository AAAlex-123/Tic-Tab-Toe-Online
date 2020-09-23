package ttt_online;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Server-side application to handle communications with the clients
 */
public class ChatServer {
	private static final int CHAT_PORT = 10002;

	private final int playerCount;
	private final boolean printStackTrace;

	private ServerSocket chatServer;
	private final ObjectOutputStream[] outputs;

	public ChatServer(int playerCount, boolean printStackTrace) {
		this.printStackTrace = printStackTrace;
		this.playerCount = playerCount;
		outputs = new ObjectOutputStream[playerCount];
	}

	private void run() {
		initialiseServer();
		getConnections();
		log("all set");
	}

	/**
	 * Initialises the server on port 12345 with <code>playerCount</code> of connections.
	 */
	private void initialiseServer() {
		try {
			chatServer = new ServerSocket(CHAT_PORT, playerCount);
			log("!chatServer! \n\nChatServer ready, waiting for %d players", playerCount);
		} catch (IOException e) {
			logerr("!chatServer! Error while setting up chatserver");
			if (printStackTrace) e.printStackTrace();
		}
	}

	/**
	 * Initialises <code>playerCount</code> connections.<br>
	 * Gets their input and output streams.<br>
	 * Exchanges some messages.<br>
	 * <br>
	 * If at any point something goes wrong, reset server :)
	 */
	private void getConnections() {
//		boolean reset = false;
		try {
			// connect to every player
			for (int i = 0; i < playerCount; i++) {
				log("!chatServer! waiting for #%d", i);

				Socket chatConnection = chatServer.accept();

				outputs[i] = new ObjectOutputStream(chatConnection.getOutputStream());

				// exchange send ack message
				outputs[i].writeObject(new Packet<String>(Server.TEXT,
						String.format("Hi player #%d, you're now connected.\nPlease wait for others to join\n", i)));

				log("!chatServer! chatPlayer #%d connected", i);

				new ChatServerThread(chatConnection, i).start();
			}
		} catch (IOException e) {
			logerr("!chatServer! IOException inside getConnections() while getting connections or sending messages\nResetting...");
			if (printStackTrace) e.printStackTrace();
//			reset = true;
		}
	}

	private class ChatServerThread extends Thread {

		private ObjectInputStream input;
		private final int count;

		public ChatServerThread(Socket socket, int count) {
			this.count = count;
			try {
				this.input = new ObjectInputStream(socket.getInputStream());
			} catch (IOException e) {
				logerr("!chatServer! IOException inside ChatServerThread() while getting connection\nResetting...");
				if (printStackTrace) e.printStackTrace();
			}
		}

		@SuppressWarnings("rawtypes")
		public void run() {
			log("!chatServer! thread #%d started", count);
			Packet p;
			while (true) {
				try {
					p = (Packet) input.readObject();
					log("!chatServer! tread #%d received '%s'", count, p.value);
					if (p.attribute == Server.CHAT) {
						broadcast((String) p.value);
					} else {
						logerr("!chatServer! non-chat message received from chat");
					}
				} catch (SocketException e) {
					logerr("!chatServer! SocketException inside run() while receiving message\nResetting...");
					if (printStackTrace) e.printStackTrace();
					break;
				} catch (IOException e) {
					logerr("!chatServer! IOException inside run() while sending or receiving messages\nResetting...");
					if (printStackTrace) e.printStackTrace();
				} catch (ClassNotFoundException e) {
					logerr("!chatServer! ClassNotFoundException inside run() while receiving message\nResetting...");
					if (printStackTrace) e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Sends message <code>msg</code> to every client connected<br>
	 * <code>System.out.printf(text, args)</code>
	 * 
	 * @param boolean isServer, whether the message was sent by the server
	 * @param text    String, text to send
	 * @param args    Object[], arguments
	 */
	private void broadcast(String msg, Object... args) {
		for (int i = 0; i < playerCount; i++) {
			try {
				outputs[i].writeObject(new Packet<String>(Server.CHAT, String.format(msg, args)));
			} catch (IOException e) {
				logerr("Error while broadcasting");
				if (printStackTrace)
					e.printStackTrace();
			}
		}
		log("!chatServer! Broadcasted: %s", String.format(msg, args));
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
	 * @param args args[0] is used for the number of players expected to connect
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
