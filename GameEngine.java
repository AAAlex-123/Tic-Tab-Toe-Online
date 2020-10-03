package ttt_online;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;

/**
 * Client-side application to handle communications with the server
 */
public class GameEngine implements Logging { // aka client

	// ports of the Game and Chat Servers
	// Constants
	private static final int GAME_PORT = 10001;
	private static final int CHAT_PORT = 10002;

	public final static char DASH = '-';

	private static final int INFORMATION = JOptionPane.INFORMATION_MESSAGE;
	private static final int WARNING = JOptionPane.WARNING_MESSAGE;
	private static final int ERROR = JOptionPane.ERROR_MESSAGE;

	private static final int CHAT = 0;
	private static final int GAME = 1;
	private static final int CHAT_GAME = 2;

	// variables initialised from UI
	private String address;
	private boolean printStackTrace;
	// used to determine UI and graphics size
	private static final int HEIGHT_MULTIPLIER = Toolkit.getDefaultToolkit().getScreenSize().height < 750 ? 1 : 2;

	private int serverCode;
	private Color color = Color.BLACK;
	private char character;

	private boolean argumentsPassed = false;

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
	 * Constructs the GameEngine and sets up the UI.
	 * 
	 * @see GameEngine#getClientOptions() getClientOptions
	 */
	public GameEngine() {
		getClientOptions();
		while (!argumentsPassed) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		log("Started client for %s", serverCode == 0 ? "chat" : serverCode == 1 ? "game" : "game and chat");
		
		this.ui = new GameUI(color, character, GameEngine.HEIGHT_MULTIPLIER);
		setupUI();
	}
	

	/**
	 * Main method. Run to create and run a client
	 */
	public static void main(String[] args) {
		GameEngine gameEngine = new GameEngine();
		gameEngine.run();
	}
	
	/**
	 * Runs the GameEngine initializing connections to Game and Chat servers
	 * according to the <code>serverCode</code>field
	 */
	private void run() {
		if (serverCode == CHAT_GAME || serverCode == CHAT) {
			if (getChatConnection() == 0) {
				initChat();
				ui.setEnableChat(true);
			}
		}
		if (serverCode == CHAT_GAME || serverCode == GAME) {
			if (getServerConnection() == 1)
				return;
			while (!gameEnded) {
				if (setup(true) == 1)
					break;
				if (play() == 1)
					break;
				if (setup(false) == 1)
					break;
			}
		}
		ui.setEnableTurn(false);
	}

	/**
	 * Sets up the UI.
	 */
	private void setupUI() {
		ui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ui.setSize(500, 300 * HEIGHT_MULTIPLIER + 300);
		ui.setVisible(true);
		ui.setResizable(true);
		ui.setEnableTurn(false);
		ui.setEnableChat(false);
		ui.pushMessage("Please wait to connect to servers");
	}

	/**
	 * Initializes a connection to the Game Server.<br>
	 * Gets its input and output streams.<br>
	 * Exchanges some messages.<br>
	 * <br>
	 * If two or more players have the same symbol, the server allocates them
	 * special unique chess pieces and informs the client here.<br>
	 * <br>
	 * If at any point something goes wrong, show pop-up message then exit.
	 * 
	 * @see GameEngine#exit(String, String, int, Exception, boolean) exit()
	 * @return int, 0 or 1, indicating success or fail
	 */
	private int getServerConnection() {
		try {
			// get connection
			serverSocket = new Socket(InetAddress.getByName(address), GAME_PORT);
			serverOutput = new ObjectOutputStream(serverSocket.getOutputStream());
			serverInput = new ObjectInputStream(serverSocket.getInputStream());

			// exchange messages and get the index of the connection to server (look below)
			serverOutput.writeObject(ui.getSymbol());
			serverOutput.writeObject(ui.getColor());

			String serverMsg = (String) serverInput.readObject();
			ui.pushMessage("\nGame Server said: %s", serverMsg);

			Matcher m = Pattern.compile(".*#(\\d).*").matcher(serverMsg);
			m.find();
			int connectionToServerIndex = Integer.parseInt(m.group(1));

			log("Connected to Game Server successfully as player '%c' with number #%d with color (r, g, b): (%d, %d %d)",
					ui.getSymbol(), connectionToServerIndex, ui.getColor().getRed(), ui.getColor().getGreen(),
					ui.getColor().getBlue());

			// wait for ready message and for updated symbols/colors
			ui.pushMessage("\nGame Server said: %s", (String) serverInput.readObject());

			char[] symbols = ((char[]) serverInput.readObject());
			Color[] colors = ((Color[]) serverInput.readObject());

			// using index of the connection to server, figure out if symbol has changed
			// if it has, show popup and update the label at the top
			if (ui.getSymbol() != symbols[connectionToServerIndex]) {
				JOptionPane.showMessageDialog(this.ui,
						"Looks like you selected the same symbol as another player connected to the Game Server.\nWorry not, because we provided you with an exclusive chess piece as your symbol!",
						"Message", INFORMATION);
				ui.setSymbol(symbols[connectionToServerIndex]);
			}
			ui.setCustomOptions(symbols, colors);

		} catch (IOException e) {
			exit("Couldn't connect to Game Server; if you're connected to chat server you may still chat.\n\nIf you don't know why this happened, please inform the developers",
					"!game! IOException in getServerConnection()\n", WARNING, e, serverCode == GAME, "Connection Error");
			return 1;
		} catch (ClassNotFoundException e) {
			exit("Something went very wrong; please exit and inform the developers",
					"!game! ClassNotFoundException in getServerConnection()\n", ERROR, e, true, "Very Serious Error");
			return 1;
		}
		return 0;
	}

	/**
	 * Sets up the UI and exchanges messages before and after the player makes their
	 * move.<br>
	 * If at any point something goes wrong, show pop-up message then exit.
	 * 
	 * @param starting boolean, whether or not it is the start or the end of the
	 *                 player's turn
	 * @see GameEngine#exit(String, String, int, Exception, boolean) exit()
	 * @return int, 0 or 1, indicating success or fail
	 */
	private int setup(boolean starting) {
		try {
			if (starting) {
				ui.pushMessage("");
				ui.focusMove();
				log("\nStarting turn");
			} else {
				// disable buttons/text
				if (serverCode == CHAT_GAME) {
					ui.focusChat();
				}
				ui.setEnableTurn(false);
			}

			// get ready message
			String response = ((String) serverInput.readObject());

			// if message is "won" or "resigned" or "tie" display some messages and stop
			// game thread
			if (response.matches("Player.*resigned") || response.matches("Player.*won!")) {
				if (response.charAt(8) == ui.getSymbol())
					ui.pushMessage("%c", '\u2713');
				updateBoard();
				ui.setEnableTurn(false);
				gameEnded = true;

				// trust the spaghetti, it just makes the correct message without 4 if
				// statements
				String msg = String.format("\n\n%s\n\nGame ended; %s",
						response.charAt(8) == ui.getSymbol()
								? response.matches("Player.*resigned") ? "You resigned :(" : "You won :)"
								: response.matches("Player.*resigned") ? response + " :)" : response + " :(",
						serverCode == GAME ? "please exit" : "you can still chat, or exit to play another game");
				exit(msg, "!game! game ended", INFORMATION, null, serverCode == GAME, "Game Over");
				return 1;
			} else if (response.equals("It's a tie!")) {
				updateBoard();
				ui.setEnableTurn(false);
				gameEnded = true;
				String msg = String.format("\n\n%s\n\nGame ended; %s", "It's a tie!",
						serverCode == GAME ? "please exit" : "you can still chat, or exit to play another game");
				exit(msg, "!game! game ended", INFORMATION, null, serverCode == GAME, "Game Over");
				return 1;
			}

			log("Got response: '%s'", response);
			ui.pushMessage(response);

			// update board
			updateBoard();

			// enable buttons/text
			ui.setEnableTurn(starting);

			log("End %s setup", starting ? "starting" : "ending");
		} catch (EOFException e) {
			exit("Another player unexpectedly disconnected; if you're connected to chat server you may still chat.\n\nIf you don't know why this happened, please inform the developers",
					"!game! EOFException in setup()", INFORMATION, e, serverCode == GAME, "Player Disconnected");
			return 1;
		} catch (IOException e) {
			exit("Connection to Game Server lost; if you're connected to chat server you may still chat.\n\nIf you don't know why this happened, please inform the developers",
					"!game! IOException in setup()", WARNING, e, serverCode == GAME, "Connection Error");
			return 1;
		} catch (ClassNotFoundException e) {
			exit("Something went very wrong; please exit and inform the developers",
					"!game! ClassNotFoundException in setup()", ERROR, e, true, "Very Serious Error");
			return 1;
		}
		return 0;
	}

	/**
	 * Gets the player's move, and sends it to the server. <br>
	 * If at any point something goes wrong, show pop-up message then exit.
	 * 
	 * @see GameEngine#exit(String, String, int, Exception, boolean) exit()
	 * @return int, 0 or 1, indicating success or fail
	 */
	private int play() {
		
		// get the move in the worst possible way (:
		int move = -1;
		while (move == -1) {
			move = ui.getAnswer();
			if (move != -1 && move != -2 && !localGameBoard.isValid(move)) {
				ui.pushMessage("You can't play %c%s!", 65 + move / 10, move % 10 + 1);
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
			ui.pushMessage("You played %c%s", 65 + move / 10, move % 10 + 1, false);

		// send the move
		try {
			serverOutput.writeObject(move);
		} catch (IOException e) {
			exit("Connection to Game Server lost; if you're connected to chat server you may still chat.\n\nIf you don't know why this happened, please inform the developers",
					"!game! IOException in play()", WARNING, e, serverCode == GAME, "Connection Error");
			return 1;
		}
		log("Got and sent move: [%d, %d]", move / 10, move % 10);
		
		return 0;
	}

	/**
	 * Initializes a connection to the Chat Server.<br>
	 * Gets its input and output streams.<br>
	 * Exchanges some messages.<br>
	 * <br>
	 * If at any point something goes wrong, show pop-up message then exit.
	 * 
	 * @see GameEngine#exit(String, String, int, Exception, boolean) exit()
	 * @return int, 0 or 1, indicating success or fail
	 */
	private int getChatConnection() {
		try {
			// get connection
			chatSocket = new Socket(InetAddress.getByName(address), CHAT_PORT);
			chatOutput = new ObjectOutputStream(chatSocket.getOutputStream());
			chatOutput.flush();

			// send symbol and wait for ack
			chatOutput.writeObject(ui.getSymbol());

			chatInput = new ObjectInputStream(chatSocket.getInputStream());
			String response = (String) chatInput.readObject();

			Matcher m = Pattern.compile(".*'(.{0,2})'.*").matcher(response);
			m.find();
			char newSymbol = m.group(1).charAt(0);
			log("Symbol after duplicate check: '%c' ('%d')", newSymbol, (int) newSymbol);

			if (newSymbol != ui.getSymbol()) {
				JOptionPane.showMessageDialog(this.ui,
						"Looks like you selected the same symbol as another player connected to the Chat Server.\nWorry not, because we provided you with an exclusive chess piece as your symbol!",
						"Message", INFORMATION);
				ui.setSymbol(newSymbol);
			}

			ui.pushMessage("\nChat Server said: %s", response);
			
			ui.focusChat();

			log("Connected to Chat Server successfully as player '%c'", ui.getSymbol());

		} catch (IOException e) {
			exit("Couldn't connect to Chat Server; if you're connected to game server you may still play.\n\nIf you don't know why this happened, please inform the developers",
					"!chat! IOException in getChatConnection()\nExiting...\n", WARNING, e, serverCode == CHAT, "Connection Error");
			return 1;
		} catch (ClassNotFoundException e) {
			exit("Something went very wrong; please exit and inform the developers.",
					"!chat! ClassNotFoundException in chatReader.run()", ERROR, e, true, "Very Serious Error");
			return 1;
		}
		return 0;
	}

	/**
	 * Initializes connection to the Chat Server and starts two threads; one for
	 * reading and one for writing to chat.
	 */
	private void initChat() {
		chatReader = new ChatReader();
		chatWriter = new ChatWriter();
		chatReader.run();
		chatWriter.run();
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
	 * @param terminate boolean, whether or not an error has occured and the
	 *                  application has to exit
	 */
	private void exit(String error_msg, String log_msg, int type, Exception e, boolean terminate, String title) {
		if (terminate)
			logerr(log_msg);
		else
			log(log_msg);

		try {
			if (printStackTrace)
				e.printStackTrace();
		} catch (NullPointerException exc) {
			;
		}

		JOptionPane.showMessageDialog(this.ui, error_msg, title, type);
		if (terminate)
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
	 * @throws ClassNotFoundException
	 * @throws IOException            thrown when server disconnects
	 * @throws EOFException           thrown when server closes connection
	 */
	private void updateBoard() throws ClassNotFoundException, IOException, EOFException {
		localGameBoardConstructor = (char[][]) serverInput.readObject();
		localGameBoard = new GameBoard(localGameBoardConstructor);
		ui.setScreen(localGameBoard);
	}

	/**
	 * Creates a UI to get the GameEngine options.
	 */
	private void getClientOptions() {

		JFrame optWind = new JFrame("Select Server Options");

		JPanel optPanel = new JPanel();
		optPanel.setLayout(new BoxLayout(optPanel, BoxLayout.PAGE_AXIS));
		optWind.setVisible(true);
		optWind.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		optWind.setSize(new Dimension(500, 500));
		optWind.setResizable(true);

		// upper panel: address + character
		JPanel upperPanel = new JPanel();
		upperPanel.setLayout(new FlowLayout());

		JPanel addressPanel = new JPanel();
		addressPanel.setLayout(new BoxLayout(addressPanel, BoxLayout.Y_AXIS));
		JLabel addressLabel = new JLabel("Server IP:");
		addressLabel.setPreferredSize(new Dimension(100, 50));
		JTextField addressField = new JTextField("127.0.0.1");
		addressPanel.add(addressLabel);
		addressPanel.add(addressField);

		JPanel listPanel = new JPanel();
		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
		String[] chars = { "X", "O", "!", "#", "$", "%", "*", "+", "A", "B", "C", "D", "E", "F", "G", "H", "I", "P",
				"Q", "R", "S", "T", "U", "V", "W", "<", "?", "~" };
		JList<String> charList = new JList<String>(chars); // JList forces me to use strings here
		charList.setBackground(Color.BLUE);
		charList.setSelectedIndex(0);
		JScrollPane scrollList = new JScrollPane(charList);
		JLabel listLabel = new JLabel("Choose your character");
		listPanel.add(listLabel);
		listPanel.add(scrollList);

		upperPanel.add(addressPanel);
		upperPanel.add(listPanel);
		optPanel.add(Box.createVerticalGlue());

		// lower panel: printStackTrace + chat/game + color/submit buttons
		JPanel lowerPanel = new JPanel();
		lowerPanel.setLayout(new BoxLayout(lowerPanel, BoxLayout.Y_AXIS));
		JCheckBox printButton = new JCheckBox("I want to receive crash reports on my command line");

		ButtonGroup bg = new ButtonGroup();
		JRadioButton gameChatButton = new JRadioButton("I want to play the game with chat enabled");
		JRadioButton gameOnlyButton = new JRadioButton("I want to play the game with chat disabled");
		JRadioButton chatOnlyButton = new JRadioButton("I just want to chat");
		bg.add(gameChatButton);
		bg.add(gameOnlyButton);
		bg.add(chatOnlyButton);
		gameChatButton.setSelected(true);
		lowerPanel.add(printButton);
		lowerPanel.add(Box.createVerticalGlue());
		lowerPanel.add(gameChatButton);
		lowerPanel.add(gameOnlyButton);
		lowerPanel.add(chatOnlyButton);
		lowerPanel.add(Box.createVerticalGlue());

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		JButton submitButton = new JButton("Submit");
		JButton colorButton = new JButton("Select color");
		buttonPanel.add(submitButton);
		buttonPanel.add(colorButton);

		lowerPanel.add(buttonPanel);
		optPanel.add(upperPanel);
		optPanel.add(lowerPanel);

		optWind.add(optPanel);
		optWind.revalidate(); // yes, this IS necessary

		// event listeners
		colorButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				color = JColorChooser.showDialog(optWind, "Choose a color", Color.BLACK);
				if (color == null)
					color = Color.BLACK;
			}
		});

		submitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				character = charList.getSelectedValue().charAt(0);
				printStackTrace = printButton.isSelected();
				address = GameEngine.Utility.myStrip(addressField.getText(), ' ', '\t');
				if (gameChatButton.isSelected())
					serverCode = CHAT_GAME;
				else if (gameOnlyButton.isSelected())
					serverCode = GAME;
				else
					serverCode = CHAT;
				argumentsPassed = true;
				optWind.setVisible(false);
			}
		});
	}
	
	/**
	 * Pushes any message it receives from Chat Server to the UI.
	 */
	private class ChatReader implements Runnable {
		public void run() {
			boolean err = false;
			while (!err) {
				try {
					// wait to receive a chat message and push it to the log JTextArea
					String msg = (String) chatInput.readObject();
					log("!chat! received message: '%s'", msg);
					ui.pushMessage(msg);
				} catch (IOException e) {
					exit("Connection to Chat Server lost; if you're connected to game server you may still play.\n\nIf you don't know why this happened, please inform the developers",
							"!chat! IOException in chatReader.run()", WARNING, e, serverCode == CHAT, "Connection Error");
					return;
				} catch (ClassNotFoundException e) {
					exit("Something went very wrong; please exit and inform the developers.",
							"!chat! ClassNotFoundException in chatReader.run()", ERROR, e, true, "Very Serious Error");
				}
			}
		}
	}

	/**
	 * Private inner class that, whenever there is chat text to send, sends it to
	 * the ChatServer's input Stream.
	 * <p>
	 * When an Exception occurs, a pop-up is displayed, the ChatServer's Streams are
	 * closed and this Thread terminates execution,
	 *
	 * @see GameEngine#exit(String, String, int, Exception, boolean) exit()
	 */
	private class ChatWriter implements Runnable {

		/**
		 * Runs this thread; whenever there is chat text to send, sends it to the
		 * ChatServer's input Stream. When an Exception occurs, a pop-up is displayed,
		 * the ChatServer's Streams are closed and this Thread terminates execution,
		 *
		 * @see GameEngine#exit(String, String, int, Exception, boolean) exit()
		 */
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
						exit("Connection to Chat Server lost; if you're connected to game server you may still play.\n\nIf you don't know why this happened, please inform the developers",
								"!chat! IOException in chatWriter.run()", WARNING, e, serverCode == CHAT, "Connection Error");
						return;
					}
				}
			}
		}
	}
	
	/**
	 * A class with a set of static utility methods to be used throughout the
	 * project.
	 */
	abstract static class Utility {

		
		/**
		 * Returns a string, stripped by <code>chars</code>.<br>
		 * Similar to python's <code>str.strip(string)</code>.
		 * Needed to make the program compatible with Java8+
		 * 
		 * @param string String, the string to strip
		 * @param chars  char[], the characters to strip from the string
		 * @return String, the stripped string
		 */
		public static String myStrip(String string, char... chars) {
			if (string.equals(""))
				return "";
			return string.substring(firstIndexOfNonChars(string, chars), lastIndexofNonChars(string, chars) + 1);
		}

		/**
		 * Returns true or false indicating if <code>item</code> is an item of
		 * <code>array</code>.<br>
		 * Same as <code>ArrayList.contains()</code>
		 * Needed to make the program compatible with Java8+
		 * 
		 * @param array char[], the array of items
		 * @param item  char, the item to check if it is in the array
		 * @return boolean, whether or not <code>item</code> is in <code>array</code>
		 * 
		 * @see ArrayList#contains(Object)
		 */
		public static boolean myContains(char[] array, char item) {
			for (int i = 0; i < array.length; i++)
				if (array[i] == item)
					return true;
			return false;
		}

		private static int firstIndexOfNonChars(String string, char... chars) {
			for (int i = 0; i < string.length(); i++)
				if (!myContains(chars, string.charAt(i)))
					return i;
			return -1;
		}

		private static int lastIndexofNonChars(String string, char... chars) {
			for (int i = string.length() - 1; i > -1; i--)
				if (!myContains(chars, string.charAt(i)))
					return i;
			return -1;
		}
	}


}
