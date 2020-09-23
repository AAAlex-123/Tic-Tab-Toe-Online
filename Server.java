package ttt_online;

import java.awt.Color;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Server-side application to handle communications with the clients
 */
public class Server {
	private static final int GAME_PORT = 10001;
	public static final int DATA = 0;
	public static final int MOVE = 1;
	public static final int TEXT = 2;
	public static final int CHAT = 3;

	private final int playerCount;
	private final boolean printStackTrace;

	private ServerSocket server;
	private final Socket[] sockets;
	private final ObjectInputStream[] inputs;
	private final ObjectOutputStream[] outputs;

	private final char[] symbols;
	private final Color[] colors;

	private final GameBoard gameBoard = new GameBoard();
	private int currentPlayer = 0;

	/**
	 * Constructor to initialise fields
	 * 
	 * @param playerCount     int, the number of players
	 * @param printStackTrace boolean, whether or not to print full Stack Trace when
	 *                        exceptions occur
	 */
	public Server(int playerCount, boolean printStackTrace) {
		this.printStackTrace = printStackTrace;
		this.playerCount = playerCount;
		sockets = new Socket[playerCount];
		inputs = new ObjectInputStream[playerCount];
		outputs = new ObjectOutputStream[playerCount];
		symbols = new char[playerCount];
		colors = new Color[playerCount];
	}

	/**
	 * Main method that calls other methods to actually run the server
	 * 
	 * @see Server#initialiseServer() initialiseServer()
	 * @see Server#getConnections2() getConnections2()
	 * @see Server#makeTurn() makeTurn()
	 */
	private void run() {
		initialiseServer();
		getConnections();
		log("Starting game");
		while (true) {
			makeTurn();
		}
	}

	/**
	 * Initialises the server on port <code>GAME_PORT</code> with <code>playerCount</code> connections.
	 */
	private void initialiseServer() {
		try {
			server = new ServerSocket(GAME_PORT, playerCount);
			log("\n\nServer ready, waiting for %d players", playerCount);
		} catch (IOException e) {
			logerr("Error while setting up server");
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
	@SuppressWarnings("unchecked")
	private void getConnections() {
		boolean reset = false;
		try {
			// connect to every player
			for (int i = 0; i < playerCount; i++) {
				// get connections
				sockets[i] = server.accept();
				inputs[i] = new ObjectInputStream(sockets[i].getInputStream());
				outputs[i] = new ObjectOutputStream(sockets[i].getOutputStream());

				// exchange send ack message
				outputs[i].writeObject(new Packet<String>(TEXT,
						String.format("Hi player #%d, you're now connected.\nPlease wait for others to join\n", i)));

				// get player symbol
				symbols[i] = ((Packet<Character>) inputs[i].readObject()).value;
				colors[i] = ((Packet<Color>) inputs[i].readObject()).value;

				log("Player #%d connected", i);
			}

			// send ready message and symbol and color array
			for (int j = 0; j < playerCount; j++) {
				outputs[j].writeObject(new Packet<String>(TEXT, "Everyone has joined; get ready to start the game!"));
				outputs[j].writeObject(new Packet<char[]>(DATA, symbols));
				outputs[j].writeObject(new Packet<Color[]>(DATA, colors));
			}

		} catch (IOException e) {
			logerr("IOException inside getConnections() while getting connections or sending messages\nResetting...");
			if (printStackTrace) e.printStackTrace();
			reset = true;
		} catch (ClassNotFoundException e) {
			logerr("ClassNotFoundException inside getConnections() while getting player symbol\nResetting...");
			if (printStackTrace) e.printStackTrace();
			reset = true;
		}

		if (reset) {
			reset();
			run();
		}
	}

	/**
	 * Executes one turn of a player.
	 * <ul>
	 * <li>Sends 'ok' to start
	 * <li>Sends the board
	 * <li>Receives the move
	 * <li>Sends acknowledgement for the move
	 * <li>Resends board
	 */
	@SuppressWarnings({ "rawtypes" })
	private void makeTurn() {
		boolean reset = false;
		log("\nPlayer #%d starts their turn", currentPlayer);
		try {
			log("Sent board:\n%s", gameBoard);
			int move;
			String response;

			// send ok to start
			outputs[currentPlayer].writeObject(new Packet<String>(TEXT, "Make your move!"));

			// send board
			sendBoard(currentPlayer);

			// get, register and respond to move
			Packet p = (Packet) inputs[currentPlayer].readObject();
			log("Just got '%s'", p.value);
			if (p.attribute == Server.MOVE) {
				move = (int) p.value;
			} else {
				logerr("Server received wrong packet; please close and inform the developers");
				while (true) {;}
			}

			if (move == -2) {
				log("Final board:\n%s", gameBoard);
				broadcast("Player '%c' resigned", symbols[currentPlayer]);
				log("Player '%c' resigned!\nGame over", symbols[currentPlayer]);
				for (int i = 0; i < playerCount; i++) {
					sendBoard(i);
				}

				log("Server will now reset");
				reset();
				run();
			}

			gameBoard.markSquare(move, symbols[currentPlayer]);
			if (gameBoard.hasWon()) {
				log("Final board:\n%s", gameBoard);
				broadcast("Player '%c' won!", symbols[currentPlayer]);
				log("Player '%c' won!\nGame over", symbols[currentPlayer]);
				for (int i = 0; i < playerCount; i++) {
					sendBoard(i);
				}

				log("Server will now reset");
				reset();
				run();
			}

			// send response to move
			response = (String.format("Move received: [%c, %d]", 65 + move / 10, move % 10 + 1));
			outputs[currentPlayer].writeObject(new Packet<String>(TEXT, response));

			log("Move received: %d, response sent %s", move, response);

			// send board again
			sendBoard(currentPlayer);

			currentPlayer = (currentPlayer + 1) % playerCount;

		} catch (SocketException e) {
			logerr("SocketException inside makeTurn()\nResetting...");
			if (printStackTrace) e.printStackTrace();
			reset = true;
		} catch (IOException e) {
			logerr("IOException inside makeTurn() while sending/receiving data\nResetting...");
			if (printStackTrace) e.printStackTrace();
			reset = true;
		} catch (ClassNotFoundException e) {
			logerr("ClassNotFoundException inside makeTurn() while getting move\nResetting...");
			if (printStackTrace) e.printStackTrace();
			reset = true;
		}

		if (reset) {
			reset();
			run();
		}
	}

	/**
	 * Resets everything in case something goes wrong while getting connections.<br>
	 * Closes connections and empties <code>symbols</code> array.
	 */
	private void reset() {
		gameBoard.clear();
		try {
			server.close();
		} catch (IOException e1) {
			logerr("IOException while closing server while reset");
			e1.printStackTrace();
		}
		for (int i = 0; i < playerCount; i++) {
			try {
				sockets[i].close();
				inputs[i].close();
				outputs[i].close();
				symbols[i] = '\u0000';
				colors[i] = new Color(0, 0, 0);
			} catch (IOException e) {
				logerr("Error while resetting %d; player disconnected\n", i);
			}
		}

		log("Done resetting");
	}

	/**
	 * Sends the board to the currentPlayer.<br>
	 * Creates a copy of the GameBoard's board,<br>
	 * and sends that copy because Java (:
	 * 
	 * @throws SocketException Thrown if client disconnects while trying to send
	 *                         board
	 */
	private void sendBoard(int currentPlayer) throws SocketException {
		char[][] newBoard = new char[5][5];
		char[][] currentBoard = gameBoard.getBoard();
		for (int i = 0; i < 5; i++)
			for (int j = 0; j < 5; j++)
				newBoard[i][j] = currentBoard[i][j];
		try {
			outputs[currentPlayer].writeObject(new Packet<char[][]>(DATA, newBoard));
		} catch (IOException e) {
			if (printStackTrace) e.printStackTrace();
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
				outputs[i].writeObject(new Packet<String>(TEXT, String.format(msg, args)));
			} catch (IOException e) {
				logerr("Error while broadcasting");
				if (printStackTrace) e.printStackTrace();
			}
		}
		
		log("Broadcasted: %s", String.format(msg, args));
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
	 * Main method. Run to create and run a server
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

		Server server = new Server(playerCount, printStackTrace);
		server.run();
	}
}
