package ttt_online;

import java.awt.Color;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

import javax.swing.JFrame;

/**
 * Client-side application to handle communications with the server
 */
public class GameEngine { // aka client
	private static final int GAME_PORT = 10001;
	private static final int CHAT_PORT = 10002;
	public final static char X = 'X';
	public final static char O = 'O';
	public final static char DASH = '-';

	private final String address;
	private final boolean printStackTrace;
	private final int servers;

	private boolean pushErrorMessages = true;

	private final GameUI ui;

	private Socket serverSocket, chatSocket;
	private ObjectOutputStream serverOutput, chatOutput;
	private ObjectInputStream serverInput, chatInput;

	private chatReader chr;
	private chatWriter chw;

	private GameBoard localGameBoard;
	private char[][] localGameBoardConstructor;

	/**
	 * Constructor with parameters <code>address</code> and
	 * <code>printStackTrace</code> Also sets up the UI
	 * 
	 * @param address         String, the IP address of the server
	 * @param printStackTrace boolean, whether or not to print stack trace when
	 *                        Exceptions occur
	 */
	public GameEngine(String address, boolean printStackTrace, int servers) {
		this.address = address;
		this.printStackTrace = printStackTrace;
		this.servers = servers;
		this.ui = new GameUI();
		setUI();
	}

	/**
	 * Runs the GameEngine initialising connections to Game and Chat servers
	 * according to the <code>servers</code>field
	 */
	private void run() {
		if (servers == 2 || servers == 0) getChatConnection();
		if (servers == 2 || servers == 0) initChat();
		if (servers == 2 || servers == 1) getServerConnection();
		if (servers == 2 || servers == 1) {
			while (true) {
				setup(true);
				play();
				setup(false);
			}
		}
	}

	/**
	 * Sets up the UI
	 * 
	 * @param symbol char, the player's symbol
	 */
	private void setUI() {
		ui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ui.setSize(320, 720);
		ui.setVisible(true);
		ui.setResizable(true);
	}

	/**
	 * Initialises a connection to the Game Server.<br>
	 * Gets its input and output streams.<br>
	 * Exchanges some messages.<br>
	 * <br>
	 * If at any point something goes wrong, exit :)
	 */
	@SuppressWarnings("unchecked")
	private void getServerConnection() {
		try {
			// get connection
			serverSocket = new Socket(InetAddress.getByName(address), GAME_PORT);
			serverOutput = new ObjectOutputStream(serverSocket.getOutputStream());
			serverInput = new ObjectInputStream(serverSocket.getInputStream());

			// exchange messages
			ui.pushMessage(String.format("server said: %s", ((Packet<String>) serverInput.readObject()).value));
			serverOutput.writeObject(new Packet<Character>(Server.DATA, ui.getSymbol()));
			serverOutput.writeObject(new Packet<Color>(Server.DATA, ui.getColor()));

			// wait for ready message and for updated symbols/colors
			ui.pushMessage(((Packet<String>) serverInput.readObject()).value);

			char[] symbols = ((Packet<char[]>) serverInput.readObject()).value;
			Color[] colors = ((Packet<Color[]>) serverInput.readObject()).value;
			ui.setCustomOptions(symbols, colors);

			log("Connected to server successfully as player '%c' with color %s", ui.getSymbol(), ui.getColor());

		} catch (IOException e) {
			logerr("IOException while getting connections or sending messages\nExiting...\n");
			if (printStackTrace) e.printStackTrace();
			if (pushErrorMessages) ui.pushMessage(
						"Couldn't connect to server; please exit\n\nIf you don't know why this happened, please inform the developers");
			while (true) {;}
		} catch (ClassNotFoundException e) {
			logerr("ClassNotFoundException while reading messages\nExiting...\n");
			if (printStackTrace) e.printStackTrace();
			if (pushErrorMessages) ui.pushMessage(
						"Couldn't connect to server; please exit\n\nIf you don't know why this happened, please inform the developers");
			while (true) {;}
		}
	}

	/**
	 * Sets up the UI and exchanges messages before and after the player makes their
	 * move.
	 * 
	 * @param starting boolean, whether or not it is the start or the end of the
	 *                 player's turn
	 */
	@SuppressWarnings("unchecked")
	private void setup(boolean starting) {
		try {
			if (starting) {
				ui.setEnableTurn(false);
				ui.pushMessage("");
				log("\nStarting turn");
			}

			// get ready message
			String response = ((Packet<String>) serverInput.readObject()).value;

			// if message is resignation (can only happen when starting==false) do something
			if (response.matches("Player.*resigned")) {
				ui.pushMessage(String.format("\n%s\n\nGame ended; please exit",
						response.charAt(8) == ui.getSymbol() ? "You resigned :(" : response + " :)"));
				updateBoard();
				ui.setEnableTurn(false);
				pushErrorMessages = false;
				while (true) {;}
			} else if (response.matches("Player.*won!")) {
				ui.pushMessage(String.format("\n%s\n\nGame ended; please exit",
						response.charAt(8) == ui.getSymbol() ? "You won :)" : response + " :("));
				updateBoard();
				ui.setEnableTurn(false);
				pushErrorMessages = false;
				while (true) {;}
			}

			log("Got response: '%s'", response);
			ui.pushMessage(response);

			// update board
			updateBoard();

			// enable/disable buttons/text
			ui.setEnableTurn(starting);

			log("End %s setup", starting ? "starting" : "ending");
		} catch (EOFException e) {
			logerr("EOFException while getting server message");
			if (printStackTrace) e.printStackTrace();
			if (pushErrorMessages) ui.pushMessage(
						"Another player unexpectedly disconnected; please exit\n\nIf you don't know why this happened, please inform the developers");
			while (true) {;}
		} catch (IOException e) {
			logerr("IOException while getting server message");
			if (printStackTrace) e.printStackTrace();
			if (pushErrorMessages) ui.pushMessage(
						"Connection to server lost; please exit\n\nIf you don't know why this happened, please inform the developers");
			while (true) {;}
		} catch (ClassNotFoundException e) {
			logerr("ClassNotFoundException while getting server message");
			if (printStackTrace) e.printStackTrace();
			if (pushErrorMessages) ui.pushMessage("Something went wrong; please exit and inform the developers");
			while (true) {;}
		}
	}

	/**
	 * Gets the player's move, and sends it to the server.
	 */
	private void play() {
		// get the move in the worst possible way
		int move = -1;
		while (move == -1) {
			move = ui.getAnswer();
			if (move != -1 && move != -2 && !localGameBoard.isValid(move)) {
				ui.pushMessage(String.format("You can't play %c%s!", 65 + move / 10, move % 10 + 1));
				log("Tried to play %c%s", 65 + move / 10, move % 10 + 1);
				move = -1;
			}
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				if (printStackTrace) e.printStackTrace();
			}
		}

		ui.pushMessage(String.format("You played %c%s", 65 + move / 10, move % 10 + 1));

		// send the move
		try {
			serverOutput.writeObject(new Packet<Integer>(Server.MOVE, move));
		} catch (IOException e) {
			logerr("IOException while getting server/player message");
			if (printStackTrace) e.printStackTrace();
			if (pushErrorMessages) ui.pushMessage(
						"Connection to server lost; please exit\n\nIf you don't know why this happened, please inform the developers");
			while (true) {;}
		}
		log("Got and sent move: [%d, %d]", move / 10, move % 10);
	}

	/**
	 * Initialises a connection to the Chat Server.<br>
	 * Gets its input and output streams.<br>
	 * Exchanges some messages.<br>
	 * <br>
	 * If at any point something goes wrong, exit :)
	 */
	@SuppressWarnings("unchecked")
	private void getChatConnection() {
		try {
			// get connection
			chatSocket = new Socket(InetAddress.getByName(address), CHAT_PORT);
			chatOutput = new ObjectOutputStream(chatSocket.getOutputStream());
			chatInput = new ObjectInputStream(chatSocket.getInputStream());

			// exchange messages
			ui.pushMessage(String.format("chat server said: %s", ((Packet<String>) chatInput.readObject()).value));

			log("Connected to chat server successfully as player '%c'", ui.getSymbol());

		} catch (IOException e) {
			logerr("!chat! IOException while getting connections or sending messages\nExiting...\n");
			if (printStackTrace) e.printStackTrace();
			if (pushErrorMessages) ui.pushMessage(
						"!chat! Couldn't connect to server; please exit\n\nIf you don't know why this happened, please inform the developers");
			while (true) {;}
		} catch (ClassNotFoundException e) {
			logerr("!chat! ClassNotFoundException while reading messages\nExiting...\n");
			if (printStackTrace)
				e.printStackTrace();
			if (pushErrorMessages) ui.pushMessage(
						"!chat! Couldn't connect to server; please exit\n\nIf you don't know why this happened, please inform the developers");
			while (true) {;}
		}
	}

	/**
	 * Initialises connection to the Chat Server and starts two threads; one for
	 * reading and one for writing to chat
	 */
	private void initChat() {
		chr = new chatReader();
		chw = new chatWriter();
		chr.start();
		chw.start();
		log("!chat! initialised chat successfully");
	}

	/**
	 * Pushes any message it receives from Chat Server to the UI
	 */
	private class chatReader extends Thread {
		@SuppressWarnings("unchecked")
		public void run() {
			boolean err = false;
			while (!err) {
				try {
					Packet<String> p = (Packet<String>) chatInput.readObject();
					log("!chat! received message: '%s'", p.value);
					ui.pushMessage(p.value);
				} catch (IOException e) {
					logerr("!chat! IOException while getting server message");
					if (printStackTrace) e.printStackTrace();
					if (pushErrorMessages) ui.pushMessage(
								"!chat! Connection to server lost; please exit\n\nIf you don't know why this happened, please inform the developers");
					err = true;
				} catch (ClassNotFoundException e) {
					logerr("!chat! ClassNotFoundException while getting server message");
					if (printStackTrace) e.printStackTrace();
					if (pushErrorMessages) ui.pushMessage("!chat! Something went wrong; please exit and inform the developers");
					err = true;
				}
			}
		}
	}

	/**
	 * When there is a message to send, it sends it
	 */
	private class chatWriter extends Thread {
		public void run() {
			boolean err = false;
			while (!err) {
				String chatText = ui.getChatText();
				if (chatText.equals("")) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						if (printStackTrace) e.printStackTrace();
					}
				} else {
					try {
						String msg = String.format("%c: %s", ui.getSymbol(), chatText);
						log("!chat! sent message %s", msg);
						chatOutput.writeObject(new Packet<String>(Server.CHAT, msg));
					} catch (IOException e) {
						logerr("!chat! IOException while getting server message");
						if (printStackTrace) e.printStackTrace();
						if (pushErrorMessages) ui.pushMessage(
									"!chat! Connection to server lost; please exit\n\nIf you don't know why this happened, please inform the developers");
						err = true;
					}
				}
			}
		}
	}

	/**
	 * Updates the board on the UI
	 * 
	 * @throws ClassNotFoundException idk when it's thrown
	 * @throws IOException            thrown when server disconnects (?)
	 * @throws EOFException           thrown when server disconnects (?)
	 */
	@SuppressWarnings("unchecked")
	private void updateBoard() throws ClassNotFoundException, IOException, EOFException {
		localGameBoardConstructor = ((Packet<char[][]>) serverInput.readObject()).value;
		localGameBoard = new GameBoard(localGameBoardConstructor);
		ui.setScreen(localGameBoard);
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
	 * Main method. Run to create and run a client
	 * 
	 * @param args args[0] is used for the player's symbol
	 * @param args args[1] is used to determine <code>printStackTrace</code> field,
	 *             '1' for true, other for false
	 * @param args args[2] is used to determine <code>servers</code> field,
	 *             'c' for chat, 't' for game, otherwise both
	 */
	public static void main(String[] args) {
		log("Getting symbol and color options");
		GameEngine gameEngine;
		String address;
		boolean printStackTrace;
		int servers;

		try {
			address = args[0];
		} catch (ArrayIndexOutOfBoundsException e) {
			logerr("Warning: No <address> argument provided;\nInitialised to '127.0.0.1';");
			address = "127.0.0.1";
		}

		try {
			printStackTrace = args[1].equals("1") ? true : false;
		} catch (ArrayIndexOutOfBoundsException e) {
			logerr("Warning: No <printStackTrace> argument provided;\nInitialised to 'false';");
			printStackTrace = false;
		}

		try {
			switch (args[2]) {
			case "c":
				servers = 0;
				break;
			case "t":
				servers = 1;
				break;
			default:
				servers = 2;
				break;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			logerr("Warning: No <servers> argument provided;\nInitialised to '2';");
			servers = 2;
		}

		gameEngine = new GameEngine(address, printStackTrace, servers);
		gameEngine.run();
	}
}
