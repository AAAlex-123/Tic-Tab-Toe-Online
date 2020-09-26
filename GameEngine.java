package ttt_online;

import java.awt.Color;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * Client-side application to handle communications with the server
 */
public class GameEngine { // aka client
	private static final int GAME_PORT = 10001;
	private static final int CHAT_PORT = 10002;
	public final static char X = 'X';
	public final static char O = 'O';
	public final static char DASH = '-';
	private static final int INFORMATION = JOptionPane.INFORMATION_MESSAGE;
	private static final int WARNING = JOptionPane.WARNING_MESSAGE;
	private static final int ERROR = JOptionPane.ERROR_MESSAGE;

	private final String address;
	private final boolean printStackTrace;
	private final int servers;

	private final GameUI ui;
	private boolean gameEnded = false;

	private Socket serverSocket, chatSocket;
	private ObjectOutputStream serverOutput, chatOutput;
	private ObjectInputStream serverInput, chatInput;

	private ChatReader chatReader;
	private ChatWriter chatWriter;

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
		log("Started client for %s", servers == 0 ? "chat" : servers == 1 ? "game" : "game and chat");
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
		if (servers == 2 || servers == 0) {
			getChatConnection();
			initChat();
			ui.setEnableChat(true);
		}
		if (servers == 2 || servers == 1) {
			getServerConnection();
			while (!gameEnded) {
				setup(true);
				play();
				setup(false);
			}
		}
	}

	/**
	 * Sets up the UI.
	 * 
	 * @param symbol char, the player's symbol
	 */
	private void setUI() {
		ui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ui.setSize(320, 720);
		ui.setVisible(true);
		ui.setResizable(true);
		ui.setEnableTurn(false);
		ui.setEnableChat(false);
		ui.pushMessage("Please wait to connect to servers");
	}

	/**
	 * Initialises a connection to the Game Server.<br>
	 * Gets its input and output streams.<br>
	 * Exchanges some messages.<br>
	 * <br>
	 * If two or more players have the same symbol, the server allocates them
	 * special unique chess pieces and informs the client here.<br>
	 * <br>
	 * If at any point something goes wrong, show pop-up message then exit.
	 */
	private void getServerConnection() {
		try {
			// get connection
			serverSocket = new Socket(InetAddress.getByName(address), GAME_PORT);
			serverOutput = new ObjectOutputStream(serverSocket.getOutputStream());
			serverInput = new ObjectInputStream(serverSocket.getInputStream());

			// exchange messages and get the index of the connection to server (look below)
			String serverMsg = (String) serverInput.readObject();
			Matcher m = Pattern.compile(".*#(\\d).*").matcher(serverMsg);
			m.find();
			int connectionToServerIndex = Integer.parseInt(m.group(1));

			ui.pushMessage(String.format("\nGame Server said: %s", serverMsg));
			serverOutput.writeObject(ui.getSymbol());
			serverOutput.writeObject(ui.getColor());

			log("Connected to Game Server successfully as player '%c' with number #%d with color (r, g, b): (%d, %d %d)",
					ui.getSymbol(), connectionToServerIndex, ui.getColor().getRed(), ui.getColor().getGreen(),
					ui.getColor().getBlue());

			// wait for ready message and for updated symbols/colors
			ui.pushMessage((String) serverInput.readObject());

			char[] symbols = ((char[]) serverInput.readObject());
			Color[] colors = ((Color[]) serverInput.readObject());

			// using index of the connection to server, figure out if symbol has changed
			// if it has, show popup and update the label at the top
			if (ui.getSymbol() != symbols[connectionToServerIndex]) {
				JOptionPane.showMessageDialog(this.ui,
						"Looks like you selected the same symbol as another player.\nWorry not, because we provided you with an exclusive chess piece as your symbol!",
						"Message", INFORMATION);
				ui.setSymbol(symbols[connectionToServerIndex]);
			}
			ui.setCustomOptions(symbols, colors);

		} catch (IOException e) {
			exit("Couldn't connect to Game Server; please exit\n\nIf you don't know why this happened, please inform the developers",
					"!game! IOException in getServerConnection()\nExiting...\n", WARNING, e, true);
		} catch (ClassNotFoundException e) {
			exit("Something went very wrong; please exit and inform the developers",
					"!game! ClassNotFoundException while reading messages\nExiting...\n", ERROR, e, true);
		}
	}

	/**
	 * Sets up the UI and exchanges messages before and after the player makes their
	 * move.
	 * 
	 * @param starting boolean, whether or not it is the start or the end of the
	 *                 player's turn
	 */
	private void setup(boolean starting) {
		try {
			if (starting) {
				ui.setEnableTurn(false);
				ui.pushMessage("");
				log("\nStarting turn");
			}

			// get ready message
			String response = ((String) serverInput.readObject());

			// if message is "won" or "resigned" display some messages and stop game thread
			if (response.matches("Player.*resigned") || response.matches("Player.*won!")) {
				if (response.charAt(8) == ui.getSymbol())
					ui.pushMessage(String.format("%c", '\u2713'));
				updateBoard();
				ui.setEnableTurn(false);
				gameEnded = true;

				// trust the spaghetti, it just makes the correct message without 4 if
				// statements
				String msg = String.format("\n\n%s\n\nGame ended; %s",
						response.charAt(8) == ui.getSymbol()
								? response.matches("Player.*resigned") ? "You resigned :(" : "You won :)"
								: response.matches("Player.*resigned") ? response + " :)" : response + " :(",
						servers == 1 ? "please exit" : "you can still chat, or exit to play another game");
				exit(msg, "!game! game ended", INFORMATION, null, false);
				if (servers == 0)
					System.exit(0);
				return;
			}

			log("Got response: '%s'", response);
			ui.pushMessage(response);

			// update board
			updateBoard();

			// enable/disable buttons/text
			ui.setEnableTurn(starting);

			log("End %s setup", starting ? "starting" : "ending");
		} catch (EOFException e) {
			exit("Another player unexpectedly disconnected; please exit\n\nIf you don't know why this happened, please inform the developers",
					"!game! EOFException in setup()", INFORMATION, e, true);
		} catch (IOException e) {
			exit("Connection to Game Server lost; please exit\n\nIf you don't know why this happened, please inform the developers",
					"!game! IOException in setup()", WARNING, e, true);
		} catch (ClassNotFoundException e) {
			exit("Something went very wrong; please exit and inform the developers",
					"!game! ClassNotFoundException in setup()", ERROR, e, true);
		}
	}

	/**
	 * Gets the player's move, and sends it to the server.
	 */
	private void play() {
		// get the move in the worst possible way (:
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
				if (printStackTrace)
					e.printStackTrace();
			}
		}

		if (move != -2)
			ui.pushMessage(String.format("You played %c%s", 65 + move / 10, move % 10 + 1), false);

		// send the move
		try {
			serverOutput.writeObject(move);
		} catch (IOException e) {
			exit("Connection to Game Server lost; please exit\n\nIf you don't know why this happened, please inform the developers",
					"!game! IOException in play()", WARNING, e, true);
		}
		log("Got and sent move: [%d, %d]", move / 10, move % 10);
	}

	/**
	 * Initialises a connection to the Chat Server.<br>
	 * Gets its input and output streams.<br>
	 * Exchanges some messages.<br>
	 * <br>
	 * If at any point something goes wrong, show pop-up message then exit.
	 */
	private void getChatConnection() {
		try {
			// get connection
			chatSocket = new Socket(InetAddress.getByName(address), CHAT_PORT);
			chatOutput = new ObjectOutputStream(chatSocket.getOutputStream());
			chatInput = new ObjectInputStream(chatSocket.getInputStream());

			// exchange messages
			ui.pushMessage(String.format("\nChat Server said: %s", ((String) chatInput.readObject())));

			log("Connected to Chat Server successfully as player '%c'", ui.getSymbol());

		} catch (IOException e) {
			exit("Couldn't connect to Chat Server; please exit\n\nIf you don't know why this happened, please inform the developers",
					"!chat! IOException in getChatConnection()\nExiting...\n", WARNING, e, true);
		} catch (ClassNotFoundException e) {
			exit("Something went very wrong; please exit and inform the developers",
					"!chat! ClassNotFoundException in chatReader.run()", ERROR, e, true);
		}
	}

	/**
	 * Initialises connection to the Chat Server and starts two threads; one for
	 * reading and one for writing to chat
	 */
	private void initChat() {
		chatReader = new ChatReader();
		chatWriter = new ChatWriter();
		chatReader.start();
		chatWriter.start();
	}

	/**
	 * Pushes any message it receives from Chat Server to the UI
	 */
	private class ChatReader extends Thread {
		public void run() {
			boolean err = false;
			while (!err) {
				try {
					// wait to receive a chat message and push it to the log JTextArea
					String msg = (String) chatInput.readObject();
					log("!chat! received message: '%s'", msg);
					ui.pushMessage(msg);
				} catch (IOException e) {
					exit("Connection to Chat Server lost; please exit\n\nIf you don't know why this happened, please inform the developers",
							"!chat! IOException in chatReader.run()", WARNING, e, true);
				} catch (ClassNotFoundException e) {
					exit("Something went very wrong; please exit and inform the developers",
							"!chat! ClassNotFoundException in chatReader.run()", ERROR, e, true);
				}
			}
		}
	}

	/**
	 * When there is a message to send, it sends it
	 */
	private class ChatWriter extends Thread {
		public void run() {
			boolean err = false;
			while (!err) {
				String chatText = ui.getChatText();
				// if no chat has been sent, try again in 1 second
				if (chatText.equals("")) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						if (printStackTrace)
							e.printStackTrace();
					}
					// if a chat is sent, send it to the Chat Server
				} else {
					try {
						String msg = String.format("%c: %s", ui.getSymbol(), chatText);
						log("!chat! sent message:     '%s'", msg);
						chatOutput.writeObject(msg);
					} catch (IOException e) {
						exit("Connection to Chat Server lost; please exit\n\nIf you don't know why this happened, please inform the developers",
								"!chat! IOException in chatWriter.run()", WARNING, e, true);
					}
				}
			}
		}
	}

	/**
	 * Exits the program when an Exception occurs.<br>
	 * Logs the error and informs the player with a pop-up which, when closed, exits
	 * the program.
	 * 
	 * @param error_msg String, the message to show the user
	 * @param log_msg   String, the message to log to the console
	 * @param type      int, ERROR, WARNING or INFORMATION, the type of the message
	 * @param e         Exception, the exception that occurred
	 * @param error     boolean, whether or not an error has occured and the
	 *                  application has to exit
	 */
	private void exit(String error_msg, String log_msg, int type, Exception e, boolean error) {
		if (error)
			logerr(log_msg);
		else
			log(log_msg);

		try {
			if (printStackTrace)
				e.printStackTrace();
		} catch (NullPointerException exc) {
			;
		}

		JOptionPane.showMessageDialog(this.ui, error_msg, error ? "Error" : "Message", type);
		if (error)
			System.exit(0);
	}

	/**
	 * Updates the board on the UI.
	 * <p>
	 * It works by de-constructing the GameBoard at the server and re-constructing
	 * it here using the <code>char[][] array</code> because there is a problem when
	 * sending GameBoard objects.
	 * 
	 * @see GameBoard#GameBoard(char[][]) GameBoard(char[][])
	 * 
	 * @throws ClassNotFoundException idk when it's thrown
	 * @throws IOException            thrown when server disconnects
	 * @throws EOFException           thrown when server closes connection
	 */
	private void updateBoard() throws ClassNotFoundException, IOException, EOFException {
		localGameBoardConstructor = (char[][]) serverInput.readObject();
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
	 * @param args args[2] is used to determine <code>servers</code> field, 'c' for
	 *             chat, 't' for game, otherwise both
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
			logerr("Warning: No <servers> argument provided;\nInitialised to 'ct';");
			servers = 2;
		}

		gameEngine = new GameEngine(address, printStackTrace, servers);
		gameEngine.run();
	}
}
