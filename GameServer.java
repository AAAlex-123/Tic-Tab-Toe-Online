package ttt_online;

import java.awt.Color;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Server-side application to handle communications with the clients
 */
public class GameServer extends Server {
	// port of the Game Server
	private static final int GAME_PORT = 10001;

	private final Socket[] sockets;

	private final char[] symbols;
	private final Color[] colors;

	private final GameBoard gameBoard;
	private int currentPlayer = 0;

	/**
	 * Constructor to initialise fields.
	 * 
	 * @see Server#Server() Server()
	 */
	public GameServer() {
		super();
		sockets = new Socket[playerCount];
		symbols = new char[playerCount];
		colors = new Color[playerCount];
		gameBoard = new GameBoard();
	}

	/**
	 * Main method that calls other methods to actually run the server
	 * 
	 * @see GameServer#initialiseServer() initialiseServer()
	 * @see GameServer#getConnections() getConnections()
	 * @see GameServer#makeTurn() makeTurn()
	 */
	protected void run() {
		initialiseServer();
		getConnections();

		log("Starting game");
		while (true) {
			makeTurn();
		}
	}

	/**
	 * Initialises the server on port <code>GAME_PORT</code> with
	 * <code>playerCount</code> total connections.
	 */
	protected void initialiseServer() {
		try {
			server = new ServerSocket(GAME_PORT, playerCount);
			log("\n\nGame Server ready, waiting for %d players", playerCount);
		} catch (BindException e) {
			logerr("BindException while setting up server; a server is already running on this port; please exit");
			if (printStackTrace)
				e.printStackTrace();
			while (true) {
				;
			}
		} catch (IOException e) {
			logerr("IOException while setting up server; if you don't know why this happened, please inform the developers");
			if (printStackTrace)
				e.printStackTrace();
		}
	}

	/**
	 * Initialises <code>playerCount</code> connections.<br>
	 * Gets their input and output streams.<br>
	 * Exchanges some messages.<br>
	 * <br>
	 * If two or more players have the same symbol, the server allocates them
	 * special unique chess pieces and the clients interpret the message
	 * accordingly.<br>
	 * <br>
	 * If at any point something goes wrong, reset server :)
	 * 
	 * @see GameServer#reset() reset()
	 */
	protected void getConnections() {
		boolean reset = false;
		try {
			// connect to every player
			for (int i = 0; i < playerCount; i++) {
				// get connections
				sockets[i] = server.accept();
				inputs[i] = new ObjectInputStream(sockets[i].getInputStream());
				outputs[i] = new ObjectOutputStream(sockets[i].getOutputStream());

				// exchange send ack message
				outputs[i].writeObject(
						String.format("Hi player #%d, you're now connected.\nPlease wait for others to join\n", i));

				// get player symbol
				symbols[i] = (char) inputs[i].readObject();
				colors[i] = (Color) inputs[i].readObject();

				log("\nPlayer #%d connected", i);
			}

			// array of chess piece characters used to replace duplicates
			ArrayList<Character> chessPieces = new ArrayList<Character>(
					Arrays.asList('\u2654', '\u2655', '\u2656', '\u2657', '\u2658'));
			char[] found = new char[playerCount];
			int foundIndex = 0;

			// check if there are duplicates and if there are, put chess piece
			// for some reason Arrays.asList(found).contains(symols[i]) doesn't work so
			// nested for loops
			for (int i = 0; i < playerCount; i++) {
				for (int j = 0; j < playerCount; j++) {
					if (found[j] == symbols[i]) {
						// if there is a duplicate, replace it with a random piece from chessPieces
						char chessPiece = chessPieces
								.remove(ThreadLocalRandom.current().nextInt(0, chessPieces.size()));
						log("Duplicate found '%c', replaced with '\\u%04x'", symbols[i], (int) chessPiece);
						symbols[i] = chessPiece;
						break;
					}
				}
				found[foundIndex++] = symbols[i];
			}

			// send ready message and symbol and color array
			for (int j = 0; j < playerCount; j++) {
				outputs[j].writeObject("Everyone has joined; get ready to start the game!");
				outputs[j].writeObject(symbols);
				outputs[j].writeObject(colors);
			}

		} catch (IOException e) {
			logerr("IOException inside getConnections() while getting connections or sending messages\nResetting...");
			if (printStackTrace)
				e.printStackTrace();
			reset = true;
		} catch (ClassNotFoundException e) {
			logerr("ClassNotFoundException inside getConnections() while getting player symbol\nResetting...");
			if (printStackTrace)
				e.printStackTrace();
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
	 * <li>Receives and processes the move
	 * <li>Sends acknowledgement for the move
	 * <li>Resends board
	 * 
	 * If something goes wrong, reset the server.
	 * 
	 * @see GameServer#sendBoard(int) sendBoard()
	 * @see GameServer#reset() reset()
	 */
	private void makeTurn() {
		boolean reset = false;
		log("\nPlayer #%d starts their turn", currentPlayer);
		try {
			log("Sent board:\n%s", gameBoard);
			int move;
			String response;

			// send ok to start
			outputs[currentPlayer].writeObject("Make your move!");

			// send board
			sendBoard(currentPlayer);

			// get, register and respond to move
			move = (int) inputs[currentPlayer].readObject();
			log("Just got '%s'", move);

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
			// TODO replace checkmark with something else as ack, because someone else
			// thinks 3 lines of code are too much :(
			response = (String.format("Move received: [%c, %d]", 65 + move / 10, move % 10 + 1));
			outputs[currentPlayer].writeObject(String.format("%c", '\u2713'));

			log("Move received: '%d', response sent '%s'", move, response);

			// send board again
			sendBoard(currentPlayer);

			currentPlayer = (currentPlayer + 1) % playerCount;

		} catch (SocketException e) {
			logerr("SocketException inside makeTurn()\nResetting...");
			if (printStackTrace)
				e.printStackTrace();
			reset = true;
		} catch (IOException e) {
			logerr("IOException inside makeTurn() while sending/receiving data\nResetting...");
			if (printStackTrace)
				e.printStackTrace();
			reset = true;
		} catch (ClassNotFoundException e) {
			logerr("ClassNotFoundException inside makeTurn() while getting move\nResetting...");
			if (printStackTrace)
				e.printStackTrace();
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
				logerr("IOException in reset() %d; player disconnected\n", i);
			} catch (NullPointerException e) {
				logerr("NullPointerException in reset() %d; player never joined\n", i);
			}
		}
		log("Done resetting");
	}

	/**
	 * Sends the board to the currentPlayer.
	 * <p>
	 * It works by de-constructing the GameBoard here and re-constructing it at the
	 * client using the GameBoard's <code>char[][] array</code> because there is a
	 * problem when sending GameBoard objects.
	 * 
	 * @see GameBoard#getBoard() getBoard()
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
			outputs[currentPlayer].writeObject(newBoard);
		} catch (IOException e) {
			logerr("Error while sending board");
			if (printStackTrace)
				e.printStackTrace();
		}
	}

	/**
	 * Main method. Run to create and run a Game Server.
	 *
	 * @param args not used
	 * @see Server#Server() Server()
	 */
	public static void main(String[] args) {
		GameServer server = new GameServer();
		server.run();
	}
}
